package com.example.administracionlucesdelhogar

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

class ArduinoRepository {

    // Reemplaza esta dirección IP con la de tu NodeMCU
    // Puedes encontrarla en el monitor serie del IDE de Arduino cuando se conecta.
    private val nodeMcuIp = "192.168.100.21" // <- ¡CAMBIA ESTO!

    private val client = HttpClient(Android) // Usamos el motor de Android

    suspend fun turnOn(lightId: String) {
        // URL actualizada para que coincida con el código de tu NodeMCU
        val response = client.get("http://$nodeMcuIp/led/on")
        // Opcional: puedes imprimir la respuesta para depurar
        println("Respuesta de NodeMCU (turnOn): ${response.bodyAsText()}")
    }

    suspend fun turnOff(lightId: String) {
        // URL actualizada para que coincida con el código de tu NodeMCU
        val response = client.get("http://$nodeMcuIp/led/off")
        // Opcional: puedes imprimir la respuesta para depurar
        println("Respuesta de NodeMCU (turnOff): ${response.bodyAsText()}")
    }
}
