package com.example.miuixledger.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 全局配色（简洁版，替代 Miuix 主题） */
object AppColors {
    val Primary = Color(0xFF3482FF)
    val Expense = Color(0xFFE94634)
    val Income = Color(0xFF1E9E5A)
    val Content = Color(0xFF1F2430)
    val Sub = Color(0xFF7A8194)
    val GlassBorder = Color(0x99FFFFFF)
    val Scrim = Color(0x66000000)
}

fun textStyle(
    size: Float,
    weight: FontWeight = FontWeight.Normal,
    color: Color = AppColors.Content,
) = TextStyle(fontSize = size.sp, fontWeight = weight, color = color)

/** 金额格式化：两位小数 + 千分位 */
fun formatMoney(amount: Double): String = "%,.2f".format(amount)
