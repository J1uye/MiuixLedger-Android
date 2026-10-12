package com.example.miuixledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miuixledger.data.ExpenseRecord
import com.example.miuixledger.data.RecordType
import java.time.Instant
import java.time.ZoneId

private val SubText = Color(0xFF8C93B0)
private val ContentDark = Color(0xFF2A3142)
private val ExpenseRed = Color(0xFFE94634)
private val IncomeBlue = Color(0xFF3482FF)

/** 统计页：本月收支 + 分类占比（玻璃卡片） */
@Composable
fun StatsPage(records: List<ExpenseRecord>) {
    val zone = ZoneId.systemDefault()
    val now = java.time.LocalDate.now(zone)
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 130.dp),
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), insidePadding = 18.dp) {
                Text("${now.year}年${now.monthValue}月", style = TextStyle(fontSize = 13.sp, color = SubText))
                Spacer(Modifier.height(8.dp))
                Text(
                    "支出 ¥" + formatMoney(exp),
                    style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, color = ExpenseRed),
                )
                Spacer(Modifier.height(4.dp))
                Text("收入 ¥" + formatMoney(inc), style = TextStyle(fontSize = 15.sp, color = IncomeBlue))
            }
        }
        item {
            Text(
                "分类支出占比",
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SubText),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
        if (byCat.isEmpty()) {
            item {
                Text(
                    "本月暂无支出记录",
                    style = TextStyle(fontSize = 14.sp, color = SubText),
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        } else {
            items(byCat.size) { i ->
                val (cat, amount) = byCat[i]
                val ratio = if (exp > 0) (amount / exp).toFloat() else 0f
                GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(cat, style = TextStyle(fontSize = 16.sp, color = ContentDark))
                        Text(
                            "¥${formatMoney(amount)} · ${(ratio * 100).toInt()}%",
                            style = TextStyle(fontSize = 13.sp, color = SubText),
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
                                .background(ExpenseRed)
                        )
                    }
                }
            }
        }
    }
}

/** 设置页：清空数据 */
@Composable
fun SettingsPage(recordCount: Int, onClearAll: () -> Unit) {
    var showClear by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Text(
            "数据",
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SubText),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
        GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), insidePadding = 18.dp) {
            Text(
                "当前共 $recordCount 条记录，保存在应用私有目录。",
                style = TextStyle(fontSize = 13.sp, color = SubText),
            )
            Spacer(Modifier.height(14.dp))
            DialogButton("清空全部记录", Modifier.fillMaxWidth(), { showClear = true })
        }
        Spacer(Modifier.height(16.dp))
        GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), insidePadding = 18.dp) {
            Text(
                "🔮 液态玻璃说明",
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ContentDark),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "底栏与记一笔按钮使用 Kyant0/AndroidLiquidGlass（Backdrop）管线：" +
                    "vibrancy 提饱和、blur 磨砂、lens 边缘折射（Android 13+ AGSL 着色器）。",
                style = TextStyle(fontSize = 13.sp, color = SubText, lineHeight = 19.sp),
            )
        }
    }

    if (showClear) {
        ConfirmDialog(
            title = "清空数据",
            summary = "将删除全部 $recordCount 条记录，且无法恢复。确定继续吗？",
            confirmText = "清空",
            onDismiss = { showClear = false },
            onConfirm = { showClear = false; onClearAll() },
        )
    }
}
