package mx.edu.itson.Mentory

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
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

        if (contrasenia != contraseniaConfirmar) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        if (telefono.length != 10 || !telefono.all { it.isDigit() }) {
            Toast.makeText(this, "El número de teléfono debe tener exactamente 10 dígitos", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            Toast.makeText(this, "Por favor ingresa un correo electrónico válido", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("Docentes")
            .whereEqualTo("telefono", telefono)
            .get()
            .addOnSuccessListener { telefonoDocs ->
                if (!telefonoDocs.isEmpty) {
                    Toast.makeText(this, "El teléfono ya está registrado", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

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
                                        val intent = Intent(this, MainActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Error al guardar en Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(this, "Error al obtener UID del usuario", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "Error al registrar usuario: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al verificar teléfono: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
