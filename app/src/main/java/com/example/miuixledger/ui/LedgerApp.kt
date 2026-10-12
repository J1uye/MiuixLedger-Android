package com.example.miuixledger.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.miuixledger.data.LedgerStore
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 记账 App 主界面。
 *
 * 两个库各司其职：
 *  - Miuix：Scaffold / TopAppBar / Text / Card / Slider / Switch / TextField 等组件与主题
 *  - AndroidLiquidGlass(Backdrop)：底栏、FAB、卡片、放大镜的玻璃折射材质
 *
 * 玻璃结构对齐官方 catalog：
 *  1. 背景层（GlassBackground）用 layerBackdrop(backdrop) 采集为「玻璃后面的世界」
 *  2. 玻璃元素用 drawBackdrop(backdrop, ...) 折射它
 *  3. blur()/lens() 参数一律 dp.toPx() 换算（库的单位是像素）
 */
@Composable
fun LedgerApp() {
    val context = LocalContext.current
    val store = remember { LedgerStore(context) }
    val scope = rememberCoroutineScope()
    val settings = remember { GlassSettings() }

    val records by store.records.collectAsState()
    var page by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }

    val backdrop = rememberLayerBackdrop()
    val titles = listOf("明细", "统计", "设置")

    LedgerTheme {
        Box(Modifier.fillMaxSize()) {
            // ① 高对比背景：玻璃折射的内容来源
            GlassBackground(
                modifier = Modifier.fillMaxSize().layerBackdrop(backdrop)
            )

            // ② Miuix Scaffold 提供页面骨架与弹层宿主
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = titles[page],
                        largeTitle = titles[page],
                    )
                },
            ) { padding ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding())
                ) {
                    when (page) {
                        0 -> RecordsPage(
                            backdrop = backdrop,
                            settings = settings,
                            records = records,
                            onDelete = { scope.launch { store.remove(it) } },
                        )
                        1 -> StatsPage(
                            backdrop = backdrop,
                            settings = settings,
                            records = records,
                        )
                        2 -> SettingsPage(
                            backdrop = backdrop,
                            settings = settings,
                            recordCount = records.size,
                            onClearAll = { scope.launch { store.clearAll() } },
                        )
                    }
                }
            }

            // ③ 液态玻璃 FAB
            LiquidFab(
                backdrop = backdrop,
                settings = settings,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 116.dp),
                onClick = { showAdd = true },
            )

            // ④ 可拖动放大镜（折射 + 色散，最直观的效果展示）
            if (settings.showMagnifier) {
                GlassMagnifier(
                    backdrop = backdrop,
                    settings = settings,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // ⑤ 液态玻璃底栏（滑动动态效果）
            LiquidTabBar(
                backdrop = backdrop,
                settings = settings,
                selected = page,
                onSelect = { page = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            )

            // ⑥ 记一笔弹窗：必须在 MiuixTheme 内 + Miuix Scaffold 内
            //    （OverlayDialog 依赖 Scaffold 提供 MiuixPopupHost）
            if (showAdd) {
                AddRecordSheet(
                    onDismiss = { showAdd = false },
                    onConfirm = { record ->
                        showAdd = false
                        scope.launch { store.add(record) }
                    },
                )
            }
        }
    }
}
