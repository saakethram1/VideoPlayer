package com.example.videoplayer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.videoplayer.databinding.ActivityPlayerBinding
import com.example.videoplayer.databinding.FoldersViewBinding
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer

class PlayerActivity : AppCompatActivity() {

    companion object{
        lateinit var player:SimpleExoPlayer
        lateinit var playerList: ArrayList<Video>
        var position:Int=-1
    }

    lateinit var binding: ActivityPlayerBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeLayout()

     }
    private fun initializeLayout(){
        when(intent.getStringExtra("class")){
            "AllVideos"->{
                playerList= ArrayList()
                playerList.addAll(MainActivity.videoList)
            }
            "FoldersActivity"->{
                playerList= ArrayList()
                playerList.addAll(FoldersActivity.currentFolderVideos)
            }
        }

        createPlayer()
    }

    private fun createPlayer(){
        player= SimpleExoPlayer.Builder(this).build()
        binding.playerView.player= player
        val mediaItem =MediaItem.fromUri(playerList[position].artUri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}