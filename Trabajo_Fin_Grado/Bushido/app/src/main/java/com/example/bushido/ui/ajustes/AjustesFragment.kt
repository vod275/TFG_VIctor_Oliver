package com.example.bushido.ui.ajustes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.bushido.databinding.FragmentAjustesBinding
import java.util.Locale

class AjustesFragment : Fragment() {

    private var _binding: FragmentAjustesBinding? = null
    private val binding get() = _binding!!

    private var listenerActivado = true  // Control para evitar disparo al setChecked programático

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAjustesBinding.inflate(inflater, container, false)
        val view = binding.root

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Detectar idioma actual del dispositivo y marcar radio correspondiente
        val idiomaActual = Locale.getDefault().language

        listenerActivado = false
        when (idiomaActual) {
            "es" -> binding.RbSpanish.isChecked = true
            "en" -> binding.RBIngles.isChecked = true
            else -> binding.RbSpanish.isChecked = true 
        }
        listenerActivado = true

        // Listener para cambiar idioma al seleccionar un RadioButton
        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (!listenerActivado) return@setOnCheckedChangeListener

            when (checkedId) {
                binding.RbSpanish.id -> cambiarIdioma("es")
                binding.RBIngles.id -> cambiarIdioma("en")
            }
        }

        return view
    }

    private fun cambiarIdioma(codigoIdioma: String) {
        val locale = Locale(codigoIdioma)
        Locale.setDefault(locale)
        val config = requireContext().resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        requireActivity().apply {
            resources.updateConfiguration(config, resources.displayMetrics)
            recreate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
