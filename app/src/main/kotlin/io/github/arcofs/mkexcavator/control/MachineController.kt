package io.github.arcofs.mkexcavator.control

import io.github.arcofs.mkexcavator.protocol.Channel
import io.github.arcofs.mkexcavator.protocol.Mk4Message
import io.github.arcofs.mkexcavator.protocol.MotorSpeeds
import io.github.arcofs.mkexcavator.radio.Radio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Turns held controls into what the radio broadcasts, and enforces the safety rules:
 * - a control only keeps running while it's refreshed at least every [REFRESH_TIMEOUT_MS];
 * - releasing a control, [stopAll] and [disconnect] bring motors to rest;
 * - [disconnect] keeps broadcasting "all stopped" for [STOP_FLUSH_MS] before the radio goes off,
 *   so no box is left running on its last command.
 *
 * Connecting sends the connect message for [CONNECT_MS], then control messages from then on, even
 * with everything stopped: a box only accepts a press of its module button while it hears control
 * messages. Not thread-safe: call from one thread.
 */
class MachineController(
  private val radio: Radio,
  private val scope: CoroutineScope,
  private val now: () -> Long = { System.nanoTime() / 1_000_000 },
) {
  enum class Link { DISCONNECTED, CONNECTING, READY }

  data class State(val link: Link = Link.DISCONNECTED, val speeds: MotorSpeeds = MotorSpeeds.STOPPED)

  private val _state = MutableStateFlow(State())
  val state: StateFlow<State> = _state.asStateFlow()

  private val lastRefresh = mutableMapOf<Pair<Int, Channel>, Long>()
  private var connectedAt = 0L
  private var watchdog: Job? = null
  private var pendingRadioOff: Job? = null
  private var lastSent: ByteArray? = null

  fun connect() {
    if (_state.value.link != Link.DISCONNECTED) return
    pendingRadioOff?.cancel()
    connectedAt = now()
    update(State(Link.CONNECTING))
    watchdog =
      scope.launch {
        while (true) {
          delay(TICK_MS)
          tick()
        }
      }
  }

  /** Run [channel] of [module] at [speed]. Call again within [REFRESH_TIMEOUT_MS] to keep it running. */
  fun hold(module: Int, channel: Channel, speed: Int) {
    if (_state.value.link == Link.DISCONNECTED) return
    lastRefresh[module to channel] = now()
    val current = _state.value
    update(current.copy(speeds = current.speeds.with(module, channel, speed)))
  }

  fun release(module: Int, channel: Channel) {
    if (_state.value.link == Link.DISCONNECTED) return
    lastRefresh.remove(module to channel)
    val current = _state.value
    update(current.copy(speeds = current.speeds.with(module, channel, 0)))
  }

  fun stopAll() {
    if (_state.value.link == Link.DISCONNECTED) return
    lastRefresh.clear()
    update(_state.value.copy(speeds = MotorSpeeds.STOPPED))
  }

  fun disconnect() {
    val wasReady = _state.value.link == Link.READY
    if (_state.value.link == Link.DISCONNECTED) return
    watchdog?.cancel()
    lastRefresh.clear()
    _state.value = State(Link.DISCONNECTED)
    lastSent = null
    if (wasReady) {
      radio.broadcast(Mk4Message.control(MotorSpeeds.STOPPED))
      pendingRadioOff =
        scope.launch {
          delay(STOP_FLUSH_MS)
          radio.stop()
        }
    } else {
      radio.stop()
    }
  }

  private fun tick() {
    val time = now()
    var speeds = _state.value.speeds
    for ((key, refreshedAt) in lastRefresh.toList()) {
      if (time - refreshedAt > REFRESH_TIMEOUT_MS) {
        lastRefresh.remove(key)
        speeds = speeds.with(key.first, key.second, 0)
      }
    }
    update(_state.value.copy(speeds = speeds))
  }

  private fun update(next: State) {
    val state =
      if (next.link == Link.CONNECTING && now() - connectedAt >= CONNECT_MS) {
        next.copy(link = Link.READY)
      } else {
        next
      }
    _state.value = state
    val payload =
      when (state.link) {
        Link.DISCONNECTED -> return
        Link.CONNECTING -> Mk4Message.connect()
        Link.READY -> Mk4Message.control(state.speeds)
      }
    if (lastSent?.contentEquals(payload) == true) return
    lastSent = payload
    radio.broadcast(payload)
  }

  companion object {
    const val TICK_MS = 100L
    const val REFRESH_TIMEOUT_MS = 500L
    const val CONNECT_MS = 2_000L
    const val STOP_FLUSH_MS = 500L
  }
}
