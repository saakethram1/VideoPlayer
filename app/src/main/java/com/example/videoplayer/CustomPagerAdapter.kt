package com.example.videoplayer.com.example.videoplayer

import androidx.fragment.app.Fragment
import com.example.videoplayer.FoldersFragment
import com.example.videoplayer.VideosFragment

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class CustomPagerAdapter(fragmentManager: FragmentManager) :
    FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        // Return the fragment instance for each position
        return when (position) {
            0 -> VideosFragment()
            1 -> FoldersFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }

    override fun getCount(): Int {
        // Return the total number of fragments
        return 2 // Adjust based on the number of fragments
    }
}
