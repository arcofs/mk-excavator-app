package io.github.arcofs.mkexcavator.ui.drive

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import io.github.arcofs.mkexcavator.ui.components.Eyebrow
import io.github.arcofs.mkexcavator.ui.drive.DriveViewModel.GuideStep
import io.github.arcofs.mkexcavator.ui.theme.Concrete
import io.github.arcofs.mkexcavator.ui.theme.SafetyYellow

// Where things are in res/drawable-nodpi/box_guide.jpg, as fractions of its width and height.
private const val PHOTO_ASPECT = 1280f / 782f
private val ARM_BOX_AREA = Rect(0.15f, 0.10f, 0.33f, 0.55f)
private val TRACK_BOX_AREA = Rect(0.36f, 0.56f, 0.70f, 0.84f)
private val TRACK_BOX_BUTTON = Offset(0.508f, 0.69f)

private val ARM_COLOUR = Concrete
private val TRACK_COLOUR = SafetyYellow
private val HALO = Color.Black.copy(alpha = 0.6f)

/** Full-screen steps from switching the boxes on to the track box being on module 2. */
@Composable
fun ConnectGuide(step: GuideStep, onConnect: () -> Unit, onDone: () -> Unit, onArmBoxPressed: () -> Unit, onCancel: () -> Unit) {
  Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = MaterialTheme.colorScheme.surfaceContainerLow,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.94f),
    ) {
      Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
        BoxPhoto(highlightTrackButton = step == GuideStep.PRESS_BUTTON, modifier = Modifier.weight(1.2f))
        Column(Modifier.weight(1f).fillMaxHeight()) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(stringResource(R.string.guide_progress, step.ordinal + 1, GuideStep.entries.size), color = SafetyYellow, modifier = Modifier.weight(1f))
            IconButton(onClick = onCancel) { Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.guide_cancel)) }
          }
          StepBar(step)
          Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
          ) {
            when (step) {
              GuideStep.SWITCH_ON -> GuideText(R.string.guide_switch_on_title, R.string.guide_switch_on_body)
              GuideStep.CONNECTING -> GuideText(R.string.guide_connecting_title, R.string.guide_connecting_body)
              GuideStep.PRESS_BUTTON -> GuideText(R.string.guide_press_title, R.string.guide_press_body)
            }
          }
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (step) {
              GuideStep.SWITCH_ON ->
                Button(onClick = onConnect, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                  Icon(painterResource(R.drawable.ic_bluetooth), contentDescription = null, modifier = Modifier.size(20.dp))
                  Spacer(Modifier.width(8.dp))
                  Text(stringResource(R.string.connect))
                }
              GuideStep.CONNECTING -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 24.dp))
              GuideStep.PRESS_BUTTON -> {
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                  Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(20.dp))
                  Spacer(Modifier.width(8.dp))
                  Text(stringResource(R.string.guide_done))
                }
                OutlinedButton(onClick = onArmBoxPressed, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                  Text(stringResource(R.string.guide_arm_box_instead))
                }
              }
            }
          }
        }
      }
    }
  }
}

/** One segment per step: done and current in yellow. */
@Composable
private fun StepBar(step: GuideStep) {
  Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    for (each in GuideStep.entries) {
      val colour = if (each.ordinal <= step.ordinal) SafetyYellow else MaterialTheme.colorScheme.outlineVariant
      Box(Modifier.weight(1f).height(4.dp).background(colour, CircleShape))
    }
  }
}

@Composable
private fun ColumnScope.GuideText(title: Int, body: Int) {
  Text(stringResource(title), style = MaterialTheme.typography.headlineSmall)
  Text(stringResource(body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** The photo of the two boxes, with each box outlined and labelled. */
@Composable
private fun BoxPhoto(highlightTrackButton: Boolean, modifier: Modifier = Modifier) {
  val pulse by
    rememberInfiniteTransition(label = "pulse")
      .animateFloat(0.6f, 1.4f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse")

  BoxWithConstraints(modifier.aspectRatio(PHOTO_ASPECT).clip(RoundedCornerShape(20.dp))) {
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
        drawCircle(TRACK_COLOUR.copy(alpha = 0.25f), radius, centre)
        drawCircle(HALO, radius, centre, style = Stroke(7.dp.toPx()))
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

/** [colour] over a dark halo, so it stands out against the yellow bricks. */
private fun DrawScope.outline(area: Rect, colour: Color) {
  val topLeft = Offset(area.left * size.width, area.top * size.height)
  val size = Size(area.width * size.width, area.height * size.height)
  val corner = CornerRadius(12.dp.toPx())
  drawRoundRect(HALO, topLeft, size, corner, style = Stroke(6.dp.toPx()))
  drawRoundRect(colour, topLeft, size, corner, style = Stroke(3.dp.toPx()))
}

@Composable
private fun Label(text: String, colour: Color, x: Dp, y: Dp) {
  Row(
    modifier = Modifier.offset(x, y).padding(6.dp).background(Color.Black.copy(alpha = 0.75f), CircleShape).padding(horizontal = 10.dp, vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(Modifier.size(8.dp).background(colour, CircleShape))
    Spacer(Modifier.width(6.dp))
    Eyebrow(text, color = colour)
  }
}
