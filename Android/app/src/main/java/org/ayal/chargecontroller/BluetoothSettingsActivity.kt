package org.ayal.chargecontroller

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.ayal.chargecontroller.databinding.ActivityBluetoothSettingsBinding

class BluetoothSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBluetoothSettingsBinding
    private lateinit var devicesRecyclerView: RecyclerView
    private lateinit var scanButton: Button
    private lateinit var scanProgressBar: ProgressBar

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private lateinit var discoveredDevicesAdapter: BluetoothDeviceAdapter
    private val discoveredDevices = mutableListOf<BluetoothDevice>()

    private val PERMISSION_REQUEST_CODE = 101

    // Receiver for Bluetooth discovery events
    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (ActivityCompat.checkSelfPermission(
                                this@BluetoothSettingsActivity,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            if (it.name != null && !discoveredDevices.any { d -> d.address == it.address }) {
                                discoveredDevices.add(it)
                                discoveredDevicesAdapter.notifyItemInserted(discoveredDevices.size - 1)
                            }
                        }
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    scanProgressBar.visibility = View.VISIBLE
                    scanButton.text = getString(R.string.scanning)
                    scanButton.isEnabled = false
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    scanProgressBar.visibility = View.GONE
                    scanButton.text = getString(R.string.scan_for_devices)
                    scanButton.isEnabled = true
                }
            }
        }
    }

    // Activity result launcher for enabling Bluetooth
    private val requestEnableBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show()
                startDiscovery()
            } else {
                Toast.makeText(this, "Bluetooth not enabled. Cannot scan.", Toast.LENGTH_LONG)
                    .show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBluetoothSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        devicesRecyclerView = binding.devicesRecyclerView
        scanButton = binding.scanButton
        scanProgressBar = binding.scanProgressBar

        // Setup RecyclerView
        discoveredDevicesAdapter = BluetoothDeviceAdapter(discoveredDevices) { device ->
            // Handle device click: Initiate connection
            connectToDevice(device)
        }
        devicesRecyclerView.layoutManager = LinearLayoutManager(this)
        devicesRecyclerView.adapter = discoveredDevicesAdapter

        scanButton.setOnClickListener {
            if (checkAndRequestPermissions()) {
                startDiscoveryWithChecks()
            }
        }

        // Register broadcast receivers
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(discoveryReceiver, filter)
    }

    private fun checkAndRequestPermissions(): Boolean {
        val requiredPermissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        // Location permission is needed for scanning on Android 6.0+
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }


        if (requiredPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                requiredPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allPermissionsGranted = true
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }
            if (allPermissionsGranted) {
                startDiscoveryWithChecks()
            } else {
                Toast.makeText(
                    this,
                    "Permissions required for Bluetooth functionality.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun startDiscoveryWithChecks() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device.", Toast.LENGTH_LONG)
                .show()
            return
        }
        if (!bluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                requestEnableBluetooth.launch(enableBtIntent)
            } else {
                Toast.makeText(this, "BLUETOOTH_CONNECT permission not granted.", Toast.LENGTH_LONG)
                    .show()
            }
            return
        }
        startDiscovery()
    }

    private fun startDiscovery() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter?.cancelDiscovery()
            }
            discoveredDevices.clear()
            discoveredDevicesAdapter.notifyDataSetChanged()
            bluetoothAdapter?.startDiscovery() // This might trigger the discoveryReceiver
        } else {
            Toast.makeText(this, "BLUETOOTH_SCAN permission not granted.", Toast.LENGTH_LONG).show()
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothAdapter?.cancelDiscovery() // Stop discovery before attempting to connect
        }

        Toast.makeText(
            this,
            "Connecting to ${device.name ?: device.address}...",
            Toast.LENGTH_SHORT
        ).show()

        // TODO: Implement your Bluetooth connection logic here.
        // This usually involves creating a BluetoothSocket and connecting in a separate thread.
        // See: https://developer.android.com/develop/connectivity/bluetooth/connect-bluetooth-devices
        // You might want to pass the selected device's address back to a previous Activity
        // or a service that handles the actual connection.

        // For now, let's just finish this activity and pass back the device address
        val resultIntent = Intent()
        resultIntent.putExtra("selected_device_address", device.address)
        setResult(RESULT_OK, resultIntent)
        finish()
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(discoveryReceiver)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothAdapter?.cancelDiscovery()
        }
    }
}