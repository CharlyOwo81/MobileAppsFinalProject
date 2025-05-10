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
    private val materias = mutableListOf<Materia>()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var nombreEstudiante: String
    private lateinit var alumnoId: String
    private var permitirEditarCampos: Boolean = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalle_alumno)

        val nombre = findViewById<TextView>(R.id.txtNombreEstudiante)
        val semestre = findViewById<TextView>(R.id.Semestre)
        listaMaterias = findViewById(R.id.listaMaterias)
        val btnImportar: Button = findViewById(R.id.btnImportarExcel)
         permitirEditarCampos = intent.getBooleanExtra("permitirEditarCampos", true)

        nombreEstudiante = intent.getStringExtra("nombre") ?: ""
        alumnoId = intent.getStringExtra("alumnoId") ?: ""
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
        db.collection("Tutorados").document(alumnoId).get()
            .addOnSuccessListener { documento ->
                val materiasEnAlumno = documento.get("materias") as? List<Map<String, Any>>
                if (!materiasEnAlumno.isNullOrEmpty()) {
                    materias.clear()
                    for (materiaInfo in materiasEnAlumno) {
                        val nombre = materiaInfo["Materia"] as? String ?: continue
                        val calificacion = (materiaInfo["Calificacion"] as? Long)?.toInt() ?: 0
                        val motivo = materiaInfo["motivo"] as? String ?: ""
                        val accion = materiaInfo["accion"] as? String ?: ""

                        materias.add(Materia(nombre, calificacion, motivo, accion, nombre))
                    }
                    mostrarMaterias()
                } else {
                    Toast.makeText(this, "El alumno no tiene materias registradas", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener datos del alumno", Toast.LENGTH_SHORT).show()
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

            val materiasDesdeExcel = mutableListOf<Map<String, Any>>()

            for (i in 1 until sheet.physicalNumberOfRows) {
                val row = sheet.getRow(i) ?: continue

                val nombre = row.getCell(0)?.toString()?.trim() ?: continue
                val calificacion = row.getCell(1)?.numericCellValue?.toInt() ?: 0
                val motivo = row.getCell(2)?.toString()?.trim() ?: ""
                val accion = row.getCell(3)?.toString()?.trim() ?: ""

                val materia = mapOf(
                    "Materia" to nombre,
                    "Calificacion" to calificacion,
                    "motivo" to motivo,
                    "accion" to accion
                )

                materiasDesdeExcel.add(materia)
            }

            workbook.close()
            inputStream?.close()

            db.collection("Tutorados").document(alumnoId).get()
                .addOnSuccessListener { document ->
                    val materiasExistentes =
                        (document.get("materias") as? MutableList<Map<String, Any>>)?.toMutableList()
                            ?: mutableListOf()

                    for (materiaNueva in materiasDesdeExcel) {
                        val nombreNueva = materiaNueva["Materia"] as String
                        val nuevaCalificacion = materiaNueva["Calificacion"]!!

                        // Buscar si ya existe una materia con el mismo nombre
                        val indexExistente = materiasExistentes.indexOfFirst {
                            (it["Materia"] as? String)?.equals(
                                nombreNueva,
                                ignoreCase = true
                            ) == true
                        }

                        if (indexExistente >= 0) {
                            // Solo actualizar calificación
                            val materiaActualizada =
                                materiasExistentes[indexExistente].toMutableMap()
                            materiaActualizada["Calificacion"] = nuevaCalificacion
                            materiasExistentes[indexExistente] = materiaActualizada
                        } else {
                            // Si no existe, agregarla completa
                            materiasExistentes.add(materiaNueva)
                        }
                    }

                    db.collection("Tutorados").document(alumnoId)
                        .update("materias", materiasExistentes)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "Materias actualizadas/importadas",
                                Toast.LENGTH_SHORT
                            ).show()
                            cargarMaterias()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al guardar materias", Toast.LENGTH_SHORT)
                                .show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "No se pudo acceder al alumno", Toast.LENGTH_SHORT).show()
                }

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
            val motivo: Spinner = view.findViewById(R.id.Motivo)
            val accion: Spinner = view.findViewById(R.id.Accion)
            val btnGuardar: Button = view.findViewById(R.id.btnGuardar)
            val labelMotivo: TextView = view.findViewById(R.id.labelMotivo)
            val labelAccion: TextView = view.findViewById(R.id.labelAccion)

            // Configurar Spinner Motivo
            ArrayAdapter.createFromResource(
                this,
                R.array.motivo_opciones,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                motivo.adapter = adapter
                motivo.setSelection(adapter.getPosition(materia.motivo))
            }

            // Configurar Spinner Acción
            ArrayAdapter.createFromResource(
                this,
                R.array.accion_opciones,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                accion.adapter = adapter
                accion.setSelection(adapter.getPosition(materia.accion))
            }

            nombreMateria.text = materia.nombre
            calificacion.text = "Calificación: ${materia.calificacion}"

            if (materia.calificacion < 70) {
                btnDesplegar.visibility = View.VISIBLE
                motivo.visibility = View.VISIBLE
                accion.visibility = View.VISIBLE
                labelMotivo.visibility = View.VISIBLE
                labelAccion.visibility = View.VISIBLE

                motivo.isEnabled = permitirEditarCampos
                accion.isEnabled = permitirEditarCampos
                btnGuardar.visibility = if (permitirEditarCampos) View.VISIBLE else View.GONE
            } else {
                btnDesplegar.visibility = View.GONE
                motivo.visibility = View.GONE
                accion.visibility = View.GONE
                labelMotivo.visibility = View.GONE
                labelAccion.visibility = View.GONE
            }

            btnDesplegar.setOnClickListener {
                layoutDetalles.visibility = if (layoutDetalles.isVisible) View.GONE else View.VISIBLE
            }

            btnGuardar.setOnClickListener {
                val nuevoMotivo = motivo.selectedItem.toString()
                val nuevaAccion = accion.selectedItem.toString()

                db.collection("Tutorados").document(alumnoId).get()
                    .addOnSuccessListener { document ->
                        val materiasArray = document.get("materias") as? MutableList<Map<String, Any>> ?: return@addOnSuccessListener

                        val materiaActualizada = materiasArray.map {
                            if ((it["Materia"] as? String)?.equals(materia.nombre, ignoreCase = true) == true) {
                                it.toMutableMap().apply {
                                    this["motivo"] = nuevoMotivo
                                    this["accion"] = nuevaAccion
                                }
                            } else {
                                it
                            }
                        }

                        db.collection("Tutorados").document(alumnoId)
                            .update("materias", materiaActualizada)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Cambios guardados", Toast.LENGTH_SHORT).show()
                                layoutDetalles.visibility = View.GONE
                                // Actualizar el objeto local
                                materia.motivo = nuevoMotivo
                                materia.accion = nuevaAccion
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Oops, algo salió mal", Toast.LENGTH_SHORT).show()
                            }
                    }
            }

            listaMaterias.addView(view)
        }
    }
}
