package com.example.administracionlucesdelhogar

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MenuItem
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.administracionlucesdelhogar.controladores.ControladorEscenarios
import com.example.administracionlucesdelhogar.controladores.ControladorHabitaciones
import com.example.administracionlucesdelhogar.modelos.Escenario
import com.example.administracionlucesdelhogar.modelos.Habitacion
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class EscenariosActivity : AppCompatActivity() {

    private lateinit var controladorEscenarios: ControladorEscenarios
    private lateinit var controladorHabitaciones: ControladorHabitaciones
    private val arduinoRepository = ArduinoRepository() // Repositorio para peticiones de red
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_escenarios)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v, insets ->
            val systemBars = insets!!.getInsets(WindowInsetsCompat.Type.systemBars())
            v!!.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)

        controladorEscenarios = ControladorEscenarios.getInstance(this)
        controladorHabitaciones = ControladorHabitaciones.getInstance(this)

        val layoutEscenarios = findViewById<LinearLayout>(R.id.layoutEscenarios)
        val btnAgregar = findViewById<Button>(R.id.btnAgregarEscenario)
        val btnEditar = findViewById<Button>(R.id.btnEditarEscenario)
        val btnEliminar = findViewById<Button>(R.id.btnEliminarEscenario)

        cargarEscenarios(layoutEscenarios)

        btnAgregar.setOnClickListener { agregarEditarEscenario(null, layoutEscenarios) }

