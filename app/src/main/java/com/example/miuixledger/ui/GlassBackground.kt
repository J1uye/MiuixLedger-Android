package com.example.miuixledger.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/**
 * 玻璃背后的「世界」。
 *
 * 关键点：折射（lens）是把背后的图案扭曲，如果背景是平滑渐变就看不出来。
 * 所以这里画高对比的斜条纹 + 色块，折射时条纹会明显弯折，效果一眼可见。
 * 官方 demo 用壁纸照片也是同一个道理。
 */
@Composable
fun GlassBackground(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        // 底色：鲜艳渐变
        drawRect(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF6FA8FF),
                    Color(0xFFB98BFF),
                    Color(0xFFFF8FC0),
                )
            )
        )

        // 斜条纹：边缘清晰，是折射是否生效的最佳参照
        rotate(26f) {
            val stripe = 62.dp.toPx()
            var x = -size.height * 2f
            var i = 0
            while (x < size.width + size.height * 2f) {
                drawRect(
                    color = if (i % 2 == 0) Color(0x30FFFFFF) else Color(0x16000000),
                    topLeft = Offset(x, -size.height),
                    size = Size(stripe, size.height * 3f),
                )
                x += stripe
                i++
            }
        }

        // 高对比色块：给折射和模糊提供参照物
        drawCircle(Color(0x66FFD166), 112.dp.toPx(), Offset(size.width * 0.22f, size.height * 0.18f))
        drawCircle(Color(0x6652D1A0), 86.dp.toPx(), Offset(size.width * 0.82f, size.height * 0.42f))
        drawCircle(Color(0x66FF5C8A), 134.dp.toPx(), Offset(size.width * 0.44f, size.height * 0.80f))
        drawCircle(Color(0x6634C6FF), 72.dp.toPx(), Offset(size.width * 0.90f, size.height * 0.90f))
        drawCircle(Color(0x4D7C4DFF), 64.dp.toPx(), Offset(size.width * 0.10f, size.height * 0.62f))
    }
}
