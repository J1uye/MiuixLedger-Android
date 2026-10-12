package com.example.miuixledger.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.miuixledger.data.ExpenseRecord
import com.example.miuixledger.data.RecordType
import com.kyant.backdrop.Backdrop
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val dayFmt = DateTimeFormatter.ofPattern("M月d日 EEEE")
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

/** 明细页：玻璃卡片流水，点按删除 */
@Composable
fun RecordsPage(
    backdrop: Backdrop,
    settings: GlassSettings,
    records: List<ExpenseRecord>,
    onDelete: (ExpenseRecord) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<ExpenseRecord?>(null) }

    if (records.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "还没有记录\n点右下角 ＋ 记一笔",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        val grouped = remember(records) { groupByDay(records) }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp),
        ) {
            grouped.forEach { (day, dayRecords) ->
                item(key = "h_$day") { DayHeader(day, dayRecords) }
                items(dayRecords, key = { it.id }) { record ->
                    GlassCard(
                        backdrop = backdrop,
                        settings = settings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .clickable { pendingDelete = record },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = record.category,
                                    style = MiuixTheme.textStyles.headline1,
                                )
                                if (record.note.isNotBlank()) {
                                    Text(
                                        text = record.note,
                                        style = MiuixTheme.textStyles.body2,
                                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                                    )
                                }
                                Text(
                                    text = timeFmt.format(
                                        Instant.ofEpochMilli(record.time)
                                            .atZone(ZoneId.systemDefault())
                                    ),
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                                )
                            }
                            Text(
                                text = (if (record.type == RecordType.INCOME) "+" else "-") +
                                    formatMoney(record.amount),
                                style = MiuixTheme.textStyles.headline1,
                                color = if (record.type == RecordType.INCOME) {
                                    MiuixTheme.colorScheme.primary
                                } else {
                                    MiuixTheme.colorScheme.error
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { target ->
        ConfirmDialog(
            title = "删除记录",
            summary = "确定删除「${target.category} ${formatMoney(target.amount)}」这笔记录吗？",
            confirmText = "删除",
            onDismiss = { pendingDelete = null },
            onConfirm = {
                pendingDelete = null
                onDelete(target)
            },
        )
    }
}

@Composable
private fun DayHeader(day: LocalDate, records: List<ExpenseRecord>) {
    val exp = records.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount }
    val inc = records.filter { it.type == RecordType.INCOME }.sumOf { it.amount }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = day.format(dayFmt),
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
        )
        Text(
            text = buildString {
                if (exp > 0) append("支出 ${formatMoney(exp)}")
                if (inc > 0) {
                    if (isNotEmpty()) append("  ")
                    append("收入 ${formatMoney(inc)}")
                }
            },
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
        )
    }
}

/** 按自然日分组（记录已按时间倒序） */
fun groupByDay(records: List<ExpenseRecord>): List<Pair<LocalDate, List<ExpenseRecord>>> {
    val zone = ZoneId.systemDefault()
    return records
        .groupBy { Instant.ofEpochMilli(it.time).atZone(zone).toLocalDate() }
        .toSortedMap(compareByDescending { it })
        .map { it.key to it.value }
}

/**
 * 确认弹窗 —— Miuix OverlayDialog。
 * ⚠️ Overlay 系列必须在 Miuix Scaffold 内调用（LedgerApp 已用 Scaffold 包裹）。
 */
@Composable
fun ConfirmDialog(
    title: String,
    summary: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    OverlayDialog(
        title = title,
        summary = summary,
        show = true,
        onDismissRequest = onDismiss,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(
                text = "取消",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = confirmText,
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}
