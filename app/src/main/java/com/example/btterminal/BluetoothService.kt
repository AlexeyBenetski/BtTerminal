package com.example.btterminal

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.delay
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothService(private val device: BluetoothDevice) {
    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null

    suspend fun connect(): Boolean {
        return try {
            val uuid = device.uuids?.firstOrNull()?.uuid
                ?: UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID
            socket = device.createRfcommSocketToServiceRecord(uuid)
            socket?.connect()
            input = socket?.inputStream
            output = socket?.outputStream
            true
        } catch (e: Exception) {
            close()
            false
        }
    }

    fun send(data: ByteArray): Boolean {
        return try {
            output?.write(data)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun readLine(): String? {
        return try {
            val buffer = ByteArray(1024)
            val bytes = input?.read(buffer) ?: return null
            String(buffer, 0, bytes)
        } catch (e: Exception) {
            null
        }
    }

    fun close() {
        try { socket?.close() } catch (_: Exception) {}
    }
}
