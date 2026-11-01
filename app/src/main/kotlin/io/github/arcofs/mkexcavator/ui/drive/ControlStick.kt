package io.github.arcofs.mkexcavator.ui.drive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.arcofs.mkexcavator.control.StickShaping
import io.github.arcofs.mkexcavator.ui.theme.GraphiteHigh
import io.github.arcofs.mkexcavator.ui.theme.GraphiteHighest
import io.github.arcofs.mkexcavator.ui.theme.GraphiteLow
import io.github.arcofs.mkexcavator.ui.theme.GraphiteMid
import io.github.arcofs.mkexcavator.ui.theme.GraphiteRaised
import io.github.arcofs.mkexcavator.ui.theme.OnSafetyYellow
import io.github.arcofs.mkexcavator.ui.theme.SafetyYellow
import io.github.arcofs.mkexcavator.ui.theme.SafetyYellowDeep
import io.github.arcofs.mkexcavator.ui.theme.Steel
import io.github.arcofs.mkexcavator.ui.theme.SteelLight
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.delay

/** How often a held stick re-sends its position: comfortably inside the controller's refresh timeout. */
private const val REFRESH_MS = 100L

/** What each direction of a stick does, printed on its bezel. Blank for nothing. */
data class AxisLabels(val up: String = "", val down: String = "", val left: String = "", val right: String = "")

/**
 * A spring-loaded stick. Reports its deflection as (x, y), each -1..1 with right and up positive,
 * on every move and again every [REFRESH_MS] while held; calls [onRelease] when let go.
 * [twoAxes] false makes it a vertical lever (x is always 0).
 *
 * Each stick follows only the finger that touched it, so several can be used at once.
 * [deadZone] and [snapDegrees] only change the drawing: the dead zone ring and which labels light up.
 * [mirror] (x, y as for [onMove]) shows where a game controller's stick is while it isn't touched.
 * [enabled] is whether it takes touch; [dimmed] greys it out, for when nothing can drive at all.
 */
@Composable
fun ControlStick(
  twoAxes: Boolean,
  enabled: Boolean,
  onMove: (x: Float, y: Float) -> Unit,
  onRelease: () -> Unit,
  modifier: Modifier = Modifier,
  labels: AxisLabels = AxisLabels(),
  deadZone: Float = 0f,
  snapDegrees: Float = 0f,
  mirror: Offset = Offset.Zero,
  dimmed: Boolean = !enabled,
) {
  var deflection by remember { mutableStateOf(Offset.Zero) }
  var held by remember { mutableStateOf(false) }
  val currentOnMove by rememberUpdatedState(onMove)
  val currentOnRelease by rememberUpdatedState(onRelease)
  val currentDeadZone by rememberUpdatedState(deadZone)
  val haptics = LocalHapticFeedback.current
  val measurer = rememberTextMeasurer()
  val labelStyle = MaterialTheme.typography.labelSmall

  LaunchedEffect(held, enabled) {
    if (!held || !enabled) return@LaunchedEffect
    try {
      while (true) {
        currentOnMove(deflection.x, -deflection.y)
        delay(REFRESH_MS)
      }
    } finally {
      currentOnRelease()
    }
  }

  // What to draw: the finger, or else the game controller (screen y points down).
  val shown = if (held) deflection else Offset(mirror.x, -mirror.y)
  val engaged = held || mirror != Offset.Zero

  // The directions that are actually driving something, after the dead zone and axis snap.
  val (sideways, forward) = if (twoAxes) StickShaping.snapToAxis(shown.x, -shown.y, snapDegrees) else 0f to -shown.y
  fun driving(amount: Float) = engaged && amount != 0f && abs(amount) >= deadZone
  val active = Directions(up = driving(forward) && forward > 0, down = driving(forward) && forward < 0, left = driving(sideways) && sideways < 0, right = driving(sideways) && sideways > 0)

  Canvas(
    modifier.alpha(if (dimmed) 0.45f else 1f).pointerInput(enabled, twoAxes) {
      if (!enabled) return@pointerInput
      awaitEachGesture {
        val down = awaitFirstDown()
        val geometry = StickGeometry(Size(size.width.toFloat(), size.height.toFloat()), twoAxes, this)
        var engaged = false
        var atEnd = false

        fun follow(position: Offset) {
          val raw = position - geometry.center
          deflection =
            if (twoAxes) {
              val d = Offset(raw.x / geometry.reach.x, raw.y / geometry.reach.y)
              if (d.getDistance() > 1f) d / d.getDistance() else d
            } else {
              Offset(0f, (raw.y / geometry.reach.y).coerceIn(-1f, 1f))
            }
          // A tick as the stick leaves its dead zone, and another at full travel.
          val push = max(abs(deflection.x), abs(deflection.y))
          val nowEngaged = push > 0f && push >= currentDeadZone
          if (nowEngaged && !engaged) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
          engaged = nowEngaged
          val travel = if (twoAxes) deflection.getDistance() else push
          if (travel >= 0.98f && !atEnd) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
          atEnd = if (atEnd) travel >= 0.9f else travel >= 0.98f
          currentOnMove(deflection.x, -deflection.y)
        }

        try {
          held = true
          follow(down.position)
          down.consume()
          while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            follow(change.position)
            change.consume()
          }
        } finally {
          held = false
          deflection = Offset.Zero
        }
      }
    },
  ) {
    val geometry = StickGeometry(size, twoAxes, this)
    val text = Labels(measurer, labelStyle, labels, active)
    if (twoAxes) drawJoystick(geometry, shown, engaged, !dimmed, deadZone, text) else drawLever(geometry, shown, engaged, !dimmed, deadZone, text)
  }
}

