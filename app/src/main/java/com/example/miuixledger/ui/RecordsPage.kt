package com.example.miuixledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ExpenseRed = Color(0xFFE94634)
private val IncomeBlue = Color(0xFF3482FF)
private val SubText = Color(0xFF8C93B0)
private val ContentDark = Color(0xFF2A3142)

private val dayFmt = DateTimeFormatter.ofPattern("M月d日 EEEE")
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

internal fun formatMoney(amount: Double): String = "%,.2f".format(amount)

/** 明细页：玻璃卡片流水列表，点按删除 */
@Composable
fun RecordsPage(records: List<ExpenseRecord>, onDelete: (ExpenseRecord) -> Unit) {
    var pendingDelete by remember { mutableStateOf<ExpenseRecord?>(null) }

    if (records.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "还没有记录\n点右下角 ＋ 记一笔",
                style = TextStyle(fontSize = 14.sp, color = SubText, lineHeight = 22.sp),
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 130.dp),
        ) {
            val grouped = groupByDay(records)
            grouped.forEach { (day, dayRecords) ->
                item(key = "h_${day}_${dayRecords.first().id}") { DayHeader(day, dayRecords) }
                items(dayRecords.size, key = { dayRecords[it].id }) { i ->
                    val record = dayRecords[i]
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clickable { pendingDelete = record },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(record.category, style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = ContentDark))
                                if (record.note.isNotBlank()) {
                                    Text(record.note, style = TextStyle(fontSize = 13.sp, color = SubText))
                                }
                                Text(
                                    timeFmt.format(Instant.ofEpochMilli(record.time).atZone(ZoneId.systemDefault())),
                                    style = TextStyle(fontSize = 11.sp, color = SubText),
                                )
                            }
                            Text(
                                (if (record.type == RecordType.INCOME) "+" else "-") + formatMoney(record.amount),
                                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                                color = if (record.type == RecordType.INCOME) IncomeBlue else ExpenseRed,
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
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(day.format(dayFmt), style = TextStyle(fontSize = 13.sp, color = SubText))
        Text(
            buildString {
                if (exp > 0) append("支出 ${formatMoney(exp)}")
                if (inc > 0) { if (isNotEmpty()) append("  "); append("收入 ${formatMoney(inc)}") }
            },
            style = TextStyle(fontSize = 12.sp, color = SubText),
        )
    }
}

internal fun groupByDay(records: List<ExpenseRecord>): List<Pair<LocalDate, List<ExpenseRecord>>> {
    val zone = ZoneId.systemDefault()
    return records
        .groupBy { Instant.ofEpochMilli(it.time).atZone(zone).toLocalDate() }
        .toSortedMap(compareByDescending { it })
        .map { it.key to it.value }
}

/** 简单确认弹窗（系统 Dialog + 玻璃卡片风格） */
@Composable
fun ConfirmDialog(
    title: String,
    summary: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            insidePadding = 22.dp,
        ) {
            Text(title, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = ContentDark))
            Spacer(Modifier.height(8.dp))
            Text(summary, style = TextStyle(fontSize = 14.sp, color = SubText, lineHeight = 20.sp))
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogButton("取消", Modifier.weight(1f), onDismiss)
                DialogButton(confirmText, Modifier.weight(1f), onConfirm, primary = true)
            }
        }
    }
}

@Composable
internal fun DialogButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    primary: Boolean = false,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (primary) IncomeBlue else Color(0x338C93B0))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = TextStyle(fontSize = 16.sp, color = if (primary) Color.White else ContentDark))
    }
}
