package com.rushibhosale.myvideoplayer1

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.rushibhosale.myvideoplayer1.databinding.FolderItemViewBinding
import com.rushibhosale.myvideoplayer1.databinding.FragmentFoldersBinding


class FoldersFragment : Fragment() {

    lateinit var adapter: FolderAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_folders, container, false)
        val binding = FragmentFoldersBinding.bind(view)
        binding.folderRV.setHasFixedSize(true)
        binding.folderRV.setItemViewCacheSize(10)
        binding.folderRV.layoutManager = LinearLayoutManager(requireContext())
        adapter  =  FolderAdapter(requireContext(), MainActivity.folderList)
        binding.folderRV.adapter = adapter
        return  view
    }

    fun updateFolderList(folderList: ArrayList<Folder>) {

            adapter.updateList(folderList)

            // Notify the adapter about the data changes
            adapter.notifyDataSetChanged()

    }


}