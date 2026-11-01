package io.github.arcofs.mkexcavator.radio

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.content.Context
import android.content.pm.PackageManager
import io.github.arcofs.mkexcavator.protocol.Mk4Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Broadcasts Mould King messages as BLE advertisements, the way MK Tech does.
 *
 * The boxes find their data at a fixed position in the packet, so the advertisement must hold
 * exactly the Flags field (Android adds it to connectable adverts) followed by the 24-byte
 * manufacturer data: legacy, connectable, no name, no TX power. One advertising set is started
 * once and its data swapped in place, so there's no gap in the broadcast when the payload changes.
 *
 * Call from the main thread; Android delivers the callbacks there too.
 */
class BleAdvertiserRadio(private val context: Context) : Radio {
  private val _state = MutableStateFlow<RadioState>(RadioState.Off)
  override val state: StateFlow<RadioState> = _state.asStateFlow()

  private var advertisingSet: AdvertisingSet? = null

  // Latest payload asked for; null once stopped. Applied when a pending start completes.
  private var wanted: ByteArray? = null

  private val callback =
    object : AdvertisingSetCallback() {
      @SuppressLint("MissingPermission") // Only reachable after broadcast() checked the permission.
      override fun onAdvertisingSetStarted(set: AdvertisingSet?, txPower: Int, status: Int) {
        val payload = wanted
        when {
          status != ADVERTISE_SUCCESS || set == null ->
            _state.value = RadioState.Failed("Couldn't start broadcasting (error $status)")
          payload == null -> advertiser()?.stopAdvertisingSet(this)
          else -> {
            advertisingSet = set
            set.setAdvertisingData(advertiseData(payload))
            _state.value = RadioState.Broadcasting
          }
        }
      }

      override fun onAdvertisingDataSet(set: AdvertisingSet?, status: Int) {
        if (status != ADVERTISE_SUCCESS) {
          _state.value = RadioState.Failed("Couldn't update the broadcast (error $status)")
        }
      }
    }

  @SuppressLint("MissingPermission") // Checked by hasPermission() first.
  override fun broadcast(payload: ByteArray) {
    if (!hasPermission()) {
      _state.value = RadioState.Failed("Nearby devices permission is needed to broadcast")
      return
    }
    wanted = payload
    advertisingSet?.let {
      it.setAdvertisingData(advertiseData(payload))
      return
    }
    if (_state.value == RadioState.Starting) return
    val advertiser = advertiser()
    if (advertiser == null) {
      _state.value = RadioState.Failed("Bluetooth is off")
      return
    }
    _state.value = RadioState.Starting
    advertiser.startAdvertisingSet(PARAMETERS, advertiseData(payload), null, null, null, callback)
  }

  @SuppressLint("MissingPermission") // Checked by hasPermission() first.
  override fun stop() {
    wanted = null
    advertisingSet = null
    if (hasPermission()) advertiser()?.stopAdvertisingSet(callback)
    _state.value = RadioState.Off
  }

  private fun advertiser() =
    context.getSystemService(BluetoothManager::class.java)?.adapter?.takeIf { it.isEnabled }?.bluetoothLeAdvertiser

  private fun hasPermission() =
    context.checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED

  private fun advertiseData(payload: ByteArray) =
    AdvertiseData.Builder()
      .setIncludeDeviceName(false)
      .setIncludeTxPowerLevel(false)
      .addManufacturerData(Mk4Message.COMPANY_ID, payload)
      .build()

  private companion object {
    val PARAMETERS: AdvertisingSetParameters =
      AdvertisingSetParameters.Builder()
        .setLegacyMode(true)
        .setConnectable(true) // Makes Android add the Flags field MK Tech's adverts have.
        .setScannable(true) // Legacy connectable adverts must also be scannable.
        .setInterval(AdvertisingSetParameters.INTERVAL_LOW) // 100 ms, same as MK Tech.
        .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
        .build()
  }
}
