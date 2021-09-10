package com.koko.smoothmedia.utils

import android.app.Application
import android.content.ComponentName
import android.content.Context
import com.koko.smoothmedia.MainActivityViewModel
import com.koko.smoothmedia.mediasession.mediaconnection.MusicServiceConnection
import com.koko.smoothmedia.mediasession.services.AudioService
import com.koko.smoothmedia.screens.homepage.tablayoutitems.audio.AudioFragmentViewModel
import com.koko.smoothmedia.screens.homepage.tablayoutitems.video.VideoFragmentViewModel

object InjectorUtils {
    private fun provideMusicService(context: Context): MusicServiceConnection {
        return MusicServiceConnection.getInstance(
            context,
            ComponentName(context, AudioService::class.java)
        )
    }

    fun provideAudioFragmentViewModel(application: Application): AudioFragmentViewModel.Factory {
       // val application = context
        val musicServiceConnection = provideMusicService(application)
        return AudioFragmentViewModel.Factory(application, musicServiceConnection)
    }
    fun provideMainActivityViewModel(application: Application):MainActivityViewModel.Factory{
        val musicServiceConnection = provideMusicService(application)
        return MainActivityViewModel.Factory(application,musicServiceConnection)
    }
    fun provideVideoFragmentViewModel(application: Application):VideoFragmentViewModel.Factory{
        val musicServiceConnection = provideMusicService(application)
        return VideoFragmentViewModel.Factory(application,musicServiceConnection)
    }
}