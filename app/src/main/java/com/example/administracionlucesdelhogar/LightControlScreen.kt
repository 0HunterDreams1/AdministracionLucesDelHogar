package com.example.administracionlucesdelhogar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun LightControlScreen() {
    val arduinoRepository = remember { ArduinoRepository() }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Control de Luces del Hogar", modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(16.dp))
        LightSwitch(lightName = "Luz de la Sala", lightId = "sala", arduinoRepository = arduinoRepository)
        LightSwitch(lightName = "Luz de la Cocina", lightId = "cocina", arduinoRepository = arduinoRepository)
        LightSwitch(lightName = "Luz del Dormitorio", lightId = "dormitorio", arduinoRepository = arduinoRepository)
    }
}

@Composable
fun LightSwitch(lightName: String, lightId: String, arduinoRepository: ArduinoRepository) {
    val isChecked = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = lightName, modifier = Modifier.weight(1f))
        Switch(
            checked = isChecked.value,
            onCheckedChange = {
                isChecked.value = it
                coroutineScope.launch {
                    if (isChecked.value) {
                        arduinoRepository.turnOn(lightId)
                    } else {
                        arduinoRepository.turnOff(lightId)
                    }
                }
            }
        )
    }
}
