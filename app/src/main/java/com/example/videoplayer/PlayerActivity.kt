package com.example.videoplayer

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.support.v4.os.IResultReceiver2.Default
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.util.rangeTo
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.videoplayer.databinding.ActivityPlayerBinding
import com.example.videoplayer.databinding.BoosterBinding
import com.example.videoplayer.databinding.FoldersViewBinding
import com.example.videoplayer.databinding.MoreFeaturesBinding
import com.example.videoplayer.databinding.SpeedDialogBinding
import com.github.vkay94.dtpv.youtube.YouTubeOverlay
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline.Window
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout.AspectRatioListener
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.lang.Math.abs
import java.text.DecimalFormat
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import kotlin.collections.ArrayList
import kotlin.system.exitProcess

class PlayerActivity : AppCompatActivity(),AudioManager.OnAudioFocusChangeListener,GestureDetector.OnGestureListener {

    private  lateinit var binding: ActivityPlayerBinding
    private lateinit var playPauseBtn:ImageButton
    private lateinit var fullScreenbtn:ImageButton
    private lateinit var videoTitle:TextView
    private lateinit var gestureDetectorCompat: GestureDetectorCompat
    companion object{
        private var audioManager:AudioManager?=null
        private lateinit var player:ExoPlayer
        lateinit var playerList: ArrayList<Video>
        var position:Int=-1
        private   var repeat:Boolean=false
        private   var isFullscreen:Boolean=false
        private var isLocked:Boolean=false
        @SuppressLint("StaticFieldLeak")
        private lateinit var trackSelector:DefaultTrackSelector
        private lateinit var loudnessEnhancer: LoudnessEnhancer
        private var speed:Float=1.0f
        private var timer: Timer?=null
        var pipStatus:Int=0
        var nowPlayingid : String=""
        private var brightness:Int=0
        private var volume:Int=0
    }

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        window.attributes.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        binding=ActivityPlayerBinding.inflate(layoutInflater)

        setTheme(R.style.PlayerActivityTheme)
        setContentView(binding.root)
        videoTitle=findViewById(R.id.videoTitle)
        playPauseBtn=findViewById(R.id.playPauseBtn)
        fullScreenbtn=findViewById(R.id.fullscreenBtn)


        gestureDetectorCompat= GestureDetectorCompat(this,this)
        //for immersive mode
        WindowCompat.setDecorFitsSystemWindows(window,false)

        val windowInsetsController = WindowInsetsControllerCompat(window, binding.root)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

       //for handling video file intent

