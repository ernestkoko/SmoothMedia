package com.koko.smoothmedia


import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.koko.smoothmedia.databinding.ActivityMainBinding
import com.koko.smoothmedia.utils.InjectorUtils


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val viewModel by viewModels<MainActivityViewModel> {
        InjectorUtils.provideMainActivityViewModel(this.application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //connect the view model
        binding.mainActivityViewModel = viewModel

        viewModel.animatePlayPauseButton.observe(this,{animateView->
            if (animateView) scaler(binding.playPauseButton) else viewModel.doneWithAnimation()

        })
        viewModel.animatePreviousButton.observe(this,{animateView->
            if (animateView) scaler(binding.previousButton) else viewModel.doneWithAnimation()

        })
        viewModel.animateNextButton.observe(this,{animateView->
            if (animateView) scaler(binding.nextButton) else viewModel.doneWithAnimation()

        })
        // Always true, but lets lint know that as well.
        val context = this ?: return


        // Attach observers to the LiveData coming from this ViewModel
        viewModel.mediaMetadata.observe(this,
            Observer { mediaItem ->
                Log.i(TAG, "MediaItem: ${mediaItem.subtitle}")
//                binding.songTitle.text = mediaItem.title
//                binding.songSubtitle.text = mediaItem.subtitle

                updateUI(binding.root, mediaItem)
            })
        viewModel.mediaButtonRes.observe(this,
            Observer { res ->

                // binding.mediaButton.setImageResource(res)
            })
        viewModel.mediaPosition.observe(this,
            Observer { pos ->
                //  binding.position.text = MainActivityViewModel.NowPlayingMetadata.timestampToMSS(context, pos)
            })



    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun ObjectAnimator.disableViewDuringAnimation(view: View, viewEnabled: Boolean=true) {
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
    /**
     * Internal function used to update all UI elements except for the current item playback
     */
    private fun updateUI(view: View, metadata: MainActivityViewModel.NowPlayingMetadata) = with(binding) {
        if (metadata.albumArtUri == Uri.EMPTY) {
            albumImage.setImageResource(R.drawable.ic_album_black_24dp)
        } else {
            Glide.with(view)
                .load(metadata.albumArtUri)
                .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .into(albumImage)
        }
        songTitle.text = metadata.title
        songSubtitle.text = metadata.subtitle
        //duration.text = metadata.duration
    }


}

private val TAG = "MainActivity"