//        btnAgregar.setOnClickListener { mostrarDialogoAgregarOEditarEscenario(null, layoutEscenarios) }
        btnEditar.setOnClickListener { seleccionarEscenarioParaEditar(layoutEscenarios) }
        btnEliminar.setOnClickListener { eliminarEscenario(layoutEscenarios) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("SetTextI18n")
    private fun cargarEscenarios(layoutEscenarios: LinearLayout) {
        layoutEscenarios.removeAllViews()

        val listaEscenarios = controladorEscenarios.listaEscenarios
        val todasHabitaciones = controladorHabitaciones.listaHabitaciones

        // Obtener el conjunto de códigos de habitaciones que están actualmente encendidas
        val habitacionesOnCodes = todasHabitaciones.filter { it.estado }.map { it.codigoHabitacion.toString() }.toSet()

        for ((index, escenario) in listaEscenarios.withIndex()) {
            // Lógica para determinar si un escenario está activo
            val escenarioCodes = escenario.habitaciones.map { it.codigoHabitacion.toString() }.toSet()
            val isScenarioActive = habitacionesOnCodes.isNotEmpty() && habitacionesOnCodes == escenarioCodes
            escenario.estado = isScenarioActive

            val itemEscenarioView = layoutInflater.inflate(R.layout.item_escenario, layoutEscenarios, false)
            val textEscenario = itemEscenarioView.findViewById<TextView>(R.id.textEscenario)
            val switchRoom = itemEscenarioView.findViewById<SwitchCompat>(R.id.switchEscenario)

            textEscenario.text = "(${escenario.id}) ${escenario.nombre} - ${escenario.habitaciones.size} habitaciones"
            val tvParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            textEscenario.layoutParams = tvParams

            switchRoom.isChecked = escenario.estado

            switchRoom.setOnCheckedChangeListener { buttonView, isChecked ->
                buttonView.isEnabled = false // Deshabilitar para evitar interacciones repetidas

                lifecycleScope.launch {
                    try {
                        if (isChecked) {
                            // --- ACTIVAR ESCENARIO ---
                            val codesToTurnOn = escenario.habitaciones.map { it.codigoHabitacion.toString() }


                            // Enviar peticiones a NodeMCU
                            if (codesToTurnOn.isNotEmpty()) arduinoRepository.turnOn(codesToTurnOn.joinToString(","))

                            // Si la red fue exitosa, actualizar el modelo y la UI
                            runOnUiThread {
                                listaEscenarios.forEachIndexed { i, other -> other.estado = (i == index) }
                                todasHabitaciones.forEach { h -> h.estado = codesToTurnOn.contains(h.codigoHabitacion.toString()) }
                                controladorEscenarios.guardarCambios()
                                controladorHabitaciones.guardarCambios()
                                Toast.makeText(this@EscenariosActivity, "Escenario '${escenario.nombre}' activado", Toast.LENGTH_SHORT).show()
                                cargarEscenarios(layoutEscenarios) // Recargar UI
                            }
                        } else {
                            // --- DESACTIVAR ESCENARIO ---
                            val codesToTurnOff = escenario.habitaciones.map { it.codigoHabitacion.toString() }

                            if (codesToTurnOff.isNotEmpty()) arduinoRepository.turnOff(codesToTurnOff.joinToString(","))

                            // Si la red fue exitosa, actualizar el modelo y la UI
                            runOnUiThread {
                                escenario.estado = false
                                escenario.habitaciones.forEach { h ->
                                    todasHabitaciones.find { it.id == h.id }?.estado = false
                                }
                                controladorEscenarios.guardarCambios()
                                controladorHabitaciones.guardarCambios()
                                Toast.makeText(this@EscenariosActivity, "Escenario '${escenario.nombre}' desactivado", Toast.LENGTH_SHORT).show()
                                cargarEscenarios(layoutEscenarios) // Recargar UI
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("EscenariosActivity", "Error de red al cambiar escenario", e)
                        // Si la red falla, mostrar error y recargar la UI para revertir el cambio
                        runOnUiThread {
                            Toast.makeText(this@EscenariosActivity, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                            cargarEscenarios(layoutEscenarios)
                        }
                    } finally {
                        // Volver a habilitar el switch después de la operación
                        runOnUiThread {
                            buttonView.isEnabled = true
                        }
                    }
                }
            }

//            layout.addView(tv)
//            layout.addView(switch)
            val params = GridLayout.LayoutParams()
            params.width = GridLayout.LayoutParams.MATCH_PARENT
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.setMargins(0, 0, 0, 16)
            itemEscenarioView.layoutParams = params
            layoutEscenarios.addView(itemEscenarioView)
        }
    }

    private fun agregarEditarEscenario(escenario: Escenario?, layoutEscenarios: LinearLayout){
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomEscenarioView = layoutInflater.inflate(R.layout.bottom_sheet_escenarios, null)

        val tvNombre = bottomEscenarioView.findViewById<TextView>(R.id.nombreEscenario)
        val inputId: Int = escenario?.id ?: controladorEscenarios.obtenerSiguienteId()

        tvNombre.hint = "Nombre del escenario"
        if (escenario != null) tvNombre.setText(escenario.nombre)

        // Agregar el listado de las habitaciones en el linearLayout (contenedorHabitaciones) dentro del scroll
        val habitaciones = controladorHabitaciones.listaHabitaciones
        if (habitaciones.isEmpty()) {
            Toast.makeText(this, "No hay habitaciones disponibles", Toast.LENGTH_SHORT).show()
            return
        }

        val contenedorHabitacionesView = bottomEscenarioView.findViewById<LinearLayout>(R.id.contenedorHabitaciones)

        val nombres = habitaciones.map { it.nombre }.toTypedArray()

        //Mostrar el listado de habitaciones
        habitaciones.forEach { habitacion  ->
            val checkBox = CheckBox(this).apply {
                text = habitacion.nombre
                isChecked = escenario?.habitaciones?.any { it.id == habitacion.id } == true

            }
            contenedorHabitacionesView.addView(checkBox)
        }
        val botonGuardar = bottomEscenarioView.findViewById<Button>(R.id.botonGuardar)

        botonGuardar.setOnClickListener {
            val nombre = tvNombre.text.toString().trim()

            if (nombre.isEmpty()) {
                Toast.makeText(this, "Debes ingresar un nombre para el escenario", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Obtener habitaciones seleccionadas
            val habitacionesSeleccionadas = mutableListOf<Habitacion>()
            for (i in 0 until contenedorHabitacionesView.childCount) {
                val view = contenedorHabitacionesView.getChildAt(i)
                if (view is CheckBox && view.isChecked) {
                    val nombreHabitacion = view.text.toString()
                    val habitacion = habitaciones.find { it.nombre == nombreHabitacion }
                    if (habitacion != null) {
                        habitacionesSeleccionadas.add(habitacion)
                    }
                }
            }
            Log.i("habitacionesEscenarioos",  ArrayList(habitacionesSeleccionadas).toString())

            if (habitacionesSeleccionadas.isEmpty()) {
                Toast.makeText(this, "Seleccioná al menos una habitación", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (escenario != null) {
                // EDITAR: reemplazar los datos del escenario existente
                escenario.id = inputId
                escenario.nombre = nombre
                escenario.habitaciones =  ArrayList(habitacionesSeleccionadas)
                controladorEscenarios.guardarCambios()
                Toast.makeText(this, "Escenario $nombre modificado", Toast.LENGTH_SHORT).show()
            } else {
                // AGREGAR: crear nuevo escenario
                val nuevoEscenario = Escenario(inputId, nombre,  ArrayList(habitacionesSeleccionadas), false)
                controladorEscenarios.agregarEscenario(nuevoEscenario)
                Toast.makeText(this, "Escenario $nombre agregado", Toast.LENGTH_SHORT).show()
            }

            bottomSheetDialog.dismiss()

            // Si querés actualizar la UI principal:
            cargarEscenarios(layoutEscenarios)
        }

        bottomSheetDialog.setContentView(bottomEscenarioView)
        bottomSheetDialog.show()
    }
    // FUNCION UNIFICADA: agregar o editar
    private fun mostrarDialogoAgregarOEditarEscenario(
        escenario: Escenario?,
        layoutEscenarios: LinearLayout
    ) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(if (escenario == null) "Agregar nuevo escenario" else "Editar escenario")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val inputId: Int = escenario?.id ?: controladorEscenarios.obtenerSiguienteId()

        val inputNombre = EditText(this)
        inputNombre.hint = "Nombre del escenario"
        if (escenario != null) inputNombre.setText(escenario.nombre)
        layout.addView(inputNombre)

        builder.setView(layout)

        builder.setPositiveButton("Siguiente") { _, _ ->
            val nombre = inputNombre.text.toString().trim()

            if (nombre.isEmpty()) {
                Toast.makeText(this, "Debes ingresar un ID y un nombre", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val id = inputId.toInt()
            // Validar que no exista otro escenario con el mismo ID o nombre
            for (e in controladorEscenarios.listaEscenarios) {
                if (escenario != null && e == escenario) continue
                if (e.id == id || e.nombre.equals(nombre, ignoreCase = true)) {
                    Toast.makeText(this, "Ya existe un escenario con ese ID o nombre", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
            }

            mostrarDialogoSeleccionarHabitaciones(id, nombre, escenario, layoutEscenarios)
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun mostrarDialogoSeleccionarHabitaciones(
        id: Int,
        nombre: String,
        escenarioExistente: Escenario?,
        layoutEscenarios: LinearLayout
    ) {
        val habitaciones = controladorHabitaciones.listaHabitaciones
        if (habitaciones.isEmpty()) {
            Toast.makeText(this, "No hay habitaciones disponibles", Toast.LENGTH_SHORT).show()
            return
        }

        val nombres = habitaciones.map { it.nombre }.toTypedArray()
        val seleccionadas = BooleanArray(habitaciones.size) { i ->
            escenarioExistente?.habitaciones?.any { it.id == habitaciones[i].id } == true
        }

        AlertDialog.Builder(this)
            .setTitle("Selecciona habitaciones para el escenario")
            .setMultiChoiceItems(nombres, seleccionadas) { _, which, isChecked ->
                seleccionadas[which] = isChecked
            }
            .setPositiveButton("Guardar") { _, _ ->
                val seleccionadasList = ArrayList<Habitacion>()
                for (i in seleccionadas.indices) {
                    if (seleccionadas[i]) seleccionadasList.add(habitaciones[i])
                }

                if (escenarioExistente != null) {
                    // EDITAR: reemplazar los datos del escenario existente
                    escenarioExistente.id = id
                    escenarioExistente.nombre = nombre
                    escenarioExistente.habitaciones = seleccionadasList
                    controladorEscenarios.guardarCambios()
                    Toast.makeText(this, "Escenario $nombre modificado", Toast.LENGTH_SHORT).show()
                } else {
                    // AGREGAR: crear nuevo escenario
                    val nuevoEscenario = Escenario(id, nombre, seleccionadasList, false)
                    controladorEscenarios.agregarEscenario(nuevoEscenario)
                    Toast.makeText(this, "Escenario $nombre agregado", Toast.LENGTH_SHORT).show()
                }

                cargarEscenarios(layoutEscenarios)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun seleccionarEscenarioParaEditar(layoutEscenarios: LinearLayout) {
        val lista = controladorEscenarios.listaEscenarios
        if (lista.isEmpty()) {
            Toast.makeText(this, "No hay escenarios para editar", Toast.LENGTH_SHORT).show()
            return
        }

        val nombres = lista.map { "(${it.id}) ${it.nombre}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Selecciona un escenario para editar")
            .setItems(nombres) { _, which ->
                val escenario = lista[which]
                agregarEditarEscenario(escenario, layoutEscenarios)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarEscenario(layoutEscenarios: LinearLayout) {
        val lista = controladorEscenarios.listaEscenarios
        if (lista.isEmpty()) {
            Toast.makeText(this, "No hay escenarios para eliminar", Toast.LENGTH_SHORT).show()
            return
        }

        val nombres = lista.map { it.nombre }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Eliminar escenario")
            .setItems(nombres) { _, which ->
                val seleccionado = lista[which]

                // Confirmación antes de eliminar
                AlertDialog.Builder(this)
                    .setTitle("Confirmar eliminación")
                    .setMessage("¿Desea eliminar el escenario \"${seleccionado.nombre}\"?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        controladorEscenarios.eliminarEscenario(seleccionado)
                        cargarEscenarios(layoutEscenarios)
                        Toast.makeText(
                            this,
                            "Escenario \"${seleccionado.nombre}\" eliminado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}