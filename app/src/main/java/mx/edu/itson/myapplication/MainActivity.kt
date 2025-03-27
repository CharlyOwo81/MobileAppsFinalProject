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

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        var usuario: EditText = findViewById(R.id.usuario)
        var contra : EditText= findViewById(R.id.contra)
        var boton : Button= findViewById(R.id.btnInicio)

        boton.setOnClickListener {
            val user = usuario.text.toString()
            val contrasena = contra.text.toString()

            when {
                user == "admin" && contrasena == "admin" -> {
                    val intent = Intent(this, ListaAlumnos::class.java)
                    intent.putExtra("usuario", user)
                    startActivity(intent)
                }
                user == "juan perez" && contrasena == "1234" -> {
                    val intent = Intent(this, DetalleAlumno::class.java)
                    intent.putExtra("nombre", "Juan Pérez")
                    intent.putExtra("semestre", "Semestre 1")
                    intent.putExtra("permitirImportar", false) // No permitir importar archivos Excel
                    startActivity(intent)
                }
                else -> {
                    Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
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