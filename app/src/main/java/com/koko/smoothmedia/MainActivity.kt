package com.koko.smoothmedia


import android.Manifest
import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import com.koko.smoothmedia.databinding.ActivityMainBinding
import com.koko.smoothmedia.utils.InjectorUtils


class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private lateinit var binding: ActivityMainBinding
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController
    private lateinit var fragmentManager: FragmentManager


    private val _permissionGranted = MutableLiveData<Boolean>()
    private val permissionGranted: LiveData<Boolean>
        get() = _permissionGranted

    private val viewModel by viewModels<MainActivityViewModel> {
        InjectorUtils.provideMainActivityViewModel(this.application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        //setSupportActionBar(toolbar)
        // viewPagerFragment = ViewPagerFragment()
        // permissionFragment = PermissionFragment()
        //connect the view model
        binding.mainActivityViewModel = viewModel
        fragmentManager = supportFragmentManager
        val navHost = fragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController
        //binding.navView.setupWithNavController(navController)


        appBarConfiguration = AppBarConfiguration(
            topLevelDestinationIds = setOf(
                R.id.view_pager,
                R.id.permissionFragment,
                R.id.appSettingsFragment
            ),
            binding.drawerLayout, fallbackOnNavigateUpListener = ::onSupportNavigateUp
        )


        //setupActionBarWithNavController(navController, binding.drawerLayout)
        binding.navView.setupWithNavController(navController)
        toolbar.setupWithNavController(navController, appBarConfiguration)


        binding.songTitle.movementMethod= ScrollingMovementMethod()


        //connect the view model
        // binding.mainActivityViewModel = viewModel
        viewModel.rootMediaId.observe(this,
            { rootId ->

                initialiseAnObservedData()
            })


    }


    //    private fun hideMetadataViews() {
//        binding.motionLayout.getConstraintSet(R.id.start)?.let {
//            it.setVisibility(R.id.reference_container, View.GONE)
//            it.setVisibility(R.id.play_pause_button, View.GONE)
//            it.setVisibility(R.id.song_title, View.GONE)
//            it.setVisibility(R.id.song_subtitle, View.GONE)
//            it.setVisibility(R.id.album_image, View.GONE)
//
//        }
//
////    }
    private fun releaseMetadataViews() {
        binding.motionLayout.getConstraintSet(R.id.start)?.let {
            it.setVisibility(R.id.reference_container, View.VISIBLE)
            it.setVisibility(R.id.play_pause_button, View.VISIBLE)
            it.setVisibility(R.id.song_title, View.VISIBLE)
            it.setVisibility(R.id.song_subtitle, View.VISIBLE)
            it.setVisibility(R.id.album_image, View.VISIBLE)

        }
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onStart() {
        super.onStart()
        registerPermissionsCallback()
        requestPermission()


    }

    private fun initialiseAnObservedData() {
// Attach observers to the LiveData coming from this ViewModel
        viewModel.mediaMetadata.observe(this,
            Observer { mediaItem ->
                mediaItem?.let {
                    updateUI(binding.root, mediaItem)

                }


            })

        viewModel.animatePlayPauseButton.observe(this, { animateView ->
            if (animateView) scaler(binding.playPauseButton) else viewModel.doneWithAnimation()

        })
        viewModel.animatePreviousButton.observe(this, { animateView ->

            if (animateView) scaler(binding.previousButton) else viewModel.doneWithAnimation()

        })
        viewModel.animateNextButton.observe(this, { animateView ->
            if (animateView) scaler(binding.nextButton) else viewModel.doneWithAnimation()

        })



        viewModel.mediaButtonRes.observe(this,
            Observer { res ->

                binding.playPauseButton.setImageResource(res)
            })
        viewModel.mediaPosition.observe(this,
            Observer { pos ->
                //  binding.position.text = MainActivityViewModel.NowPlayingMetadata.timestampToMSS(context, pos)
            })


    }


    private fun registerPermissionsCallback() {
        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts
                    .RequestPermission()
            ) { isGranted ->
                Log.i(TAG, "registerPermissionsCallback: isGranted: $isGranted")
                if (isGranted) {
                    Log.i(TAG, "registerPermissionsCallback: isGranted1: $isGranted")
                    _permissionGranted.value = true


                    //beginTransaction(viewPagerFragment)

                } else {
                    Log.i(TAG, "registerPermissionsCallback: isGranted2: $isGranted")
                    _permissionGranted.value = false
                    //beginTransaction(permissionFragment)


                }

            }
    }


    private fun requestPermission() {
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.i(TAG, "requestPermission(): Permission: Granted")
                    //permission granted. Continue with the ui flow
                    _permissionGranted.value = true
                    //beginTransaction(viewPagerFragment)


                }
                shouldShowRequestPermissionRationale(permission) -> {
                    Log.i(TAG, "requestPermission(): ShowUi")
                    _permissionGranted.value = false
                    //display an educational ui
                    // beginTransaction(permissionFragment)

                }
                else -> {
                    Log.i(TAG, "requestPermission(): Permission: NotGranted")

                    //request permission
                    requestPermissionLauncher.launch(permission)
                }
            }
        }

    }


    private fun ObjectAnimator.disableViewDuringAnimation(view: View, viewEnabled: Boolean = true) {
        addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
                view.isEnabled = viewEnabled
            }

            override fun onAnimationEnd(animation: Animator?) {
                view.isEnabled = viewEnabled
            }

            override fun onAnimationCancel(animation: Animator?) {

            }

            override fun onAnimationRepeat(animation: Animator?) {

            }
        })

    }

    /**
     * AN animation function that changes the size of a view when clicked
     */
    private fun scaler(view: View) {
        //define the properties
        Log.i(TAG, "Scaler Called")
        val scaleX: PropertyValuesHolder = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.3f)
        val scaleY: PropertyValuesHolder =
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.3f)
        //define the object
        val animator: ObjectAnimator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY)

        animator.repeatCount = 1
        animator.repeatMode = ObjectAnimator.REVERSE

        // animator.disableViewDuringAnimation(binding.playPauseButton )
        animator.start()

    }

    private fun trackMotionLayout() {
        //binding.motionLayout
    }

    /**
     * Internal function used to update all UI elements except for the current item playback
     */
    private fun updateUI(view: View, metadata: MainActivityViewModel.NowPlayingMetadata) =
        with(binding) {
            //release views to VISIBLE
            releaseMetadataViews()


            Log.i(TAG, "updateUI: imageUri: ${metadata.albumArtUri}")
            if (metadata.albumArtUri == Uri.EMPTY) {
                albumImage.setImageResource(R.drawable.ic_album_black_24dp)
            } else {
                Glide.with(view)
                    .load(metadata.albumArtUri).transform(CenterCrop(), RoundedCorners(12))
                    .override(SIZE_ORIGINAL, SIZE_ORIGINAL)
                    .into(albumImage)
            }
            Log.i(TAG, "updateUI: id: ${metadata.id}, metaData: ${metadata}")


            songTitle.text = metadata.title
            songSubtitle.text = metadata.subtitle
            //duration.text = metadata.duration


        }


}

private val TAG = "MainActivity"
private val FRAGMENT_TAG = "FRAGMENT_TAG"