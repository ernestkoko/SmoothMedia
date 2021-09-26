package com.koko.smoothmedia.screens.homepage.tablayoutitems.audio

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.koko.smoothmedia.R
import com.koko.smoothmedia.databinding.FragmentHomeScreenBinding
import com.koko.smoothmedia.dataclass.Song
import com.koko.smoothmedia.utils.InjectorUtils

//import com.koko.smoothmedia.mediasession.services.AudioService


/**
 * A simple [Fragment] subclass.
 * Use the [AudioFragment] factory method to
 * create an instance of this fragment.
 *
 */


class AudioFragment : Fragment() {
    private var binding: FragmentHomeScreenBinding? = null
    private lateinit var mMyAdapter: AudioFragmentRecyclerViewAdapter
    private val viewModel by viewModels<AudioFragmentViewModel> {
        InjectorUtils.provideAudioFragmentViewModel(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home_screen, container, false)

        binding?.lifecycleOwner = this
        binding!!.homeViewModel = viewModel



        return binding!!.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.rootMediaId.observe(viewLifecycleOwner, { rootMediaId ->
            Log.i(TAG, "$rootMediaId: RootMediaId")
            rootMediaId?.let {
                Log.i(TAG, "$rootMediaId: RootMediaId")
                viewModel.subscribe(rootMediaId)
            }

        })

        //initialises the adapter
        initialiseAdapter()
    }

    override fun onStart() {
        super.onStart()
    }


    /**
     * initialises the adapter and assigns the manager and adapter to to the view
     */
    private fun initialiseAdapter() {

        //initialise the adapter
        mMyAdapter = AudioFragmentRecyclerViewAdapter(
            AudioFragmentRecyclerViewAdapter.OnClickListener(
                menuClickListener = { song, v ->

                    listItemMenu(song, v)
                },
                clickListener = {
                    viewModel.onSongClicked(it)
                })


        )
        //set the layout manager and adapter for the Recycler view
        binding!!.songsListView.layoutManager = LinearLayoutManager(context)
        binding!!.songsListView.adapter = mMyAdapter
        //observe the list of songs, if it is not null submit the list to the adapter
        viewModel.songsList?.observe(viewLifecycleOwner, {
            Log.i("HomeScreen", "List: ${it}")
            it?.let {
                val hasTrue = it.forEach {
                    Log.i(TAG, "MainSong: ${it.isPlaying}")
                    Log.i(TAG, "MainSong: ${it}")
                }
                mMyAdapter.submitSongsList(it)
            }
        })
    }

    /**
     * function for inflating the popup menu and setting the click listener for each item click
     */
    private fun listItemMenu(song: Song, view: View) {
        Log.i(TAG, "PopUpMenu: called")
        val popupmenu = PopupMenu(this.requireContext(), view)
        popupmenu.inflate(R.menu.list_item_more_menu)
        //popupmenu.menu.add("Text")
        popupmenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.add_to_playlist -> {
                    Log.i(TAG, "Add to Playlist clicked")
                    Toast.makeText(requireContext(), "Add to playlist: Clicked", Toast.LENGTH_LONG)
                        .show()
                    true
                }
                R.id.play_song -> {
                    Log.i(TAG, "Play song: Clicked")
                    Toast.makeText(requireContext(), "Play Song: Clicked", Toast.LENGTH_LONG)
                        .show()
                    //play the song
                    viewModel.onSongClicked(song)
                    true
                }
                else -> true
            }
        }
        popupmenu.show()
        val popup = popupmenu::class.java.getDeclaredField("mPopup")
        popup.isAccessible = true
        val menu = popup.get(popupmenu)
        menu.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java).invoke(menu, true)


    }


    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }


}

private const val TAG: String = "HomeScreen"