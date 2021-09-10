package com.koko.smoothmedia.screens.homepage.tablayoutitems.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.koko.smoothmedia.R
import com.koko.smoothmedia.screens.homepage.tablayoutitems.audio.AudioFragmentViewModel
import com.koko.smoothmedia.utils.InjectorUtils


class VideoFragment : Fragment() {
    private val viewModel by viewModels<VideoFragmentViewModel> {
        InjectorUtils.provideVideoFragmentViewModel(requireActivity().application)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_video, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.subscribe()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment VideoFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() =
            VideoFragment()
    }
}