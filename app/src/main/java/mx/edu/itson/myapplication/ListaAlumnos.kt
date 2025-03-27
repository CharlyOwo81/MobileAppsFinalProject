package mx.edu.itson.myapplication

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ListaAlumnos : AppCompatActivity() {
    lateinit var listaAlumnos: ListView
    lateinit var adapter: ArrayAdapter<String>
    var alumnos = arrayListOf(
        Alumno("Juan Pérez", "Semestre 1", "Ninguno"),
        Alumno("María López", "Semestre 2", "Asesorías")
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lista_alumnos)
        var boton:Button = findViewById(R.id.btnAgregar)
        var logout:Button = findViewById(R.id.btnLogout)

        listaAlumnos= findViewById(R.id.listaAlumnos)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, alumnos.map { "${it.nombre} - ${it.semestre}" })
        listaAlumnos.adapter = adapter

        listaAlumnos.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, DetalleAlumno::class.java)
            intent.putExtra("nombre", alumnos[position].nombre)
            intent.putExtra("semestre", alumnos[position].semestre)
            startActivity(intent)
        }
        boton.setOnClickListener {
            mostrarDialogoAgregarAlumno()
        }

        logout.setOnClickListener {
            var intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    private fun mostrarDialogoAgregarAlumno() {
        val dialogView = layoutInflater.inflate(R.layout.agregar_alumno, null)
        val Nombre : EditText= dialogView.findViewById(R.id.Nombre)
        val Semestre : EditText= dialogView.findViewById(R.id.Semestre)
        val ListaColores : Spinner= dialogView.findViewById(R.id.colores)

        val opcionesColor = arrayOf("Ninguno", "Asesorías", "Atención Psicológica", "Ambas")
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opcionesColor)
        ListaColores.adapter = adapterSpinner

        AlertDialog.Builder(this)
            .setTitle("Agregar Alumno")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = Nombre.text.toString()
                val semestre = Semestre.text.toString()
                val color = ListaColores.selectedItem.toString()

                if (nombre.isNotEmpty() && semestre.isNotEmpty()) {
                    alumnos.add(Alumno(nombre, semestre, color))
                    adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, alumnos.map { "${it.nombre} - ${it.semestre}" })
                    listaAlumnos.adapter = adapter
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


}