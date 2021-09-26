package com.koko.smoothmedia.screens.homepage.tablayoutitems.album

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.koko.smoothmedia.R
import com.koko.smoothmedia.databinding.FragmentAlbumBinding
import com.koko.smoothmedia.screens.homepage.tablayoutitems.audio.AudioFragmentViewModel
import com.koko.smoothmedia.utils.InjectorUtils

class AlbumFragment : Fragment() {

    private var binding: FragmentAlbumBinding? = null
    private lateinit var mMyAdapter: AlbumFragmentRecyclerViewAdapter
    private val viewModel by viewModels<AlbumFragmentViewModel> {
        InjectorUtils.provideAlbumFragmentViewModel(requireActivity().application)
    }
    private val listAdapter = AlbumFragmentRecyclerViewAdapter { clickedItem ->
        Log.i(TAG, "ItemClicked: $clickedItem")
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_album, container, false)
        binding!!.viewModel = viewModel
        binding!!.albumRecyclerView.layoutManager= LinearLayoutManager(context)
        binding!!.albumRecyclerView.adapter = listAdapter
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.songsList.observe(viewLifecycleOwner, {
            it?.let {
                Log.i(TAG, "Children: $it")
                listAdapter.submitList(it)
            }
        })

    }


    override fun onDestroy() {
        super.onDestroy()
        //for garbage collection
        binding = null
    }
}
private val TAG = "AlbumFragment"