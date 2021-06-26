package com.koko.smoothmedia.screens.home

import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.koko.smoothmedia.R
import com.koko.smoothmedia.dataclass.SongData

/**
 * [setTitle] sets the text  of the [TextView] to the title of the song [item]
 */
@BindingAdapter("songTitle")
fun  TextView.setTitle(item: SongData?){
    item?.let {
        text= it.title

    }

}

/**
 * [setArtistName] sets the text of the [TextView] to the artist name of the [item]
 */
@BindingAdapter("artistName")
fun TextView.setArtistName(item: SongData?){
    item?.let {
        text = it.displayName
    }
}

@BindingAdapter("songImage")
fun ImageView.setSongImage(item: SongData?){
    item?.let {
        setImageResource(R.drawable.exo_icon_circular_play)


    }
}