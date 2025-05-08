package mx.edu.itson.Mentory

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
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
                        val semestre = document.getLong("semestre")?.toString() ?: ""
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
                val nombre = Nombre.text.toString().trim()
                val semestreTexto = Semestre.text.toString().trim()
                val color = ListaColores.selectedItem.toString()

                if (nombre.isNotEmpty() && semestreTexto.isNotEmpty()) {
                    val semestre = semestreTexto.toIntOrNull() ?: 1

                    val alumno = hashMapOf(
                        "nombre" to nombre,
                        "semestre" to semestre,
                        "color" to color,
                        "apellido_paterno" to "",
                        "apellido_materno" to "",
                        "correo" to "",
                        "contrasenia" to "",
                        "telefono" to "",
                        "estatus" to "Sin seguimiento",
                        "materias" to emptyList<Map<String, Any>>(),
                        "accionesRegistradas" to emptyList<Any>(),
                        "alertas" to emptyList<Any>()
                    )

                    db.collection("Tutorados").add(alumno)
                        .addOnSuccessListener { docRef ->
                            val nuevoId = docRef.id

                            // Agregar el nuevo ID a la lista local
                            if (tutoradosIds == null) {
                                tutoradosIds = arrayListOf()
                            }
                            tutoradosIds!!.add(nuevoId)

                            // Actualizar el campo tutoradosImpartidos del docente
                            db.collection("Docentes").document(docenteId!!)
                                .update("tutoradosImpartidos", tutoradosIds)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Alumno agregado y asignado al docente", Toast.LENGTH_SHORT).show()
                                    cargarAlumnos()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Error al asignar alumno al docente", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al guardar alumno", Toast.LENGTH_SHORT).show()
                        }

                } else {
                    Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
