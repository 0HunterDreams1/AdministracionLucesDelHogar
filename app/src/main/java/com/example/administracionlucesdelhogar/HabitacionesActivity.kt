package com.example.administracionlucesdelhogar

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import com.example.administracionlucesdelhogar.controladores.ControladorEscenarios
import com.example.administracionlucesdelhogar.controladores.ControladorHabitaciones
import com.example.administracionlucesdelhogar.modelos.Habitacion
import com.example.administracionlucesdelhogar.modelos.TipoHabitacion
import kotlin.math.roundToInt

@Suppress("DEPRECATION")
class HabitacionesActivity : AppCompatActivity() {
    private lateinit var controladorHabitaciones: ControladorHabitaciones

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
        cargarHabitacionesDinamico(gridLayout)

        val layoutHabitaciones = findViewById<LinearLayout>(R.id.layoutHabitaciones)
        val btnAgregar = findViewById<Button>(R.id.btnAgregarHabitacion)
        val btnEditar = findViewById<Button>(R.id.btnEditarHabitacion)
        val btnEliminar = findViewById<Button>(R.id.btnEliminarHabitacion)



        btnAgregar.setOnClickListener { v: View? ->
            mostrarDialogoAgregarHabitacion(
                gridLayout
            )
        }
        btnEditar.setOnClickListener { v: View? ->
            editarHabitacion(
                gridLayout
            )
        }
        btnEliminar.setOnClickListener { v: View? ->
            eliminarHabitacion(
                gridLayout
            )
        }
    }

    private fun cargarHabitacionesDinamico(gridLayout: GridLayout){
        val lista_habitaciones: ArrayList<Habitacion> = controladorHabitaciones.listaHabitaciones
        val layoutHabitaciones = findViewById<LinearLayout>(R.id.layoutHabitaciones)
        layoutHabitaciones.removeAllViews()
        if (lista_habitaciones.isNotEmpty()) {
            gridLayout.visibility = View.VISIBLE
            for (habitacion in lista_habitaciones) {
                val itemHabitacionView = layoutInflater.inflate(R.layout.item_habitacion, gridLayout, false)
                val textView = itemHabitacionView.findViewById<TextView>(R.id.textRoomName)
                val switchRoom = itemHabitacionView.findViewById<SwitchCompat>(R.id.switchRoom)
                val iconView = itemHabitacionView.findViewById<ImageView>(R.id.iconRoom)

                iconView.setImageResource(habitacion.tipoHabitacion)
                textView.text = "(" + habitacion.id + ") " + habitacion.nombre

                // Seteo el estado del switch de la habitación
                switchRoom.isChecked = habitacion.estado

                switchRoom.setOnCheckedChangeListener { _, isChecked ->
                    val estado = if (isChecked) "encendida" else "apagada"
                    controladorHabitaciones.actualizarEstado(habitacion, isChecked)
                    Toast.makeText(this, "${habitacion.nombre} está $estado", Toast.LENGTH_SHORT).show()
                }
                gridLayout.columnCount = 1

                val params = GridLayout.LayoutParams()
                params.width = GridLayout.LayoutParams.MATCH_PARENT
                params.height = GridLayout.LayoutParams.WRAP_CONTENT
                params.setMargins(0, 0, 0, 16)
                itemHabitacionView.layoutParams = params
                layoutHabitaciones.addView(itemHabitacionView)


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
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Agregar nueva habitación")

        // Contenedor vertical para los dos EditText
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

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
        val label = TextView(this)
        label.text = "Tipo de habitación"
        label.setTextSize(16f)
        label.setPadding(0, 16, 0, 8)
        layout.addView(label)

        val spinner = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresTipos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        layout.addView(spinner)

        val inputId = EditText(this)
        inputId.setHint("ID numérico (único)")
        inputId.setInputType(InputType.TYPE_CLASS_NUMBER)
        layout.addView(inputId)

        val inputNombre = EditText(this)
        inputNombre.setHint("Nombre de la habitación")
        inputNombre.setInputType(InputType.TYPE_CLASS_TEXT)
        layout.addView(inputNombre)

        builder.setView(layout)

        builder.setPositiveButton(
            "Agregar",
            DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                val idText = inputId.getText().toString().trim { it <= ' ' }
                val nombre = inputNombre.getText().toString().trim { it <= ' ' }

                if (idText.isEmpty() || nombre.isEmpty()) {
                    Toast.makeText(this, "Debes ingresar un ID y un nombre", Toast.LENGTH_SHORT)
                        .show()
                    return@OnClickListener
                }

                val id = idText.toInt()

                if (id < 1 || id > 10) {
                    Toast.makeText(this, "El ID debe ser un número entre 1 y 10", Toast.LENGTH_SHORT)
                        .show()
                    return@OnClickListener
                }

                // Verificar duplicados
                for (h in controladorHabitaciones.listaHabitaciones) {
                    if (h.id == id) {
                        Toast.makeText(
                            this,
                            "Ya existe una habitación con ese ID",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@OnClickListener
                    }
                    if (h.nombre.equals(nombre, ignoreCase = true)) {
                        Toast.makeText(
                            this,
                            "Ya existe una habitación con ese nombre",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@OnClickListener
                    }
                }
                val nueva = Habitacion(id, nombre, false, tipos[spinner.selectedItemPosition].iconoResId)
                controladorHabitaciones.agregarHabitacion(nueva)
                controladorHabitaciones.guardarCambios()
               /** cargarHabitaciones(layoutHabitaciones) */
                cargarHabitacionesDinamico(gridLayout)
                Toast.makeText(this, "Habitación $nombre agregada", Toast.LENGTH_SHORT)
                    .show()
            })

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
        val nombres = arrayOfNulls<String>(lista.size)
        for (i in lista.indices) {
            val h: Habitacion = lista[i]
            nombres[i] = h.id.toString() + " - " + h.nombre
        }
        val layoutHabitaciones = findViewById<LinearLayout>(R.id.layoutHabitaciones)
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

    private fun mostrarDialogoEditarHabitacion(habitacion: Habitacion,gridLayout: GridLayout) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Editar habitación")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val tipos = listOf(
            TipoHabitacion.Cocina,
            TipoHabitacion.Habitacion,
            TipoHabitacion.Baño,
            TipoHabitacion.Comedor,
            TipoHabitacion.Patio,
            TipoHabitacion.Living,
            TipoHabitacion.Garage
        )

        val nombresTipos = tipos.map {it.nombre }
        val label = TextView(this)
        label.text = "Tipo de habitación"
        label.setTextSize(16f)
        label.setPadding(0, 16, 0, 8)
        layout.addView(label)

        val spinner = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresTipos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        layout.addView(spinner)

        val iconBuscado = habitacion.tipoHabitacion // o el que corresponda
        val tipoEncontrado = tipos.find { it.iconoResId == iconBuscado }

        if (tipoEncontrado != null) {
            val posicion = tipos.indexOfFirst { it.iconoResId  == tipoEncontrado.iconoResId }
            Log.i("posicion",posicion.toString())
            if (posicion >= 0) {
                spinner.setSelection(posicion)
            }
        }

        val inputId = EditText(this)
        inputId.setHint("ID numérico (único)")
        inputId.setInputType(InputType.TYPE_CLASS_NUMBER)
        inputId.setText(habitacion.id.toString())
        layout.addView(inputId)

        val inputNombre = EditText(this)
        inputNombre.setHint("Nombre de la habitación")
        inputNombre.setInputType(InputType.TYPE_CLASS_TEXT)
        inputNombre.setText(habitacion.nombre)
        layout.addView(inputNombre)

        builder.setView(layout)

        builder.setPositiveButton(
            "Guardar cambios",
            DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                val idText = inputId.getText().toString().trim { it <= ' ' }
                val nombre = inputNombre.getText().toString().trim { it <= ' ' }

                if (idText.isEmpty() || nombre.isEmpty()) {
                    Toast.makeText(this, "Debes ingresar un ID y un nombre", Toast.LENGTH_SHORT)
                        .show()
                    return@OnClickListener
                }

                val nuevoId = idText.toInt()

                // Valido que el id sea un número entre 1 y 6
                if (nuevoId < 1 || nuevoId > 10) {
                    Toast.makeText(this, "El ID debe ser un número entre 1 y 10", Toast.LENGTH_SHORT)
                        .show()
                    return@OnClickListener
                }

                // Verificar duplicados (exceptuando la habitación actual)
                for (h in controladorHabitaciones.listaHabitaciones) {
                    if (h !== habitacion) {
                        if (h.id == nuevoId) {
                            Toast.makeText(
                                this,
                                "Ya existe una habitación con ese ID",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@OnClickListener
                        }
                        if (h.nombre.equals(nombre, ignoreCase = true)) {
                            Toast.makeText(
                                this,
                                "Ya existe una habitación con ese nombre",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@OnClickListener
                        }
                    }
                }

                // Actualizar datos
                habitacion.id = nuevoId
                habitacion.nombre = nombre
                habitacion.tipoHabitacion = tipos[spinner.selectedItemPosition].iconoResId
                controladorHabitaciones.guardarCambios()
                /** cargarHabitaciones(layoutHabitaciones) */
                cargarHabitacionesDinamico(gridLayout)
                Toast.makeText(this, "Habitación actualizada", Toast.LENGTH_SHORT).show()
            })

        builder.setNegativeButton(
            "Cancelar"
        ) { dialog: DialogInterface?, which: Int -> dialog!!.cancel() }
        builder.show()
    }

    private fun eliminarHabitacion(gridLayout: GridLayout) {
        val lista: ArrayList<Habitacion> = controladorHabitaciones.listaHabitaciones
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