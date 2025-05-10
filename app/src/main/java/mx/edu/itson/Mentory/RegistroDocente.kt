package mx.edu.itson.Mentory

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore

class RegistroDocente : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etContrasenia: EditText
    private lateinit var etContraseniaConfirmar: EditText
    private lateinit var btnRegistrar: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_docente)

        etNombre = findViewById(R.id.etNombre)
        etCorreo = findViewById(R.id.etCorreo)
        etTelefono = findViewById(R.id.etTelefono)
        etContrasenia = findViewById(R.id.etContrasenia)
        etContraseniaConfirmar = findViewById(R.id.etContraseniaConfirmar)
        btnRegistrar = findViewById(R.id.btnRegistrarDocente)

        btnRegistrar.setOnClickListener {
            validarYRegistrar()
        }
    }

    private fun validarYRegistrar() {
        val nombre = etNombre.text.toString().trim()
        val correo = etCorreo.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()
        val contrasenia = etContrasenia.text.toString().trim()
        val contraseniaConfirmar = etContraseniaConfirmar.text.toString().trim()

        if (nombre.isEmpty() || correo.isEmpty() || telefono.isEmpty() || contrasenia.isEmpty() || contraseniaConfirmar.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.error = "Correo inválido"
            return
        }

        if (telefono.length != 10 || !telefono.all { it.isDigit() }) {
            etTelefono.error = "El teléfono debe tener exactamente 10 dígitos"
            return
        }

        if (contrasenia.length < 6) {
            etContrasenia.error = "La contraseña debe tener al menos 6 caracteres"
            return
        }

        if (contrasenia != contraseniaConfirmar) {
            etContraseniaConfirmar.error = "Las contraseñas no coinciden"
            return
        }

        // Verifica si el teléfono ya está registrado
        db.collection("Docentes")
            .whereEqualTo("telefono", telefono)
            .get()
            .addOnSuccessListener { telefonoDocs ->
                if (!telefonoDocs.isEmpty) {
                    etTelefono.error = "Este número ya está registrado"
                    return@addOnSuccessListener
                }

                // Intenta registrar el usuario
                auth.createUserWithEmailAndPassword(correo, contrasenia)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = auth.currentUser?.uid
                            if (uid != null) {
                                val docente = hashMapOf(
                                    "nombre" to nombre,
                                    "correo" to correo,
                                    "telefono" to telefono,
                                    "tutoradosImpartidos" to arrayListOf<String>()
                                )

                                db.collection("Docentes").document(uid).set(docente)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, MainActivity::class.java))
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Error al guardar en Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(this, "Error al obtener UID del usuario", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (task.exception is FirebaseAuthUserCollisionException) {
                                etCorreo.error = "Este correo ya está registrado"
                            } else {
                                Toast.makeText(this, "Error al registrar: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al verificar teléfono: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
