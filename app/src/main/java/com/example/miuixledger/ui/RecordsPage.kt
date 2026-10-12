package com.example.miuixledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.miuixledger.data.ExpenseRecord
import com.example.miuixledger.data.RecordType
import com.kyant.backdrop.Backdrop
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayFmt = DateTimeFormatter.ofPattern("M月d日 EEEE")
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

/** 明细页：玻璃卡片流水，点按删除 */
@Composable
fun RecordsPage(
    backdrop: Backdrop,
    records: List<ExpenseRecord>,
    onDelete: (ExpenseRecord) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<ExpenseRecord?>(null) }

    if (records.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            BasicText(
                text = "还没有记录\n点右下角 ＋ 记一笔",
                style = textStyle(15f, FontWeight.Normal, AppColors.Sub).copy(textAlign = TextAlign.Center),
            )
        }
    } else {
        val grouped = remember(records) { groupByDay(records) }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp),
        ) {
            grouped.forEach { (day, dayRecords) ->
                item(key = "h_$day") {
                    DayHeader(day, dayRecords)
                }
                items(dayRecords, key = { it.id }) { record ->
                    GlassCard(
                        backdrop = backdrop,
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
                                BasicText(record.category, style = textStyle(17f, FontWeight.Medium))
                                if (record.note.isNotBlank()) {
                                    BasicText(record.note, style = textStyle(13f, color = AppColors.Sub))
                                }
                                BasicText(
                                    timeFmt.format(
                                        Instant.ofEpochMilli(record.time).atZone(ZoneId.systemDefault())
                                    ),
                                    style = textStyle(11f, color = AppColors.Sub),
                                )
                            }
                            BasicText(
                                text = (if (record.type == RecordType.INCOME) "+" else "-") +
                                    formatMoney(record.amount),
                                style = textStyle(
                                    18f,
                                    FontWeight.SemiBold,
                                    if (record.type == RecordType.INCOME) AppColors.Income
                                    else AppColors.Expense,
                                ),
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicText(day.format(dayFmt), style = textStyle(13f, color = AppColors.Sub))
        BasicText(
            text = buildString {
                if (exp > 0) append("支出 ${formatMoney(exp)}")
                if (inc > 0) {
                    if (isNotEmpty()) append("  ")
                    append("收入 ${formatMoney(inc)}")
                }
            },
            style = textStyle(12f, color = AppColors.Sub),
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

/** 确认弹窗（自绘遮罩 + 玻璃卡片） */
@Composable
fun ConfirmDialog(
    title: String,
    summary: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Scrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.97f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { /* 拦截点击，避免穿透到遮罩 */ }
                .padding(22.dp),
        ) {
            BasicText(title, style = textStyle(19f, FontWeight.SemiBold))
            Spacer(Modifier.height(8.dp))
            BasicText(summary, style = textStyle(14f, color = AppColors.Sub))
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppButton("取消", Modifier.weight(1f)) { onDismiss() }
                AppButton(confirmText, Modifier.weight(1f), primary = true) { onConfirm() }
            }
        }
    }
}
