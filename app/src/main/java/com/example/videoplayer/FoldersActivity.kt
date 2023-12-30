package com.example.videoplayer

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.videoplayer.databinding.ActivityFoldersBinding
import java.io.File

class FoldersActivity : AppCompatActivity() {
    lateinit var adapter:VideoAdapter
      companion object {
          lateinit var currentFolderVideos: ArrayList<Video>

      }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding=ActivityFoldersBinding.inflate(layoutInflater)
        setTheme(MainActivity.themeList[MainActivity.themeIndex])
        setContentView(binding.root)



        val position=intent.getIntExtra("position",0)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title=MainActivity.folderList[position].folderName
        currentFolderVideos=getAllVideos(MainActivity.folderList[position].id)
        binding.videoRVFA.setHasFixedSize(true)
        binding.videoRVFA.setItemViewCacheSize(10)
        binding.videoRVFA.layoutManager= LinearLayoutManager(this@FoldersActivity)
        adapter=VideoAdapter(this@FoldersActivity,currentFolderVideos, isFolder = true)
        binding.videoRVFA.adapter=adapter
        binding.totalVideosFA.text="Total Videos:${currentFolderVideos.size}"

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return true
    }
    @SuppressLint("InLinedApi","Recycle")
    private fun getAllVideos(folderId:String):ArrayList<Video>{
        val tempList = ArrayList<Video>()
         val selection=MediaStore.Video.Media.BUCKET_ID+" like? "
        val projection = arrayOf(
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_ID
        )
        val cursor = this.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,selection,
           arrayOf(folderId),MediaStore.Video.Media.DATE_ADDED+" DESC"
        )
        cursor?.use { // Use cursor in a safe manner using the use function to auto-close it
            val titleColumnIndex = it.getColumnIndex(MediaStore.Video.Media.TITLE)
            val idColumnIndex = it.getColumnIndex(MediaStore.Video.Media._ID)
            val folderColumnIndex = it.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

            val sizeColumnIndex = it.getColumnIndex(MediaStore.Video.Media.SIZE)
            val pathColumnIndex = it.getColumnIndex(MediaStore.Video.Media.DATA)
            val durationColumnIndex = it.getColumnIndex(MediaStore.Video.Media.DURATION)

            while (it.moveToNext()) {
                val titleC = if (titleColumnIndex != -1) it.getString(titleColumnIndex) else ""
                val idC = if (idColumnIndex != -1) it.getString(idColumnIndex) else ""
                val folderC = if (folderColumnIndex != -1) it.getString(folderColumnIndex) else ""

                val sizeC = if (sizeColumnIndex != -1) it.getString(sizeColumnIndex) else ""
                val pathC = if (pathColumnIndex != -1) it.getString(pathColumnIndex) else ""
                val durationC = if (durationColumnIndex != -1) it.getString(durationColumnIndex)?.toLong()
                    ?: 0L else 0L

                try {
                    val file = File(pathC)
                    if (file.exists()) {
                        val artUriC = Uri.fromFile(file)
                        val video = Video(
                            title = titleC,
                            id = idC,
                            folderName = folderC,
                            duration = durationC,
                            size = sizeC,
                            path = pathC,
                            artUri = artUriC
                        )
                        tempList.add(video)
                    }
                    //for adding folders

                } catch (e: Exception) {
                    // Handle the exception if necessary
                }
            }
        }
        return tempList
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        adapter.onResult(requestCode,resultCode)
    }
}