package io.github.arcofs.mkexcavator.ui.drive

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.arcofs.mkexcavator.R
import io.github.arcofs.mkexcavator.ui.drive.DriveViewModel.GuideStep

// Where things are in res/drawable-nodpi/box_guide.jpg, as fractions of its width and height.
private const val PHOTO_ASPECT = 1280f / 782f
private val ARM_BOX_AREA = Rect(0.15f, 0.10f, 0.33f, 0.55f)
private val TRACK_BOX_AREA = Rect(0.36f, 0.56f, 0.70f, 0.84f)
private val TRACK_BOX_BUTTON = Offset(0.508f, 0.69f)

private val ARM_COLOUR = Color(0xFFFFFFFF)
private val TRACK_COLOUR = Color(0xFF00E5FF)

/** Full-screen steps from switching the boxes on to the track box being on module 2. */
@Composable
fun ConnectGuide(step: GuideStep, onConnect: () -> Unit, onDone: () -> Unit, onArmBoxPressed: () -> Unit, onCancel: () -> Unit) {
  Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Surface(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.92f)) {
      Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
        BoxPhoto(highlightTrackButton = step == GuideStep.PRESS_BUTTON, modifier = Modifier.weight(1.5f))
        Column(
          modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
          when (step) {
            GuideStep.SWITCH_ON -> {
              GuideText(R.string.guide_switch_on_title, R.string.guide_switch_on_body)
              Button(onClick = onConnect, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.connect)) }
            }
            GuideStep.CONNECTING -> {
              GuideText(R.string.guide_connecting_title, R.string.guide_connecting_body)
              CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            }
            GuideStep.PRESS_BUTTON -> {
              GuideText(R.string.guide_press_title, R.string.guide_press_body)
              Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.guide_done)) }
              OutlinedButton(onClick = onArmBoxPressed, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.guide_arm_box_instead))
              }
            }
          }
          TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.guide_cancel)) }
        }
      }
    }
  }
}

@Composable
private fun GuideText(title: Int, body: Int) {
  Text(stringResource(title), style = MaterialTheme.typography.headlineSmall)
  Text(stringResource(body), style = MaterialTheme.typography.bodyLarge)
}

/** The photo of the two boxes, with each box outlined and labelled. */
@Composable
private fun BoxPhoto(highlightTrackButton: Boolean, modifier: Modifier = Modifier) {
  val pulse by
    rememberInfiniteTransition(label = "pulse")
      .animateFloat(0.6f, 1.4f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse")

  BoxWithConstraints(modifier.aspectRatio(PHOTO_ASPECT).clip(RoundedCornerShape(16.dp))) {
    Image(
      painterResource(R.drawable.box_guide),
      contentDescription = stringResource(R.string.guide_photo_description),
      contentScale = ContentScale.FillBounds,
      modifier = Modifier.fillMaxSize(),
    )
    Canvas(Modifier.fillMaxSize()) {
      outline(ARM_BOX_AREA, ARM_COLOUR)
      outline(TRACK_BOX_AREA, TRACK_COLOUR)
      if (highlightTrackButton) {
        val radius = size.minDimension * 0.05f * pulse
        val centre = Offset(TRACK_BOX_BUTTON.x * size.width, TRACK_BOX_BUTTON.y * size.height)
        drawCircle(TRACK_COLOUR, radius, centre, style = Stroke(4.dp.toPx()))
      }
    }
    Label(stringResource(R.string.arm_box), ARM_COLOUR, x = maxWidth * ARM_BOX_AREA.left, y = maxHeight * ARM_BOX_AREA.top)
    Label(
      stringResource(R.string.track_box) + if (highlightTrackButton) " · " + stringResource(R.string.press_once) else "",
      TRACK_COLOUR,
      x = maxWidth * TRACK_BOX_AREA.left,
      y = maxHeight * TRACK_BOX_AREA.bottom,
    )
  }
}

private fun DrawScope.outline(area: Rect, colour: Color) {
  drawRoundRect(
    colour,
    topLeft = Offset(area.left * size.width, area.top * size.height),
    size = Size(area.width * size.width, area.height * size.height),
    cornerRadius = CornerRadius(12.dp.toPx()),
    style = Stroke(4.dp.toPx()),
  )
}

@Composable
private fun Label(text: String, colour: Color, x: Dp, y: Dp) {
  Box(Modifier.offset(x, y).padding(4.dp)) {
    Surface(color = Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(8.dp)) {
      Text(text, color = colour, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
  }
}
