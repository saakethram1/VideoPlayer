package com.example.videoplayer

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.pm.PackageManager

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.videoplayer.databinding.ActivityMainBinding
import java.io.File
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

   private lateinit var binding: ActivityMainBinding
     private lateinit var toggle:ActionBarDrawerToggle

    companion object{
        lateinit var videoList:ArrayList<Video>
        lateinit var folderList: ArrayList<Folder>
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setTheme(R.style.coolPinkNav)
        setContentView(binding.root)

        //for nav drawer
        toggle= ActionBarDrawerToggle(this,binding.root,R.string.open,R.string.close)
        binding.root.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

         if(requestRuntimePermission()){
             folderList= ArrayList()
             videoList=getAllVideos()
             setFragment(VideosFragment())
         }

       binding.bottomNav.setOnItemSelectedListener {
           when(it.itemId){
               R.id.videoView->setFragment(VideosFragment())
               R.id.foldersView->setFragment(FoldersFragment())
           }
           return@setOnItemSelectedListener true
       }
        binding.navView.setNavigationItemSelectedListener {
            when(it.itemId){
                R.id.feedbackNav->Toast.makeText(this,"Feedback",Toast.LENGTH_SHORT).show()
                R.id.themesNav->Toast.makeText(this,"Themes",Toast.LENGTH_SHORT).show()
                R.id.sortOrderNav->Toast.makeText(this,"Sort Order",Toast.LENGTH_SHORT).show()
                R.id.aboutNav->Toast.makeText(this,"About",Toast.LENGTH_SHORT).show()
                R.id.exitNav-> exitProcess(1)


            }
            return@setNavigationItemSelectedListener true
        }
    }
    private fun setFragment(fragment: Fragment){
        val transaction=supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentFL,fragment)
        transaction.disallowAddToBackStack()
        transaction.commit()
    }
   //for requesting permission
    private fun requestRuntimePermission():Boolean{
        if(ActivityCompat.checkSelfPermission(this,WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE),13)
            return false
        }
       return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode==13)
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Permission GRanted",Toast.LENGTH_SHORT).show()
                  folderList=ArrayList()
                  videoList=getAllVideos()
                 setFragment(VideosFragment())}
        else
                ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE),13)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(toggle.onOptionsItemSelected(item))
            return true
        return super.onOptionsItemSelected(item)
    }
    @SuppressLint("InLinedApi","Recycle")
   private fun getAllVideos():ArrayList<Video>{
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
        val cursor = this.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            MediaStore.Video.Media.DATE_ADDED + " DESC"
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
                        folderList.add(Folder(id=folderIdC, folderName = folderC))
                    }
                } catch (e: Exception) {
                    // Handle the exception if necessary
                }
            }
        }
        return tempList
   }
}