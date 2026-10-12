package com.example.miuixledger.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

/**
 * 拖拽动画状态机 —— 对应官方 catalog 的 DampedDragAnimation。
 * 负责：透镜位置、按压进度、果冻形变。
 */
@Stable
class DampedDrag(
    private val scope: CoroutineScope,
    initialValue: Float,
    private val valueRange: ClosedFloatingPointRange<Float>,
    private val pressedScale: Float = 1.32f,
) {
    private val valueAnim = Animatable(initialValue)
    private val pressAnim = Animatable(0f)
    private val scaleXAnim = Animatable(1f)
    private val scaleYAnim = Animatable(1f)

    val value: Float get() = valueAnim.value
    val targetValue: Float get() = valueAnim.targetValue
    val pressProgress: Float get() = pressAnim.value
    val scaleX: Float get() = scaleXAnim.value
    val scaleY: Float get() = scaleYAnim.value

    private val valueSpec = spring<Float>(1f, 900f, 0.001f)
    private val pressSpec = spring<Float>(1f, 1000f, 0.001f)
    private val scaleXSpec = spring<Float>(0.6f, 250f, 0.001f)
    private val scaleYSpec = spring<Float>(0.7f, 250f, 0.001f)

    fun press() {
        scope.launch {
            launch { pressAnim.animateTo(1f, pressSpec) }
            launch { scaleXAnim.animateTo(pressedScale, scaleXSpec) }
            launch { scaleYAnim.animateTo(pressedScale, scaleYSpec) }
        }
    }

    fun release() {
        scope.launch {
            launch { pressAnim.animateTo(0f, pressSpec) }
            launch { scaleXAnim.animateTo(1f, scaleXSpec) }
            launch { scaleYAnim.animateTo(1f, scaleYSpec) }
        }
    }

    fun updateValue(v: Float) {
        val target = v.coerceIn(valueRange)
        scope.launch { valueAnim.animateTo(target, valueSpec) }
    }

    fun animateToValue(v: Float) {
        scope.launch {
            press()
            launch { valueAnim.animateTo(v.coerceIn(valueRange), valueSpec) }
            release()
        }
    }
}

/**
 * 液态玻璃底栏 —— 官方 LiquidBottomTabs 移植。
 *
 * 动态效果：透镜跟随手指 + 速度果冻拉伸 + 按压折射增强 + 色散 + 松手回弹吸附。
 * 库参数单位是像素，全部经 dp.toPx() 换算。
 */
@Composable
fun LiquidTabBar(
    backdrop: Backdrop,
    settings: GlassSettings,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels = listOf("明细", "统计", "设置")
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val count = labels.size

    // 底栏自身图层：让透镜折射「底栏内容」，对应官方 tabsBackdrop
    val tabsBackdrop = rememberLayerBackdrop()
    val lensBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)

    val drag = remember(scope) {
        DampedDrag(scope, selected.toFloat(), 0f..(count - 1).toFloat())
    }
    val velocityAnim = remember { Animatable(0f) }
    var panelOffsetDp by remember { mutableFloatStateOf(0f) }
    val tracker = remember { VelocityTracker() }

    var firstRun by remember { mutableStateOf(true) }
    LaunchedEffect(selected) {
        if (firstRun) firstRun = false else drag.animateToValue(selected.toFloat())
    }

    val blurPx = with(density) { settings.blurRadius.dp.toPx() }
    val lensHPx = with(density) { settings.refractionHeight.dp.toPx() }
    val lensAPx = with(density) { settings.refractionAmount.dp.toPx() }
    val padPx = with(density) { 4.dp.toPx() }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().height(68.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val tabWidthPx = ((constraints.maxWidth - padPx * 2f) / count).coerceAtLeast(1f)
        val progress = drag.pressProgress
        val vel = velocityAnim.value
        val stretchX = 1f + (vel * 0.055f).coerceIn(-0.20f, 0.20f)
        val stretchY = 1f - (vel * 0.022f).coerceIn(-0.09f, 0.09f)

        // ① 玻璃条 + 标签，一起采集进 tabsBackdrop
        Box(Modifier.fillMaxSize().layerBackdrop(tabsBackdrop)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(34.dp) },
                        effects = {
                            if (settings.vibrancyOn) vibrancy()
                            blur(blurPx)
                            lens(lensHPx, lensAPx)
                        },
                        highlight = { Highlight.Default },
                        shadow = { Shadow(alpha = 0.14f) },
                        layerBlock = {
                            val s = 1f + 0.03f * progress
                            scaleX = s
                            scaleY = s
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = settings.surfaceTint))
                        },
                    )
            )

            Row(
                Modifier.fillMaxSize().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                labels.forEachIndexed { index, label ->
                    val active = selected == index
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            color = if (active) MiuixTheme.colorScheme.primary
                            else MiuixTheme.colorScheme.onBackgroundVariant,
                            style = if (active) MiuixTheme.textStyles.subtitle
                            else MiuixTheme.textStyles.body1,
                        )
                    }
                }
            }
        }

        // ② 移动透镜：跟随手指 + 果冻 + 按压色散
        Box(
            Modifier
                .width(with(density) { tabWidthPx.toDp() })
                .height(60.dp)
                .graphicsLayer {
                    translationX = padPx + drag.value * tabWidthPx +
                        with(density) { panelOffsetDp.dp.toPx() }
                    scaleX = drag.scaleX * stretchX
                    scaleY = drag.scaleY * stretchY
                }
                .drawBackdrop(
                    backdrop = lensBackdrop,
                    shape = { RoundedCornerShape(30.dp) },
                    effects = {
                        if (settings.vibrancyOn) vibrancy()
                        lens(
                            refractionHeight = lensHPx * (0.30f + 0.70f * progress),
                            refractionAmount = lensAPx * (0.30f + 0.70f * progress),
                            chromaticAberration = true,
                        )
                    },
                    highlight = { Highlight.Default.copy(alpha = 0.30f + 0.70f * progress) },
                    shadow = { Shadow(alpha = 0.08f + 0.22f * progress) },
                    onDrawBehind = { drawRect(Color.Red, blendMode = BlendMode.Clear) },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.10f * (1f - progress)))
                    },
                )
        )

        // ③ 手势层
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val idx = ((offset.x - padPx) / tabWidthPx).toInt().coerceIn(0, count - 1)
                        onSelect(idx)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            tracker.resetTracking()
                            drag.press()
                        },
                        onDragEnd = {
                            val target = drag.value.roundToInt().coerceIn(0, count - 1)
                            drag.animateToValue(target.toFloat())
                            onSelect(target)
                            scope.launch {
                                launch { velocityAnim.animateTo(0f, spring(0.7f, 260f, 0.001f)) }
                                launch { panelOffsetDp = 0f }
                            }
                        },
                        onDragCancel = {
                            drag.release()
                            scope.launch {
                                launch { velocityAnim.animateTo(0f, spring(0.7f, 260f, 0.001f)) }
                                launch { panelOffsetDp = 0f }
                            }
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        tracker.addPosition(change.uptimeMillis, change.position)
                        val v = tracker.calculateVelocity().x / tabWidthPx
                        scope.launch { velocityAnim.snapTo(v.coerceIn(-8f, 8f)) }
                        panelOffsetDp = (-v * 0.55f).coerceIn(-4f, 4f)
                        drag.updateValue(drag.targetValue + dragAmount.x / tabWidthPx)
                    }
                }
        )
    }
}

