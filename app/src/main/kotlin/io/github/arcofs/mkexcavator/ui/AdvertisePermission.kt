package io.github.arcofs.mkexcavator.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * Returns a function that runs [onGranted] once the app may broadcast over Bluetooth, asking for
 * the Nearby devices permission first if needed.
 */
@Composable
fun rememberWithAdvertisePermission(onGranted: () -> Unit): () -> Unit {
  val context = LocalContext.current
  val currentOnGranted by rememberUpdatedState(onGranted)
  val launcher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) currentOnGranted()
    }
  return {
    if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
      currentOnGranted()
    } else {
      launcher.launch(Manifest.permission.BLUETOOTH_ADVERTISE)
    }
  }
}