private data class Directions(val up: Boolean, val down: Boolean, val left: Boolean, val right: Boolean)

private class Labels(val measurer: TextMeasurer, val style: TextStyle, val text: AxisLabels, val active: Directions)

/** Where everything sits on a stick of [size]. Drawing and touch share it, so they always agree. */
private class StickGeometry(size: Size, twoAxes: Boolean, density: Density) {
  val center = Offset(size.width / 2f, size.height / 2f)

  // Joystick: a round bezel carrying the labels, around a well the knob moves in.
  val outer = min(size.width, size.height) / 2f
  val bezel = max(outer * 0.19f, with(density) { 24.dp.toPx() })
  val well = outer - bezel
  val knob = well * 0.4f

  // Lever: labels above and below a slot, with a grip sliding in it.
  val labelBand = with(density) { 28.dp.toPx() }
  val slot = with(density) { Rect(center.x - min(size.width * 0.21f, 20.dp.toPx()), labelBand, center.x + min(size.width * 0.21f, 20.dp.toPx()), size.height - labelBand) }
  val grip = with(density) { Size(min(size.width * 0.86f, 84.dp.toPx()), 44.dp.toPx()) }

  val reach = if (twoAxes) Offset(well - knob, well - knob) else Offset(0f, slot.height / 2f - grip.height / 2f)
}

private fun DrawScope.drawJoystick(g: StickGeometry, deflection: Offset, held: Boolean, enabled: Boolean, deadZone: Float, labels: Labels) {
  val c = g.center
  drawCircle(Brush.verticalGradient(listOf(GraphiteHigh, GraphiteLow), c.y - g.outer, c.y + g.outer), g.outer, c)
  drawCircle(GraphiteHighest, g.outer - 0.5.dp.toPx(), c, style = Stroke(1.dp.toPx()))

  // Ticks around the bezel, leaving room for the labels.
  for (degrees in 0 until 360 step 15) {
    if (degrees % 90 < 30 || degrees % 90 > 60) continue
    val angle = Math.toRadians(degrees.toDouble())
    val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
    val length = (if (degrees % 90 == 45) 9.dp else 5.dp).toPx()
    val from = g.well + 5.dp.toPx()
    drawLine(Steel.copy(alpha = 0.7f), c + direction * from, c + direction * (from + length), 1.5.dp.toPx(), StrokeCap.Round)
  }

  drawCircle(Brush.radialGradient(listOf(GraphiteRaised, Color(0xFF08090B)), c, g.well), g.well, c)
  drawCircle(Color.Black.copy(alpha = 0.6f), g.well, c, style = Stroke(2.dp.toPx()))
  drawLine(GraphiteHighest, Offset(c.x - g.well, c.y), Offset(c.x + g.well, c.y), 1.dp.toPx())
  drawLine(GraphiteHighest, Offset(c.x, c.y - g.well), Offset(c.x, c.y + g.well), 1.dp.toPx())
  // The knob's edge has to cross this ring before anything moves.
  if (deadZone > 0f) {
    val dash = 4.dp.toPx()
    drawCircle(Steel.copy(alpha = 0.6f), g.knob + deadZone * g.reach.x, c, style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))))
  }

  val band = g.outer - g.bezel / 2f
  drawLabel(labels, labels.text.up, labels.active.up, Offset(c.x, c.y - band), 0f)
  drawLabel(labels, labels.text.down, labels.active.down, Offset(c.x, c.y + band), 0f)
  drawLabel(labels, labels.text.left, labels.active.left, Offset(c.x - band, c.y), -90f)
  drawLabel(labels, labels.text.right, labels.active.right, Offset(c.x + band, c.y), 90f)

  val knob = c + Offset(deflection.x * g.reach.x, deflection.y * g.reach.y)
  if (held) {
    drawLine(SafetyYellow.copy(alpha = 0.45f), c, knob, 6.dp.toPx(), StrokeCap.Round)
    drawCircle(SafetyYellow.copy(alpha = 0.16f), g.knob * 1.3f, knob)
  }
  drawCircle(Color.Black.copy(alpha = 0.45f), g.knob, knob + Offset(0f, 3.dp.toPx()))
  drawCircle(Brush.radialGradient(capColours(held), knob + Offset(-g.knob * 0.35f, -g.knob * 0.4f), g.knob * 1.6f), g.knob, knob)
  drawCircle(rimColour(held, enabled), g.knob, knob, style = Stroke(2.dp.toPx()))
  val grip = gripColour(held)
  drawCircle(grip, g.knob * 0.55f, knob, style = Stroke(1.5.dp.toPx()))
  drawCircle(grip, g.knob * 0.12f, knob)
}

