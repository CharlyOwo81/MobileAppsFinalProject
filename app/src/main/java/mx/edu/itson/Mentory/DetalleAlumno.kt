package mx.edu.itson.Mentory

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.firebase.firestore.FirebaseFirestore
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream

class DetalleAlumno : AppCompatActivity() {

    private lateinit var listaMaterias: LinearLayout
    val materias = mutableListOf<Materia>()
    val db = FirebaseFirestore.getInstance()
    lateinit var nombreEstudiante: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalle_alumno)

        val nombre = findViewById<TextView>(R.id.txtNombreEstudiante)
        val semestre = findViewById<TextView>(R.id.Semestre)
        listaMaterias = findViewById(R.id.listaMaterias)
        val btnImportar: Button = findViewById(R.id.btnImportarExcel)

        nombreEstudiante = intent.getStringExtra("nombre") ?: ""
        val semestreEstudiante = intent.getStringExtra("semestre") ?: ""
        val permitirImportar = intent.getBooleanExtra("permitirImportar", true)

        nombre.text = nombreEstudiante
        semestre.text = semestreEstudiante

        if (!permitirImportar) {
            btnImportar.visibility = View.GONE
        } else {
            btnImportar.setOnClickListener { seleccionarArchivoExcel() }
        }

        cargarMaterias()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun cargarMaterias() {
        db.collection("materias")
            .whereEqualTo("alumno", nombreEstudiante)
            .get()
            .addOnSuccessListener { result ->
                materias.clear()
                for (document in result) {
                    val nombre = document.getString("nombre") ?: ""
                    val calificacion = document.getLong("calificacion")?.toInt() ?: 0
                    val motivo = document.getString("motivo") ?: ""
                    val accion = document.getString("accion") ?: ""
                    materias.add(Materia(nombre, calificacion, motivo, accion))
                }
                mostrarMaterias()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar materias", Toast.LENGTH_SHORT).show()
            }
    }

    private val seleccionarArchivo =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri -> procesarArchivoExcel(uri) }
            }
        }

    private fun seleccionarArchivoExcel() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        seleccionarArchivo.launch(intent)
    }

    private fun procesarArchivoExcel(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val workbook = XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(0)

            for (i in 1 until sheet.physicalNumberOfRows) {
                val row = sheet.getRow(i) ?: continue

                val nombre = row.getCell(0)?.toString() ?: continue
                val calificacion = row.getCell(1)?.numericCellValue?.toInt() ?: 0
                val motivo = row.getCell(2)?.toString() ?: ""
                val accion = row.getCell(3)?.toString() ?: ""

                val materia = hashMapOf(
                    "alumno" to nombreEstudiante,
                    "nombre" to nombre,
                    "calificacion" to calificacion,
                    "motivo" to motivo,
                    "accion" to accion
                )

                db.collection("materias").add(materia)
            }

            workbook.close()
            inputStream?.close()

            cargarMaterias()

        } catch (e: Exception) {
            Toast.makeText(this, "Error al leer el archivo", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun mostrarMaterias() {
        listaMaterias.removeAllViews()
        for (materia in materias) {
            val view = layoutInflater.inflate(R.layout.item_materia, listaMaterias, false)

            val nombreMateria: TextView = view.findViewById(R.id.Materia)
            val calificacion: TextView = view.findViewById(R.id.Calificacion)
            val btnDesplegar: ImageButton = view.findViewById(R.id.btnDesplegar)
            val layoutDetalles: LinearLayout = view.findViewById(R.id.layoutDetalles)
            val motivo: EditText = view.findViewById(R.id.Motivo)
            val accion: EditText = view.findViewById(R.id.Accion)

            nombreMateria.text = materia.nombre
            calificacion.text = "Calificación: ${materia.calificacion}"

            if (materia.calificacion < 70) {
                btnDesplegar.visibility = View.VISIBLE
                motivo.setText(materia.motivo)
                accion.setText(materia.accion)
                motivo.visibility = View.VISIBLE
                accion.visibility = View.VISIBLE
            } else {
                btnDesplegar.visibility = View.GONE
            }

            btnDesplegar.setOnClickListener {
                layoutDetalles.visibility = if (layoutDetalles.isVisible) View.GONE else View.VISIBLE
            }

            listaMaterias.addView(view)
        }
    }
}
