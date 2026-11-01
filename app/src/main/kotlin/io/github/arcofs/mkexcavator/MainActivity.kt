package io.github.arcofs.mkexcavator

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import io.github.arcofs.mkexcavator.ui.theme.MKExcavatorTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // The app is always dark, so the system bar icons are always light.
    enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
    // The app stops every motor when it leaves the screen, so don't let the screen lock mid-drive.
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    setContent {
      MKExcavatorTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }

  // A game controller's sticks and buttons go to the gamepad first while it's driving.
  override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean =
    (application as MkExcavatorApp).gamepad.onMotion(event) || super.dispatchGenericMotionEvent(event)

  // Lint flags calling up to ComponentActivity's override, but overriding it is the documented way
  // to see key events before the views do.
  @SuppressLint("RestrictedApi")
  override fun dispatchKeyEvent(event: KeyEvent): Boolean =
    (application as MkExcavatorApp).gamepad.onKey(event) || super.dispatchKeyEvent(event)

  override fun onStop() {
    super.onStop()
    // Safety rule: nothing keeps running while the app isn't on screen. A rotation isn't leaving.
    if (!isChangingConfigurations) (application as MkExcavatorApp).machine.disconnect()
  }
}
