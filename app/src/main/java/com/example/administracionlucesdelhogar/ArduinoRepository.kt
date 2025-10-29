package com.example.administracionlucesdelhogar

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get

class ArduinoRepository {

    private val client = HttpClient(CIO)

    suspend fun turnOn(lightId: String) {
        client.get("http://<your_arduino_ip>/on?lightId=$lightId")
    }

    suspend fun turnOff(lightId: String) {
        client.get("http://<your_arduino_ip>/off?lightId=$lightId")
    }
}
