package com.example.administracionlucesdelhogar

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class ArduinoRepository {

    private val nodeMcuIp = "192.168.100.21"

    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 20_000
            socketTimeoutMillis = 20_000
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("Ktor", message)
                }
            }
            level = LogLevel.INFO
        }
    }

    // Llama y no lee el body
    suspend fun turnOn(lightCode: String) {
        val url = "http://$nodeMcuIp/led/on?ids=$lightCode"
        Log.d("ArduinoRepository", "Llamando a: $url")
        try {
            withContext(Dispatchers.IO) {
                client.get(url)
            }
            Log.d("ArduinoRepository", "Petición (turnOn) enviada.")
        } catch (e: Exception) {
            Log.e("ArduinoRepository", "Error en turnOn", e)
            throw e
        }
    }

    suspend fun turnOff(lightCode: String) {
        val url = "http://$nodeMcuIp/led/off?ids=$lightCode"
        Log.d("ArduinoRepository", "Llamando a: $url")
        try {
            withContext(Dispatchers.IO) {
                client.get(url)
            }
            Log.d("ArduinoRepository", "Petición (turnOff) enviada.")
        } catch (e: Exception) {
            Log.e("ArduinoRepository", "Error en turnOff", e)
            throw e
        }
    }

    // Método alternativo usando HttpURLConnection para diagnóstico/comparación
    /*suspend fun pingWithHttpUrl(path: String = ""): Int = withContext(Dispatchers.IO) {
        val urlStr = "http://$nodeMcuIp$path"
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            requestMethod = "GET"
        }
        try {
            conn.connect()
            conn.responseCode
        } finally {
            conn.disconnect()
        }
    }*/

    fun close() {
        client.close()
    }
}
