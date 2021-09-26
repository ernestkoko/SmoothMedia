package com.koko.smoothmedia.screens.homepage

import android.content.Context
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.koko.smoothmedia.mediasession.services.PersistentStorage
import com.koko.smoothmedia.screens.homepage.tablayoutitems.album.AlbumFragment
import com.koko.smoothmedia.screens.homepage.tablayoutitems.audio.AudioFragment
import com.koko.smoothmedia.screens.homepage.tablayoutitems.video.VideoFragment

/**
 * [HomePageFragmentAdapter] is an adapter that switches and creates views in the [ViewPagerFragment]
 * depending on the number of [getItemCount]
 * It takes a fragment
 */

class HomePageFragmentAdapter(context: Context, fragment: Fragment) :
    FragmentStateAdapter(fragment) {
    private val TAG = "HomePageFragAdapter"

    /**
     * [getItemCount] returns the number of fragments to be displayed
     */
    override fun getItemCount(): Int = 5
//    override fun getItemId(position: Int): Long {
//        Log.i(TAG, "FragmentId: $position")
//        storage.savePresentTab(position)
//
//        return
//    }

    private val storage = PersistentStorage.getInstance(context.applicationContext)

    /**
     * [createFragment] creates the fragment at a given [position] and returns it
     */
    override fun createFragment(position: Int): Fragment {

        Log.i("ViewPager", " Adapter: Called")
//        val index = storage.getPresentTab()
//        Log.i(TAG, "index: $index")
        return when (position) {

            0 -> {

                AudioFragment()
            }
            2 -> AlbumFragment()
            else -> VideoFragment.newInstance()
        }
    }
}