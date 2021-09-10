package com.koko.smoothmedia.screens.homepage.permissions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PermissionViewModel: ViewModel() {

    private val _isPermissionGranted = MutableLiveData<Boolean>()
    val isPermissionGranted : LiveData<Boolean> get() = _isPermissionGranted
    fun setPermissionTrue(){
        _isPermissionGranted.value =true
    }
}