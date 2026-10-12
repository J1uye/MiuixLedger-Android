package com.example.miuixledger.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import kotlin.math.abs
import kotlin.math.roundToInt

/** 滑行时长的下界（ms）：几乎原地时用，保证「按一下就有反应」 */
private const val GLIDE_MIN_MS = 150

/** 滑行时长的上界（ms）：跨 2 格时用，保证滑动过程看得清 */
private const val GLIDE_MAX_MS = 320

/** 透镜缩回 dock 的时长（ms） */
private const val SETTLE_MS = 200

/** 点击时透镜「按下鼓起」的时长（ms） */
private const val PRESS_MS = 120

/**
 * 按位移距离决定滑行时长。
 *
 * 这是修「到位后停留太久」的关键：
 *  - 点击跨整格（|delta| 大）→ 需要滑行过程，用较长时长
 *  - 拖动松手（透镜已在目标附近，|delta| 很小）→ 几乎不用滑，
 *    时长自动缩短，于是复位立刻开始，不会干等
 */
private fun glideDurationFor(delta: Float): Int {
    val d = abs(delta).coerceAtMost(2f)
    return (GLIDE_MIN_MS + (GLIDE_MAX_MS - GLIDE_MIN_MS) * (d / 2f)).toInt()
}

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
     * 按压生长 / 复位都用定长 tween，让时间线可精确编排。
     * 用弹簧的话「何时算结束」不可控，复位时机就会漂移，
     * 表现为「透镜到位后还鼓着不动一段时间」。
     */
    private val pressGrowSpec = tween<Float>(PRESS_MS, easing = FastOutSlowInEasing)
    private val settleSpec = tween<Float>(SETTLE_MS, easing = FastOutSlowInEasing)

    /** 拖动手势期间用的按压反馈：弹簧更自然（此处不追求精确收尾） */
    private val pressSpec = spring<Float>(0.9f, 700f, 0.002f)
    private val scaleSpec = spring<Float>(0.72f, 420f, 0.002f)

    // 用 Job 持有「按压/形变」动画，确保同一时刻只有一段在跑，
    // 不再让 press() 与 release() 各起协程互相抢占、打断滑动动画。
    private var pressJob: Job? = null
    private var scaleJob: Job? = null

    /** 位置动画的 Job：拖动跟手 / 滑行 / 吸附都复用它，避免协程堆积 */
    private var valueJob: Job? = null

    /** 按压：生长时长可指定（点击路径用定长 tween，拖动路径用弹簧） */
    fun press(
        pressAnimation: AnimationSpec<Float> = pressSpec,
        scaleAnimation: AnimationSpec<Float> = scaleSpec,
    ) {
        pressJob?.cancel()
        scaleJob?.cancel()
        pressJob = scope.launch { pressAnim.animateTo(1f, pressAnimation) }
        scaleJob = scope.launch {
            launch { scaleXAnim.animateTo(pressedScale, scaleAnimation) }
            launch { scaleYAnim.animateTo(pressedScale, scaleAnimation) }
        }
    }

    /** 复位：缩回 dock 的时长可指定 */
    fun release(
        pressAnimation: AnimationSpec<Float> = pressSpec,
        scaleAnimation: AnimationSpec<Float> = scaleSpec,
    ) {
        pressJob?.cancel()
        scaleJob?.cancel()
        pressJob = scope.launch { pressAnim.animateTo(0f, pressAnimation) }
        scaleJob = scope.launch {
            launch { scaleXAnim.animateTo(1f, scaleAnimation) }
            launch { scaleYAnim.animateTo(1f, scaleAnimation) }
        }
    }

    /**
     * 拖动中：跟随手指（小步长，用 dragSpec 保持跟手）。
     * 复用同一个 Job，避免每帧起新协程造成堆积、松手后动画还在追赶。
     */
    fun updateValue(v: Float) {
        val target = v.coerceIn(valueRange)
        valueJob?.cancel()
        valueJob = scope.launch { valueAnim.animateTo(target, dragSpec) }
    }

    /** 拖动中：直接设置位置，不做动画（最跟手，松手时无需再滑） */
    fun snapValue(v: Float) {
        val target = v.coerceIn(valueRange)
        valueJob?.cancel()
        valueJob = scope.launch { valueAnim.snapTo(target) }
    }

    /**
     * 点击 / 松手吸附：按压 → 滑过去 → 复位，**并且同时收尾**。
     *
     * 关键时序（这是修「到位后停留太久」的核心）：
     *   glideMs 由位移距离算出（见 glideDurationFor）：
     *     点击跨 2 格 → 320ms，跨 1 格 → 235ms，
     *     拖动松手（已到位）→ 150ms
     *   0ms              按压 + 滑行开始
     *   glideMs-SETTLE   复位开始（若 glideMs < SETTLE 则立即开始）
     *   glideMs          滑行与复位**同一时刻结束** → 一落地就正好收回 dock
     *
     * ⚠️ 两个坑：
     *  1. 不能写成 `animateTo(target, spec)` 之后再 `release()` ——
     *     那是「先滑完、再缩回」的串行，中间鼓着不动就是被抱怨的停留。
     *  2. 滑行时长不能写死。拖动松手时透镜已经到位，写死长时长会让它
     *     在原地干等，同样表现为「停留」。必须按距离动态给时长。
     */
    fun animateToValue(v: Float) {
        val target = v.coerceIn(valueRange)
        val delta = target - valueAnim.value
        val glideMs = glideDurationFor(delta)
        // 取消拖动残留的位置动画，避免两段动画抢同一个 Animatable
        valueJob?.cancel()
        valueJob = scope.launch {
            // 按压用定长 tween：生长结束时刻精确，便于与滑行、复位对齐
            press(pressGrowSpec, pressGrowSpec)
            // 冲量与位移同向；系数让 1 格 ≈ 2.6，跨 2 格 ≈ 5.2，落在拉伸限幅内
            glideVelAnim.snapTo(delta * 2.6f)
            launch { glideVelAnim.animateTo(0f, tween(glideMs, easing = FastOutSlowInEasing)) }

            // 与滑行并行：等滑行接近尾声再开始复位，两者同时结束
            launch {
                delay((glideMs - SETTLE_MS).coerceAtLeast(0).toLong())
                release(settleSpec, settleSpec)
            }

            valueAnim.animateTo(target, tween(glideMs, easing = FastOutSlowInEasing))
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
            press(pressGrowSpec, pressGrowSpec)
            delay(90)
            release(settleSpec, settleSpec)
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

    /**
     * 拖动期间的透镜位置，用普通状态同步累加。
     *
     * 不能靠 `drag.targetValue` 累加：snapValue 是异步的（协程里执行），
     * 快速拖动时它还没执行完，读到的 targetValue 是旧值，
     * 累加就会丢掉一部分拖动量，表现为「拖了但没跟上」。
     */
    var dragAccum by remember { mutableFloatStateOf(0f) }

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
                            // 以当前透镜位置为拖动起点（同步记录，避免异步读到旧值）
                            dragAccum = drag.value
                            drag.press()
                        },
                        onDragEnd = {
                            val target = dragAccum.roundToInt().coerceIn(0, count - 1)
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
                        // 拖动时直接把位置写到位（不做动画）：
                        // 这样松手时透镜已经在手指位置，animateToValue 的位移很小，
                        // 滑行时长自动缩到最短，复位立刻开始 → 不会「停留」
                        dragAccum = (dragAccum + dragAmount.x / tabWidthPx)
                            .coerceIn(0f, (count - 1).toFloat())
                        drag.snapValue(dragAccum)
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
