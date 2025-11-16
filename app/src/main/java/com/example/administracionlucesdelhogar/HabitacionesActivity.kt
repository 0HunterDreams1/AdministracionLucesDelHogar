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
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
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
                Toast.makeText(this@HabitacionesActivity, "Habitación \"${habitacion.nombre}\" $estado", Toast.LENGTH_SHORT).show()
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

        if (!controladorHabitaciones.puedoCargarHabitacion()) {
            Toast.makeText(this, "Ya se llegó al límite de habitaciones cargadas.", Toast.LENGTH_SHORT).show()
            return
        }

        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_habitaciones, null)
        bottomSheet.setContentView(view)

        val lblIdHabitacion = view.findViewById<TextView>(R.id.lblIdHabitacion)
        val spinnerTipo = view.findViewById<Spinner>(R.id.spinnerTipoHabitacion)
        val spinnerCodigo = view.findViewById<Spinner>(R.id.spinnerCodigoHabitacion)
        val inputNombre = view.findViewById<EditText>(R.id.inputNombre)
        val btnAgregar = view.findViewById<Button>(R.id.btnAgregar)

        // ID
        val inputId = controladorHabitaciones.obtenerSiguienteId()
        lblIdHabitacion.text = "Id: $inputId"

        // Tipos de habitación
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

        val adapterTipos = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresTipos)
        adapterTipos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipo.adapter = adapterTipos

        // Códigos disponibles
        val codigosDisponibles = CodigoHabitacion.entries.filter { codigo ->
            controladorHabitaciones.listaHabitaciones.none { it.codigoHabitacion == codigo.codigo }
        }

        val adapterCodigos = ArrayAdapter(this, android.R.layout.simple_spinner_item, codigosDisponibles)
        adapterCodigos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCodigo.adapter = adapterCodigos

        btnAgregar.setOnClickListener {
            val nombre = inputNombre.text.toString().trim()

            if (nombre.isEmpty()) {
                Toast.makeText(this, "Debe ingresar un nombre", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar duplicado
            if (controladorHabitaciones.listaHabitaciones.any { it.nombre.equals(nombre, ignoreCase = true) }) {
                Toast.makeText(this, "Ya existe una habitación con ese nombre", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val codigoSeleccionado = spinnerCodigo.selectedItem as CodigoHabitacion
            val tipoSeleccionado = tipos[spinnerTipo.selectedItemPosition]

            val nuevaHabitacion = Habitacion(
                inputId,
                nombre,
                false,
                tipoSeleccionado.iconoResId,
                codigoSeleccionado.codigo
            )

            controladorHabitaciones.agregarHabitacion(nuevaHabitacion)
            controladorHabitaciones.guardarCambios()
            cargarHabitacionesDinamico(gridLayout)
            Toast.makeText(this, "Habitación \"$nombre\" agregada", Toast.LENGTH_SHORT).show()

            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }

    private fun editarHabitacion(gridLayout: GridLayout) {
        val lista: ArrayList<Habitacion> = controladorHabitaciones.listaHabitaciones
        if (lista.isEmpty()) {
            Toast.makeText(this, "No hay habitaciones para editar", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar lista de habitaciones para elegir cuál editar
        val nombres = arrayOfNulls<String>(lista.size)
        for (i in lista.indices) {
            val h: Habitacion = lista[i]
            nombres[i] = h.id.toString() + " - " + h.nombre
        }
        AlertDialog.Builder(this)
            .setTitle("Selecciona una habitación para editar")
            .setItems(
                nombres
            ) { dialog: DialogInterface?, which: Int ->
                mostrarDialogoEditarHabitacion(
                    lista[which],
                    gridLayout
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEditarHabitacion(habitacion: Habitacion, gridLayout: GridLayout) {

        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_habitaciones, null)
        bottomSheet.setContentView(view)

        val lblIdHabitacion = view.findViewById<TextView>(R.id.lblIdHabitacion)
        val spinnerTipo = view.findViewById<Spinner>(R.id.spinnerTipoHabitacion)
        val spinnerCodigo = view.findViewById<Spinner>(R.id.spinnerCodigoHabitacion)
        val inputNombre = view.findViewById<EditText>(R.id.inputNombre)
        val btnAgregar = view.findViewById<Button>(R.id.btnAgregar)

        // Cambiar texto del botón
        btnAgregar.text = "Guardar cambios"

        // Mostrar ID existente
        lblIdHabitacion.text = "Id: ${habitacion.id}"

        // Tipos de habitación
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

        val adapterTipos = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresTipos)
        adapterTipos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipo.adapter = adapterTipos

        // Seleccionar el tipo actual
        val tipoEncontrado = tipos.indexOfFirst { it.iconoResId == habitacion.tipoHabitacion }
        if (tipoEncontrado != -1) spinnerTipo.setSelection(tipoEncontrado)

        // Nombre actual
        inputNombre.setText(habitacion.nombre)

        // Códigos disponibles (evita el duplicado excepto el suyo propio)
        val codigosDisponibles = CodigoHabitacion.entries.filter { codigo ->
            controladorHabitaciones.listaHabitaciones.none {
                it.codigoHabitacion == codigo.codigo && it.id != habitacion.id
            }
        }

        val adapterCodigos = ArrayAdapter(this, android.R.layout.simple_spinner_item, codigosDisponibles)
        adapterCodigos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCodigo.adapter = adapterCodigos

        // Seleccionar código actual
        val idxCodigo = codigosDisponibles.indexOfFirst { it.codigo == habitacion.codigoHabitacion }
        if (idxCodigo != -1) spinnerCodigo.setSelection(idxCodigo)

        // Guardar cambios
        btnAgregar.setOnClickListener {

            val nombre = inputNombre.text.toString().trim()

            if (nombre.isEmpty()) {
                Toast.makeText(this, "Debe ingresar un nombre", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar duplicado excepto él mismo
            if (controladorHabitaciones.listaHabitaciones.any {
                    it.nombre.equals(nombre, ignoreCase = true) && it.id != habitacion.id
                }) {
                Toast.makeText(this, "Ya existe una habitación con ese nombre", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Actualizar datos
            habitacion.nombre = nombre
            habitacion.tipoHabitacion = tipos[spinnerTipo.selectedItemPosition].iconoResId
            val codigoSeleccionado = spinnerCodigo.selectedItem as CodigoHabitacion
            habitacion.codigoHabitacion = codigoSeleccionado.codigo

            controladorHabitaciones.guardarCambios()
            cargarHabitacionesDinamico(gridLayout)

            Toast.makeText(this, "Habitación \"${habitacion.nombre}\" actualizada", Toast.LENGTH_SHORT).show()
            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }


    private fun eliminarHabitacion(gridLayout: GridLayout) {
        val lista: ArrayList<Habitacion> = controladorHabitaciones.listaHabitaciones
        // Verifico si exiten habitaciones para eliminar
        if (lista.isEmpty()) {
            Toast.makeText(this, "No hay habitaciones para eliminar", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear lista de nombres para el diálogo
        val nombres = arrayOfNulls<String>(lista.size)
        for (i in lista.indices) {
            nombres[i] = lista[i].nombre
        }

        AlertDialog.Builder(this)
            .setTitle("Eliminar habitación")
            .setItems(nombres) { dialog: DialogInterface?, which: Int ->
                val seleccionada: Habitacion = lista[which]

                // Verificar si la habitación está en algún escenario
                val controladorEscenarios = ControladorEscenarios.getInstance(this)
                val escenariosConHabitacion = controladorEscenarios.listaEscenarios.filter { escenario ->
                    escenario.habitaciones.any { it.id == seleccionada.id }
                }
                // Existe al menos un escenario que contiene a la habitación
                if (escenariosConHabitacion.isNotEmpty()) {
                    // Construyo una lista de nombres de los escenarios
                    val nombresEscenarios = escenariosConHabitacion.joinToString(", ") { it.nombre ?: "Sin nombre" }

                    AlertDialog.Builder(this)
                        .setTitle("Confirmar eliminación")
                        .setMessage(
                            "La habitación \"${seleccionada.nombre}\" está incluida en los escenarios:\n\n" +
                                    nombresEscenarios +
                                    "\n\n¿Desea eliminarla? Se actualizarán los escenarios mencionados."
                        )
                        .setPositiveButton("Eliminar") { _, _ ->
                            // Eliminar la habitación de los escenarios
                            for (escenario in escenariosConHabitacion) {
                                escenario.habitaciones.removeAll { it.id == seleccionada.id }
                            }
                            controladorEscenarios.guardarCambios()

                            // Eliminar definitivamente la habitación
                            controladorHabitaciones.eliminarHabitacion(seleccionada)

                            // Refrescar la vista
                            cargarHabitacionesDinamico(gridLayout)

                            Toast.makeText(
                                this,
                                "Habitación \"${seleccionada.nombre}\" eliminada",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                } else {
                    // No está en ningún escenario, eliminar directamente
                    AlertDialog.Builder(this)
                        .setTitle("Confirmar eliminación")
                        .setMessage("¿Desea eliminar la habitación \"${seleccionada.nombre}\"?")
                        .setPositiveButton("Eliminar") { _, _ ->
                            controladorHabitaciones.eliminarHabitacion(seleccionada)
                            cargarHabitacionesDinamico(gridLayout)
                            Toast.makeText(this, "Habitación \"${seleccionada.nombre}\" eliminada", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}