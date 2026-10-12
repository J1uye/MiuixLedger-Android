package com.example.miuixledger.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.miuixledger.data.ExpenseRecord
import com.example.miuixledger.data.LedgerStore
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

/**
 * 记账 App 主界面：Backdrop 液态玻璃管线版。
 *
 * 结构对应 Kyant0/AndroidLiquidGlass 官方 catalog：
 *  - 彩色渐变背景层 Modifier.layerBackdrop(backdrop) 采集为「玻璃后面的世界」
 *  - 底部导航与 FAB：玻璃 + vibrancy() + blur() + lens()
 *    （折射参数来自网页版调试结果：amount=60, height=24px）
 */
@Composable
fun LedgerApp() {
    val context = LocalContext.current
    val store = remember { LedgerStore(context) }
    val scope = rememberCoroutineScope()

    val records by store.records.collectAsState()
    var page by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }

    // Backdrop：采集背景内容，供玻璃元素折射
    val backdrop = rememberLayerBackdrop()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .layerBackdrop(backdrop)
    ) {
        // 彩色渐变背景（玻璃后面的世界）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFDFE9FF), Color(0xFFF3E9FF), Color(0xFFFFE9F2))
                    )
                )
        )

        // 页面内容
        Box(modifier = Modifier.fillMaxSize()) {
            when (page) {
                0 -> RecordsPage(
                    records = records,
                    onDelete = { scope.launch { store.remove(it) } },
                )
                1 -> StatsPage(records = records)
                2 -> SettingsPage(
                    recordCount = records.size,
                    onClearAll = { scope.launch { store.clearAll() } },
                )
            }
        }

        // 记一笔 FAB（液态玻璃）
        LiquidFab(
            backdrop = backdrop,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 112.dp, end = 20.dp),
            onClick = { showAdd = true },
        )

        // 液态玻璃底栏
        LiquidTabBar(
            backdrop = backdrop,
            selected = page,
            onSelect = { page = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 18.dp),
        )
    }

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
