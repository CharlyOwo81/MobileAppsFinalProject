package mx.edu.itson.myapplication

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

class DetalleAlumno : AppCompatActivity() {
     lateinit var listaMaterias: LinearLayout
     val materias = mutableListOf(
        Materia("Matemáticas", 65, "No estudió lo suficiente", "Asistir a tutorías"),
        Materia("Física", 80),
        Materia("Historia", 55, "No entregó tareas", "Cumplir con tareas y estudiar"),
        Materia("Programación", 90)
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalle_alumno)

        val nombre = findViewById<TextView>(R.id.txtNombreEstudiante)
        val semestre = findViewById<TextView>(R.id.Semestre)
        listaMaterias = findViewById(R.id.listaMaterias)

        val nombreEstudiante = intent.getStringExtra("nombre")
        val semestreEstudiante = intent.getStringExtra("semestre")

        nombre.text = nombreEstudiante
        semestre.text = semestreEstudiante
        val btnImportar: Button = findViewById(R.id.btnImportarExcel)
        btnImportar.setOnClickListener { seleccionarArchivoExcel() }

        mostrarMaterias()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)

            for (row in sheet) {
                if (row.rowNum == 0) continue // Saltar encabezados
                val nombre = row.getCell(0).stringCellValue
                val calificacion = row.getCell(1).numericCellValue.toInt()
                val motivo = if (row.physicalNumberOfCells > 2) row.getCell(2).stringCellValue else ""
                val accion = if (row.physicalNumberOfCells > 3) row.getCell(3).stringCellValue else ""
                materias.add(Materia(nombre, calificacion, motivo, accion))
            }
            workbook.close()
            mostrarMaterias()
        } catch (e: Exception) {
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
                layoutDetalles.visibility = if (layoutDetalles.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }

            listaMaterias.addView(view)
        }
    }

    }