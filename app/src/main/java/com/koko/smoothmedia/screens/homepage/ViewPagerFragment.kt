package com.koko.smoothmedia.screens.homepage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.koko.smoothmedia.R
import com.koko.smoothmedia.databinding.FragmentViewPagerBinding


class ViewPagerFragment : Fragment() {
    private var binding: FragmentViewPagerBinding? = null
    private lateinit var viewPager: ViewPager2
    private lateinit var mHomePageFragmentAdapter: HomePageFragmentAdapter
    private lateinit var tabLayout: TabLayout

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
        mHomePageFragmentAdapter = HomePageFragmentAdapter(this)
        viewPager = binding?.viewPager!!
        tabLayout = binding?.tabLayout!!
        viewPager.adapter = mHomePageFragmentAdapter
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text= "Songs"
                1-> tab.text= "Videos"
                else -> tab.text = "Position ${position + 1}"
            }

        }.attach()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}