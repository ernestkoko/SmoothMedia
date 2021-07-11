package com.koko.smoothmedia.mediasession

import android.support.v4.media.session.MediaSessionCompat

class MediaSessionCallbacks:MediaSessionCompat.Callback() {
    override fun onPlay() {
        super.onPlay()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onSeekTo(pos: Long) {
        super.onSeekTo(pos)
    }

    override fun onSkipToNext() {
        super.onSkipToNext()
    }

    override fun onSkipToPrevious() {
        super.onSkipToPrevious()
    }

    override fun onSkipToQueueItem(id: Long) {
        super.onSkipToQueueItem(id)
    }
}