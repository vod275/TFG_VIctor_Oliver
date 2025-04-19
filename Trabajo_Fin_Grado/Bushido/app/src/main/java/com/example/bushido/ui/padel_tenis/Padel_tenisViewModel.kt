package com.example.bushido.ui.padel_tenis

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class Padel_tenisViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is Padel/tenis Fragment"
    }
    val text: LiveData<String> = _text
}