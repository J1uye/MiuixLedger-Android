package com.example.miuixledger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.miuixledger.data.Categories
import com.example.miuixledger.data.ExpenseRecord
import com.example.miuixledger.data.RecordType
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 记一笔弹窗 —— Miuix OverlayDialog + TextField。
 * ⚠️ Overlay 系列必须在 Miuix Scaffold 内（LedgerApp 已包裹）。
 */
@Composable
fun AddRecordSheet(
    onDismiss: () -> Unit,
    onConfirm: (ExpenseRecord) -> Unit,
) {
    var type by remember { mutableStateOf(RecordType.EXPENSE) }
    var category by remember { mutableStateOf(Categories.expense.first()) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val cats = Categories.forType(type)
    val amount = amountText.toDoubleOrNull()

    OverlayDialog(
        title = "记一笔",
        show = true,
        onDismissRequest = onDismiss,
    ) {
        // 类型
        SmallTitle(text = "类型")
        SegmentedRow(
            options = RecordType.entries.map { it.label },
            selectedIndex = if (type == RecordType.EXPENSE) 0 else 1,
            onSelect = {
                type = if (it == 0) RecordType.EXPENSE else RecordType.INCOME
                category = Categories.forType(type).first()
            },
        )

        Spacer(Modifier.height(12.dp))

        // 分类
        SmallTitle(text = "分类")
        cats.chunked(4).forEach { row ->
            SegmentedRow(
                options = row,
                selectedIndex = row.indexOf(category).coerceAtLeast(0),
                onSelect = { category = row[it] },
            )
            Spacer(Modifier.height(6.dp))
        }

        Spacer(Modifier.height(12.dp))

        // 金额
        TextField(
            value = amountText,
            onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
            label = "金额（元）",
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(10.dp))

        // 备注
        TextField(
            value = note,
            onValueChange = { note = it },
            label = "备注（可选）",
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(
                text = "取消",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = "保存",
                enabled = amount != null && amount > 0.0,
                onClick = {
                    onConfirm(
                        ExpenseRecord(
                            id = System.currentTimeMillis(),
                            amount = amount ?: 0.0,
                            category = category,
                            note = note.trim(),
                            time = System.currentTimeMillis(),
                            type = type,
                        )
                    )
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = "提示：液态玻璃参数可在「设置」页调节",
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}
