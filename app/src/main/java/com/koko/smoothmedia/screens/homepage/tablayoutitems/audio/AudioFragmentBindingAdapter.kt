package com.koko.smoothmedia.screens.homepage.tablayoutitems.audio

import android.net.Uri
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.koko.smoothmedia.R
import com.koko.smoothmedia.dataclass.Song
import com.koko.smoothmedia.mediasession.services.NOTIFICATION_LARGE_ICON_SIZE

/**
 * [setTitle] sets the text  of the [TextView] to the title of the song [item]
 */
@BindingAdapter("songTitle")
fun TextView.setTitle(item: Song?) {
    item?.let {
        text = it.title

    }

}

/**
 * [setArtistName] sets the text of the [TextView] to the artist name of the [item]
 */
@BindingAdapter("artistName")
fun TextView.setArtistName(item: Song?) {
    item?.let {
        text = it.artistName
    }
}

@BindingAdapter("songImage")
fun ImageView.setSongImage(item: Song?) {
    item?.let {
        setImageResource(R.drawable.exo_icon_circular_play)


    }
}

@BindingAdapter("theImage")
fun ImageView.manageImage(uri: Uri?) {

    Glide.with(this ).load(uri).into(this)

}

private val glideOptions = RequestOptions()
    .fallback(R.drawable.exo_ic_default_album_image)
    .diskCacheStrategy(DiskCacheStrategy.DATA)