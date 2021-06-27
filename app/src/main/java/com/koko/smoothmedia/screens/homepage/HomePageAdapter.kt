package com.koko.smoothmedia.screens.homepage

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.koko.smoothmedia.screens.home.HomeScreen

class HomePageAdapter(val fragment: Fragment): FragmentStateAdapter(fragment) {
    /**
     * [getItemCount] returns the number of fragments to be displayed
     */
    override fun getItemCount(): Int =4

    /**
     * [createFragment] creates the fragment at a given [position] and returns it
     */
    override fun createFragment(position: Int): Fragment {
        return when(position){
            0-> HomeScreen()
            else-> HomeScreen()
        }
    }
}