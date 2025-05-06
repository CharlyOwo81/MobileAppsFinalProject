package mx.edu.itson.myapplication

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import mx.edu.itson.Mentory.R

class ListaAlumnos : AppCompatActivity() {

    lateinit var listaAlumnos: ListView
    lateinit var adapter: ArrayAdapter<String>
    val alumnos = mutableListOf<Alumno>()
    val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lista_alumnos)

        val boton: Button = findViewById(R.id.btnAgregar)
        val logout: Button = findViewById(R.id.btnLogout)
        listaAlumnos = findViewById(R.id.listaAlumnos)

        cargarAlumnos()

        listaAlumnos.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, DetalleAlumno::class.java)
            intent.putExtra("nombre", alumnos[position].nombre)
            intent.putExtra("semestre", alumnos[position].semestre)
            startActivity(intent)
        }

        boton.setOnClickListener { mostrarDialogoAgregarAlumno() }
        logout.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun cargarAlumnos() {
        db.collection("alumnos").get()
            .addOnSuccessListener { result ->
                alumnos.clear()
                for (document in result) {
                    val nombre = document.getString("nombre") ?: ""
                    val semestre = document.getString("semestre") ?: ""
                    val color = document.getString("color") ?: "Ninguno"
                    alumnos.add(Alumno(nombre, semestre, color))
                }
                adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, alumnos.map { "${it.nombre} - ${it.semestre}" })
                listaAlumnos.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar alumnos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarDialogoAgregarAlumno() {
        val dialogView = layoutInflater.inflate(R.layout.agregar_alumno, null)
        val Nombre: EditText = dialogView.findViewById(R.id.Nombre)
        val Semestre: EditText = dialogView.findViewById(R.id.Semestre)
        val ListaColores: Spinner = dialogView.findViewById(R.id.colores)

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
                    val alumno = hashMapOf(
                        "nombre" to nombre,
                        "semestre" to semestre,
                        "color" to color
                    )
                    db.collection("alumnos").add(alumno)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Alumno agregado", Toast.LENGTH_SHORT).show()
                            cargarAlumnos()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al guardar alumno", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
