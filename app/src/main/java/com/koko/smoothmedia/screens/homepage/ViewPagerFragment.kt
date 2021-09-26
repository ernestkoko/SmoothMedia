package com.koko.smoothmedia.screens.homepage

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.koko.smoothmedia.R
import com.koko.smoothmedia.databinding.FragmentViewPagerBinding
import com.koko.smoothmedia.screens.homepage.permissions.PermissionFragment.Companion.PERMISSION_SUCCESSFUL
import com.koko.smoothmedia.screens.homepage.permissions.PermissionViewModel


class ViewPagerFragment : Fragment() {
    private val TAG = "ViewPagerFragment"
    private var binding: FragmentViewPagerBinding? = null
    private lateinit var viewPager: ViewPager2
    private lateinit var mHomePageFragmentAdapter: HomePageFragmentAdapter
    private lateinit var tabLayout: TabLayout
    private val permissionViewModel: PermissionViewModel by activityViewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navController = findNavController()


    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_view_pager, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.i(TAG, "onCreateView")


        mHomePageFragmentAdapter = HomePageFragmentAdapter(requireContext(),this)
        viewPager = binding?.viewPager!!
        tabLayout = binding?.tabLayout!!
        viewPager.adapter = mHomePageFragmentAdapter
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            Log.i(TAG, "Mediator:Called")
            when (position) {
                0 -> tab.text = "Songs"
                1 -> tab.text = "Videos"
                2 -> tab.text = "Albums"
                else -> tab.text = "Position ${position + 1}"
            }

        }.attach()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}