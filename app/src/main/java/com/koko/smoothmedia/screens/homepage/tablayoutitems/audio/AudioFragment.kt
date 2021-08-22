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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.koko.smoothmedia.R
import com.koko.smoothmedia.databinding.FragmentHomeScreenBinding
import com.koko.smoothmedia.dataclass.Song
import com.koko.smoothmedia.dataclass.SongData
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
val permissions: Array<String> = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
const val AUDIO_CHANNEL_ID = "AUDIO_CHANNEL_ID"

class AudioFragment : Fragment() {
    private lateinit var binding: FragmentHomeScreenBinding
    private val EXTERNAL_READ_PERMISSION_CODE = 11
   // private lateinit var mAudioFragmentViewModel: AudioFragmentViewModel
    private lateinit var mMyAdapter: AudioFragmentRecyclerViewAdapter
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private  val viewModel by viewModels<AudioFragmentViewModel> {
        InjectorUtils.provideAudioFragmentViewModel(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home_screen, container, false)
        binding.lifecycleOwner = this


        binding.homeViewModel = viewModel
        //initialises the adapter
        initialiseAdapter()


        checkForPermission(requireContext())

  viewModel.rootMediaId.observe(viewLifecycleOwner, {rootMediaId ->
      rootMediaId?.let {
          Log.i(TAG, "$rootMediaId: RootMediaId")
          viewModel.subscribe(rootMediaId)
      }

  })

        return binding.root

    }

    private fun provideMusicServiceConnection(context: Context): MusicServiceConnection {
        return MusicServiceConnection.getInstance(
            context,
            ComponentName(context, AudioService::class.java)
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)


        registerForActivityResult()
        //create notification channel
        createNotificationChannel()
    }

    private fun registerForActivityResult() {
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            if (it) {
                Log.i(TAG, "RegisterForResult Permission: Granted")
            } else {
                Log.i(TAG, "RegisterForResult Permission: Not Granted")


            }


        }
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
        binding.songsListView.layoutManager = LinearLayoutManager(context)
        binding.songsListView.adapter = mMyAdapter
        //observe the list of songs, if it is not null submit the list to the adapter
        viewModel.songsList.observe(viewLifecycleOwner, {
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
    private fun submitSongsList(it: List<Song>) {

        mMyAdapter.submitSongsList(it)
    }

    /**
     * [checkForPermission] checks if permission is already granted, if not it diplays a page to the
     * user why the permission is needed and then requests for permission
     */
    private fun checkForPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                //check if permission is granted already
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.i(TAG, "Permission granted1")
                    //initialise adapter and submit list
                    initialiseAdapter()

                }
                //show a reason to the user why permission is needed to be granted by the user
                shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                    Log.i(TAG, "Permission Show Reason to grant permission")


                    //display educational ui showing the reason permission is needed
                }
                //launch the permission dialog
                else -> {
                    Log.i(TAG, "Permission Not granted1")
                    //directly ask for permission now
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }

        }
    }

    private fun isPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val readExternalStoragePermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            readExternalStoragePermission == PackageManager.PERMISSION_GRANTED
        }

    }

    private fun takePermission() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.setData(Uri.parse(String.format("package:%s", requireContext().packageName)))
                // activity.startActivity()

            } catch (error: Exception) {

            }
        }
    }

    override fun onResume() {
        super.onResume()

        Log.i(TAG, "onResume: Fired")
        //demand for the permission to be granted again
        // checkForPermission(requireContext())
    }

    /**
     * [createNotificationChannel] creates notification channel that will be used for the foreground
     * Service
     */
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = ("Notification Test")
            val descriptionText = ("Notification description")
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(AUDIO_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                requireActivity().getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager
            notificationManager.createNotificationChannel(channel)
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