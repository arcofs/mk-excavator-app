package io.github.arcofs.mkexcavator.ui.debug

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.arcofs.mkexcavator.MkExcavatorApp
import io.github.arcofs.mkexcavator.control.MachineController
import io.github.arcofs.mkexcavator.control.MachineController.Link
import io.github.arcofs.mkexcavator.protocol.Channel
import io.github.arcofs.mkexcavator.protocol.MotorSpeeds
import io.github.arcofs.mkexcavator.radio.RadioState
import io.github.arcofs.mkexcavator.ui.rememberWithAdvertisePermission
import io.github.arcofs.mkexcavator.ui.theme.MKExcavatorTheme
import kotlinx.coroutines.delay

private const val ARM_BOX = 1
private const val TRACK_BOX = 2

// Comfortably inside MachineController.REFRESH_TIMEOUT_MS.
private const val HOLD_REFRESH_MS = 100L

private class TestControl(val label: String, val drives: List<Drive>)

private val ARM_CONTROLS =
  listOf(
    TestControl("Bucket (A)", listOf(Drive(ARM_BOX, Channel.A, 1))),
    TestControl("Stick (B)", listOf(Drive(ARM_BOX, Channel.B, 1))),
    TestControl("Slew (C)", listOf(Drive(ARM_BOX, Channel.C, 1))),
    TestControl("Boom (D)", listOf(Drive(ARM_BOX, Channel.D, 1))),
  )

private val TRACK_CONTROLS =
  listOf(
    TestControl("Right track (A)", listOf(Drive(TRACK_BOX, Channel.A, 1))),
    TestControl("Left track (C)", listOf(Drive(TRACK_BOX, Channel.C, 1))),
  )

private val BOTH_BOX_CONTROLS =
  listOf(
    TestControl("Bucket + right track", listOf(Drive(ARM_BOX, Channel.A, 1), Drive(TRACK_BOX, Channel.A, 1))),
    TestControl("Both tracks", listOf(Drive(TRACK_BOX, Channel.A, 1), Drive(TRACK_BOX, Channel.C, 1))),
  )

@Composable
fun DebugScreen(modifier: Modifier = Modifier) {
  val app = LocalContext.current.applicationContext as MkExcavatorApp
  val viewModel: DebugViewModel = viewModel { DebugViewModel(app.machine, app.radio.state) }
  val machine by viewModel.machine.collectAsStateWithLifecycle()
  val radio by viewModel.radioState.collectAsStateWithLifecycle()
  var speed by rememberSaveable { mutableIntStateOf(4) }
  val connect = rememberWithAdvertisePermission(viewModel::connect)

  DebugScreen(
    machine = machine,
    radio = radio,
    speed = speed,
    onSpeedChange = { speed = it },
    onConnect = connect,
    onDisconnect = viewModel::disconnect,
    onHold = { drives -> viewModel.hold(drives, speed) },
    onRelease = viewModel::release,
    onStop = viewModel::stopAll,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DebugScreen(
  machine: MachineController.State,
  radio: RadioState,
  speed: Int,
  onSpeedChange: (Int) -> Unit,
  onConnect: () -> Unit,
  onDisconnect: () -> Unit,
  onHold: (List<Drive>) -> Unit,
  onRelease: (List<Drive>) -> Unit,
  onStop: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val connected = machine.link != Link.DISCONNECTED
  Scaffold(
    modifier = modifier,
    topBar = { CenterAlignedTopAppBar(title = { Text("MK Excavator test bench") }) },
  ) { innerPadding ->
    Column(
      modifier = Modifier.padding(innerPadding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        "1. Switch on both battery boxes. 2. Tap Connect. 3. Short-press the track box's power button " +
          "once: two quick flashes = Module 2. 4. Hold a button to run a motor; let go to stop.",
        style = MaterialTheme.typography.bodyMedium,
      )
      Text("Link: ${machine.link.label()}    Radio: ${radio.label()}", style = MaterialTheme.typography.titleSmall)
      Text(machine.speeds.summary(), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onConnect, enabled = !connected) { Text("Connect") }
        OutlinedButton(onClick = onDisconnect, enabled = connected) { Text("Disconnect") }
      }

      Text("Speed: $speed of ${MotorSpeeds.MAX_SPEED}", style = MaterialTheme.typography.titleSmall)
      Slider(
        value = speed.toFloat(),
        onValueChange = { onSpeedChange(it.toInt()) },
        valueRange = 1f..MotorSpeeds.MAX_SPEED.toFloat(),
        steps = MotorSpeeds.MAX_SPEED - 2,
      )

      ControlGroup("Arm box (Module 1)", ARM_CONTROLS, connected, onHold, onRelease)
      ControlGroup("Track box (Module 2)", TRACK_CONTROLS, connected, onHold, onRelease)
      ControlGroup("Both boxes at once", BOTH_BOX_CONTROLS, connected, onHold, onRelease)

      Button(
        onClick = onStop,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.fillMaxWidth().height(72.dp).padding(bottom = 16.dp),
      ) {
        Text("STOP", style = MaterialTheme.typography.headlineMedium)
      }
    }
  }
}

@Composable
private fun ControlGroup(
  title: String,
  controls: List<TestControl>,
  enabled: Boolean,
  onHold: (List<Drive>) -> Unit,
  onRelease: (List<Drive>) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    for (control in controls) {
      val reverse = control.drives.map { it.copy(direction = -it.direction) }
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(control.label, modifier = Modifier.width(170.dp))
        HoldButton("−", enabled, onHold = { onHold(reverse) }, onRelease = { onRelease(reverse) })
        HoldButton("+", enabled, onHold = { onHold(control.drives) }, onRelease = { onRelease(control.drives) })
      }
    }
  }
}

/** Calls [onHold] every [HOLD_REFRESH_MS] while pressed, then [onRelease] once let go. */
@Composable
private fun HoldButton(text: String, enabled: Boolean, onHold: () -> Unit, onRelease: () -> Unit) {
  val interactions = remember { MutableInteractionSource() }
  val pressed by interactions.collectIsPressedAsState()
  val currentOnHold by rememberUpdatedState(onHold)
  val currentOnRelease by rememberUpdatedState(onRelease)

  LaunchedEffect(pressed, enabled) {
    if (!pressed || !enabled) return@LaunchedEffect
    try {
      while (true) {
        currentOnHold()
        delay(HOLD_REFRESH_MS)
      }
    } finally {
      currentOnRelease()
    }
  }

  Button(onClick = {}, enabled = enabled, interactionSource = interactions) { Text(text) }
}

private fun Link.label() =
  when (this) {
    Link.DISCONNECTED -> "not connected"
    Link.CONNECTING -> "connecting (sending connect message)"
    Link.READY -> "ready"
  }

private fun RadioState.label() =
  when (this) {
    RadioState.Off -> "off"
    RadioState.Starting -> "starting"
    RadioState.Broadcasting -> "broadcasting"
    is RadioState.Failed -> reason
  }

private fun MotorSpeeds.summary() =
  (1..2).joinToString("\n") { module ->
    "Module $module: " + Channel.entries.joinToString("  ") { "${it.name} ${"%+d".format(this[module, it])}" }
  }

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun DebugScreenPreview() {
  MKExcavatorTheme {
    DebugScreen(
      machine = MachineController.State(Link.READY, MotorSpeeds.STOPPED.with(1, Channel.A, 4).with(2, Channel.A, 4)),
      radio = RadioState.Broadcasting,
      speed = 4,
      onSpeedChange = {},
      onConnect = {},
      onDisconnect = {},
      onHold = {},
      onRelease = {},
      onStop = {},
    )
  }
}
