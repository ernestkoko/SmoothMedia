package com.koko.smoothmedia.screens.homepage.tablayoutitems.audio

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koko.smoothmedia.databinding.SongItemBinding
import com.koko.smoothmedia.dataclass.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [AudioFragmentRecyclerViewAdapter] is the adapter for the recycler view
 */

class AudioFragmentRecyclerViewAdapter(val clickListener: OnClickListener) :
    ListAdapter<Song, RecyclerView.ViewHolder>(SongDiffCallback()) {
    val adapterScope = CoroutineScope(Dispatchers.Default)

    /**
     * [onCreateViewHolder] creates the view holder and returns an instance of [RecyclerView.ViewHolder]
     * We can manipulate the [ViewGroup] it returns here, that is if we have more than one(1)
     * parent view to return
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return SongViewHolder.from(parent)
    }

    /**
     * [onBindViewHolder] triggers to bind the view
     */
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {

        onBindViewHolder(holder, position)


    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val data = getItem(position)
        when (holder) {
            is SongViewHolder -> {
                holder.bind(data, clickListener)


            }
        }
    }

    /**
     * [submitSongsList] is a method that takes a list of songs[list] to be given to the
     * [ListAdapter]
     */
    fun submitSongsList(list: List<Song>?) {
        adapterScope.launch {
            val items = when (list) {
                null -> null
                else -> list.map { it }.toList()
            }
            withContext(Dispatchers.Main) {

                submitList(items!!.toList())


            }
        }
    }

    /**
     * [SongViewHolder] is the view holder for the recycler view
     * [binding] is the binding object
     */

    class SongViewHolder private constructor(val binding: SongItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        /**
         * [bind] is a function that [songData] to the song  and [clickListener]
         *  to the click listener in the binding object and then executes any pending bindings
         *
         */
        fun bind(song: Song, clickListener: OnClickListener) {
            binding.song = song

            binding.clickListener = clickListener
           binding.imageView=  binding.moreIcon

            binding.executePendingBindings()

        }

        companion object {
            /**
             * [from] is a class method that returns [SongViewHolder] and takes in a
             * view group [parent]. It inflates the layer from the [ViewGroup] context
             */
            fun from(parent: ViewGroup): SongViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val view = SongItemBinding.inflate(inflater, parent, false)
                return SongViewHolder(view)
            }
        }

    }


    /**
     * [OnClickListener] is a class that has a call back [clickListener] that will be triggered
     * when an item(song) is clicked in the recycler view
     */
    class OnClickListener(
        val menuClickListener: (song: Song,view: View) -> Unit,
        val clickListener: (song: Song) -> Unit
    ) {
        /* This handles clicking a list item */
        fun onClick(song: Song) = clickListener(song)
        /* This handles clicking a "more" view for menu in the list item */
        fun onMenuItemClick(song: Song, v: View) = menuClickListener(song, v)
    }
}

/**
 * [SongDiffCallback] is a class that checks if a song has changed in any form on the list
 */
class SongDiffCallback : DiffUtil.ItemCallback<Song>() {
    private val TAG = "SongDiffCallback"


    override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
        Log.i(TAG, "AreCItemsTheSame")
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
        Log.i(TAG, "AreContentTheSame")

        return oldItem.id == newItem.id && oldItem.isPlaying == newItem.isPlaying
    }

    override fun getChangePayload(oldItem: Song, newItem: Song): Any? {
        Log.i(TAG, "getChangePayload")
        return if (oldItem.isPlaying != newItem.isPlaying) true else null

    }

}

