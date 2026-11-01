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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** How often a held stick re-sends its position: comfortably inside the controller's refresh timeout. */
private const val REFRESH_MS = 100L

/**
 * A spring-loaded stick. Reports its deflection as (x, y), each -1..1 with right and up positive,
 * on every move and again every [REFRESH_MS] while held; calls [onRelease] when let go.
 * [twoAxes] false makes it a vertical lever (x is always 0).
 *
 * Each stick follows only the finger that touched it, so several can be used at once.
 */
@Composable
fun ControlStick(
  twoAxes: Boolean,
  enabled: Boolean,
  onMove: (x: Float, y: Float) -> Unit,
  onRelease: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var deflection by remember { mutableStateOf(Offset.Zero) }
  var held by remember { mutableStateOf(false) }
  val currentOnMove by rememberUpdatedState(onMove)
  val currentOnRelease by rememberUpdatedState(onRelease)

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

  val track = MaterialTheme.colorScheme.surfaceVariant
  val outline = MaterialTheme.colorScheme.outline
  val knob = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

  Canvas(
    modifier.pointerInput(enabled, twoAxes) {
      if (!enabled) return@pointerInput
      awaitEachGesture {
        val down = awaitFirstDown()
        val knobRadius = knobRadius(size.width.toFloat(), size.height.toFloat())
        val center = Offset(size.width / 2f, size.height / 2f)
        val reachX = size.width / 2f - knobRadius
        val reachY = size.height / 2f - knobRadius

        fun follow(position: Offset) {
          val raw = position - center
          deflection =
            if (twoAxes) {
              val d = Offset(raw.x / reachX, raw.y / reachY)
              if (d.getDistance() > 1f) d / d.getDistance() else d
            } else {
              Offset(0f, (raw.y / reachY).coerceIn(-1f, 1f))
            }
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
    val knobRadius = knobRadius(size.width, size.height)
    val reach = Offset(size.width / 2f - knobRadius, size.height / 2f - knobRadius)
    if (twoAxes) {
      drawCircle(track)
      drawCircle(outline, style = Stroke(2.dp.toPx()))
      drawLine(outline, Offset(center.x, knobRadius), Offset(center.x, size.height - knobRadius))
      drawLine(outline, Offset(knobRadius, center.y), Offset(size.width - knobRadius, center.y))
    } else {
      val corner = CornerRadius(size.width / 2f)
      drawRoundRect(track, cornerRadius = corner)
      drawRoundRect(outline, cornerRadius = corner, style = Stroke(2.dp.toPx()))
      drawLine(outline, Offset(knobRadius, center.y), Offset(size.width - knobRadius, center.y))
    }
    drawCircle(knob, knobRadius, center + Offset(deflection.x * reach.x, deflection.y * reach.y))
  }
}

private fun knobRadius(width: Float, height: Float) = minOf(width, height) * 0.22f
