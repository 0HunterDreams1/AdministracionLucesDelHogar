package com.example.administracionlucesdelhogar

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun LightControlScreen() {
    val arduinoRepository = remember { ArduinoRepository() }
    val isChecked = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Control de Luz", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Luz Principal", fontSize = 18.sp)
            Switch(
                checked = isChecked.value,
                onCheckedChange = {
                    isChecked.value = it
                    coroutineScope.launch {
                        try {
                            if (isChecked.value) {
                                arduinoRepository.turnOn("principal") // lightId no se usa, pero lo mantenemos por consistencia
                            } else {
                                arduinoRepository.turnOff("principal")
                            }
                        } catch (e: Exception) {
                            Log.e("LightControlScreen", "Error al comunicarse con el NodeMCU", e)
                        }
                    }
                }
            )
        }
    }
}
