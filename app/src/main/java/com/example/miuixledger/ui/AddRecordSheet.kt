package com.example.miuixledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miuixledger.data.Categories
import com.example.miuixledger.data.ExpenseRecord
import com.example.miuixledger.data.RecordType
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.EmptyBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangularShape

/**
 * 玻璃卡片：内容列表里的卡片使用静态降级（EmptyBackdrop + blur），
 * 因为列表卡片数量多、背后主要是渐变背景，直接半透明白 + 模糊即可。
 * 底栏和 FAB 才用真正的 backdrop lens 折射（突出重点，性能也好）。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    insidePadding: androidx.compose.ui.unit.Dp = 14.dp,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .drawBackdrop(
                backdrop = EmptyBackdrop,
                shape = { RoundedRectangularShape(16.dp.toPx()) },
                effects = {
                    blur(14f)
                },
                highlight = { Highlight.Default },
                shadow = { Shadow(alpha = 0.10f) },
            )
            .padding(insidePadding),
        content = content,
    )
}

/** 记一笔底部弹窗：类型/分类分段选择 + 金额/备注 */
@Composable
fun AddRecordSheet(
    onDismiss: () -> Unit,
    onConfirm: (ExpenseRecord) -> Unit,
) {
    var type by remember { mutableStateOf(RecordType.EXPENSE) }
    var category by remember { mutableStateOf(Categories.expense.first()) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .drawBackdrop(
                    backdrop = EmptyBackdrop,
                    shape = { RoundedRectangularShape(24.dp.toPx()) },
                    effects = { blur(16f) },
                    highlight = { Highlight.Default },
                    shadow = { Shadow(alpha = 0.18f) },
                )
                .padding(22.dp),
        ) {
            androidx.compose.material3.Text(
                "记一笔",
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2A3142)),
            )
            Spacer(Modifier.padding(top = 14.dp))

            // 类型分段选择
            SegmentedRow(
                options = RecordType.entries.map { it.label },
                selectedIndex = if (type == RecordType.EXPENSE) 0 else 1,
                onSelect = {
                    type = if (it == 0) RecordType.EXPENSE else RecordType.INCOME
                    category = Categories.forType(type).first()
                },
            )
            Spacer(Modifier.padding(top = 10.dp))

            // 分类分段选择（按 4 个一行分行）
            val cats = Categories.forType(type)
            cats.chunked(4).forEach { row ->
                SegmentedRow(
                    options = row,
                    selectedIndex = row.indexOf(category).coerceAtLeast(0),
                    onSelect = { category = row[it] },
                )
                Spacer(Modifier.padding(top = 6.dp))
            }

            Spacer(Modifier.padding(top = 8.dp))
            androidx.compose.material3.OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { androidx.compose.material3.Text("金额（元）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.padding(top = 8.dp))
            androidx.compose.material3.OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { androidx.compose.material3.Text("备注（可选）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.padding(top = 16.dp))

            Row {
                DialogButton(
                    "取消",
                    Modifier.fillMaxWidth(0.5f).padding(end = 5.dp),
                    onDismiss,
                )
                DialogButton(
                    "保存",
                    Modifier.fillMaxWidth().padding(start = 5.dp),
                    {
                        val amount = amountText.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            onConfirm(
                                ExpenseRecord(
                                    id = System.currentTimeMillis(),
                                    amount = amount,
                                    category = category,
                                    note = note.trim(),
                                    time = System.currentTimeMillis(),
                                    type = type,
                                )
                            )
                        }
                    },
                    primary = true,
                )
            }
        }
    }
}

/** 简单分段选择行（透镜滑动 + 弹性动画） */
@Composable
fun SegmentedRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, opt ->
            val selected = i == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selected) Color(0xFF3482FF) else Color(0x338C93B0)
                    )
                    .clickable { onSelect(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Text(
                    opt,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = if (selected) Color.White else Color(0xFF2A3142),
                    ),
                )
            }
        }
    }
}
