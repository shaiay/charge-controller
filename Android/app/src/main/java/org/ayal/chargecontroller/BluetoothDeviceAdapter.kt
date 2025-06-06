package org.ayal.chargecontroller

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView

class BluetoothDeviceAdapter(
    private val devices: List<BluetoothDevice>,
    private val onItemClicked: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<BluetoothDeviceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceNameTextView: TextView = view.findViewById(R.id.deviceNameTextView)
        val deviceAddressTextView: TextView = view.findViewById(R.id.deviceAddressTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bluetooth_device, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]
        // BLUETOOTH_CONNECT permission check needed for device.name and other properties on Android 12+
        if (ActivityCompat.checkSelfPermission(
                holder.itemView.context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            holder.deviceNameTextView.text = "Permission Denied"
            holder.deviceAddressTextView.text = device.address
        } else {
            holder.deviceNameTextView.text = device.name ?: "Unknown Device"
            holder.deviceAddressTextView.text = device.address
        }

        holder.itemView.setOnClickListener {
            onItemClicked(device)
        }
    }

    override fun getItemCount() = devices.size
}