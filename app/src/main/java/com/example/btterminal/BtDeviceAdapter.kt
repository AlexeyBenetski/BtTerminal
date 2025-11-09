package com.example.btterminal

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BtDeviceAdapter(
    private val devices: MutableList<BluetoothDevice>,
    private val onClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<BtDeviceAdapter.ViewHolder>() {

    fun setDevices(list: List<BluetoothDevice>) {
        devices.clear()
        devices.addAll(list)
        notifyDataSetChanged()
    }

    fun addDevice(device: BluetoothDevice) {
        if (devices.none { it.address == device.address }) {
            devices.add(device)
            notifyItemInserted(devices.size - 1)
        }
    }

    fun clear() {
        devices.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount() = devices.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val d = devices[position]
        holder.name.text = d.name ?: "Unknown"
        holder.addr.text = d.address
        holder.itemView.setOnClickListener { onClick(d) }
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(android.R.id.text1)
        val addr: TextView = v.findViewById(android.R.id.text2)
    }
}
