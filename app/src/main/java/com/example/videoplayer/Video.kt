package com.example.videoplayer

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import java.io.File


data class Video(val id:String,
                 var title: String, val duration: Long=0, val folderName:String, val size:String, var path:String,
                 var artUri: Uri )

data class Folder(val id: String,val folderName: String)

@SuppressLint("InLinedApi","Recycle")
fun getAllVideos(context: Context):ArrayList<Video>{
    val sortEditor=context.getSharedPreferences("Sorting", AppCompatActivity.MODE_PRIVATE)
    MainActivity.sortValue = sortEditor.getInt("sortValue",0)
    val tempList = ArrayList<Video>()
    val tempFolderList=ArrayList<String>()
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
    val cursor = context.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
       MainActivity.sortList[MainActivity.sortValue]
    )
    cursor?.use { // Use cursor in a safe manner using the use function to auto-close it
        val titleColumnIndex = it.getColumnIndex(MediaStore.Video.Media.TITLE)
        val idColumnIndex = it.getColumnIndex(MediaStore.Video.Media._ID)
        val folderColumnIndex = it.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
        val folderIdColumnIndex=it.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)
        val sizeColumnIndex = it.getColumnIndex(MediaStore.Video.Media.SIZE)
        val pathColumnIndex = it.getColumnIndex(MediaStore.Video.Media.DATA)
        val durationColumnIndex = it.getColumnIndex(MediaStore.Video.Media.DURATION)

        while (it.moveToNext()) {
            val titleC = if (titleColumnIndex != -1) it.getString(titleColumnIndex) else ""
            val idC = if (idColumnIndex != -1) it.getString(idColumnIndex) else ""
            val folderC = if (folderColumnIndex != -1) it.getString(folderColumnIndex) else ""
            val folderIdC=if (folderIdColumnIndex!=-1) it.getString(folderIdColumnIndex) else ""
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
                if(!tempFolderList.contains(folderC)){
                    tempFolderList.add(folderC)
                    MainActivity.folderList.add(Folder(id=folderIdC, folderName = folderC))
                }
            } catch (e: Exception) {
                // Handle the exception if necessary
            }
        }
    }
    return tempList
}