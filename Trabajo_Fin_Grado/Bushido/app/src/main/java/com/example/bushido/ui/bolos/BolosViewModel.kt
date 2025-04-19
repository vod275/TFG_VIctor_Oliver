package com.example.bushido.ui.bolos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BolosViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is bolos Fragment"
    }
    val text: LiveData<String> = _text
}