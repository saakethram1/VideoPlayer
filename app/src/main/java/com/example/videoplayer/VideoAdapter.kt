package com.example.videoplayer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.videoplayer.databinding.DetailsViewBinding
import com.example.videoplayer.databinding.MoreFeaturesBinding
import com.example.videoplayer.databinding.MoreVideoFeaturesBinding
import com.example.videoplayer.databinding.RenameFieldBinding
import com.example.videoplayer.databinding.VideoViewBinding
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class VideoAdapter(private val context: Context, private var videoList: ArrayList<Video>,private val isFolder:Boolean=false):
    RecyclerView.Adapter<VideoAdapter.MyHolder>() {
    private var newPosition=0
    private lateinit var dialogRF:AlertDialog
    class MyHolder(binding:VideoViewBinding):RecyclerView.ViewHolder(binding.root){
       val title=binding.videoName
        val folder=binding.folderName
        val duration=binding.duration
        val image=binding.videoImage
        val root=binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(VideoViewBinding.inflate(LayoutInflater.from(context),parent,false))
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: MyHolder, @SuppressLint("RecyclerView") position: Int) {
        holder.title.text=videoList[position].title
        holder.folder.text=videoList[position].folderName
        holder.duration.text=DateUtils.formatElapsedTime(videoList[position].duration/1000)
        Glide.with(context)
            .asBitmap()
            .load(videoList[position].artUri)
            .apply(RequestOptions().placeholder(R.mipmap.ic_video_player).centerCrop())
            .into(holder.image)
        holder.root.setOnClickListener {
            when{
                videoList[position].id==PlayerActivity.nowPlayingid->{
                    sendIntent(pos = position, ref = "NowPlaying")
                }
                isFolder->{
                    PlayerActivity.pipStatus=1
                    sendIntent(pos = position, ref = "FoldersActivity")
                }
                MainActivity.search->{
                    PlayerActivity.pipStatus=2
                    sendIntent(pos = position, ref = "SearchedVideos")
                }
                else->{
                    PlayerActivity.pipStatus=2
                    sendIntent(pos=position, ref = "AllVideos")
                }


            }
        }
        holder.root.setOnLongClickListener{
            newPosition=position
            val customDialog=LayoutInflater.from(context).inflate(R.layout.more_video_features,holder.root,false)
            val bindingMF = MoreVideoFeaturesBinding.bind(customDialog)
            val dialog= MaterialAlertDialogBuilder(context).setView(customDialog)

                .create()
            dialog.show()
            bindingMF.renameBtn.setOnClickListener{
                dialog.dismiss()
               requestWriteR()
            }

            bindingMF.shareBtn.setOnClickListener {
                dialog.dismiss()
                val shareIntent=Intent()
                shareIntent.action=Intent.ACTION_SEND
                shareIntent.type="video/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM,Uri.parse(videoList[position].path))
                ContextCompat.startActivity(context,Intent.createChooser(shareIntent,"Sharing Video File!!"),null)
            }

            bindingMF.infoBtn.setOnClickListener {

                dialog.dismiss()
                val customDialogIf=LayoutInflater.from(context).inflate(R.layout.details_view,holder.root,false)
                val bindingIf =DetailsViewBinding.bind(customDialogIf)
                val dialogIf= MaterialAlertDialogBuilder(context).setView(customDialogIf)
                    .setCancelable(false)
                    .setPositiveButton("Ok"){
                            self,_->

                        self.dismiss()
                    }

                    .create()
                dialogIf.show()
                val infoText=SpannableStringBuilder().bold { append("DETAILS\n\nName: ") }.append(videoList[position].title)
                    .bold { append("\n\nDuration: ") }.append(DateUtils.formatElapsedTime(videoList[position].duration/1000))
                    .bold { append("\n\nFile Size: ") }.append(Formatter.formatShortFileSize(context,videoList[position].size.toLong()))
                    .bold { append("\n\nLocation: ") }.append(videoList[position].path)
                bindingIf.detailTv.text=infoText
                dialogIf.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(MaterialColors.getColor(context,R.attr.themeColor,
                    Color.RED))

            }

            bindingMF.deleteBtn.setOnClickListener {
                dialog.dismiss()
                requestDeleteR(position=position)
            }

            return@setOnLongClickListener true
        }
    }
    fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> VideosFragment()
            1 -> FoldersFragment()
            else -> Fragment()
        }
    }
    override fun getItemCount(): Int {
        return videoList.size
    }
    private fun sendIntent(pos:Int,ref:String){
        PlayerActivity.position= pos
        val intent=Intent(context,PlayerActivity::class.java)
        intent.putExtra("class",ref)
        ContextCompat.startActivity(context,intent,null)
    }
  @SuppressLint("NotifyDataSetChanged")
   fun updateList(searchList:ArrayList<Video>){
      videoList= ArrayList()
      videoList.addAll(searchList)
      notifyDataSetChanged()
  }
    //for requesting android 11 or higher storage permissions
