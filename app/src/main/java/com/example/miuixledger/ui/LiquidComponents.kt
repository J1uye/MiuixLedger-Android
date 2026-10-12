package com.example.miuixledger.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

/**
 * 液态玻璃底栏：Capsule 玻璃 + vibrancy + blur + lens（真折射）。
 * 折射参数对齐官方 LiquidButton 示例：lens(12dp, 24dp)，这里按底栏尺寸放大。
 */
@Composable
fun LiquidTabBar(
    backdrop: Backdrop,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels = listOf("明细", "统计", "设置")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(66.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(33.dp) },
                effects = {
                    vibrancy()
                    blur(6f)
                    lens(refractionHeight = 22f, refractionAmount = 46f)
                },
                highlight = { Highlight.Default },
                shadow = { Shadow(alpha = 0.14f) },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.30f)) },
            )
            .padding(horizontal = 7.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { index, label ->
            val active = selected == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = label,
                    style = textStyle(
                        size = 15f,
                        weight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (active) AppColors.Primary else AppColors.Sub,
                    ),
                )
            }
        }
    }
}

/** 液态玻璃 FAB：按下下压反馈 */
@Composable
fun LiquidFab(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "fabScale",
    )

    Box(
        modifier = modifier
            .size(60.dp)
            .scale(scale)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(30.dp) },
                effects = {
                    vibrancy()
                    blur(4f)
                    lens(refractionHeight = 16f, refractionAmount = 56f)
                },
                highlight = { Highlight.Default },
                shadow = { Shadow(alpha = 0.24f) },
                onDrawSurface = { drawRect(AppColors.Primary.copy(alpha = 0.90f)) },
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text = "＋", style = textStyle(30f, FontWeight.Normal, Color.White))
    }
}

/** 玻璃卡片：磨砂 + 半透白膜（不做折射，保证列表滚动性能） */
@Composable
fun GlassCard(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    insidePadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(cornerRadius) },
                effects = { blur(12f) },
                highlight = { Highlight.Default },
                shadow = { Shadow(alpha = 0.08f) },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.46f)) },
            )
            .padding(insidePadding),
        content = content,
    )
}

/** 分段选择器：选中项填充主色 */
@Composable
fun SegmentedRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x338C93B0))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { i, opt ->
            val selected = i == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) AppColors.Primary else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = opt,
                    style = textStyle(
                        size = 14f,
                        weight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) Color.White else AppColors.Content,
                    ),
                )
            }
        }
    }
}

/** 通用按钮 */
@Composable
fun AppButton(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (primary) AppColors.Primary else Color(0x338C93B0))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = textStyle(16f, FontWeight.Medium, if (primary) Color.White else AppColors.Content),
        )
    }
}
