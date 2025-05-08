package mx.edu.itson.Mentory

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class ListaAlumnos : AppCompatActivity() {

    lateinit var listaAlumnos: ListView
    lateinit var adapter: ArrayAdapter<String>
    val alumnos = mutableListOf<Alumno>()
    val db = FirebaseFirestore.getInstance()
    var tutoradosIds: ArrayList<String>? = null
    var docenteId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lista_alumnos)

        val boton: Button = findViewById(R.id.btnAgregar)
        val logout: Button = findViewById(R.id.btnLogout)
        listaAlumnos = findViewById(R.id.listaAlumnos)
        docenteId = intent.getStringExtra("docenteId")



        tutoradosIds = intent.getStringArrayListExtra("tutoradosIds")

        cargarAlumnos()

        listaAlumnos.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, DetalleAlumno::class.java)
            intent.putExtra("alumnoId", alumnos[position].id)
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
        if (tutoradosIds.isNullOrEmpty()) {
            Toast.makeText(this, "No hay tutorados asignados", Toast.LENGTH_SHORT).show()
            return
        }

        // Dividir los IDs en grupos de 10 si hay más de 10, porque Firebase permite máximo 10 elementos en whereIn
        val gruposIds = tutoradosIds!!.chunked(10)
        alumnos.clear()

        var gruposProcesados = 0

        for (grupo in gruposIds) {
            db.collection("Tutorados")
                .whereIn(FieldPath.documentId(), grupo)
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        val id = document.id
                        val nombre = document.getString("nombre") ?: ""
                        val semestre = document.getString("semestre") ?: ""
                        val color = document.getString("color") ?: "Ninguno"
                        alumnos.add(Alumno(nombre, id, semestre, color))
                    }

                    gruposProcesados++
                    if (gruposProcesados == gruposIds.size) {
                        // Solo actualizamos la vista cuando todos los grupos se hayan cargado
                        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, alumnos.map { "${it.nombre} - ${it.semestre}" })
                        listaAlumnos.adapter = adapter
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al cargar tutorados", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun mostrarDialogoAgregarAlumno() {
        val intent = Intent(this, AgregarAlumnoActivity::class.java)
        intent.putExtra("docenteId", docenteId)
        intent.putStringArrayListExtra("tutoradosIds", ArrayList(tutoradosIds ?: listOf()))
        agregarAlumnoLauncher.launch(intent)
    }

    private val agregarAlumnoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            cargarAlumnos() // vuelve a cargar alumnos después de agregar
        }
    }
}
