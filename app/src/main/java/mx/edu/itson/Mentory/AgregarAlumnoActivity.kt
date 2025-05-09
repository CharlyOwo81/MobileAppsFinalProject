package mx.edu.itson.Mentory

import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AgregarAlumnoActivity : AppCompatActivity() {

    lateinit var nombreEditText: EditText
    lateinit var semestreSpinner: Spinner
    lateinit var listaColores: Spinner
    lateinit var contenedorMaterias: LinearLayout
    lateinit var btnAgregarMateria: Button
    lateinit var btnGuardar: Button
    lateinit var btnEliminarMateria: Button

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.agregar_alumno)

        nombreEditText = findViewById(R.id.Nombre)
        semestreSpinner = findViewById(R.id.Semestre)
        listaColores = findViewById(R.id.colores)
        contenedorMaterias = findViewById(R.id.contenedorMaterias)
        btnAgregarMateria = findViewById(R.id.btnAgregarMateria)
        btnEliminarMateria = findViewById(R.id.btnEliminarMateria)

        val opcionesSemestre = arrayOf("Primero", "Segundo", "Tercero", "Cuarto", "Quinto", "Sexto", "Septimo", "Octavo", "Noveno", "Decimo")
        val adapterSemestreSpinner = ArrayAdapter(this, R.layout.spinner_item, opcionesSemestre)
        adapterSemestreSpinner.setDropDownViewResource(R.layout.spinner_item)
        semestreSpinner.adapter = adapterSemestreSpinner

        val opcionesColor = arrayOf("Ninguno", "Asesorías", "Atención Psicológica", "Ambas")
        val adapterSpinner = ArrayAdapter(this, R.layout.spinner_item, opcionesColor)
        adapterSpinner.setDropDownViewResource(R.layout.spinner_item)
        listaColores.adapter = adapterSpinner

        agregarCampoMateria()

        btnAgregarMateria.setOnClickListener {
            agregarCampoMateria()
        }

        btnEliminarMateria.setOnClickListener {
            eliminarUltimaMateria()
        }

        btnGuardar = findViewById(R.id.btnGuardar)
        btnGuardar.setOnClickListener {
            guardarAlumno()
        }
    }

    private fun agregarCampoMateria() {
        val layoutMateria = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 16, 0, 16)
            layoutParams = params
        }

        val nombreMateria = EditText(this).apply {
            hint = "Materia"
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
            setBackgroundResource(R.drawable.edittext_background)
        }

        val calificacion = EditText(this)
        val accion = EditText(this)
        val motivo = EditText(this)

        layoutMateria.addView(nombreMateria)
        layoutMateria.tag = listOf(calificacion, accion, motivo)

        contenedorMaterias.addView(layoutMateria, contenedorMaterias.childCount - 1)
    }

    private fun eliminarUltimaMateria() {
        val count = contenedorMaterias.childCount
        if (count > 1) {
            contenedorMaterias.removeViewAt(count - 2)
        } else {
            Toast.makeText(this, "No hay materias para eliminar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarAlumno() {
        val nombre = nombreEditText.text.toString().trim()
        val semestre = semestreSpinner.selectedItem.toString()
        val color = listaColores.selectedItem.toString()

        if (nombre.isEmpty() || semestre.isEmpty()) {
            Toast.makeText(this, "Por favor completa Nombre y Semestre", Toast.LENGTH_SHORT).show()
            return
        }

        val correoGenerado = generarCorreo(nombre)
        val contraseniaGenerada = generarContrasenia()

        val materias = mutableListOf<Map<String, Any>>()
        for (i in 0 until contenedorMaterias.childCount) {
            val layout = contenedorMaterias.getChildAt(i) as LinearLayout
            val nombreMateria = (layout.getChildAt(0) as EditText).text.toString().trim()
            val (calificacionEditText, accionEditText, motivoEditText) = layout.tag as List<EditText>
            val calificacionStr = calificacionEditText.text.toString().trim()
            val accion = accionEditText.text.toString().trim()
            val motivo = motivoEditText.text.toString().trim()
            if (nombreMateria.isNotEmpty()) {
                val calificacion = calificacionStr.toIntOrNull() ?: 0
                materias.add(
                    mapOf(
                        "Materia" to nombreMateria,
                        "Calificacion" to calificacion,
                        "accion" to accion,
                        "motivo" to motivo
                    )
                )
            }
        }

        auth.createUserWithEmailAndPassword(correoGenerado, contraseniaGenerada)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        val alumno = hashMapOf(
                            "nombre" to nombre,
                            "semestre" to semestre,
                            "color" to color,
                            "correo" to correoGenerado,
                            "contrasenia" to contraseniaGenerada,
                            "telefono" to "",
                            "materias" to materias
                        )

                        val docenteId = intent.getStringExtra("docenteId")
                        val tutoradosIds = intent.getStringArrayListExtra("tutoradosIds") ?: arrayListOf()

                        db.collection("Tutorados").document(uid).set(alumno)
                            .addOnSuccessListener {
                                tutoradosIds.add(uid)
                                if (docenteId != null) {
                                    db.collection("Docentes").document(docenteId)
                                        .update("tutoradosImpartidos", tutoradosIds)
                                        .addOnSuccessListener {
                                            mostrarDialogoCredenciales(correoGenerado, contraseniaGenerada)
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(this, "Error al asignar alumno al docente", Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    Toast.makeText(this, "No se pudo asignar al docente (docenteId nulo)", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error al guardar alumno en Firestore", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Error al registrar usuario: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun mostrarDialogoCredenciales(correo: String, contrasenia: String) {
        AlertDialog.Builder(this)
            .setTitle("Credenciales generadas")
            .setMessage("Correo: $correo\nContraseña: $contrasenia")
            .setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()
                setResult(RESULT_OK)
                finish()
            }
            .show()
    }

    private fun generarCorreo(nombre: String): String {
        val nombreSinEspacios = nombre.replace("\\s+".toRegex(), "").lowercase()
        return "$nombreSinEspacios@gmail.com"
    }

    private fun generarContrasenia(): String {
        val random = (10000000..99999999).random()
        return "$random"
    }
}
