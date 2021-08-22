package com.koko.smoothmedia.screens.homepage.tablayoutitems.audio

import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.koko.smoothmedia.R
import com.koko.smoothmedia.dataclass.Song
import com.koko.smoothmedia.dataclass.SongData

/**
 * [setTitle] sets the text  of the [TextView] to the title of the song [item]
 */
@BindingAdapter("songTitle")
fun  TextView.setTitle(item: Song?){
    item?.let {
        text= it.title

    }

}

/**
 * [setArtistName] sets the text of the [TextView] to the artist name of the [item]
 */
@BindingAdapter("artistName")
fun TextView.setArtistName(item: Song?){
    item?.let {
        text = it.artistName
    }
}

@BindingAdapter("songImage")
fun ImageView.setSongImage(item: Song?){
    item?.let {
        setImageResource(R.drawable.exo_icon_circular_play)


    }
}