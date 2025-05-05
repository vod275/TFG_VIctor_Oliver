package com.example.bushido

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.bushido.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import objetos.UserSession

private const val RC_SIGN_IN = 9001

/**
 * LoginActivity gestiona el inicio de sesión con correo y con Google.
 * Utiliza Firebase Authentication para autenticar a los usuarios
 * y Firestore para posibles futuras operaciones con base de datos.
 */
class LoginActivity : AppCompatActivity() {

    // View binding para acceder a los elementos de la interfaz
    private lateinit var binding: ActivityLoginBinding

    // Instancia de FirebaseAuth para autenticación
    private lateinit var auth: FirebaseAuth

    // Cliente para One Tap Sign-In de Google
    private lateinit var oneTapClient: SignInClient

    // Solicitud de inicio de sesión con Google
    private lateinit var signInRequest: BeginSignInRequest

    // Cliente tradicional de Google Sign-In
    private lateinit var googleSignInClient: GoogleSignInClient

    // Instancia perezosa de Firestore (aún no se usa activamente aquí)
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflar layout y asignar binding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Configurar One Tap Sign-In de Google
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            ).build()

        // Configurar Google Sign-In tradicional (por compatibilidad)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Configurar botones
        binding.googleSignInButton.setOnClickListener { signInWithGoogle() }
        binding.loginButton.setOnClickListener { handleEmailSignIn() }
    }

    /**
     * Valida campos de email y contraseña, y realiza el login con correo si son válidos.
     */
    private fun handleEmailSignIn() {
        val email = binding.emailInput.editText?.text.toString().trim()
        val password = binding.passwordInput.editText?.text.toString().trim()

        if (email.isNotEmpty() && password.isNotEmpty()) {
            if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                signIn(email, password)
            } else {
                showToast(getString(R.string.por_favor_ingresa_un_correo_v_lido))
            }
        } else {
            showToast(getString(R.string.por_favor_completa_todos_los_campos))
        }
    }

    /**
     * Realiza autenticación con correo electrónico y contraseña en Firebase.
     */
    private fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    Log.w("Login", "signInWithEmail:failure", task.exception)
                    showToast("Error: ${task.exception?.message}")
                    updateUI(null)
                }
            }
    }

    /**
     * Inicia el proceso de autenticación con Google.
     * Intenta usar One Tap, y si falla, usa el flujo tradicional.
     */
    private fun signInWithGoogle() {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(this) { result ->
                try {
                    // Lanza One Tap Sign-In
                    signInLauncher.launch(IntentSenderRequest.Builder(result.pendingIntent).build())
                } catch (e: android.content.IntentSender.SendIntentException) {
                    Log.e("GoogleSignIn", "Couldn't start One Tap UI:" + e.localizedMessage)
                    // Fallback a Google Sign-In tradicional
                    val signInIntent = googleSignInClient.signInIntent
                    startActivityForResult(signInIntent, RC_SIGN_IN)
                }
            }
            .addOnFailureListener { e ->
                Log.w("GoogleSignIn", "One Tap Sign-In failed", e)
                // Fallback a Google Sign-In tradicional
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
    }

    /**
     * Callback que recibe el resultado del intent lanzado para One Tap Sign-In.
     */
    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                val idToken = credential.googleIdToken
                if (idToken != null) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                updateUI(user)
                            } else {
                                showToast(getString(R.string.error_al_autenticar_con_google))
                            }
                        }
                }
            }
        }

    /**
     * Si el usuario está autenticado, guarda sus datos en UserSession
     * y lo redirige al MainActivity. Si no, muestra mensaje de error.
     */
    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            val userEmail = user.email
            val userName = user.displayName ?: getString(R.string.usuario_sin_nombre)
            val userId = user.uid

            // Guardar datos en sesión global
            UserSession.id = userId
            UserSession.email = userEmail
            UserSession.nombre = userName

            // Ir a la pantalla principal
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            showToast(getString(R.string.por_favor_inicia_sesi_n_o_reg_strate))
        }
    }

    /**
     * Muestra un mensaje Toast breve.
     */
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Comprueba si el usuario ya ha iniciado sesión al arrancar la actividad.
     */
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }
}
