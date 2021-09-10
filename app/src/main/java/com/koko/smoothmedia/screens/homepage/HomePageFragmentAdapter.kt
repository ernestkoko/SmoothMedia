package com.koko.smoothmedia.screens.homepage

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.koko.smoothmedia.screens.homepage.tablayoutitems.audio.AudioFragment
import com.koko.smoothmedia.screens.homepage.tablayoutitems.video.VideoFragment

/**
 * [HomePageFragmentAdapter] is an adapter that switches and creates views in the [ViewPagerFragment]
 * depending on the number of [getItemCount]
 * It takes a fragment
 */

class HomePageFragmentAdapter(fragment: Fragment): FragmentStateAdapter(fragment) {
    /**
     * [getItemCount] returns the number of fragments to be displayed
     */
    override fun getItemCount(): Int =5

    /**
     * [createFragment] creates the fragment at a given [position] and returns it
     */
    override fun createFragment(position: Int): Fragment {
        return when(position){
            0-> AudioFragment()
            else-> VideoFragment.newInstance()
        }
    }
}