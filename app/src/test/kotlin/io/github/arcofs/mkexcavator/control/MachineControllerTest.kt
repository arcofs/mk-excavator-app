package io.github.arcofs.mkexcavator.control

import io.github.arcofs.mkexcavator.control.MachineController.Link
import io.github.arcofs.mkexcavator.protocol.Channel
import io.github.arcofs.mkexcavator.protocol.Mk4Message
import io.github.arcofs.mkexcavator.protocol.MotorSpeeds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MachineControllerTest {
  private val radio = FakeRadio()

  private fun TestScope.controller() = MachineController(radio, backgroundScope) { testScheduler.currentTime }

  private fun control(speeds: MotorSpeeds) = Mk4Message.control(speeds).toList()

  private val connect = Mk4Message.connect().toList()

  private val stopped = control(MotorSpeeds.STOPPED)

  @Test
  fun connectBroadcastsTheConnectMessage() = runTest {
    val controller = controller()
    controller.connect()

    assertEquals(Link.CONNECTING, controller.state.value.link)
    assertEquals(listOf(connect), radio.sent)
  }

  @Test
  fun ignoresControlsUntilConnected() = runTest {
    val controller = controller()
    controller.hold(1, Channel.A, 5)

    assertTrue(radio.sent.isEmpty())
  }

  @Test
  fun sendsConnectForTheConnectPeriodThenStoppedControlMessages() = runTest {
    // The boxes only accept a module-button press while they hear control messages.
    val controller = controller()
    controller.connect()
    advanceTimeBy(MachineController.CONNECT_MS - 1)
    assertEquals(connect, radio.last)

    advanceTimeBy(MachineController.TICK_MS + 1)

    assertEquals(Link.READY, controller.state.value.link)
    assertEquals(stopped, radio.last)
  }

  @Test
  fun aControlHeldWhileConnectingStartsOnceTheConnectPeriodEnds() = runTest {
    val controller = controller()
    controller.connect()
    controller.hold(1, Channel.A, 5)
    assertEquals(connect, radio.last)

    repeat((MachineController.CONNECT_MS / 100).toInt()) {
      advanceTimeBy(100)
      controller.hold(1, Channel.A, 5)
    }
    runCurrent()

    assertEquals(control(MotorSpeeds.STOPPED.with(1, Channel.A, 5)), radio.last)
  }

  @Test
  fun drivesBothBoxesInOneMessage() = runTest {
    val controller = controller()
    controller.connect()
    advanceTimeBy(MachineController.CONNECT_MS)

    controller.hold(1, Channel.A, 4)
    controller.hold(2, Channel.C, -6)

    assertEquals(control(MotorSpeeds.STOPPED.with(1, Channel.A, 4).with(2, Channel.C, -6)), radio.last)
  }

  @Test
  fun releaseStopsThatChannelOnly() = runTest {
    val controller = controller()
    controller.connect()
    advanceTimeBy(MachineController.CONNECT_MS)
    controller.hold(1, Channel.A, 4)
    controller.hold(2, Channel.A, 4)

    controller.release(1, Channel.A)

    assertEquals(control(MotorSpeeds.STOPPED.with(2, Channel.A, 4)), radio.last)
  }

  @Test
  fun aControlThatIsNotRefreshedStopsByItself() = runTest {
    val controller = controller()
    controller.connect()
    advanceTimeBy(MachineController.CONNECT_MS)
    controller.hold(1, Channel.B, 7)

    advanceTimeBy(MachineController.REFRESH_TIMEOUT_MS + MachineController.TICK_MS + 1)

    assertEquals(MotorSpeeds.STOPPED, controller.state.value.speeds)
    assertEquals(stopped, radio.last)
  }

  @Test
  fun aRefreshedControlKeepsRunning() = runTest {
    val controller = controller()
    controller.connect()
    advanceTimeBy(MachineController.CONNECT_MS)

    repeat(30) {
      controller.hold(1, Channel.B, 7)
      advanceTimeBy(100)
    }

    assertEquals(7, controller.state.value.speeds[1, Channel.B])
  }

  @Test
  fun stopAllBringsEveryChannelToRest() = runTest {
    val controller = controller()
    controller.connect()
    advanceTimeBy(MachineController.CONNECT_MS)
    controller.hold(1, Channel.A, 4)
    controller.hold(2, Channel.C, 4)

    controller.stopAll()

    assertEquals(MotorSpeeds.STOPPED, controller.state.value.speeds)
    assertEquals(stopped, radio.last)
  }

  @Test
  fun stopLatchesSoAStillHeldControlCannotRestart() = runTest {
    val controller = controller()
    controller.connect()
    advanceTimeBy(MachineController.CONNECT_MS)
    controller.hold(1, Channel.A, 4)

    controller.stopAll()
    controller.hold(1, Channel.A, 4)

    assertTrue(controller.state.value.stopped)
    assertEquals(MotorSpeeds.STOPPED, controller.state.value.speeds)
    assertEquals(stopped, radio.last)
  }

  @Test
  fun afterResumeAControlHeldThroughTheStopMustBeReleasedFirst() = runTest {
    val controller = controller()
    controller.connect()
    advanceTimeBy(MachineController.CONNECT_MS)
    controller.hold(1, Channel.A, 4)
    controller.stopAll()

    controller.resume()
    controller.hold(1, Channel.A, 4)
    assertFalse(controller.state.value.stopped)
    assertEquals(MotorSpeeds.STOPPED, controller.state.value.speeds)

    controller.release(1, Channel.A)
    controller.hold(1, Channel.A, 4)
    assertEquals(4, controller.state.value.speeds[1, Channel.A])
  }

  @Test
  fun aControlPressedDuringTheStopMustAlsoBeReleasedFirst() = runTest {
    val controller = controller()
    controller.connect()
    advanceTimeBy(MachineController.CONNECT_MS)
    controller.stopAll()
    controller.hold(2, Channel.C, 5)

    controller.resume()
    controller.hold(2, Channel.C, 5)
    assertEquals(MotorSpeeds.STOPPED, controller.state.value.speeds)

    controller.release(2, Channel.C)
    controller.hold(2, Channel.C, 5)
    assertEquals(5, controller.state.value.speeds[2, Channel.C])
  }

  @Test
  fun controlsUntouchedSinceTheStopWorkStraightAfterResume() = runTest {
    val controller = controller()
    controller.connect()
    advanceTimeBy(MachineController.CONNECT_MS)
    controller.hold(1, Channel.A, 4)
    controller.stopAll()

    controller.resume()
    controller.hold(1, Channel.D, 3)

    assertEquals(3, controller.state.value.speeds[1, Channel.D])
  }

  @Test
  fun releaseAllRestsEverythingWithoutLatching() = runTest {
    val controller = controller()
    controller.connect()
    advanceTimeBy(MachineController.CONNECT_MS)
    controller.hold(1, Channel.A, 4)

    controller.releaseAll()
    assertFalse(controller.state.value.stopped)
    assertEquals(MotorSpeeds.STOPPED, controller.state.value.speeds)

    controller.hold(1, Channel.A, 4)
    controller.hold(1, Channel.D, 3)
    assertEquals(0, controller.state.value.speeds[1, Channel.A])
    assertEquals(3, controller.state.value.speeds[1, Channel.D])
  }

  @Test
  fun reconnectingClearsTheStop() = runTest {
    val controller = controller()
    controller.connect()
    advanceTimeBy(MachineController.CONNECT_MS)
    controller.stopAll()
    controller.disconnect()

    controller.connect()

    assertFalse(controller.state.value.stopped)
  }

  @Test
  fun disconnectSendsStopBeforeTurningTheRadioOff() = runTest {
    val controller = controller()
    controller.connect()
    advanceTimeBy(MachineController.CONNECT_MS)
    controller.hold(1, Channel.A, 4)

    controller.disconnect()

    assertEquals(Link.DISCONNECTED, controller.state.value.link)
    assertEquals(stopped, radio.last)
    assertEquals(0, radio.stopped)

    advanceTimeBy(MachineController.STOP_FLUSH_MS + 1)

    assertEquals(1, radio.stopped)
  }

  @Test
  fun disconnectWhileConnectingTurnsTheRadioOffStraightAway() = runTest {
    val controller = controller()
    controller.connect()

    controller.disconnect()

    assertEquals(1, radio.stopped)
  }

  @Test
  fun reconnectingSoonAfterDisconnectKeepsTheRadioOn() = runTest {
    val controller = controller()
    controller.connect()
    advanceTimeBy(MachineController.CONNECT_MS)
    controller.hold(1, Channel.A, 4)
    controller.disconnect()

    controller.connect()
    advanceTimeBy(MachineController.STOP_FLUSH_MS + 1)

    assertEquals(0, radio.stopped)
    assertEquals(connect, radio.last)
  }
}
