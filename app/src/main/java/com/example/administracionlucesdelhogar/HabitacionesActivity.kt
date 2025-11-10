package com.example.administracionlucesdelhogar

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.CompoundButton
import android.widget.Spinner
import androidx.appcompat.widget.SwitchCompat
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.administracionlucesdelhogar.controladores.ControladorEscenarios
import com.example.administracionlucesdelhogar.controladores.ControladorHabitaciones
import com.example.administracionlucesdelhogar.modelos.CodigoHabitacion
import com.example.administracionlucesdelhogar.modelos.Habitacion
import com.example.administracionlucesdelhogar.modelos.TipoHabitacion
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.util.network.UnresolvedAddressException

@Suppress("DEPRECATION")
class HabitacionesActivity : AppCompatActivity() {
    private lateinit var controladorHabitaciones: ControladorHabitaciones
    private val arduinoRepository = ArduinoRepository() // Instancia del repositorio

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_habitaciones)
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v: View?, insets: WindowInsetsCompat? ->
            val systemBars = insets!!.getInsets(WindowInsetsCompat.Type.systemBars())
            v!!.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Manejo la barra de acciones
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        toolbar.navigationIcon!!.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)

        // Referencio al controlador de habitaciones
        controladorHabitaciones = ControladorHabitaciones.getInstance(this)

        val gridLayout = findViewById<GridLayout>(R.id.roomGrid)
        // Cargo las habitaciones guardadas
        cargarHabitacionesDinamico(gridLayout)

        // Informo la cantidad de posibles habitaciones a cargar
        val txtSecundario = findViewById<TextView>(R.id.txtSecundario)
        txtSecundario.text = "* Puede agregar hasta ${CodigoHabitacion.entries.size} habitaciones."

        val btnAgregar = findViewById<Button>(R.id.btnAgregarHabitacion)
        val btnEditar = findViewById<Button>(R.id.btnEditarHabitacion)
        val btnEliminar = findViewById<Button>(R.id.btnEliminarHabitacion)

        btnAgregar.setOnClickListener { mostrarDialogoAgregarHabitacion(gridLayout) }
        btnEditar.setOnClickListener { editarHabitacion(gridLayout) }
        btnEliminar.setOnClickListener { eliminarHabitacion(gridLayout) }
    }

    // 1) Metodo que maneja el cambio del switch (firma requerida por setOnCheckedChangeListener)
    fun onSwitchChanged(button: CompoundButton, isChecked: Boolean) {
        val switchRoom = button as? SwitchCompat ?: return
        val habitacion = switchRoom.tag as? Habitacion ?: return

        // bloquear interacción mientras se procesa
        switchRoom.isEnabled = false

        lifecycleScope.launch {
            try {
                // Ejecutar la llamada de red en IO
                withContext(Dispatchers.IO) {
                    if (isChecked) {
                        arduinoRepository.turnOn(habitacion.codigoHabitacion.toString())
                    } else {
                        arduinoRepository.turnOff(habitacion.codigoHabitacion.toString())
                    }
                }

                // actualizar modelo solo si la petición fue exitosa
                controladorHabitaciones.actualizarEstado(habitacion, isChecked)
                val estado = if (isChecked) "encendida" else "apagada"
                Toast.makeText(this@HabitacionesActivity, "${habitacion.nombre} está $estado", Toast.LENGTH_SHORT).show()
            } catch (e: ClientRequestException) {
                // 4xx
                Toast.makeText(this@HabitacionesActivity, "Error en la petición: ${e.message}", Toast.LENGTH_LONG).show()
                revertSwitch(switchRoom, isChecked, habitacion)
            } catch (e: ServerResponseException) {
                // 5xx
                Toast.makeText(this@HabitacionesActivity, "Error de servidor: ${e.message}", Toast.LENGTH_LONG).show()
                revertSwitch(switchRoom, isChecked, habitacion)
            } catch (e: UnresolvedAddressException) {
                Toast.makeText(this@HabitacionesActivity, "No se encuentra la dirección: ${e.message}", Toast.LENGTH_LONG).show()
                revertSwitch(switchRoom, isChecked, habitacion)
            } catch (e: Exception) {
                Toast.makeText(this@HabitacionesActivity, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                revertSwitch(switchRoom, isChecked, habitacion)
            } finally {
                switchRoom.isEnabled = true
            }
        }
    }

    // Helper para revertir el switch sin reentradas
    private fun revertSwitch(switchRoom: SwitchCompat, attemptedChecked: Boolean, habitacion: Habitacion) {
        // quitar listener antes de cambiar el estado
        switchRoom.setOnCheckedChangeListener(null)
        switchRoom.isChecked = !attemptedChecked
        controladorHabitaciones.actualizarEstado(habitacion, !attemptedChecked)
        // reasignar listener
        switchRoom.setOnCheckedChangeListener(this@HabitacionesActivity::onSwitchChanged)
    }
    private fun cargarHabitacionesDinamico(gridLayout: GridLayout){
        val listaHabitaciones: ArrayList<Habitacion> = controladorHabitaciones.listaHabitaciones
        
        // BUG FIX: Limpiar el GridLayout directamente en lugar del LinearLayout incorrecto
        gridLayout.removeAllViews()

        // Verifico si tengo habitaciones cargadas
        if (listaHabitaciones.isNotEmpty()) {
            gridLayout.visibility = View.VISIBLE
            for (habitacion in listaHabitaciones) {
                val itemHabitacionView = layoutInflater.inflate(R.layout.item_habitacion, gridLayout, false)
                val textView = itemHabitacionView.findViewById<TextView>(R.id.textRoomName)
                val switchRoom = itemHabitacionView.findViewById<SwitchCompat>(R.id.switchRoom)
                val iconView = itemHabitacionView.findViewById<ImageView>(R.id.iconRoom)

                // Dentro de cargarHabitacionesDinamico, por cada habitacion al inflar itemHabitacionView:
                iconView.setImageResource(habitacion.tipoHabitacion)
                textView.text = "(${habitacion.id}) ${habitacion.nombre} (${habitacion.getNombreCodigoHabitacion()})"

                // asociar habitacion al switch para recuperarla luego en onSwitchChanged
                switchRoom.tag = habitacion

                // setear estado sin disparar el listener (asegurar estado inicial)
                switchRoom.setOnCheckedChangeListener(null)
                switchRoom.isChecked = habitacion.estado
                // ahora asignar el listener por referencia al metodo definido arriba
                switchRoom.setOnCheckedChangeListener(this@HabitacionesActivity::onSwitchChanged)
                gridLayout.columnCount = 1

                val params = GridLayout.LayoutParams()
                params.width = GridLayout.LayoutParams.MATCH_PARENT
                params.height = GridLayout.LayoutParams.WRAP_CONTENT
                params.setMargins(0, 0, 0, 16)
                itemHabitacionView.layoutParams = params
                gridLayout.addView(itemHabitacionView)
            }
        } else {
            gridLayout.visibility = View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun mostrarDialogoAgregarHabitacion(gridLayout: GridLayout) {
        // Verifico si quedan códigos de habitación no usados
        if (!controladorHabitaciones.puedoCargarHabitacion()) {
            Toast.makeText(this, "Ya se llegó al límite de habitaciones cargadas.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Agregar nueva habitación")

        // Contenedor vertical para los dos EditText
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        // Id de habitación
        val inputId = controladorHabitaciones.obtenerSiguienteId().toString()
        val lblIdHabitacion = TextView(this)
        lblIdHabitacion.text = "Id: ${inputId}"
        lblIdHabitacion.textSize = 16f
        lblIdHabitacion.setPadding(0, 16, 0, 8)
        layout.addView(lblIdHabitacion)

        // Armo la colección de tipos de habitaciones posibles
        val tipos = listOf(
            TipoHabitacion.Cocina,
            TipoHabitacion.Habitacion,
            TipoHabitacion.Baño,
            TipoHabitacion.Comedor,
            TipoHabitacion.Patio,
            TipoHabitacion.Living,
            TipoHabitacion.Garage
        )
        val nombresTipos = tipos.map { it.nombre }

        // Tipo de habitación
        val lblTipoHabitacion = TextView(this)
        lblTipoHabitacion.text = "Tipo de habitación:"
        lblTipoHabitacion.textSize = 16f
        lblTipoHabitacion.setPadding(0, 16, 0, 8)
        layout.addView(lblTipoHabitacion)
        val spinner = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresTipos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        layout.addView(spinner)

        // Nombre de habitación
        val lblNombre = TextView(this)
        lblNombre.text = "Nombre:"
        lblNombre.textSize = 16f
        lblNombre.setPadding(0, 16, 0, 8)
        layout.addView(lblNombre)
        val inputNombre = EditText(this)
        inputNombre.setHint("Ej: Cocina")
        inputNombre.setInputType(InputType.TYPE_CLASS_TEXT)
        layout.addView(inputNombre)

        // Código de habitación
        val lblCodigoHabitacion = TextView(this)
        lblCodigoHabitacion.text = "Código de habitación:"
        lblCodigoHabitacion.textSize = 16f
        lblCodigoHabitacion.setPadding(0, 16, 0, 8)
        layout.addView(lblCodigoHabitacion)
        // Obtengo los distintos códigos de habitación disponibles
        val codigosHabitacion = CodigoHabitacion.entries
        val codigosSpinnerH = mutableListOf<CodigoHabitacion>()
        for (auxCodigoHabitacion in codigosHabitacion) {
            var codigoEnUso = false
            for (h in controladorHabitaciones.listaHabitaciones) {
                if (auxCodigoHabitacion.codigo == h.codigoHabitacion) {
                    codigoEnUso = true
                }
            }
            if (!codigoEnUso) {
                codigosSpinnerH.add(auxCodigoHabitacion)
            }
        }
        val spinnerH = Spinner(this)
        val adapterH = ArrayAdapter(this, android.R.layout.simple_spinner_item, codigosSpinnerH)
        adapterH.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerH.adapter = adapterH
        layout.addView(spinnerH)

        builder.setView(layout)

        // Defino el botón de confirmación de agregado de habitación
        builder.setPositiveButton(
            "Agregar",
            DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                val id = inputId.toInt()
                val nombre = inputNombre.getText().toString().trim { it <= ' ' }

                // Verifico si el usuario escribió un nombre de habitación
                if (nombre.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Debe ingresar un nombre para la habitación",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    return@OnClickListener
                }

                // Obtengo el código de habitación seleccionado
                val codigoSeleccionado =  spinnerH.getItemAtPosition(spinnerH.selectedItemPosition) as CodigoHabitacion

                // Verificar duplicados
                for (h in controladorHabitaciones.listaHabitaciones) {
                    if (h.nombre.equals(nombre, ignoreCase = true)) {
                        Toast.makeText(
                            this,
                            "Ya existe una habitación con ese nombre",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@OnClickListener
                    }
                }

                // Creo la nueva habitación y la guardo
                val auxNuevaHabitacion = Habitacion(id, nombre, false, tipos[spinner.selectedItemPosition].iconoResId, codigoSeleccionado.codigo)
                controladorHabitaciones.agregarHabitacion(auxNuevaHabitacion)
                controladorHabitaciones.guardarCambios()
                // Recargo las habitaciones
                cargarHabitacionesDinamico(gridLayout)
                Toast.makeText(this, "Habitación \"$nombre\" agregada", Toast.LENGTH_SHORT)
                    .show()
            })
        // Defino el botón de cancelación
        builder.setNegativeButton(
            "Cancelar"
        ) { dialog: DialogInterface?, which: Int -> dialog!!.cancel() }
        builder.show()
    }

    private fun editarHabitacion(gridLayout: GridLayout) {
        val lista: ArrayList<Habitacion> = controladorHabitaciones.listaHabitaciones
        if (lista.isEmpty()) {
            Toast.makeText(this, "No hay habitaciones para editar", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar lista de habitaciones para elegir cuál editar
        val nombres = Array(lista.size) { i -> "${lista[i].id} - ${lista[i].nombre}" }
        AlertDialog.Builder(this)
            .setTitle("Selecciona una habitación para editar")
            .setItems(nombres) { _, which ->
                mostrarDialogoEditarHabitacion(lista[which], gridLayout)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEditarHabitacion(habitacion: Habitacion, gridLayout: GridLayout) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Editar habitación")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val inputNombre = EditText(this)
        inputNombre.hint = "Nombre de la habitación"
        inputNombre.inputType = InputType.TYPE_CLASS_TEXT
        inputNombre.setText(habitacion.nombre)
        layout.addView(inputNombre)

        builder.setView(layout)

        builder.setPositiveButton("Guardar cambios") { _, _ ->
            val nombre = inputNombre.text.toString().trim()

            if (nombre.isEmpty()) {
                Toast.makeText(this, "Debe ingresar un nombre", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            if (controladorHabitaciones.listaHabitaciones.any { it !== habitacion && it.nombre.equals(nombre, ignoreCase = true) }) {
                Toast.makeText(this, "Ya existe otra habitación con ese nombre", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            habitacion.nombre = nombre
            controladorHabitaciones.guardarCambios()
            cargarHabitacionesDinamico(gridLayout)
            Toast.makeText(this, "Habitación actualizada", Toast.LENGTH_SHORT).show()
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun eliminarHabitacion(gridLayout: GridLayout) {
        val lista: ArrayList<Habitacion> = controladorHabitaciones.listaHabitaciones
        if (lista.isEmpty()) {
            Toast.makeText(this, "No hay habitaciones para eliminar", Toast.LENGTH_SHORT).show()
            return
        }

        val nombres = Array(lista.size) { i -> "${lista[i].id} - ${lista[i].nombre}" }

        AlertDialog.Builder(this)
            .setTitle("Selecciona una habitación para eliminar")
            .setItems(nombres) { _, which ->
                val habitacionAEliminar = lista[which]

                AlertDialog.Builder(this@HabitacionesActivity)
                    .setTitle("Confirmar eliminación")
                    .setMessage("¿Estás seguro de que deseas eliminar la habitación '${habitacionAEliminar.nombre}'?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        controladorHabitaciones.eliminarHabitacion(habitacionAEliminar)
                        cargarHabitacionesDinamico(gridLayout)
                        Toast.makeText(this@HabitacionesActivity, "Habitación eliminada", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
