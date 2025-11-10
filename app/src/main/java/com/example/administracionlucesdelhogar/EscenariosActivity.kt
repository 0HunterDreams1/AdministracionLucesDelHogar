package com.example.administracionlucesdelhogar

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.administracionlucesdelhogar.controladores.ControladorEscenarios
import com.example.administracionlucesdelhogar.controladores.ControladorHabitaciones
import com.example.administracionlucesdelhogar.modelos.Escenario
import com.example.administracionlucesdelhogar.modelos.Habitacion

@Suppress("DEPRECATION")
class EscenariosActivity : AppCompatActivity() {
    private lateinit var controladorEscenarios: ControladorEscenarios
    private lateinit var controladorHabitaciones: ControladorHabitaciones

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

        // Manejo la barra de acciones
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)

        // Referencia al controlador de escenarios
        controladorEscenarios = ControladorEscenarios.getInstance(this)
        // Referencia al controlador de habitaciones
        controladorHabitaciones = ControladorHabitaciones.getInstance(this)

        val layoutEscenarios = findViewById<LinearLayout>(R.id.layoutEscenarios)
        val btnAgregar = findViewById<Button>(R.id.btnAgregarEscenario)
        val btnEditar = findViewById<Button>(R.id.btnEditarEscenario)
        val btnEliminar = findViewById<Button>(R.id.btnEliminarEscenario)

        // Cargo los escenarios cargados
        cargarEscenarios(layoutEscenarios)

        btnAgregar.setOnClickListener {
            mostrarDialogoAgregarOEditarEscenario(null, layoutEscenarios)
        }
        btnEditar.setOnClickListener {
            seleccionarEscenarioParaEditar(layoutEscenarios)
        }
        btnEliminar.setOnClickListener {
            eliminarEscenario(layoutEscenarios)
        }
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

        // Recupero las habitaciones y los escenarios
        val lista = controladorEscenarios.listaEscenarios
        val todasHabitaciones = controladorHabitaciones.listaHabitaciones

        // Recorro los escenarios
        for ((index, e) in lista.withIndex()) {
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.HORIZONTAL
            layout.setPadding(10, 10, 10, 10)
            layout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val tv = TextView(this)
            tv.text = "(${e.id}) ${e.nombre} - ${e.habitaciones.size} habitaciones"
            tv.textSize = 18f
            tv.setTextColor(Color.BLACK)
            val tvParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            tv.layoutParams = tvParams

            val switch = Switch(this)
            switch.isChecked = e.estado
            switch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Apagar todos los demás escenarios
                    for ((i, otro) in lista.withIndex()) {
                        otro.estado = (i == index)
                    }

                    // Actualizar estados de las habitaciones
                    for (h in todasHabitaciones) {
                        h.estado = e.habitaciones.any { it.id == h.id }
                        if (h.estado) {
                            Log.i("EscenariosActivity.cargarHabitaciones", "Enciendo habitación ${h.id}")
                        } else {
                            Log.i("EscenariosActivity.cargarHabitaciones", "Apago habitación ${h.id}")
                        }
                    }

                    controladorEscenarios.guardarCambios()
                    controladorHabitaciones.guardarCambios() // asegúrate de tener este método
                    // Recargar la UI
                    cargarEscenarios(layoutEscenarios)
                    Toast.makeText(this, "Escenario ${e.nombre} activado", Toast.LENGTH_SHORT).show()
                } else {
                    // Permitir apagar el escenario actual y apagar sus habitaciones
                    e.estado = false
                    for (h in todasHabitaciones) {
                        if (e.habitaciones.any { it.id == h.id }) {
                            h.estado = false
                        }
                    }
                    controladorEscenarios.guardarCambios()
                    controladorHabitaciones.guardarCambios()
                    Toast.makeText(this, "Escenario ${e.nombre} desactivado", Toast.LENGTH_SHORT).show()
                    cargarEscenarios(layoutEscenarios)
                }
            }

            layout.addView(tv)
            layout.addView(switch)
            layoutEscenarios.addView(layout)
        }
    }

    private fun mostrarDialogoAgregarOEditarEscenario(escenario: Escenario?, layoutEscenarios: LinearLayout) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(if (escenario == null) "Agregar nuevo escenario" else "Editar escenario")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        // Id de escenario
        var inputId: Int
        if (escenario != null) {
            inputId = escenario.id
        } else {
            inputId = controladorEscenarios.obtenerSiguienteId()
        }
        val lblIdEscenario = TextView(this)
        lblIdEscenario.text = "Id: ${inputId}"
        lblIdEscenario.textSize = 16f
        lblIdEscenario.setPadding(0, 16, 0, 8)
        layout.addView(lblIdEscenario)

        // Nombre de escenario
        val lblNombre = TextView(this)
        lblNombre.text = "Nombre:"
        lblNombre.textSize = 16f
        lblNombre.setPadding(0, 16, 0, 8)
        layout.addView(lblNombre)
        val inputNombre = EditText(this)
        inputNombre.hint = "Nombre del escenario"
        if (escenario != null) inputNombre.setText(escenario.nombre)
        layout.addView(inputNombre)

        builder.setView(layout)

        // Defino el botón de confirmación de agregado de escenario
        builder.setPositiveButton("Siguiente") { _, _ ->
            val nombre = inputNombre.text.toString().trim()

            // Verifico que el nombre no haya quedado vacío
            if (nombre.isEmpty()) {
                Toast.makeText(this, "Debe ingresar un nombre para el escenario", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            // Validar que no exista otro escenario con el mismo ID o nombre
            for (e in controladorEscenarios.listaEscenarios) {
                if (escenario != null && e == escenario) continue
                if (e.nombre.equals(nombre, ignoreCase = true)) {
                    Toast.makeText(this, "Ya existe un escenario con ese nombre", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
            }

            // Muestro la pantalla de selección de habitaciones
            mostrarDialogoSeleccionarHabitaciones(inputId, nombre, escenario, layoutEscenarios)
        }
        // Defino el botón de cancelación
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
        // Verifico si la lista de escenarios está vacía
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
                    Toast.makeText(this, "Escenario \"$nombre\" modificado", Toast.LENGTH_SHORT).show()
                } else {
                    // AGREGAR: crear nuevo escenario
                    val nuevoEscenario = Escenario(id, nombre, seleccionadasList, false)
                    controladorEscenarios.agregarEscenario(nuevoEscenario)
                    Toast.makeText(this, "Escenario \"$nombre\" agregado", Toast.LENGTH_SHORT).show()
                }
                // Recargo los escnarios
                cargarEscenarios(layoutEscenarios)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun seleccionarEscenarioParaEditar(layoutEscenarios: LinearLayout) {
        val lista = controladorEscenarios.listaEscenarios
        // Verifico si la lista de escenarios está vacía
        if (lista.isEmpty()) {
            Toast.makeText(this, "No hay escenarios para editar", Toast.LENGTH_SHORT).show()
            return
        }

        val nombres = lista.map { "(${it.id}) ${it.nombre}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Selecciona un escenario para editar")
            .setItems(nombres) { _, which ->
                val escenario = lista[which]
                mostrarDialogoAgregarOEditarEscenario(escenario, layoutEscenarios)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarEscenario(layoutEscenarios: LinearLayout) {
        val lista = controladorEscenarios.listaEscenarios
        // Verifico si la lista de escenarios está vacía
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