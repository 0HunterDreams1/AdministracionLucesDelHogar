package com.example.administracionlucesdelhogar

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.InputType
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.administracionlucesdelhogar.controladores.ControladorHabitaciones
import com.example.administracionlucesdelhogar.modelos.Habitacion
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

        val layoutHabitaciones = findViewById<LinearLayout>(R.id.layoutHabitaciones)
        val btnAgregar = findViewById<Button>(R.id.btnAgregarHabitacion)
        val btnEditar = findViewById<Button>(R.id.btnEditarHabitacion)
        val btnEliminar = findViewById<Button>(R.id.btnEliminarHabitacion)

        cargarHabitaciones(layoutHabitaciones)

        btnAgregar.setOnClickListener { v: View? ->
            mostrarDialogoAgregarHabitacion(
                layoutHabitaciones
            )
        }
        btnEditar.setOnClickListener { v: View? ->
            editarHabitacion(
                layoutHabitaciones
            )
        }
        btnEliminar.setOnClickListener { v: View? ->
            eliminarHabitacion(
                layoutHabitaciones
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun cargarHabitaciones(layoutHabitaciones: LinearLayout) {
        layoutHabitaciones.removeAllViews()
        val lista: ArrayList<Habitacion> = controladorHabitaciones.listaHabitaciones

        for (h in lista) {
            val sw = Switch(this)
            val txtHabitacion = "(" + h.id + ") " + h.nombre
            sw.text = txtHabitacion
            sw.setChecked(h.estado)
            // Aplico margenes a los switchs
            val params =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            // Convierto los 20dp en pixeles
            val densidad = getResources().displayMetrics.density
            val margenLateral = (20 * densidad).roundToInt()
            val margenVertical = (10 * densidad).roundToInt()
            // Aplicar los márgenes izquierdo y derecho
            params.setMargins(margenLateral, margenVertical, margenLateral, margenVertical)
            sw.setLayoutParams(params)
            // Fin de aplicaión de margenes
            sw.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                h.estado = isChecked
                controladorHabitaciones.actualizarEstado(h, isChecked)
                Toast.makeText(
                    this,
                    h.nombre + (if (isChecked) " encendida" else " apagada"),
                    Toast.LENGTH_SHORT
                ).show()
            }
            layoutHabitaciones.addView(sw)
        }
    }

    private fun mostrarDialogoAgregarHabitacion(layoutHabitaciones: LinearLayout) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Agregar nueva habitación")

        // Contenedor vertical para los dos EditText
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

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

                val nueva = Habitacion(id, nombre, false)
                controladorHabitaciones.agregarHabitacion(nueva)
                cargarHabitaciones(layoutHabitaciones)
                Toast.makeText(this, "Habitación $nombre agregada", Toast.LENGTH_SHORT)
                    .show()
            })

        builder.setNegativeButton(
            "Cancelar"
        ) { dialog: DialogInterface?, which: Int -> dialog!!.cancel() }
        builder.show()
    }

    private fun editarHabitacion(layoutHabitaciones: LinearLayout) {
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
                    layoutHabitaciones
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEditarHabitacion(
        habitacion: Habitacion,
        layoutHabitaciones: LinearLayout
    ) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Editar habitación")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

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
                controladorHabitaciones.guardarCambios()
                cargarHabitaciones(layoutHabitaciones)
                Toast.makeText(this, "Habitación actualizada", Toast.LENGTH_SHORT).show()
            })

        builder.setNegativeButton(
            "Cancelar"
        ) { dialog: DialogInterface?, which: Int -> dialog!!.cancel() }
        builder.show()
    }

    private fun eliminarHabitacion(layoutHabitaciones: LinearLayout) {
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
            .setItems(
                nombres
            ) { dialog: DialogInterface?, which: Int ->
                val seleccionada: Habitacion = lista[which]
                controladorHabitaciones.eliminarHabitacion(seleccionada)
                cargarHabitaciones(layoutHabitaciones)
                Toast.makeText(
                    this,
                    seleccionada.nombre + " eliminada",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}