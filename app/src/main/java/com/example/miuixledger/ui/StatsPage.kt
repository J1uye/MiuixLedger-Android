package com.example.miuixledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.miuixledger.data.ExpenseRecord
import com.example.miuixledger.data.RecordType
import com.kyant.backdrop.Backdrop
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 统计页：本月收支 + 分类占比 */
@Composable
fun StatsPage(backdrop: Backdrop, records: List<ExpenseRecord>) {
    val zone = ZoneId.systemDefault()
    val now = LocalDate.now(zone)
    val month = records.filter {
        Instant.ofEpochMilli(it.time).atZone(zone).toLocalDate().let { d ->
            d.year == now.year && d.month == now.month
        }
    }
    val exp = month.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount }
    val inc = month.filter { it.type == RecordType.INCOME }.sumOf { it.amount }
    val byCat = month.filter { it.type == RecordType.EXPENSE }
        .groupBy { it.category }
        .map { (c, l) -> c to l.sumOf { it.amount } }
        .sortedByDescending { it.second }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp),
    ) {
        item {
            GlassCard(
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                insidePadding = 20.dp,
            ) {
                BasicText("${now.year}年${now.monthValue}月", style = textStyle(13f, color = AppColors.Sub))
                Spacer(Modifier.height(10.dp))
                BasicText(
                    "支出 ¥" + formatMoney(exp),
                    style = textStyle(34f, FontWeight.Bold, AppColors.Expense),
                )
                Spacer(Modifier.height(6.dp))
                BasicText("收入 ¥" + formatMoney(inc), style = textStyle(15f, color = AppColors.Income))
            }
        }
        item {
            BasicText(
                "分类支出占比",
                style = textStyle(14f, FontWeight.SemiBold, AppColors.Sub),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
        if (byCat.isEmpty()) {
            item {
                BasicText(
                    "本月暂无支出记录",
                    style = textStyle(14f, color = AppColors.Sub),
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        } else {
            items(byCat.size) { i ->
                val (cat, amount) = byCat[i]
                val ratio = if (exp > 0) (amount / exp).toFloat() else 0f
                GlassCard(
                    backdrop = backdrop,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BasicText(cat, style = textStyle(16f))
                        BasicText(
                            "¥${formatMoney(amount)} · ${(ratio * 100).toInt()}%",
                            style = textStyle(13f, color = AppColors.Sub),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0x4D8C93B0))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(ratio.coerceIn(0.02f, 1f))
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(AppColors.Expense)
                        )
                    }
                }
            }
        }
    }
}

/** 设置页：数据管理 + 玻璃说明 */
@Composable
fun SettingsPage(
    backdrop: Backdrop,
    recordCount: Int,
    onClearAll: () -> Unit,
) {
    var showClear by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        BasicText(
            "数据",
            style = textStyle(14f, FontWeight.SemiBold, AppColors.Sub),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            insidePadding = 20.dp,
        ) {
            BasicText(
                "当前共 $recordCount 条记录，保存在应用私有目录，卸载后清除。",
                style = textStyle(13f, color = AppColors.Sub),
            )
            Spacer(Modifier.height(16.dp))
            AppButton("清空全部记录", Modifier.fillMaxWidth()) { showClear = true }
        }

        Spacer(Modifier.height(20.dp))
        BasicText(
            "关于液态玻璃",
            style = textStyle(14f, FontWeight.SemiBold, AppColors.Sub),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            insidePadding = 20.dp,
        ) {
            BasicText(
                "底栏与记一笔按钮使用 Kyant0/AndroidLiquidGlass（Backdrop）管线：\n" +
                    "vibrancy() 提饱和 → blur() 磨砂 → lens() 边缘折射。\n\n" +
                    "完整折射效果需要 Android 13+（AGSL RuntimeShader），" +
                    "低版本会自动降级为普通模糊。",
                style = textStyle(13f, color = AppColors.Sub),
            )
        }
    }

    if (showClear) {
        ConfirmDialog(
            title = "清空数据",
            summary = "将删除全部 $recordCount 条记录，且无法恢复。确定继续吗？",
            confirmText = "清空",
            onDismiss = { showClear = false },
            onConfirm = {
                showClear = false
                onClearAll()
            },
        )
    }
}
