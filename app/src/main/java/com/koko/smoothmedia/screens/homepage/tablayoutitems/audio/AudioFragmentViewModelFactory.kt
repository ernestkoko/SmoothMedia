package com.koko.smoothmedia.screens.homepage.tablayoutitems.audio

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
A factory class that creates a view model for the Home screen
 */

class AudioFragmentViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AudioFragmentViewModel::class.java)) {
            return AudioFragmentViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown Model Class")
    }
}