       try{
           if (intent.data?.scheme.contentEquals("content")){
               playerList= ArrayList()
               position=0
               val cursor=contentResolver.query(intent.data!!, arrayOf(MediaStore.Video.Media.DATA),null,null,null)
               cursor?.let {
                   it.moveToFirst()
                   val path= it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                   val file=File(path)
                   val video=Video(id="",title=file.name, duration = 0L, artUri = Uri.fromFile(file), path = path,size="", folderName = "")
                   playerList.add(video)
                   cursor.close()
               }
               createPlayer()
               initializeBinding()
           }
           else{
               initializeLayout()
               initializeBinding()
           }
       } catch (e:Exception){Toast.makeText(this,e.toString(),Toast.LENGTH_SHORT).show()}

     }
    private fun initializeLayout(){
        when(intent.getStringExtra("class")){
            "AllVideos"->{
                playerList= ArrayList()
                playerList.addAll(MainActivity.videoList)
                createPlayer()
            }
            "FoldersActivity"->{
                playerList= ArrayList()
                playerList.addAll(FoldersActivity.currentFolderVideos)
                createPlayer()
            }
            "SearchedVideos"->{
                playerList= ArrayList()
                playerList.addAll(MainActivity.searchList)
                createPlayer()
            }
            "NowPlaying"->{

                speed=1.0f
                videoTitle.text= playerList[position].title
                videoTitle.isSelected=true
                doubleTapEnabled()
                binding.playerView.player= player
                playVideo()
                playInFullscreen(enable = isFullscreen)
                seekBarFeature()

            }
        }
     if(repeat) findViewById<ImageButton>(R.id.repeatBtn).setImageResource(R.drawable.controls_repeat_off)
       else findViewById<ImageButton>(R.id.repeatBtn).setImageResource(R.drawable.controls_repeat_off)

    }
  @RequiresApi(Build.VERSION_CODES.O)
  @SuppressLint("SetTextI18n","SourceLockedOrientationActivity", "ObsoleteSdkInt")
   private fun initializeBinding(){

       findViewById<ImageButton>(R.id.orientationBtn).setOnClickListener {
           if (resources.configuration.orientation==Configuration.ORIENTATION_PORTRAIT)
               requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
           else
               requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
       }


       findViewById<ImageButton>(R.id.backBtn).setOnClickListener{
           finish()
       }
       playPauseBtn.setOnClickListener {
           if(player.isPlaying) pauseVideo()
           else playVideo()
       }
       findViewById<ImageButton>(R.id.nextBtn).setOnClickListener{
           nextPrevVideo()
       }
       findViewById<ImageButton>(R.id.prevBtn).setOnClickListener{
           nextPrevVideo(isNext = false)
       }
       findViewById<ImageButton>(R.id.repeatBtn).setOnClickListener {
           if(repeat){
               repeat=false
               player.repeatMode=Player.REPEAT_MODE_OFF
               findViewById<ImageButton>(R.id.repeatBtn).setImageResource(R.drawable.controls_repeat_off)
           }
           else{
               repeat=true
               player.repeatMode=Player.REPEAT_MODE_ONE
               findViewById<ImageButton>(R.id.repeatBtn).setImageResource(R.drawable.controls_repeat_off)



           }
       }
       fullScreenbtn.setOnClickListener {
           if(isFullscreen){
               isFullscreen=false
               playInFullscreen(enable = false)
           }
           else{
               isFullscreen=true
               playInFullscreen(enable = true)
           }
       }
       binding.lockBtn.setOnClickListener {
           if(!isLocked){
               isLocked=true
               binding.playerView.hideController()
               binding.playerView.useController=false
               binding.lockBtn.setImageResource(R.drawable.lock_close_icon)
           }
           else{
               isLocked=false
               binding.playerView.useController=true
               binding.playerView.showController()
               binding.lockBtn.setImageResource(R.drawable.lock_open_icon)


           }
       }
       findViewById<ImageButton>(R.id.moreFeatures).setOnClickListener {
           pauseVideo()
           val customDialog=LayoutInflater.from(this).inflate(R.layout.more_features,binding.root,false)
           val bindingMF=MoreFeaturesBinding.bind(customDialog)
           val dialog=MaterialAlertDialogBuilder(this).setView(customDialog)
               .setOnCancelListener{playVideo()}
               .setBackground(ColorDrawable(0x803700B3.toInt()))
               .create()
           dialog.show()
           bindingMF.audioTrack.setOnClickListener {
               dialog.dismiss()
               playVideo()
               val audioTrack=ArrayList<String>()
               val audioList=ArrayList<String>()
               for(group in player.currentTracks.groups){
                   if(group.type==C.TRACK_TYPE_AUDIO){
                       val groupInfo=group.mediaTrackGroup
                       for (i in 0 until groupInfo.length){
                           audioTrack.add(groupInfo.getFormat(i).language.toString())
                           audioList.add("${audioList.size+1}."+Locale(groupInfo.getFormat(i).language.toString()).displayLanguage
                           +"(${groupInfo.getFormat(i).label})")

                       }
                   }
               }
               if(audioList[0].contains("null")) audioList[0]="1. Default Track"
               val tempTracks=audioList.toArray(arrayOfNulls<CharSequence>(audioList.size))
               val audioDialog=MaterialAlertDialogBuilder(this,R.style.alertdialog)

                   .setTitle("Select Language")
                   .setOnCancelListener{playVideo()}
                   .setBackground(ColorDrawable(0x803700B3.toInt()))
                   .setPositiveButton("Off Audio"){ self, _ ->
                       trackSelector.setParameters(trackSelector.buildUponParameters().setRendererDisabled(
                           C.TRACK_TYPE_AUDIO, true
                       ))
                       self.dismiss()
                   }
                   .setItems(tempTracks){
                      _,position->
                       Toast.makeText(this,audioList[position]+" Selected",Toast.LENGTH_SHORT).show()
                       trackSelector.setParameters(trackSelector.buildUponParameters().setPreferredAudioLanguage(audioTrack[position]))

                   }
                   .create()
                   audioDialog.show()
               audioDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE)
               audioDialog.window?.setBackgroundDrawable(ColorDrawable(0x99000000.toInt()))
           }
           bindingMF.subtitleBtn.setOnClickListener {
               dialog.dismiss()
               playVideo()
               val subtitle=ArrayList<String>()
               val subtitilesList=ArrayList<String>()
               for(group in player.currentTracks.groups){
                   if(group.type==C.TRACK_TYPE_TEXT){
                       val groupInfo=group.mediaTrackGroup
                       for (i in 0 until groupInfo.length){
                           subtitle.add(groupInfo.getFormat(i).language.toString())
                           subtitilesList.add("${subtitilesList.size+1}."+Locale(groupInfo.getFormat(i).language.toString()).displayLanguage
                                   +"(${groupInfo.getFormat(i).label})")

                       }
                   }
               }

               val tempTracks=subtitilesList.toArray(arrayOfNulls<CharSequence>(subtitilesList.size))
               val sDialog=MaterialAlertDialogBuilder(this,R.style.alertdialog)

                   .setTitle("Select Subtitles")
                   .setOnCancelListener{playVideo()}
                   .setBackground(ColorDrawable(0x803700B3.toInt()))
                   .setPositiveButton("Off Subtitles"){ self, _ ->
                       trackSelector.setParameters(trackSelector.buildUponParameters().setRendererDisabled(
                           C.TRACK_TYPE_VIDEO, true
                       ))
                       self.dismiss()
                   }
                   .setItems(tempTracks){
                           _,position->
                      Snackbar.make(binding.root,subtitilesList[position]+" Selected",3000).show()
                       trackSelector
                           .buildUponParameters()
                           .setRendererDisabled(C.TRACK_TYPE_VIDEO,false)
                               .setPreferredTextLanguage(subtitle[position])

                   }
                   .create()
               sDialog.show()
               sDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE)
               sDialog.window?.setBackgroundDrawable(ColorDrawable(0x99000000.toInt()))
           }
           bindingMF.audioBooster.setOnClickListener {
             dialog.dismiss()
               val customDialogB=LayoutInflater.from(this).inflate(R.layout.booster,binding.root,false)
               val bindingB=BoosterBinding.bind(customDialogB)
               val dialogB=MaterialAlertDialogBuilder(this).setView(customDialogB)
                   .setOnCancelListener{playVideo()}
                   .setPositiveButton("OK"){self,_->
                       loudnessEnhancer.setTargetGain(bindingB.verticalBar.progress*100)
                       playVideo()
                       self.dismiss()
                   }
                   .setBackground(ColorDrawable(0x803700B3.toInt()))
                   .create()
               dialogB.show()
               bindingB.verticalBar.progress= loudnessEnhancer.targetGain.toInt()/100
               bindingB.progressText.text="Audio Boost\n\n${loudnessEnhancer.targetGain.toInt()/10}"
               bindingB.verticalBar.setOnProgressChangeListener {
                   bindingB.progressText.text="Audio Boost\n\n${it*10} %"

               }

           }
           bindingMF.speedBtn.setOnClickListener {
               dialog.dismiss()
               playVideo()
               val customDialogS=LayoutInflater.from(this).inflate(R.layout.speed_dialog,binding.root,false)
               val bindingS=SpeedDialogBinding.bind(customDialogS)
               val dialogS=MaterialAlertDialogBuilder(this).setView(customDialogS)
                   .setCancelable(false)
                   .setPositiveButton("OK"){self,_->
                       self.dismiss()
                   }
                   .setBackground(ColorDrawable(0x803700B3.toInt()))
                   .create()
               dialogS.show()
               bindingS.speedText.text="${DecimalFormat("#.##").format(speed)} X"
               bindingS.minusBtn.setOnClickListener{
                   changeSpeed(isIncrement = false)
                   bindingS.speedText.text="${DecimalFormat("#.##").format(speed)} X"
               }
               bindingS.plusBTn.setOnClickListener{
                   changeSpeed(isIncrement = true)
                   bindingS.speedText.text="${
                       DecimalFormat("#.##").format(speed)} X"
               }

           }
           bindingMF.sleepTimerBtn.setOnClickListener {
               dialog.dismiss()
               if(timer!=null) Toast.makeText(this,"Timer Alert Running!\nClose APP to Reset Timer.",Toast.LENGTH_SHORT).show()
               else{
                   var sleepTime=15
                   val customDialogS=LayoutInflater.from(this).inflate(R.layout.speed_dialog,binding.root,false)
                   val bindingS=SpeedDialogBinding.bind(customDialogS)
                   val dialogS=MaterialAlertDialogBuilder(this).setView(customDialogS)
                       .setCancelable(false)
                       .setPositiveButton("OK"){self,_->
                           timer=Timer()
                           val task=object :TimerTask(){
                               override fun run() {
                                   moveTaskToBack(true)
                                   exitProcess(1)
                               }
                           }
                           timer!!.schedule(task,sleepTime*1000.toLong())
                           self.dismiss()
                           playVideo()
                       }
                       .setBackground(ColorDrawable(0x803700B3.toInt()))
                       .create()
                   dialogS.show()
                   bindingS.speedText.text="$sleepTime Min"
                   bindingS.minusBtn.setOnClickListener{
                       if(sleepTime>15)  sleepTime-=15
                       bindingS.speedText.text="$sleepTime Min"
                   }
                   bindingS.plusBTn.setOnClickListener{
                       if(sleepTime<120) sleepTime+=15
                       bindingS.speedText.text="$sleepTime Min"

                   }
               }


           }
           bindingMF.pipModeBtn.setOnClickListener {
               val appOps =getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
               val status=if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.ECLAIR_0_1){
               appOps.checkOpNoThrow(AppOpsManager.OPSTR_PICTURE_IN_PICTURE,android.os.Process.myUid(),packageName)== AppOpsManager.MODE_ALLOWED}
               else { false }
               if(status){
                   this.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                   dialog.dismiss()
                   binding.playerView.hideController()
                    playVideo()
                   pipStatus=0
               }
               else{
                   val intent=Intent("android.settings.PICTURE_IN_PICTURE_SETTINGS",
                       Uri.parse("package:$packageName"))
                   startActivity(intent)
               }
           }
       }
   }

    private fun createPlayer(){
        try {
            player.release()
        }catch (e:Exception){}
        speed=1.0f
        trackSelector=DefaultTrackSelector(this)
        videoTitle.text= playerList[position].title
        videoTitle.isSelected=true
        player= ExoPlayer.Builder(this).setTrackSelector(trackSelector).build()
        doubleTapEnabled()
        binding.playerView.player= player
        val mediaItem =MediaItem.fromUri(playerList[position].artUri)
        player.setMediaItem(mediaItem)
        player.prepare()
        playVideo()
        player.addListener(object:Player.Listener{
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState==Player.STATE_ENDED) nextPrevVideo()
            }
        } )
       playInFullscreen(enable = isFullscreen)

        loudnessEnhancer= LoudnessEnhancer(player.audioSessionId)
        loudnessEnhancer.enabled=true
        nowPlayingid= playerList[position].id
        seekBarFeature()
         binding.playerView.setControllerVisibilityListener {
             when{
                 isLocked->binding.lockBtn.visibility=View.VISIBLE
                 binding.playerView.isControllerVisible->binding.lockBtn.visibility=View.VISIBLE
                 else-> binding.lockBtn.visibility=View.INVISIBLE
             }

         }
    }
   private fun playVideo(){
       playPauseBtn.setImageResource(R.drawable.pause_icon)
       player.play()
   }
    private fun pauseVideo(){
        playPauseBtn.setImageResource(R.drawable.play_icon)
        player.pause()
    }
    private fun nextPrevVideo(isNext:Boolean=true){
        if(isNext)  setPosition()
        else setPosition(isIncrement = false)
        player.release()
        createPlayer()
    }
    private fun setPosition(isIncrement:Boolean=true){
      if(!repeat){
          if(isIncrement){
              if(playerList.size-1== position)
                  position=0
              else ++position
          }
          else{
              if(position== 0)
                  position= playerList.size-1
              else --position
          }
      }
    }
    private fun playInFullscreen(enable:Boolean){
        if(enable){
            binding.playerView.resizeMode= AspectRatioFrameLayout.RESIZE_MODE_FILL
            player.videoScalingMode= C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            fullScreenbtn.setImageResource(R.drawable.fullscreen_exit_icon)
        }
        else{
            binding.playerView.resizeMode= AspectRatioFrameLayout.RESIZE_MODE_FIT
            player.videoScalingMode= C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            fullScreenbtn.setImageResource(R.drawable.fullscreen_icon)

        }
    }


    private fun changeSpeed(isIncrement: Boolean){
        if(isIncrement){
           if(speed<=2.9f){
               speed +=0.10f
           }
        }
        else{
           if(speed>0.20f){
               speed-=0.10f
           }
        }
        player.setPlaybackSpeed(speed)
    }

    @SuppressLint("MissingSuperCall")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        if(pipStatus!=0){
            finish()
            val intent=Intent(this,PlayerActivity::class.java)
            when(pipStatus){
                1-> intent.putExtra("class","FoldersActivity")
                2-> intent.putExtra("class","SearchedVideos")
                3-> intent.putExtra("class","AllVideos")
            }

          startActivity(intent)
        }
        if(!isInPictureInPictureMode) pauseVideo()

    }
    override fun onDestroy() {
        super.onDestroy()
        player.pause()
        audioManager?.abandonAudioFocus { this }
    }

    override fun onAudioFocusChange(focusChange: Int) {
       if(focusChange<=0) pauseVideo()
    }

    override fun onResume() {
        super.onResume()
        if(audioManager==null) audioManager=getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager!!.requestAudioFocus(this,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN)
        if(brightness!=0) setScreenBrightness(brightness)

    }
    @SuppressLint("ClickableViewAccessibility")
    private fun doubleTapEnabled(){
        binding.playerView.player= player
        binding.YtOverlay.performListener(object :YouTubeOverlay.PerformListener{
            override fun onAnimationEnd() {
             binding.YtOverlay.visibility=View.GONE
            }

            override fun onAnimationStart() {
               binding.YtOverlay.visibility=View.VISIBLE
            }

        })
        binding.YtOverlay.player(player)
        binding.playerView.setOnTouchListener{
            _,motionEvent->
           binding.playerView.isDoubleTapEnabled=false
            if(!isLocked){
                binding.playerView.isDoubleTapEnabled=true
                gestureDetectorCompat.onTouchEvent(motionEvent)
                if(motionEvent.action==MotionEvent.ACTION_UP){
                    binding.brightnessIcon.visibility=View.GONE
                    binding.volumeIcon.visibility=View.GONE
                 //for immersive mode
                    WindowCompat.setDecorFitsSystemWindows(window,false)

                    val windowInsetsController = WindowInsetsControllerCompat(window, binding.root)
                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                    windowInsetsController.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                }}
            return@setOnTouchListener false
        }
    }
    private fun seekBarFeature(){
        findViewById<DefaultTimeBar>(R.id.exo_progress).addListener(object :TimeBar.OnScrubListener{
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
              pauseVideo()
            }

            override fun onScrubMove(timeBar: TimeBar, position: Long) {
               player.seekTo(position)
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
            playVideo()
            }

        })
    }

    override fun onDown(e: MotionEvent): Boolean =false

    override fun onShowPress(e: MotionEvent)=Unit

    override fun onSingleTapUp(e: MotionEvent): Boolean =false

    override fun onLongPress(e: MotionEvent) =Unit

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean =false

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        val sWidth= Resources.getSystem().displayMetrics.widthPixels
        val sHeight= Resources.getSystem().displayMetrics.heightPixels

       val border=100 *Resources.getSystem().displayMetrics.density.toInt()
        if(e1!!.x<border|| e1.y<border||e1.x>sWidth-border|| e1.y> sHeight-border)
            return false

        if(abs(distanceX)<abs(distanceY)){
            if(e1!!.x <sWidth/2){
                //brightness
                binding.brightnessIcon.visibility=View.VISIBLE
                binding.volumeIcon.visibility=View.GONE
                val increase=distanceY>0
               val newValue= if(increase) brightness+1 else brightness-1
                if(newValue in 0..30) brightness=newValue
                binding.brightnessIcon.text= brightness.toString()
                setScreenBrightness(brightness)
            }
            else{
                //volume
                binding.brightnessIcon.visibility=View.GONE
                binding.volumeIcon.visibility=View.VISIBLE
                val maxVolume= audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val increase=distanceY>0
                val newValue= if(increase) volume+1 else volume-1
                if(newValue in 0..maxVolume) volume=newValue
                binding.volumeIcon.text= volume.toString()
               audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, volume,0)
            }
        }
        return true
    }
    private fun setScreenBrightness(value:Int){
        val d =1.0f/30
        val lp=this.window.attributes
        lp.screenBrightness=d*value
        this.window.attributes=lp
    }
}