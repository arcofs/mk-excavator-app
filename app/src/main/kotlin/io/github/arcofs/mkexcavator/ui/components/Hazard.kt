package io.github.arcofs.mkexcavator.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.arcofs.mkexcavator.ui.theme.HazardBlack
import io.github.arcofs.mkexcavator.ui.theme.SafetyYellow

/** Yellow and black 45° stripes over the whole draw area, moved along by [shift] px. Clip first for other shapes. */
fun DrawScope.drawHazardStripes(stripeWidth: Float, shift: Float = 0f, yellow: Color = SafetyYellow, black: Color = HazardBlack) {
  drawRect(black)
  val period = stripeWidth * 2
  val height = size.height
  var x = -height - period + shift.mod(period)
  while (x < size.width) {
    val stripe =
      Path().apply {
        moveTo(x, height)
        lineTo(x + stripeWidth, height)
        lineTo(x + stripeWidth + height, 0f)
        lineTo(x + height, 0f)
        close()
      }
    drawPath(stripe, yellow)
    x += period
  }
}

/** A strip of hazard tape whose stripes creep sideways, for "something needs your attention". */
@Composable
fun HazardTape(modifier: Modifier = Modifier, stripeWidth: Dp = 10.dp) {
  val shift by
    rememberInfiniteTransition(label = "tape")
      .animateFloat(0f, 1f, infiniteRepeatable(tween(1_400, easing = LinearEasing)), label = "tape")
  Canvas(modifier) {
    val stripe = stripeWidth.toPx()
    drawHazardStripes(stripe, shift = shift * stripe * 2)
  }
}
