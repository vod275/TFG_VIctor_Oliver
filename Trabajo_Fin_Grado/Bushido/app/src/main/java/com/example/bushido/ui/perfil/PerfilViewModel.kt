package com.example.bushido.ui.perfil

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PerfilViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is perfil Fragment"
    }
    val text: LiveData<String> = _text
}