private fun DrawScope.drawLever(g: StickGeometry, deflection: Offset, held: Boolean, enabled: Boolean, deadZone: Float, labels: Labels) {
  val c = g.center
  val slot = g.slot
  val round = CornerRadius(slot.width / 2f)
  drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF08090B), GraphiteRaised, Color(0xFF08090B)), slot.left, slot.right), slot.topLeft, slot.size, round)
  drawRoundRect(GraphiteHighest, slot.topLeft, slot.size, round, style = Stroke(1.dp.toPx()))

  // Ticks either side of the slot at centre, half and full travel.
  for (fraction in listOf(-1f, -0.5f, 0f, 0.5f, 1f)) {
    val y = c.y + fraction * g.reach.y
    val length = (if (fraction == 0f) 10.dp else 6.dp).toPx()
    val colour = if (fraction == 0f) SteelLight.copy(alpha = 0.8f) else Steel.copy(alpha = 0.7f)
    val gap = 5.dp.toPx()
    drawLine(colour, Offset(slot.left - gap - length, y), Offset(slot.left - gap, y), 1.5.dp.toPx(), StrokeCap.Round)
    drawLine(colour, Offset(slot.right + gap, y), Offset(slot.right + gap + length, y), 1.5.dp.toPx(), StrokeCap.Round)
  }
  if (deadZone > 0f) {
    val inset = 3.dp.toPx()
    drawRect(Steel.copy(alpha = 0.2f), Offset(slot.left + inset, c.y - deadZone * g.reach.y), Size(slot.width - 2 * inset, 2 * deadZone * g.reach.y))
  }

  val gripY = c.y + deflection.y * g.reach.y
  if (held && deflection.y != 0f) {
    val bar = slot.width * 0.3f
    drawRoundRect(SafetyYellow.copy(alpha = 0.85f), Offset(c.x - bar / 2f, min(c.y, gripY)), Size(bar, abs(gripY - c.y)), CornerRadius(bar / 2f))
  }
  drawLabel(labels, labels.text.up, labels.active.up, Offset(c.x, g.labelBand / 2f), 0f)
  drawLabel(labels, labels.text.down, labels.active.down, Offset(c.x, size.height - g.labelBand / 2f), 0f)

  val topLeft = Offset(c.x - g.grip.width / 2f, gripY - g.grip.height / 2f)
  val corner = CornerRadius(g.grip.height * 0.32f)
  drawRoundRect(Color.Black.copy(alpha = 0.45f), topLeft + Offset(0f, 3.dp.toPx()), g.grip, corner)
  drawRoundRect(Brush.verticalGradient(capColours(held), topLeft.y, topLeft.y + g.grip.height), topLeft, g.grip, corner)
  drawRoundRect(rimColour(held, enabled), topLeft, g.grip, corner, style = Stroke(2.dp.toPx()))
  val knurl = g.grip.width * 0.42f
  for (line in -1..1) {
    val y = gripY + line * 6.dp.toPx()
    drawLine(gripColour(held), Offset(c.x - knurl / 2f, y), Offset(c.x + knurl / 2f, y), 2.dp.toPx(), StrokeCap.Round)
  }
}

/** [text] centred on [centre], turned by [degrees]; safety yellow while that direction is driving. */
private fun DrawScope.drawLabel(labels: Labels, text: String, active: Boolean, centre: Offset, degrees: Float) {
  if (text.isEmpty()) return
  val layout = labels.measurer.measure(text.uppercase(), labels.style)
  rotate(degrees, pivot = centre) {
    drawText(layout, if (active) SafetyYellow else SteelLight, centre - Offset(layout.size.width / 2f, layout.size.height / 2f))
  }
}

private fun capColours(held: Boolean) =
  if (held) listOf(Color(0xFFFFDC6E), SafetyYellow, SafetyYellowDeep) else listOf(Color(0xFF4A515B), GraphiteHighest, GraphiteMid)

private fun rimColour(held: Boolean, enabled: Boolean) =
  when {
    held -> SafetyYellowDeep
    enabled -> SafetyYellow.copy(alpha = 0.85f)
    else -> Steel.copy(alpha = 0.6f)
  }

private fun gripColour(held: Boolean) = if (held) OnSafetyYellow.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.35f)
