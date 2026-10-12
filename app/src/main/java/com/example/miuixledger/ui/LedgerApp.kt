package com.example.miuixledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.miuixledger.data.LedgerStore
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

/**
 * 记账 App 主界面 —— Kyant0/AndroidLiquidGlass（Backdrop）液态玻璃版。
 *
 * 结构对齐官方 catalog 的 BackdropDemoScaffold：
 *  1. 背景层用 Modifier.layerBackdrop(backdrop) 采集为「玻璃后面的世界」
 *  2. 玻璃元素（底栏 / FAB / 卡片）用 Modifier.drawBackdrop(backdrop, ...) 折射它
 *  3. 折射参数：lens(refractionHeight, refractionAmount) + blur() + vibrancy()
 */
@Composable
fun LedgerApp() {
    val context = LocalContext.current
    val store = remember { LedgerStore(context) }
    val scope = rememberCoroutineScope()

    val records by store.records.collectAsState()
    var page by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }

    val backdrop = rememberLayerBackdrop()

    Box(Modifier.fillMaxSize()) {
        // ① 彩色渐变背景：玻璃折射的内容来源
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFD8E6FF),
                            Color(0xFFEFE4FF),
                            Color(0xFFFFE4EF),
                        )
                    )
                )
        )

        Column(Modifier.fillMaxSize()) {
            // 顶栏
            Column(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 8.dp)
            ) {
                BasicText(
                    text = listOf("明细", "统计", "设置")[page],
                    style = textStyle(28f, FontWeight.SemiBold),
                )
            }

            // 页面内容
            Box(Modifier.weight(1f)) {
                when (page) {
                    0 -> RecordsPage(
                        backdrop = backdrop,
                        records = records,
                        onDelete = { scope.launch { store.remove(it) } },
                    )
                    1 -> StatsPage(backdrop = backdrop, records = records)
                    2 -> SettingsPage(
                        backdrop = backdrop,
                        recordCount = records.size,
                        onClearAll = { scope.launch { store.clearAll() } },
                    )
                }
            }
        }

        // ② 液态玻璃 FAB
        LiquidFab(
            backdrop = backdrop,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 116.dp),
            onClick = { showAdd = true },
        )

        // ② 液态玻璃底栏
        LiquidTabBar(
            backdrop = backdrop,
            selected = page,
            onSelect = { page = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        )
    }

    if (showAdd) {
        AddRecordSheet(
            backdrop = backdrop,
            onDismiss = { showAdd = false },
            onConfirm = { record ->
                showAdd = false
                scope.launch { store.add(record) }
            },
        )
    }
}
