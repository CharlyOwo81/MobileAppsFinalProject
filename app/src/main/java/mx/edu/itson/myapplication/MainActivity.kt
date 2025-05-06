package mx.edu.itson.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import mx.edu.itson.Mentory.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val usuario: EditText = findViewById(R.id.usuario)
        val contra: EditText = findViewById(R.id.contra)
        val boton: Button = findViewById(R.id.btnInicio)
        val db = FirebaseFirestore.getInstance()

        boton.setOnClickListener {
            val user = usuario.text.toString()
            val contrasena = contra.text.toString()

            // Verificar si es docente
            db.collection("Docentes")
                .whereEqualTo("nombre", user)
                .whereEqualTo("contrasenia", contrasena)
                .get()
                .addOnSuccessListener { docentes ->
                    if (!docentes.isEmpty) {
                        val docenteDoc = docentes.documents[0]
                        val tutoradosIds = docenteDoc.get("tutoradosImpartidos") as? ArrayList<String>
                        val intent = Intent(this, ListaAlumnos::class.java)
                        intent.putExtra("docenteId", docenteDoc.id)
                        intent.putStringArrayListExtra("tutoradosIds", tutoradosIds)
                        startActivity(intent)
                    } else {
                        // Si no es docente, verificar si es tutorado (alumno)
                        db.collection("Tutorados")
                            .whereEqualTo("nombre", user)
                            .whereEqualTo("contrasenia", contrasena)
                            .get()
                            .addOnSuccessListener { tutorados ->
                                if (!tutorados.isEmpty) {
                                    val tutoradoDoc = tutorados.documents[0]
                                    val materias = tutoradoDoc.get("materias") as? ArrayList<String>
                                    val intent = Intent(this, DetalleAlumno::class.java)
                                    intent.putExtra("nombre", tutoradoDoc.getString("nombre"))
                                    intent.putExtra("semestre", tutoradoDoc.getLong("semestre").toString())
                                    intent.putStringArrayListExtra("materias", materias)
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error al verificar tutorado", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al verificar docente", Toast.LENGTH_SHORT).show()
                }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
