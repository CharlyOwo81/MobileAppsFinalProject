package mx.edu.itson.Mentory

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val usuario: EditText = findViewById(R.id.usuario)
        val contra: EditText = findViewById(R.id.contra)
        val boton: Button = findViewById(R.id.btnInicio)
        val btnRegistroDocente = findViewById<Button>(R.id.btnRegistroDocente)
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()

        btnRegistroDocente.setOnClickListener {
            val intent = Intent(this, RegistroDocente::class.java)
            startActivity(intent)
        }

        boton.setOnClickListener {
            val user = usuario.text.toString().trim()
            val contrasena = contra.text.toString().trim()

            auth.signInWithEmailAndPassword(user, contrasena)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val currentUser = auth.currentUser
                        val uid = currentUser?.uid

                        if (uid != null) {
                            db.collection("Docentes").document(uid).get()
                                .addOnSuccessListener { docenteDoc ->
                                    if (docenteDoc.exists()) {
                                        val tutoradosIds = docenteDoc.get("tutoradosImpartidos") as? ArrayList<String>
                                        val intent = Intent(this, ListaAlumnos::class.java)
                                        intent.putExtra("docenteId", docenteDoc.id)
                                        intent.putStringArrayListExtra("tutoradosIds", tutoradosIds)
                                        intent.putExtra("permitirEditarCampos", false)
                                        startActivity(intent)
                                    } else {
                                        // 🔥 CAMBIO → Si no es Docente, buscamos en Tutorados
                                        db.collection("Tutorados").document(uid).get()
                                            .addOnSuccessListener { tutoradoDoc ->
                                                if (tutoradoDoc.exists()) {
                                                    val intent = Intent(this, DetalleAlumno::class.java)
                                                    intent.putExtra("nombre", tutoradoDoc.getString("nombre"))
                                                    intent.putExtra("semestre", tutoradoDoc.getString("semestre"))
                                                    intent.putExtra("alumnoId", tutoradoDoc.id)
                                                    intent.putExtra("permitirImportar", false)
                                                    intent.putExtra("permitirEditarCampos", true)
                                                    startActivity(intent)
                                                } else {
                                                    Toast.makeText(this, "No se encontró en Docentes ni Tutorados", Toast.LENGTH_SHORT).show()
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
                        } else {
                            Toast.makeText(this, "Error: UID no encontrado", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
