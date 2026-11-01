package io.github.arcofs.mkexcavator.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.arcofs.mkexcavator.R

/** A small all-caps heading, like the stencilled labels on a machine. */
@Composable
fun Eyebrow(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
  textAlign: TextAlign? = null,
) {
  Text(text.uppercase(), modifier = modifier, style = MaterialTheme.typography.labelMedium, color = color, textAlign = textAlign, maxLines = 1)
}

/** A raised housing for a group of controls or settings. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(28.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    content = content,
  )
}

/** A status light and its label; [pulsing] for "in progress". */
@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier, pulsing: Boolean = false) {
  val light =
    if (pulsing) {
      val pulse by
        rememberInfiniteTransition(label = "status")
          .animateFloat(0.25f, 1f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "status")
      pulse
    } else {
      1f
    }
  Row(
    modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape).padding(horizontal = 14.dp, vertical = 7.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(Modifier.size(9.dp).alpha(light).background(color, CircleShape))
    Spacer(Modifier.width(8.dp))
    Eyebrow(text, color = MaterialTheme.colorScheme.onSurface)
  }
}

/** Back arrow, title and optional actions, for the screens behind the drive screen. */
@Composable
fun ScreenTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
  Row(modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
    IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back)) }
    Spacer(Modifier.width(4.dp))
    Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
    actions()
  }
}
