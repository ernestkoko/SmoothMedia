package com.koko.smoothmedia.screens.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.koko.smoothmedia.R
import com.koko.smoothmedia.databinding.FragmentHomeScreenBinding
import com.koko.smoothmedia.dataclass.SongData


/**
 * A simple [Fragment] subclass.
 * Use the [HomeScreen] factory method to
 * create an instance of this fragment.
 *
 */
val permissions: Array<String> = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

class HomeScreen : Fragment() {
    private lateinit var binding: FragmentHomeScreenBinding
    private val EXTERNAL_READ_PERMISSION_CODE = 11
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var myAdapter: SongAdapter
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home_screen, container, false)
        binding.lifecycleOwner = this

        //get the application
        val application = requireNotNull(this.activity).application
        //create the view model factory
        val viewModelFactory = HomeViewModelFactory(application)
        //create the view model
        homeViewModel = ViewModelProvider(this, viewModelFactory).get(HomeViewModel::class.java)

        //bind the view model
        binding.homeViewModel = homeViewModel
        //initialises the adapter
        initialiseAdapter()


        //initialise the activity launcher for permission
        requestPermissionLauncher = activityResultLauncher()
        //checks for permission and request for it when not granted already
        checkForPermission(requireContext())

        return binding.root

    }

    /**
     * initialises the adapter and assigns the manager and adapter to to the view
     */
    private fun initialiseAdapter() {
        //query for songs from view model
        homeViewModel.launchQuerySongs()
        //initialise the adapter
        myAdapter = SongAdapter(
            SongAdapter.OnClickListener {
                homeViewModel.onSongClicked(it)
            }
        )
        //set the layout manager and adapter for the Recycler view
        binding.songsListView.layoutManager = LinearLayoutManager(context)
        binding.songsListView.adapter = myAdapter

        //observe the list of songs, if it is not null submit the list to the adapter
        homeViewModel.songsList.observe(viewLifecycleOwner, {
            Log.i("HomeScreen:", "List: ${it}")
            it?.let {
                submitSongsList(it)
            }
        })
    }

    /**
     * [activityResultLauncher] runs the first time the app is started and registers and call back
     * that will be called when the permission dialog pops up to check for the users optionof
     * whether the permission was GRANTED or DENIED
     */
    private fun activityResultLauncher() = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted. Continue the action or workflow in your
            // app.
            Log.i(TAG, "Permission granted22")
            //initialise adapter and submit list
            initialiseAdapter()

        } else {
            Log.i(TAG, "Permission Not granted22")
            // Explain to the user that the feature is unavailable because the
            // features requires a permission that the user has denied. At the
            // same time, respect the user's decision. Don't link to system
            // settings in an effort to convince the user to change their

            // decision.
            Toast.makeText(
                requireContext(),
                "Permission to get Audio denied",
                Toast.LENGTH_LONG
            ).show()

        }
    }

    /**
     * [submitSongsList] submits the list to the recycler view adapter
     */
    private fun submitSongsList(it: List<SongData>) {

        myAdapter.submitSongsList(it)
    }

    /**
     * [checkForPermission] checks if permission is already granted, if not it diplays a page to the
     * user why the permission is needed and then requests for permission
     */
    private fun checkForPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.i(TAG, "Permission granted1")
                    //initialise adapter and submit list
                    initialiseAdapter()

                }
                shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {

                    //display educational ui showing the reason permission is needed
                }
                else -> {
                    Log.i(TAG, "Permission Not granted1")
                    //directly ask for permission now
                    requestPermissionLauncher.launch(

                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                }
            }

        }
    }


    /**
     * [onRequestPermissionsResult] gets fired when permission request result(s) is/are ready
     */
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//
//        Log.i(TAG, "Permission Request called")
//        when (requestCode) {
//            EXTERNAL_READ_PERMISSION_CODE -> {
//                if (grantResults.isNotEmpty() &&
//                    grantResults[0] == PackageManager.PERMISSION_GRANTED
//                ) {
//                    Log.i(TAG, "Permission granted2")
//                    Log.i(TAG, "Permission granted2")
////                    homeViewModel.launchQuerySongs()
////                    myAdapter.submitSongsList(homeViewModel.songsList.value)
////                    myAdapter.notifyDataSetChanged()
//
//                } else {
//                    Toast.makeText(
//                        requireContext(),
//                        "Permission to get Audio denied",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            }
//            else -> {
//
//
//            }
//
//        }
  //  }


}

private const val TAG: String = "HomeScreen"