/** 液态玻璃 FAB：按下下压反馈 */
@Composable
fun LiquidFab(
    backdrop: Backdrop,
    settings: GlassSettings,
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
    val density = LocalDensity.current
    val blurPx = with(density) { (settings.blurRadius * 0.6f).dp.toPx() }
    val lensHPx = with(density) { (settings.refractionHeight * 0.8f).dp.toPx() }
    val lensAPx = with(density) { (settings.refractionAmount * 1.2f).dp.toPx() }

    Box(
        modifier = modifier
            .size(60.dp)
            .scale(scale)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    if (settings.vibrancyOn) vibrancy()
                    blur(blurPx)
                    lens(refractionHeight = lensHPx, refractionAmount = lensAPx)
                },
                highlight = { Highlight.Default },
                shadow = { Shadow(alpha = 0.24f) },
                onDrawSurface = { drawRect(MiuixTheme.colorScheme.primary.copy(alpha = 0.62f)) },
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "＋",
            color = Color.White,
            style = MiuixTheme.textStyles.title2,
        )
    }
}

/** 玻璃卡片：磨砂 + 极淡白膜（列表里不做折射，保证滚动性能） */
@Composable
fun GlassCard(
    backdrop: Backdrop,
    settings: GlassSettings,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    insidePadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val blurPx = with(density) { settings.blurRadius.dp.toPx() }

    Column(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(cornerRadius) },
                effects = {
                    if (settings.vibrancyOn) vibrancy()
                    blur(blurPx)
                },
                highlight = { Highlight.Default },
                shadow = { Shadow(alpha = 0.08f) },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.42f)) },
            )
            .padding(insidePadding),
        content = content,
    )
}

/** 可拖动放大镜：折射弯折 + 色散彩虹，最直观的效果展示 */
@Composable
fun GlassMagnifier(
    backdrop: Backdrop,
    settings: GlassSettings,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var parentW by remember { mutableFloatStateOf(0f) }
    var parentH by remember { mutableFloatStateOf(0f) }

    val sizeDp = 120.dp
    val sizePx = with(density) { sizeDp.toPx() }
    val lensHPx = with(density) { (settings.refractionHeight * 1.4f).dp.toPx() }
    val lensAPx = with(density) { (settings.refractionAmount * 1.8f).dp.toPx() }

    Box(
        modifier = modifier.onSizeChanged {
            parentW = it.width.toFloat()
            parentH = it.height.toFloat()
            if (offsetX == 0f) {
                offsetX = (parentW - sizePx - with(density) { 24.dp.toPx() }).coerceAtLeast(0f)
                offsetY = parentH * 0.42f
            }
        }
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(sizeDp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { CircleShape },
                    effects = {
                        if (settings.vibrancyOn) vibrancy()
                        lens(
                            refractionHeight = lensHPx,
                            refractionAmount = lensAPx,
                            chromaticAberration = true,
                        )
                    },
                    highlight = { Highlight.Default },
                    shadow = { Shadow(alpha = 0.28f) },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.04f)) },
                )
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x)
                            .coerceIn(0f, (parentW - sizePx).coerceAtLeast(0f))
                        offsetY = (offsetY + dragAmount.y)
                            .coerceIn(0f, (parentH - sizePx).coerceAtLeast(0f))
                    }
                },
        )
    }
}

/** 分段选择器（Miuix 无对应组件，用 Miuix Text 自绘布局） */
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
            .background(MiuixTheme.colorScheme.secondaryContainer)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { i, opt ->
            val selected = i == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected) MiuixTheme.colorScheme.primary else Color.Transparent
                    )
                    .clickable { onSelect(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = opt,
                    color = if (selected) Color.White else MiuixTheme.colorScheme.onBackground,
                    style = if (selected) MiuixTheme.textStyles.subtitle
                    else MiuixTheme.textStyles.body2,
                )
            }
        }
    }
}
