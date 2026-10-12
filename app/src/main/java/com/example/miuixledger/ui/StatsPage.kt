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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.miuixledger.data.ExpenseRecord
import com.example.miuixledger.data.RecordType
import com.kyant.backdrop.Backdrop
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 统计页：本月收支 + 分类占比 */
@Composable
fun StatsPage(
    backdrop: Backdrop,
    settings: GlassSettings,
    records: List<ExpenseRecord>,
) {
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
        contentPadding = PaddingValues(top = 8.dp, bottom = 150.dp),
    ) {
        item {
            GlassCard(
                backdrop = backdrop,
                settings = settings,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                insidePadding = 20.dp,
            ) {
                Text(
                    text = "${now.year}年${now.monthValue}月",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "支出 ¥" + formatMoney(exp),
                    style = MiuixTheme.textStyles.title1,
                    color = MiuixTheme.colorScheme.error,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "收入 ¥" + formatMoney(inc),
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.primary,
                )
            }
        }

        item { SmallTitle(text = "分类支出占比", modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }

        if (byCat.isEmpty()) {
            item {
                Text(
                    text = "本月暂无支出记录",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        } else {
            items(byCat.size) { i ->
                val (cat, amount) = byCat[i]
                val ratio = if (exp > 0) (amount / exp).toFloat() else 0f
                GlassCard(
                    backdrop = backdrop,
                    settings = settings,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = cat, style = MiuixTheme.textStyles.body1)
                        Text(
                            text = "¥${formatMoney(amount)} · ${(ratio * 100).toInt()}%",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MiuixTheme.colorScheme.secondaryContainer)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(ratio.coerceIn(0.02f, 1f))
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MiuixTheme.colorScheme.error)
                        )
                    }
                }
            }
        }
    }
}

/** 设置页：玻璃参数实时调节（Miuix Slider/Switch）+ 数据管理 */
@Composable
fun SettingsPage(
    backdrop: Backdrop,
    settings: GlassSettings,
    recordCount: Int,
    onClearAll: () -> Unit,
) {
    var showClear by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 150.dp),
    ) {
        item { SmallTitle(text = "液态玻璃参数（实时生效）", modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }

        item {
            GlassCard(
                backdrop = backdrop,
                settings = settings,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                insidePadding = 18.dp,
            ) {
                ParamSlider(
                    label = "折射强度 refractionAmount",
                    value = settings.refractionAmount,
                    range = 0f..160f,
                    display = "${settings.refractionAmount.toInt()} dp",
                    onChange = { settings.refractionAmount = it },
                )
                ParamSlider(
                    label = "折射边缘 refractionHeight",
                    value = settings.refractionHeight,
                    range = 2f..60f,
                    display = "${settings.refractionHeight.toInt()} dp",
                    onChange = { settings.refractionHeight = it },
                )
                ParamSlider(
                    label = "磨砂 blur",
                    value = settings.blurRadius,
                    range = 0f..30f,
                    display = "${settings.blurRadius.toInt()} dp",
                    onChange = { settings.blurRadius = it },
                )
                ParamSlider(
                    label = "玻璃白膜 surfaceTint",
                    value = settings.surfaceTint,
                    range = 0f..0.6f,
                    display = "${(settings.surfaceTint * 100).toInt()}%",
                    onChange = { settings.surfaceTint = it },
                )
                Spacer(Modifier.height(8.dp))
                SwitchRow(
                    label = "vibrancy() 提饱和",
                    checked = settings.vibrancyOn,
                    onChange = { settings.vibrancyOn = it },
                )
                SwitchRow(
                    label = "显示可拖动放大镜",
                    checked = settings.showMagnifier,
                    onChange = { settings.showMagnifier = it },
                )
            }
        }

        item { SmallTitle(text = "数据", modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }

        item {
            GlassCard(
                backdrop = backdrop,
                settings = settings,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                insidePadding = 18.dp,
            ) {
                Text(
                    text = "当前共 $recordCount 条记录，保存在应用私有目录，卸载后清除。",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showClear = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "清空全部记录", style = MiuixTheme.textStyles.button)
                }
            }
        }

        item { SmallTitle(text = "关于", modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }

        item {
            GlassCard(
                backdrop = backdrop,
                settings = settings,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                insidePadding = 18.dp,
            ) {
                Text(
                    text = "界面组件来自 Miuix（HyperOS 风格），玻璃材质来自 " +
                        "Kyant0/AndroidLiquidGlass（Backdrop）：\n" +
                        "vibrancy() 提饱和 → blur() 磨砂 → lens() 边缘折射。\n\n" +
                        "完整折射需要 Android 13+（AGSL RuntimeShader）。",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            }
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

/** 参数滑杆（Miuix Slider + 数值显示） */
@Composable
private fun ParamSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MiuixTheme.textStyles.body2)
            Text(
                text = display,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 开关行（Miuix Switch） */
@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MiuixTheme.textStyles.body2)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
