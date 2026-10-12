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
import androidx.compose.runtime.rememberUpdatedState
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    /**
     * 「滑过去」过程中人为注入的速度，用来驱动果冻拉伸。
     * 拖动时速度来自手指，点击时没有手指，这里按位移补一个冲量，
     * 让点击也有和拖动一致的液态拉伸感。
     */
    private val glideVelAnim = Animatable(0f)

    val value: Float get() = valueAnim.value
    val targetValue: Float get() = valueAnim.targetValue
    val pressProgress: Float get() = pressAnim.value
    val scaleX: Float get() = scaleXAnim.value
    val scaleY: Float get() = scaleYAnim.value
    val glideVelocity: Float get() = glideVelAnim.value

    /** 拖动跟手用：够快，几乎无延迟 */
    private val dragSpec = spring<Float>(1f, 900f, 0.001f)

    /**
     * 点击/吸附滑动用：明显柔和。
     * 之前这里和拖动共用「很硬」的弹簧（stiffness 900 → 约 130ms 到位），
     * 跨一整格时肉眼几乎看不出过程，所以点击显得「直接就是结果」。
     * 现在 stiffness 190 + 轻微回弹（阻尼 0.78）→ 约 370ms，滑动过程清晰可见。
     */
    private val glideSpec = spring<Float>(0.78f, 190f, 0.001f)

    private val pressSpec = spring<Float>(0.9f, 700f, 0.002f)
    private val scaleSpec = spring<Float>(0.72f, 420f, 0.002f)

    /** 点击滑行时的冲量衰减：比滑动本身略快，收尾时先回正 */
    private val glideVelSpec = spring<Float>(0.62f, 320f, 0.002f)

    // 用 Job 持有「按压/形变」动画，确保同一时刻只有一段在跑，
    // 不再让 press() 与 release() 各起协程互相抢占、打断滑动动画。
    private var pressJob: Job? = null
    private var scaleJob: Job? = null

    fun press() {
        pressJob?.cancel()
        scaleJob?.cancel()
        pressJob = scope.launch { pressAnim.animateTo(1f, pressSpec) }
        scaleJob = scope.launch {
            launch { scaleXAnim.animateTo(pressedScale, scaleSpec) }
            launch { scaleYAnim.animateTo(pressedScale, scaleSpec) }
        }
    }

    fun release() {
        pressJob?.cancel()
        scaleJob?.cancel()
        pressJob = scope.launch { pressAnim.animateTo(0f, pressSpec) }
        scaleJob = scope.launch {
            launch { scaleXAnim.animateTo(1f, scaleSpec) }
            launch { scaleYAnim.animateTo(1f, scaleSpec) }
        }
    }

    /** 拖动中：跟随手指（小步长，用 dragSpec 保持跟手） */
    fun updateValue(v: Float) {
        val target = v.coerceIn(valueRange)
        scope.launch { valueAnim.animateTo(target, dragSpec) }
    }

    /**
     * 点击 / 松手吸附：**在同一个协程里顺序执行** 按压 → 滑过去 → 回弹。
     *
     * 关键修正：
     *  1. valueAnim.animateTo 直接写在协程体内（不再包一层 launch），
     *     保证「按压」先起效、「滑动」完整跑完、「回弹」最后收尾。
     *  2. 滑动用 glideSpec（柔和，约 370ms），拖动用 dragSpec（跟手），两者分开。
     *  3. 按位移注入一个速度冲量并衰减 → 滑动途中出现果冻拉伸，
     *     与拖动时的手感一致，而不是干巴巴地平移。
     */
    fun animateToValue(v: Float) {
        val target = v.coerceIn(valueRange)
        val delta = target - valueAnim.value
        scope.launch {
            press()
            // 冲量与位移同向；系数让 1 格 ≈ 2.6，跨 2 格 ≈ 5.2，落在拉伸限幅内
            glideVelAnim.snapTo(delta * 2.6f)
            launch { glideVelAnim.animateTo(0f, glideVelSpec) }
            valueAnim.animateTo(target, glideSpec)
            release()
        }
    }

    /** 拖动手势开始时调用，清掉点击残留的冲量，避免两者叠加 */
    fun clearGlideVelocity() {
        scope.launch { glideVelAnim.snapTo(0f) }
    }

    /**
     * 点击「当前已选中的那一格」时用：原地按压回弹一下。
     * 因为 selected 没变化，LaunchedEffect 不会触发，需要单独给个反馈。
     */
    fun pulse() {
        scope.launch {
            press()
            delay(90)
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

    // ⚠️ pointerInput(Unit) 的闭包只在首次组合时创建一次，之后不再重建。
    //    直接在里面读 selected / onSelect 会永远拿到初始值（selected 恒为 0），
    //    导致「点当前页」判断失效。用 rememberUpdatedState 让闭包读到最新值。
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)

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
        // 速度 = 手指拖动速度 + 点击滑行的注入冲量（两者不会同时存在）
        val vel = velocityAnim.value + drag.glideVelocity
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
                        if (idx == currentSelected) {
                            // 点当前页：selected 不变，LaunchedEffect 不会触发，
                            // 单独给一个原地按压回弹，避免「点了没反应」
                            drag.pulse()
                        } else {
                            // 换页：交给 LaunchedEffect(selected) 驱动滑行动画
                            currentOnSelect(idx)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            tracker.resetTracking()
                            // 清掉点击残留的冲量，避免与手指速度叠加
                            drag.clearGlideVelocity()
                            scope.launch { velocityAnim.snapTo(0f) }
                            drag.press()
                        },
                        onDragEnd = {
                            val target = drag.value.roundToInt().coerceIn(0, count - 1)
                            if (target == currentSelected) {
                                // 同格松手：selected 不变 → LaunchedEffect 不会触发，
                                // 需要自己吸附回去
                                drag.animateToValue(target.toFloat())
                            } else {
                                // 跨格：交给 LaunchedEffect(selected) 统一驱动滑动，
                                // 避免这里和它各跑一次动画导致抖动
                                currentOnSelect(target)
                            }
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

    // ⚠️ 必须在组合期把颜色取出来。
    //    MiuixTheme.colorScheme 是 @Composable 属性（内部读 LocalColorScheme），
    //    而 onDrawSurface 是绘制回调（DrawScope.() -> Unit，不是 @Composable），
    //    在里面直接读会报：
    //      "@Composable invocations can only happen from the context of
    //       a @Composable function"
    val surfaceColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.62f)

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
                onDrawSurface = { drawRect(surfaceColor) },
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
