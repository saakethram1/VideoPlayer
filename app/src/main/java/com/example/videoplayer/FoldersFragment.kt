package com.example.videoplayer

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.videoplayer.databinding.FragmentFoldersBinding
import com.example.videoplayer.databinding.FragmentVideosBinding

class FoldersFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view= inflater.inflate(R.layout.fragment_folders, container, false)

        val binding= FragmentFoldersBinding.bind(view)


        binding.FoldersRV.setHasFixedSize(true)
        binding.FoldersRV.setItemViewCacheSize(10)
        binding.FoldersRV.layoutManager= LinearLayoutManager(requireContext())
        binding.FoldersRV.adapter=FoldersAdapter(requireContext(),MainActivity.folderList)
        binding.totalFolders.text="Total Folders:${MainActivity.folderList.size}"

        return view
    }


}