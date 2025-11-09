package com.example.btterminal

import android.view.View
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import android.os.Build

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: BtDeviceAdapter
    private var btAdapter: BluetoothAdapter? = null
    private var btService: BluetoothService? = null
    private var readJob: Job? = null

    private lateinit var rvDevices: RecyclerView
    private lateinit var btnScan: Button
    private lateinit var etInput: EditText
    private lateinit var btnSend: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvReceived: TextView

    private val requiredPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val requestPermissionLauncher =
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
        ) { _ -> startIfPermissionsGranted() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvDevices = findViewById(R.id.rvDevices)
        btnScan = findViewById(R.id.btnScan)
        etInput = findViewById(R.id.etInput)
        btnSend = findViewById(R.id.btnSend)
        tvStatus = findViewById(R.id.tvStatus)
        tvReceived = findViewById(R.id.tvReceived)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btAdapter = bluetoothManager.adapter

        adapter = BtDeviceAdapter(mutableListOf()) { device -> connectToDevice(device) }
        rvDevices.layoutManager = LinearLayoutManager(this)
        rvDevices.adapter = adapter

        btnScan.setOnClickListener { startDiscovery() }
        btnSend.setOnClickListener { sendMessage() }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        registerReceiver(discoveryReceiver, filter)

        requestPermissionLauncher.launch(requiredPermissions)
    }

    private fun startIfPermissionsGranted() {
        if (!checkPermissions()) {
            tvStatus.text = "Разрешения не выданы"
            return
        }
        if (btAdapter == null) {
            tvStatus.text = "Bluetooth не поддерживается"
            return
        }
        if (btAdapter?.isEnabled == false) {
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } else {
            val paired = btAdapter?.bondedDevices?.toList() ?: emptyList()
            adapter.setDevices(paired)
        }
    }

    private fun checkPermissions(): Boolean =
        requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun startDiscovery() {
        if (!checkPermissions()) {
            requestPermissionLauncher.launch(requiredPermissions)
            return
        }
        btAdapter?.cancelDiscovery()
        adapter.clear()
        btAdapter?.startDiscovery()
        tvStatus.text = "Идёт поиск..."
    }

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    @Suppress("DEPRECATION")
                    val device: BluetoothDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }



                    device?.let { adapter.addDevice(it) }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    tvStatus.text = "Поиск завершён"
                }
            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (!checkPermissions()) {
            requestPermissionLauncher.launch(requiredPermissions)
            return
        }
        tvStatus.text = "Подключение к ${device.name ?: device.address}..."
        btService?.close()
        btService = BluetoothService(device)

        readJob?.cancel()
        readJob = CoroutineScope(Dispatchers.IO).launch {
            val ok = btService?.connect() ?: false
            withContext(Dispatchers.Main) {
                if (ok) {
                    tvStatus.text = "Подключено: ${device.name ?: device.address}"
                    startReading()
                } else {
                    tvStatus.text = "Ошибка подключения"
                }
            }
        }
    }

    private fun sendMessage() {
        val text = etInput.text.toString()
        if (text.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            val ok = btService?.send((text + "\r\n").toByteArray()) ?: false
            withContext(Dispatchers.Main) {
                tvStatus.text = if (ok) "Отправлено" else "Ошибка отправки"
            }
        }
    }

    private fun startReading() {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                val msg = btService?.readLine() ?: break
                withContext(Dispatchers.Main) {
                    tvReceived.append(msg + "\n")
                }

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(discoveryReceiver)
        } catch (_: Exception) {
        }
        readJob?.cancel()
        btService?.close()
    }
}
