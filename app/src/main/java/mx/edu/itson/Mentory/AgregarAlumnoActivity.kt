package mx.edu.itson.Mentory

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.agregar_alumno) // tu layout xml

        nombreEditText = findViewById(R.id.Nombre)
        semestreSpinner = findViewById(R.id.Semestre)
        listaColores = findViewById(R.id.colores)
        contenedorMaterias = findViewById(R.id.contenedorMaterias)
        btnAgregarMateria = findViewById(R.id.btnAgregarMateria)
        btnEliminarMateria = findViewById(R.id.btnEliminarMateria)

        // Spinner setup
        val opcionesSemestre = arrayOf("Primero", "Segundo", "Tercero", "Cuarto", "Quinto", "Sexto", "Septimo", "Octavo", "Noveno", "Decimo")
        val adapterSemestreSpinner = ArrayAdapter(this, R.layout.spinner_item, opcionesSemestre)
        adapterSemestreSpinner.setDropDownViewResource(R.layout.spinner_item)
        semestreSpinner.adapter = adapterSemestreSpinner

        // Spinner setup
        val opcionesColor = arrayOf("Ninguno", "Asesorías", "Atención Psicológica", "Ambas")
        val adapterSpinner = ArrayAdapter(this, R.layout.spinner_item, opcionesColor)
        adapterSpinner.setDropDownViewResource(R.layout.spinner_item)
        listaColores.adapter = adapterSpinner

        // Primer campo de materia por defecto
        agregarCampoMateria()

        // Botón para agregar más materias
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
            hint = "Materia"
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
            setBackgroundResource(R.drawable.edittext_background)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        layoutMateria.addView(nuevaMateria)

        // Insertamos antes del botón Agregar Materia
        contenedorMaterias.addView(layoutMateria, contenedorMaterias.childCount - 1)
    }

    private fun eliminarUltimaMateria() {
        val count = contenedorMaterias.childCount
        if (count > 1) { // Para no intentar eliminar el botón u otro layout base
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

        val materias = mutableListOf<String>()
        for (i in 0 until contenedorMaterias.childCount - 1) { // -1 porque el último es el botón
            val layout = contenedorMaterias.getChildAt(i) as LinearLayout
            val editText = layout.getChildAt(0) as EditText
            val materia = editText.text.toString().trim()
            if (materia.isNotEmpty()) materias.add(materia)
        }

        val alumno = hashMapOf(
            "nombre" to nombre,
            "semestre" to semestre,
            "color" to color,
            "materias" to materias // <-- ahora también guardamos las materias
        )

        db.collection("alumnos").add(alumno)
            .addOnSuccessListener {
                Toast.makeText(this, "Alumno agregado correctamente", Toast.LENGTH_SHORT).show()
                finish() // Cierra la pantalla y regresa
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar alumno", Toast.LENGTH_SHORT).show()
            }
    }
}
