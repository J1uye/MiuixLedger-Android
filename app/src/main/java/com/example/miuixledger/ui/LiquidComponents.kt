package com.example.miuixledger.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule

private data class TabSpec(val label: String, val icon: ImageVector)

private val TabInactive = Color(0xFF2A3142).copy(alpha = 0.45f)
private val TabActive = Color(0xFF3482FF)
private val ContentDark = Color(0xFF2A3142)

/**
 * 液态玻璃底栏（官方 LiquidBottomTabs 精简移植）：
 * Capsule 玻璃 + vibrancy() + blur() + lens()，选中指示用 spring 动画。
 * 折射参数对应网页调试结果：refractionAmount=60, refractionHeight=24。
 */
@Composable
fun LiquidTabBar(
    backdrop: Backdrop,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        TabSpec("明细", Icons.Filled.Receipt),
        TabSpec("统计", Icons.Filled.PieChart),
        TabSpec("设置", Icons.Filled.Settings),
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(8f)
                    lens(refractionHeight = 24f, refractionAmount = 60f)
                },
                highlight = { Highlight.Default },
                shadow = { Shadow(alpha = 0.12f) },
            )
            .padding(horizontal = 6.dp),
    ) {
        // 选中指示条：spring 滑动（简化版透镜），宽度 = (总宽 - 内边距) / tab 数
        val lensOffset = remember { Animatable(0f) }
        LaunchedEffect(selected) {
            lensOffset.animateTo(
                selected.toFloat(),
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                )
            )
        }
        val lensWidth = (maxWidth - 12.dp) / tabs.size
        Box(
            modifier = Modifier
                .width(lensWidth)
                .height(56.dp)
                .align(Alignment.CenterStart)
                .offset { IntOffset((lensOffset.value * lensWidth.toPx()).toInt(), 0) }
                .clip(RoundedCornerShape(26.dp))
                .background(Color.White.copy(alpha = 0.35f)),
        )

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { index, tab ->
                val active = selected == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (active) TabActive else TabInactive,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = tab.label,
                            color = if (active) TabActive else TabInactive,
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                        )
                    }
                }
            }
        }
    }
}

/** 液态玻璃 FAB：按下下压，玻璃管线与底栏一致 */
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
            .size(58.dp)
            .scale(scale)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(4f)
                    lens(refractionHeight = 20f, refractionAmount = 75f)
                },
                highlight = { Highlight.Default },
                shadow = { Shadow(alpha = 0.22f) },
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "记一笔",
            tint = ContentDark,
            modifier = Modifier.size(28.dp),
        )
    }
}
