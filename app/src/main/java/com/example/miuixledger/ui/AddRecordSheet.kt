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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
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
import com.example.miuixledger.data.Categories
import com.example.miuixledger.data.ExpenseRecord
import com.example.miuixledger.data.RecordType
import com.kyant.backdrop.Backdrop

/** 记一笔弹窗：类型/分类分段选择 + 金额/备注 */
@Composable
fun AddRecordSheet(
    @Suppress("UNUSED_PARAMETER") backdrop: Backdrop,
    onDismiss: () -> Unit,
    onConfirm: (ExpenseRecord) -> Unit,
) {
    var type by remember { mutableStateOf(RecordType.EXPENSE) }
    var category by remember { mutableStateOf(Categories.expense.first()) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

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
                .padding(horizontal = 22.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.97f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { /* 拦截点击，避免穿透 */ }
                .padding(22.dp),
        ) {
            BasicText("记一笔", style = textStyle(20f, FontWeight.SemiBold))
            Spacer(Modifier.height(16.dp))

            BasicText("类型", style = textStyle(12f, color = AppColors.Sub))
            Spacer(Modifier.height(6.dp))
            SegmentedRow(
                options = RecordType.entries.map { it.label },
                selectedIndex = if (type == RecordType.EXPENSE) 0 else 1,
                onSelect = {
                    type = if (it == 0) RecordType.EXPENSE else RecordType.INCOME
                    category = Categories.forType(type).first()
                },
            )

            Spacer(Modifier.height(14.dp))
            BasicText("分类", style = textStyle(12f, color = AppColors.Sub))
            Spacer(Modifier.height(6.dp))
            val cats = Categories.forType(type)
            cats.chunked(4).forEach { row ->
                SegmentedRow(
                    options = row,
                    selectedIndex = row.indexOf(category).coerceAtLeast(0),
                    onSelect = { category = row[it] },
                )
                Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(8.dp))
            InputField(
                label = "金额（元）",
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
            )
            Spacer(Modifier.height(10.dp))
            InputField(
                label = "备注（可选）",
                value = note,
                onValueChange = { note = it },
            )

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppButton("取消", Modifier.weight(1f)) { onDismiss() }
                AppButton("保存", Modifier.weight(1f), primary = true) {
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
                }
            }
        }
    }
}

/** 简易输入框 */
@Composable
private fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        BasicText(label, style = textStyle(12f, color = AppColors.Sub))
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(Color(0x1F8C93B0))
                .padding(horizontal = 14.dp, vertical = 13.dp),
        ) {
            if (value.isEmpty()) {
                BasicText("请输入", style = textStyle(16f, color = AppColors.Sub.copy(alpha = 0.6f)))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = textStyle(16f),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
