package io.github.arcofs.mkexcavator.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.arcofs.mkexcavator.R
import io.github.arcofs.mkexcavator.ui.theme.Concrete
import io.github.arcofs.mkexcavator.ui.theme.HazardBlack
import io.github.arcofs.mkexcavator.ui.theme.MkText
import io.github.arcofs.mkexcavator.ui.theme.SafetyYellow
import io.github.arcofs.mkexcavator.ui.theme.SafetyYellowDeep
import io.github.arcofs.mkexcavator.ui.theme.SignalRed
import io.github.arcofs.mkexcavator.ui.theme.SignalRedDeep
import io.github.arcofs.mkexcavator.ui.theme.SignalRedLight

/** The collar's share of the button; the rest is room for the latched glow. */
private const val COLLAR = 0.88f

/**
 * An emergency stop: a red mushroom on a yellow collar. Once pressed it latches: the collar turns
 * to creeping hazard stripes with a pulsing red halo and the cap reads "STOPPED, tap to resume".
 */
@Composable
fun StopButton(stopped: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 136.dp) {
  val haptics = LocalHapticFeedback.current
  val interactions = remember { MutableInteractionSource() }
  val pressed by interactions.collectIsPressedAsState()
  val press by animateFloatAsState(if (pressed) 0.94f else 1f, label = "press")
  val description = stringResource(if (stopped) R.string.stop_latched_description else R.string.stop)

  Box(
    modifier =
      modifier
        .size(size)
        .graphicsLayer {
          scaleX = press
          scaleY = press
        }
        .clip(CircleShape)
        .clickable(interactionSource = interactions, indication = ripple(color = Color.White), role = Role.Button) {
          haptics.performHapticFeedback(if (stopped) HapticFeedbackType.Confirm else HapticFeedbackType.Reject)
          onClick()
        }
        .semantics { contentDescription = description },
    contentAlignment = Alignment.Center,
  ) {
    if (stopped) LatchedFace() else ReadyFace()
  }
}

@Composable
private fun ReadyFace() {
  Canvas(Modifier.fillMaxSize()) {
    val collar = size.minDimension / 2f * COLLAR
    val cap = collar * 0.8f
    drawCircle(Brush.verticalGradient(listOf(SafetyYellow, SafetyYellowDeep)), collar)
    drawCircle(Color.Black.copy(alpha = 0.35f), cap + 3.dp.toPx(), center + Offset(0f, 2.dp.toPx()))
    drawCircle(
      Brush.radialGradient(listOf(SignalRedLight, SignalRed, SignalRedDeep), center + Offset(-cap * 0.3f, -cap * 0.35f), cap * 1.5f),
      cap,
    )
    drawCircle(SignalRedDeep, cap, style = Stroke(1.5.dp.toPx()))
  }
  Text(stringResource(R.string.stop), style = MkText.stop, color = Color.White)
}

@Composable
private fun LatchedFace() {
  val transition = rememberInfiniteTransition(label = "latched")
  val halo by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "halo")
  val shift by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1_400, easing = LinearEasing)), label = "shift")

  Canvas(Modifier.fillMaxSize()) {
    val outer = size.minDimension / 2f
    val collar = outer * COLLAR
    val face = collar * 0.77f
    drawCircle(SignalRed.copy(alpha = 0.15f + 0.3f * halo), collar + (outer - collar) * halo)
    clipPath(Path().apply { addOval(Rect(center, collar)) }) {
      val stripe = collar * 0.24f
      drawHazardStripes(stripe, shift = shift * stripe * 2)
    }
    drawCircle(SignalRed, collar, style = Stroke(3.dp.toPx()))
    drawCircle(HazardBlack, face)
    drawCircle(SafetyYellow, face, style = Stroke(2.dp.toPx()))
  }
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(stringResource(R.string.stop_latched), style = MkText.stop.copy(fontSize = 21.sp, letterSpacing = 1.sp), color = SafetyYellow)
    Text(
      stringResource(R.string.stop_resume_hint),
      style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
      color = Concrete,
      textAlign = TextAlign.Center,
    )
  }
}
