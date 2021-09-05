package com.koko.smoothmedia.screens.homepage.tablayoutitems.audio

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.koko.smoothmedia.R
import com.koko.smoothmedia.databinding.FragmentHomeScreenBinding
import com.koko.smoothmedia.dataclass.Song
import com.koko.smoothmedia.mediasession.mediaconnection.MusicServiceConnection
import com.koko.smoothmedia.mediasession.services.AudioService
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
        //initialises the adapter
        initialiseAdapter()
        binding?.lifecycleOwner = this
        binding!!.homeViewModel = viewModel

        viewModel.rootMediaId.observe(viewLifecycleOwner, { rootMediaId ->
            rootMediaId?.let {
                Log.i(TAG, "$rootMediaId: RootMediaId")
                viewModel.subscribe(rootMediaId)
            }

        })

        return binding!!.root

    }




    /**
     * initialises the adapter and assigns the manager and adapter to to the view
     */
    private fun initialiseAdapter() {

        //initialise the adapter
        mMyAdapter = AudioFragmentRecyclerViewAdapter(
            AudioFragmentRecyclerViewAdapter.OnClickListener {
                viewModel.onSongClicked(it)
            }
        )
        //set the layout manager and adapter for the Recycler view
        binding!!.songsListView.layoutManager = LinearLayoutManager(context)
        binding!!.songsListView.adapter = mMyAdapter
        //observe the list of songs, if it is not null submit the list to the adapter
        viewModel.songsList.observe(viewLifecycleOwner, {
            Log.i("HomeScreen:", "List: ${it}")
            it?.let {
                mMyAdapter.submitSongsList(it)
            }
        })
    }







    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }


}

private const val TAG: String = "HomeScreen"