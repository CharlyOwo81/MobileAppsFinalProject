package mx.edu.itson.Mentory

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
            intent.putExtra("permitirEditarCampos", false)
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
        if (docenteId.isNullOrEmpty()) {
            Toast.makeText(this, "ID del docente no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("Docentes").document(docenteId!!)
            .get()
            .addOnSuccessListener { document ->
                val ids = document.get("tutoradosImpartidos") as? List<String>
                if (ids.isNullOrEmpty()) {
                    Toast.makeText(this, "No hay tutorados asignados", Toast.LENGTH_SHORT).show()
                    alumnos.clear()
                    listaAlumnos.adapter = null
                    return@addOnSuccessListener
                }

                tutoradosIds = ArrayList(ids)
                alumnos.clear()
                val gruposIds = tutoradosIds!!.chunked(10)
                val alumnosTemp = mutableListOf<Alumno>()

                var gruposProcesados = 0
                var huboError = false

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
                                alumnosTemp.add(Alumno(nombre, id, semestre, color))
                            }
                            gruposProcesados++
                            val ordenSemestres = listOf(
                                "Primero", "Segundo", "Tercero", "Cuarto", "Quinto",
                                "Sexto", "Septimo", "Octavo", "Noveno", "Decimo"
                            )

                            if (gruposProcesados == gruposIds.size && !huboError) {
                                alumnos.clear()
                                alumnos.addAll(alumnosTemp.sortedWith(compareBy(
                                    { ordenSemestres.indexOf(it.semestre) },
                                    { it.nombre }
                                )))
                                adapter = object : ArrayAdapter<String>(
                                    this,
                                    android.R.layout.simple_list_item_1,
                                    alumnos.map { "${it.nombre} - ${it.semestre}" }
                                ) {
                                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                                        val view = super.getView(position, convertView, parent)
                                        val alumno = alumnos[position]
                                        val textView = view.findViewById<TextView>(android.R.id.text1)

                                        // Aquí decides el color según el campo 'color'
                                        when (alumno.color) {
                                            "Asesorías" -> textView.setTextColor(ContextCompat.getColor(context, R.color.tu_color_asesoria))
                                            "Atención Psicológica" -> textView.setTextColor(ContextCompat.getColor(context, R.color.tu_color_psicologia))
                                            "Ambas" -> textView.setTextColor(ContextCompat.getColor(context, R.color.tu_color_ambas))
                                            else -> textView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                                        }

                                        return view
                                    }
                                }

                                listaAlumnos.adapter = adapter
                            }

                        }
                        .addOnFailureListener {
                            if (!huboError) {
                                huboError = true
                                Toast.makeText(this, "Error al cargar tutorados", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener los datos del docente", Toast.LENGTH_SHORT).show()
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
