package com.example.datasimulator

import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.datasimulator.databinding.ActivityMainBinding
import kotlin.random.Random
import androidx.core.app.NotificationCompat
import android.os.CountDownTimer
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var dataAdapter: DataAdapter
    private lateinit var recentDataAdapter: DataAdapter

    private val handler = Handler(Looper.getMainLooper())
    private var isBluetoothConnected = false
    private val heartRateMin = 60
    private val heartRateMax = 100
    private val heartRateInterval: Long = 2000 // ms
    private val CHANNEL_ID = "bluetooth_status_channel"

    private val dataTypes = listOf(
        "Ritmo Cardiaco",
        "Oxígeno en Sangre",
        "Pasos",
        "Estrés"
    )
    private val dataRanges = mapOf(
        "Ritmo Cardiaco" to (80..100),
        "Oxígeno en Sangre" to (96..99),
        "Pasos" to (2000..8000),
        "Estrés" to (2..7)
    )
    private var currentSimulatedData: MutableMap<String, Double> = mutableMapOf()
    private var updateTimer: CountDownTimer? = null

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                updateBluetoothStatus(state == BluetoothAdapter.STATE_ON)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar base de datos
        databaseHelper = DatabaseHelper(this)

        // Configurar RecyclerView
        setupRecyclerView()
        setupRecentRecyclerView()

        // Configurar botones
        setupButtons()

        // Simular datos iniciales
        simulateAllData()
        showSimulatedBlock()

        // Configurar canal de notificaciones
        createNotificationChannel()

        // Registrar receiver para cambios de Bluetooth
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        // Estado inicial de Bluetooth
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        updateBluetoothStatus(bluetoothAdapter?.isEnabled == true)

        // Iniciar simulación de ritmo cardiaco
        startHeartRateSimulation()

        // Botón para regresar a la pantalla principal
        binding.backToMainButton.setOnClickListener {
            showMainScreen()
        }
    }

    private fun setupRecyclerView() {
        dataAdapter = DataAdapter(emptyList())
        binding.dataRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = dataAdapter
        }
    }

    private fun setupRecentRecyclerView() {
        recentDataAdapter = DataAdapter(emptyList())
        binding.recentDataRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = recentDataAdapter
        }
    }

    private fun simulateAllData() {
        for (type in dataTypes) {
            val range = dataRanges[type] ?: (0..1)
            val prev = currentSimulatedData[type]?.toInt() ?: (range as IntRange).random()
            val newValue = when (type) {
                "Ritmo Cardiaco" -> (prev + (-2..2).random()).coerceIn(range)
                "Oxígeno en Sangre" -> (prev + (-1..1).random()).coerceIn(range)
                "Pasos" -> (prev + (50..500).random()).coerceIn(range)
                "Estrés" -> (prev + (-1..1).random()).coerceIn(range)
                else -> (range as IntRange).random()
            }
            currentSimulatedData[type] = newValue.toDouble() // Guardar como Double por compatibilidad, pero solo enteros
        }
    }

    private fun showSimulatedBlock() {
        // Mostrar los datos simulados en la pantalla principal
        val sb = StringBuilder()
        for (type in dataTypes) {
            val value = currentSimulatedData[type]?.toInt() ?: 0
            val formatted = when (type) {
                "Ritmo Cardiaco" -> "$value bpm"
                "Oxígeno en Sangre" -> "$value %"
                "Pasos" -> "$value pasos"
                "Estrés" -> "Nivel: $value"
                else -> value.toString()
            }
            sb.append("$type: $formatted\n")
        }
        binding.heartRateLabel.text = "Datos de Salud"
        binding.heartRateValue.text = sb.toString().trim()
        binding.connectionStatus.visibility = View.VISIBLE
    }

    private fun saveSimulatedDataToDB() {
        for (type in dataTypes) {
            val value = currentSimulatedData[type]?.toInt() ?: continue
            databaseHelper.insertData(type, value.toDouble()) // Guardar como Double pero solo enteros
        }
    }

    private fun saveMedicionToDB() {
        val ritmo = currentSimulatedData["Ritmo Cardiaco"]?.toInt() ?: 0
        val oxigeno = currentSimulatedData["Oxígeno en Sangre"]?.toInt() ?: 0
        val pasos = currentSimulatedData["Pasos"]?.toInt() ?: 0
        val estres = currentSimulatedData["Estrés"]?.toInt() ?: 0
        databaseHelper.insertMedicion(ritmo, oxigeno, pasos, estres)
    }

    private fun sendSimulatedDataToServer() {
        val json = JSONObject()
        for (type in dataTypes) {
            val value = currentSimulatedData[type]?.toInt() ?: 0
            json.put(type, value)
        }

        // Agregar timestamp para mejor tracking
        json.put("timestamp", System.currentTimeMillis())
        json.put("device_id", "android_app")

        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body: RequestBody = json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("http://3.145.62.106:9000/upload/")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()

        thread {
            try {
                println("Enviando datos: ${json.toString()}")
                val response = client.newCall(request).execute()

                runOnUiThread {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        println("Respuesta del servidor: $responseBody")
                        Toast.makeText(this, "Datos enviados al servidor exitosamente", Toast.LENGTH_SHORT).show()
                    } else {
                        val errorBody = response.body?.string()
                        println("Error HTTP ${response.code}: $errorBody")
                        Toast.makeText(this, "Error del servidor: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
                response.close()
            } catch (e: java.net.ConnectException) {
                println("Error de conexión: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this, "No se puede conectar al servidor. Verifica la URL.", Toast.LENGTH_LONG).show()
                }
            } catch (e: java.net.SocketTimeoutException) {
                println("Timeout: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this, "Tiempo de espera agotado. Servidor lento.", Toast.LENGTH_LONG).show()
                }
            } catch (e: java.net.UnknownHostException) {
                println("Host desconocido: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this, "Servidor no encontrado. Verifica la URL.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                println("Error general: ${e.message}")
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupButtons() {
        binding.sendDataButton.setOnClickListener {
            simulateAllData()
            showSimulatedBlock()
            saveSimulatedDataToDB()
            saveMedicionToDB()
            sendSimulatedDataToServer()
            Toast.makeText(this, getString(R.string.data_sent), Toast.LENGTH_SHORT).show()
        }

        binding.viewDataButton.setOnClickListener {
            showDataTable()
            startAutoUpdateTable()
        }

        binding.clearDataButton.setOnClickListener {
            clearAllData()
        }
        binding.historyButton?.setOnClickListener {
            showMedicionesHistory()
        }
    }

    private fun showDataTable() {
        // Mostrar solo los datos de los 4 tipos en la tabla
        val allData = databaseHelper.getAllData().filter { it.type in dataTypes }
        dataAdapter.updateData(allData)
        binding.dataRecyclerView.visibility = View.VISIBLE
        binding.recentDataRecyclerView.visibility = View.GONE
        binding.heartRateLabel.visibility = View.GONE
        binding.heartRateValue.visibility = View.GONE
        binding.connectionStatus.visibility = View.GONE
        binding.backToMainButton.visibility = View.VISIBLE
    }

    private fun showMedicionesHistory() {
        val lista = databaseHelper.getAllMediciones()
        val sb = StringBuilder()
        sb.append("Historial de Mediciones:\n\n")
        for (m in lista) {
            sb.append("${formatDate(m.timestamp)} | ")
            sb.append("Ritmo: ${m.ritmo} bpm, ")
            sb.append("Oxígeno: ${m.oxigeno} %, ")
            sb.append("Pasos: ${m.pasos}, ")
            sb.append("Estrés: ${m.estres}\n")
        }
        if (lista.isEmpty()) sb.append("Sin registros")
        binding.heartRateLabel.text = "Historial"
        binding.heartRateValue.text = sb.toString()
        binding.heartRateLabel.visibility = View.VISIBLE
        binding.heartRateValue.visibility = View.VISIBLE
        binding.dataRecyclerView.visibility = View.GONE
        binding.recentDataRecyclerView.visibility = View.GONE
        binding.connectionStatus.visibility = View.GONE
        binding.backToMainButton.visibility = View.VISIBLE
    }

    private fun formatDate(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        return format.format(date)
    }

    private fun showMainScreen() {
        binding.dataRecyclerView.visibility = View.GONE
        binding.recentDataRecyclerView.visibility = View.GONE
        binding.heartRateLabel.visibility = View.VISIBLE
        binding.heartRateValue.visibility = View.VISIBLE
        binding.connectionStatus.visibility = View.VISIBLE
        binding.backToMainButton.visibility = View.GONE
        showSimulatedBlock()
        updateTimer?.cancel()
    }

    private fun startAutoUpdateTable() {
        updateTimer?.cancel()
        updateTimer = object : CountDownTimer(60000, 60000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                showDataTable()
                startAutoUpdateTable()
            }
        }.start()
    }

    private fun loadData() {
        val dataList = databaseHelper.getAllData()
        
        if (dataList.isEmpty()) {
            binding.emptyText.visibility = View.VISIBLE
            binding.dataRecyclerView.visibility = View.GONE
        } else {
            binding.emptyText.visibility = View.GONE
            binding.dataRecyclerView.visibility = View.VISIBLE
            dataAdapter.updateData(dataList)
        }
        // Cuando se muestra la lista completa, ocultar recientes
        binding.recentDataRecyclerView.visibility = View.GONE
    }

    private fun loadRecentData() {
        val dataList = databaseHelper.getAllData()
        val recentList = if (dataList.size > 3) dataList.takeLast(3).reversed() else dataList.reversed()
        recentDataAdapter.updateData(recentList)
        binding.recentDataRecyclerView.visibility = if (recentList.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun clearAllData() {
        databaseHelper.clearAllData()
        Toast.makeText(this, getString(R.string.data_cleared), Toast.LENGTH_SHORT).show()
        loadData()
        loadRecentData()
    }

    private fun startHeartRateSimulation() {
        handler.post(object : Runnable {
            override fun run() {
                val simulatedHeartRate = Random.nextInt(heartRateMin, heartRateMax + 1)
                binding.heartRateValue.text = "$simulatedHeartRate bpm"
                handler.postDelayed(this, heartRateInterval)
            }
        })
    }

    private fun updateBluetoothStatus(isConnected: Boolean) {
        isBluetoothConnected = isConnected
        if (isConnected) {
            binding.connectionStatus.text = getString(R.string.connected)
        } else {
            binding.connectionStatus.text = getString(R.string.not_connected)
            sendBluetoothOffNotification()
        }
    }

    private fun sendBluetoothOffNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.bluetooth_off_notification))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Estado Bluetooth"
            val descriptionText = "Notificaciones de conexión Bluetooth"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        databaseHelper.close()
        unregisterReceiver(bluetoothReceiver)
        handler.removeCallbacksAndMessages(null)
        updateTimer?.cancel()
    }
} 