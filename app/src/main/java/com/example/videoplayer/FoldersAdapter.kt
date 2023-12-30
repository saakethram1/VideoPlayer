package com.example.videoplayer

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.videoplayer.databinding.FoldersViewBinding
import com.example.videoplayer.databinding.VideoViewBinding

class FoldersAdapter(private val context: Context, private var foldersList: ArrayList<Folder>):
    RecyclerView.Adapter<FoldersAdapter.MyHolder>() {
    class MyHolder(binding: FoldersViewBinding): RecyclerView.ViewHolder(binding.root){
        val folderName=binding.folderNameFV
        val root=binding.root

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(FoldersViewBinding.inflate(LayoutInflater.from(context),parent,false))
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
       holder.folderName.text=foldersList[position].folderName
        holder.root.setOnClickListener{
            val intent=Intent(context,FoldersActivity::class.java)
            intent.putExtra("position",position)
            ContextCompat.startActivity(context,intent,null)

        }
    }

    override fun getItemCount(): Int {
        return foldersList.size
    }
}