package com.rushibhosale.myvideoplayer1

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.rushibhosale.myvideoplayer1.databinding.FragmentFoldersBinding
import com.rushibhosale.myvideoplayer1.databinding.FragmentVideoBinding

class VideoFragment : Fragment() {
    lateinit var adapter: VideoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_video, container, false)

        val binding = FragmentVideoBinding.bind(view)
        binding.videoRV.setHasFixedSize(true)
        binding.videoRV.setItemViewCacheSize(50)
        binding.videoRV.layoutManager = LinearLayoutManager(requireContext())
        adapter = VideoAdapter(requireContext(), MainActivity.videoList)
        binding.videoRV.adapter = adapter
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.options_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    fun updateVideoList(videoList: ArrayList<Video>) {
        // Update the video list in your adapter
        adapter.updateList(videoList)

        // Notify the adapter about the data changes
        adapter.notifyDataSetChanged()
    }

}