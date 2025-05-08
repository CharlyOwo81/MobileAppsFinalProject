package mx.edu.itson.Mentory

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

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
                adapter = ArrayAdapter(this, R.layout.list_item, R.id.textViewAlumno, alumnos.map { "${it.nombre} - ${it.semestre}" })
                listaAlumnos.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar alumnos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarDialogoAgregarAlumno() {
        val intent = Intent(this, AgregarAlumnoActivity::class.java)
        startActivity(intent)
    }

    private fun agregarCampoMateria(contenedor: LinearLayout) {
        val layoutMateria = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 8, 0, 8)
            layoutParams = params
        }

        val nuevaMateria = EditText(this).apply {
            id = View.generateViewId()
            setHint("Materia")
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
            setBackgroundResource(R.drawable.edittext_background)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        nuevaMateria.id = View.generateViewId()
        val btnEliminar = Button(this).apply {
            text = "X"
            setBackgroundColor(R.drawable.button_background)
            setTextColor(Color.WHITE)
            setOnClickListener {
                if (contenedor.childCount > 1) { // si hay más de 1 campo
                    contenedor.removeView(layoutMateria)
                } else {
                    Toast.makeText(this@ListaAlumnos, "Debe haber al menos una materia", Toast.LENGTH_SHORT).show()
                }
            }
        }

        layoutMateria.addView(nuevaMateria)
        layoutMateria.addView(btnEliminar)

        contenedor.addView(layoutMateria, contenedor.childCount - 1) // lo agrega antes del botón Agregar
    }


}