//    private fun requestPermissionR(){
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//               if( !Environment.isExternalStorageManager()){
//                   val intent=Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
//                   intent.addCategory("android.intent.category.DEFAULT")
//                   intent.data= Uri.parse("package:${context.applicationContext.packageName}")
//                   ContextCompat.startActivity(context,intent,null)
//               }
//            }
//
//    }
    private fun requestDeleteR(position: Int){
        val uriList:List<Uri> = listOf(Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,videoList[position].id))

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
          //requesting for delete permission
            val pi=MediaStore.createDeleteRequest(context.contentResolver,uriList)
            (context as Activity).startIntentSenderForResult(pi.intentSender,123,null
            ,0,0,0,null)
        }
        else{
            val file = File(videoList[position].path)
            val builder = MaterialAlertDialogBuilder(context)
            builder.setTitle("Delete Video?")
                .setMessage(videoList[position].title)
                .setPositiveButton("Yes"){ self, _ ->
                    if(file.exists() && file.delete()){
                        MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
                        updateDeleteUI(position = position)
                    }
                    self.dismiss()
                }
                .setNegativeButton("No"){self, _ -> self.dismiss() }
            val delDialog = builder.create()
            delDialog.show()
            delDialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(
                MaterialColors.getColor(context, R.attr.themeColor, Color.RED))
            delDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(
                MaterialColors.getColor(context, R.attr.themeColor, Color.RED))
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun updateDeleteUI(position: Int){
        when{
            MainActivity.search->{
                MainActivity.dataChanged=true
                videoList.removeAt(position)
                notifyDataSetChanged()
            }
            isFolder->{
                MainActivity.dataChanged=true
                FoldersActivity.currentFolderVideos.removeAt(position)
                notifyDataSetChanged()
            }
            else->{
                MainActivity.videoList.removeAt(position)
                notifyDataSetChanged()
            }
        }
    }
    private fun requestWriteR(){
        //files to modify
        val uriList: List<Uri> = listOf(Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            videoList[newPosition].id))

        //requesting file write permission for specific files
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pi = MediaStore.createWriteRequest(context.contentResolver, uriList)
            (context as Activity).startIntentSenderForResult(pi.intentSender, 124,
                null, 0, 0, 0, null)
        }else renameFunction(newPosition)
    }
    private fun renameFunction(position: Int){
        val customDialogRF = LayoutInflater.from(context).inflate(R.layout.rename_field,
            (context as Activity).findViewById(R.id.drawerLayoutMA), false)
        val bindingRF = RenameFieldBinding.bind(customDialogRF)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            dialogRF = MaterialAlertDialogBuilder(context).setView(customDialogRF)
                .setCancelable(false)
                .setPositiveButton("Rename"){ self, _ ->
                    val currentFile = File(videoList[position].path)
                    val newName = bindingRF.renameField.text
                    if(newName != null && currentFile.exists() && newName.toString().isNotEmpty()){
                        val newFile = File(currentFile.parentFile, newName.toString()+"."+currentFile.extension)

                        val fromUri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            videoList[position].id)

                        ContentValues().also {
                            it.put(MediaStore.Files.FileColumns.IS_PENDING, 1)
                            context.contentResolver.update(fromUri, it, null, null)
                            it.clear()

                            //updating file details
                            it.put(MediaStore.Files.FileColumns.DISPLAY_NAME, newName.toString())
                            it.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
                            context.contentResolver.update(fromUri, it, null, null)
                        }

                        updateRenameUI(position, newName = newName.toString(), newFile = newFile)
                    }
                    self.dismiss()
                }
                .setNegativeButton("Cancel"){self, _ ->
                    self.dismiss()
                }
                .create()
        }
        else{
            dialogRF = MaterialAlertDialogBuilder(context).setView(customDialogRF)
                .setCancelable(false)
                .setPositiveButton("Rename"){ self, _ ->
                    val currentFile = File(videoList[position].path)
                    val newName = bindingRF.renameField.text
                    if(newName != null && currentFile.exists() && newName.toString().isNotEmpty()){
                        val newFile = File(currentFile.parentFile, newName.toString()+"."+currentFile.extension)
                        if(currentFile.renameTo(newFile)){
                            MediaScannerConnection.scanFile(context, arrayOf(newFile.toString()), arrayOf("video/*"), null)
                            updateRenameUI(position = position, newName = newName.toString(), newFile = newFile)
                        }
                    }
                    self.dismiss()
                }
                .setNegativeButton("Cancel"){self, _ ->
                    self.dismiss()
                }
                .create()
        }
        bindingRF.renameField.text = SpannableStringBuilder(videoList[newPosition].title)
        dialogRF.show()
        dialogRF.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setBackgroundColor(
            MaterialColors.getColor(context,R.attr.themeColor, Color.BLACK))
        dialogRF.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(
            MaterialColors.getColor(context,R.attr.themeColor, Color.BLACK))
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateRenameUI(position: Int, newName: String, newFile: File){
        when{
            MainActivity.search -> {
//                MainActivity.searchList[position].title = newName
//                MainActivity.searchList[position].path = newFile.path
//                MainActivity.searchList[position].artUri = Uri.fromFile(newFile)
//                notifyItemChanged(position)
                MainActivity.videoList = getAllVideos(context)
                videoList = MainActivity.videoList
                notifyDataSetChanged()
            }
            isFolder -> {
                FoldersActivity.currentFolderVideos[position].title = newName
                FoldersActivity.currentFolderVideos[position].path = newFile.path
                FoldersActivity.currentFolderVideos[position].artUri = Uri.fromFile(newFile)
                notifyItemChanged(position)
//                MainActivity.dataChanged = true
                MainActivity.videoList = getAllVideos(context)
            }
            else -> {
//                MainActivity.videoList[position].title = newName
//                MainActivity.videoList[position].path = newFile.path
//                MainActivity.videoList[position].artUri = Uri.fromFile(newFile)
//                notifyItemChanged(position)
                MainActivity.videoList = getAllVideos(context)
                videoList = MainActivity.videoList
                notifyDataSetChanged()
            }
        }
    }
    fun onResult(requestCode:Int,resultCode:Int){
    when(requestCode){
        123->{
            if(resultCode == Activity.RESULT_OK) updateDeleteUI(newPosition)
        }
        124->{
            if(resultCode==Activity.RESULT_OK) renameFunction(newPosition)
        }
    }
    }
}