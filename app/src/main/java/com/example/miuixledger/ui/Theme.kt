package com.example.miuixledger.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 应用主题入口（Miuix 库）：
 * 所有界面必须包裹在 MiuixTheme 中，由 ThemeController 管理模式，
 * 不自行判断系统深色。
 */
@Composable
fun LedgerTheme(content: @Composable () -> Unit) {
    val controller = remember { ThemeController(ColorSchemeMode.System) }
    MiuixTheme(controller = controller, content = content)
}

/**
 * 液态玻璃参数（设置页可实时调节，用来观察 Backdrop 管线效果）。
 * 单位是 dp，传给库时必须 .toPx() 换算——库的参数单位是像素。
 */
@Stable
class GlassSettings {
    /** lens() 的折射带宽度 */
    var refractionHeight by mutableFloatStateOf(24f)

    /** lens() 的折射强度 */
    var refractionAmount by mutableFloatStateOf(48f)

    /** blur() 的磨砂半径 */
    var blurRadius by mutableFloatStateOf(8f)

    /** vibrancy() 提饱和开关 */
    var vibrancyOn by mutableStateOf(true)

    /** 玻璃表面白膜透明度（0 = 最通透） */
    var surfaceTint by mutableFloatStateOf(0.10f)

    /** 是否显示可拖动的放大镜镜片 */
    var showMagnifier by mutableStateOf(true)
}

/** 金额格式化：两位小数 + 千分位 */
fun formatMoney(amount: Double): String = "%,.2f".format(amount)
