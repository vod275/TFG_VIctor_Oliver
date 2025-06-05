package com.example.bushido

import android.os.Bundle
import android.view.Menu
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.bushido.databinding.ActivityMainBinding
import android.content.Intent
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import objetos.UserSession

/**
 * Actividad principal que actúa como contenedor para el resto de fragmentos de la aplicación.
 * Implementa navegación con DrawerLayout y permite al usuario cerrar sesión.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var storageRef: StorageReference

    /**
     * Método llamado al crear la actividad. Se configura el layout, navegación,
     * Google Sign-In y carga la información del usuario en el Drawer.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflar layout con binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialización de Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Configuración de Google Sign-In
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        // Referencia al Firebase Storage
        storageRef = FirebaseStorage.getInstance().reference

        setSupportActionBar(binding.appBarMain.toolbar)

        // Acción del botón flotante (muestra un Snackbar de mantenimiento)
        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Mantenimiento", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }

        // Configuración del Navigation Drawer
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Configuración de las secciones principales del Drawer
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_perfil, R.id.nav_padel_tenis, R.id.nav_bolos, R.id.nav_admin
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Ocultar o mostrar "Admin" según rol o email root
        val menu = navView.menu
        val db = FirebaseFirestore.getInstance()
        val userId = UserSession.id
        val email = UserSession.email

        if (email == "vod27577@hotmail.com") {
            // Usuario root admin precreado
            UserSession.rol = "admin"
            menu.findItem(R.id.nav_admin)?.isVisible = true
        } else if (userId != null) {
            // Consultar Firestore para el rol del usuario
            db.collection(getString(R.string.usuarios)).document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val rol = document.getString("rol") ?: "usuario"
                        UserSession.rol = rol
                        menu.findItem(R.id.nav_admin)?.isVisible = rol == "admin"
                    } else {
                        menu.findItem(R.id.nav_admin)?.isVisible = false
                    }
                }
                .addOnFailureListener {
                    menu.findItem(R.id.nav_admin)?.isVisible = false
                }
        } else {
            menu.findItem(R.id.nav_admin)?.isVisible = false
        }

        // Obtener el header del NavigationView
        val headerView = navView.getHeaderView(0)
        val tvEmail = headerView.findViewById<TextView>(R.id.tvemail)
        val ibFotoPerfil = headerView.findViewById<ImageButton>(R.id.ibFotoPerfil)

        // Mostrar el nombre del usuario en tiempo real
        if (userId != null) {
            db.collection(getString(R.string.usuarios)).document(userId)
                .addSnapshotListener { document, error ->
                    if (error != null) {
                        tvEmail.text = getString(R.string.error_al_obtener_usuario)
                        return@addSnapshotListener
                    }

                    if (document != null && document.exists()) {
                        val nombre = document.getString("nombre") ?: getString(R.string.nombre_no_disponible)
                        tvEmail.text = nombre
                        UserSession.nombre = nombre // Opcional: actualizar el singleton
                    } else {
                        tvEmail.text = getString(R.string.usuario_no_encontrado)
                    }
                }
        } else {
            tvEmail.text = getString(R.string.id_no_disponible)
        }

        // Cargar la foto de perfil del usuario desde Firebase Storage
        cargarFotoPerfil(ibFotoPerfil)
    }


    /**
     * Carga la foto de perfil del usuario desde Firebase Storage en el botón de imagen del Drawer.
     * Si falla la carga, se muestra una imagen por defecto.
     */
    private fun cargarFotoPerfil(ibFotoPerfil: ImageButton) {
        val uid = UserSession.id ?: return
        val ref = storageRef.child("FotosUser/$uid.jpg")
        ref.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(this).load(uri).into(ibFotoPerfil)
        }.addOnFailureListener {
            ibFotoPerfil.setImageResource(R.drawable.default_profile_image)
        }
    }

    /**
     * Infla el menú superior de la Toolbar.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    /**
     * Maneja las opciones seleccionadas en el menú superior.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_salir -> {
                showLogoutDialog()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    /**
     * Muestra un cuadro de diálogo de confirmación para cerrar sesión.
     */
    private fun showLogoutDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Cerrar sesión")
        builder.setMessage("¿Estás seguro de que quieres cerrar sesión?")
        builder.setPositiveButton("Sí") { _, _ -> logout() }
        builder.setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    /**
     * Realiza el cierre de sesión del usuario tanto de Firebase como de Google,
     * y redirige a la actividad de login.
     */
    private fun logout() {
        auth.signOut()
        googleSignInClient.signOut()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    /**
     * Método que permite la navegación hacia arriba con el botón de la barra de herramientas.
     */
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
