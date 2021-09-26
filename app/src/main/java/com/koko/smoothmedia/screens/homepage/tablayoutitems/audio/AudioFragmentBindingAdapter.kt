package com.koko.smoothmedia.screens.homepage.tablayoutitems.audio

import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.koko.smoothmedia.R
import com.koko.smoothmedia.dataclass.Song
import com.koko.smoothmedia.screens.homepage.tablayoutitems.audio.AudioFragmentRecyclerViewAdapter.*
import java.util.concurrent.TimeUnit

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
@BindingAdapter("numOSongs")
fun TextView.setNumOfSongs(item: Song?) {
    item?.let {
        text = it.itemCount.toString() +"songs"
    }
}

@BindingAdapter("songImage")
fun ImageView.setSongImage(item: Song?) {
    item?.let {
        setImageResource(R.drawable.exo_icon_circular_play)


    }
}


@BindingAdapter("duration")
fun TextView.setIsPlaying(item: Song?){

    item?.let{
        val time = item.duration/1000/60
        val seconds = (item.duration/1000 % 60)
        text = "$time:$seconds"
//        if(it.isPlaying!!){
//            Log.i("Adapter: ", "It's true: ID: ${it.id}: ")
//            text= "isPlaying"
//            setTextColor(resources.getColor(R.color.design_default_color_primary,null))
//        }
    }
}
@BindingAdapter("itemPopupMenu")
fun ImageView.setClickListener(onclick: OnClickListener){
    this.setOnClickListener {

    }

}
@BindingAdapter("theImage")
fun ImageView.manageImage(uri: Uri?) {
    Log.i("BindingAdapter", "setImage: uri: $uri")

    Glide.with(this).applyDefaultRequestOptions(glideOptions).load(uri)
        .transform(CenterCrop(), RoundedCorners(8)).into(this)

}

private val glideOptions = RequestOptions()
    .fallback(R.drawable.ic_album_black_24dp)
    .placeholder(R.drawable.ic_album_black_24dp)
    .diskCacheStrategy(DiskCacheStrategy.DATA)