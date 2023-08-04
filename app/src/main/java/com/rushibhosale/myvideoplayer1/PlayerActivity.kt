package com.rushibhosale.myvideoplayer1

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rushibhosale.myvideoplayer1.databinding.ActivityPlayerBinding
import com.rushibhosale.myvideoplayer1.databinding.CustomControlViewBinding
import java.lang.Math.abs
import java.util.Locale
import java.io.File

class PlayerActivity : AppCompatActivity() {
    lateinit var binding: ActivityPlayerBinding
    private lateinit var gestureDetector: GestureDetector
    private lateinit var gestureDetector1: GestureDetector
    private var swipeStartX: Float = 0f
    private var swipeStartY: Float = 0f
    private var swipeEndX: Float = 0f
    private var swipeEndY: Float = 0f
    private val swipeDistanceThreshold = 200
    private val textHideHandler = Handler(Looper.getMainLooper())
    private val textHideRunnable = Runnable {
        binding.brightnessTV.visibility = View.GONE
    }
    lateinit var runnable: Runnable
    private var controlsVisible = true
    private var isLandscape =false
    private var isFullScreen =false
    private var isLocked =false
    private var isLockBtnVisible =true
    companion object {
        lateinit var player: ExoPlayer
        lateinit var playerList: ArrayList<Video>
        var position:Int = -1
        lateinit var trackSelector:DefaultTrackSelector
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setTheme(R.style.coolPinkNav)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        supportActionBar?.hide()
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        try{
            when {
                intent.data?.scheme.equals("content", ignoreCase = true) -> {
                    playerList = ArrayList()
                    position = 0

                    val resolver = contentResolver
                    val cursor = resolver.query(intent.data!!, null, null, null, null)

                    cursor?.use {
                        if (it.moveToFirst()) {
                            val displayName = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
                            val file = File(cacheDir, displayName)
                            val inputStream = resolver.openInputStream(intent.data!!)
                            file.outputStream().use { outputStream ->
                                inputStream?.copyTo(outputStream)
                            }
                            val video = Video(
                                id = "",
                                title = displayName,
                                duration = 0L,
                                folderName = "",
                                size = "",
                                path = file.absolutePath,
                                artUri = Uri.fromFile(file)
                            )
                            playerList.add(video)
                        }
                    }

                    createPlayer()
                    initializeLayout()
                }
                intent.data?.scheme.equals("file", ignoreCase = true) -> {
                    playerList = ArrayList()
                    position = 0

                    val fileUri = intent.data
                    val filePath = fileUri?.path
                    val fileName = filePath?.split('/')?.last();
                    val file = File(filePath)
                    val video = fileName?.let {
                        Video(
                            id = "",
                            title = it, // Use the file name as the title
                            duration = 0L,
                            folderName = "",
                            size = "",
                            path = "",
                            artUri = Uri.fromFile(file)
                        )
                    }
                    if (video != null) {
                        playerList.add(video)
                    }
                    createPlayer()
                    initializeLayout()
                }
                intent.data?.scheme.equals("http", ignoreCase = true) ||
                        intent.data?.scheme.equals("https", ignoreCase = true) -> {
                    playerList = ArrayList()
                    position = 0

                    val videoUrl = intent.data.toString()
                    val video = Video(
                        id = "",
                        title = "title",
                        duration = 0L,
                        folderName = "",
                        size = "",
                        path = "",
                        artUri = Uri.parse(videoUrl)
                    )
                    playerList.add(video)
                    createPlayer()
                    initializeLayout()
                }
                else -> {
                    initializeLayout()

                }
            }

        }catch (e:Exception){
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
        }
        initializeBinding()
        gestureDetector1 = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                isLockBtnVisible  =! isLockBtnVisible
                if(isLockBtnVisible){
                    binding.lockBtn.visibility = View.INVISIBLE
                }
                else{
                    binding.lockBtn.visibility = View.VISIBLE
                }
                swipeStartX = e.x
                swipeStartY = e.y
                return super.onDown(e)
            }})
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                toggleControlsVisibility()
                swipeStartX = e.x
                swipeStartY = e.y
                return super.onDown(e)
            }

            override fun onScroll(
                e1: MotionEvent,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                swipeEndX = e2?.x ?: swipeStartX
                swipeEndY = e2?.y ?: swipeStartY
                val deltaX = swipeEndX - swipeStartX
                val deltaY = swipeEndY - swipeStartY

                val swipeDistanceX = kotlin.math.abs(deltaX)
                val swipeDistanceY = kotlin.math.abs(deltaY)
                if(swipeDistanceX>swipeDistanceY*2){
                    if (swipeDistanceX >= swipeDistanceThreshold) {
                        // Swipe distance exceeds the threshold
                        val durationChange =
                            (swipeDistanceX / swipeDistanceThreshold) * 1000 // Adjust the factor as needed

                        if (deltaX > 0) {
                            // Swipe right, increase the duration
                            val newDuration = player.currentPosition + durationChange.toLong()
                            player.seekTo(newDuration)
                        } else {
                            // Swipe left, decrease the duration
                            val newDuration = player.currentPosition - durationChange.toLong()
                            player.seekTo(newDuration)
                        }
                    }
                }
//                    val screenHeight = binding.playerView.height
                val screenWidth = binding.playerView.width
                if (swipeDistanceY>swipeDistanceThreshold){
                    if (swipeStartX < screenWidth / 2 && swipeEndX < screenWidth / 2) {
                        // Left half of the screen, adjust brightness
                        if (deltaY > 0) {
                            val brightnessChange =
                                (swipeDistanceY / swipeDistanceThreshold) * 0.01 // Adjust the factor as needed
                            adjustBrightness(-brightnessChange.toFloat()) // Adjust brightness
                        } else {
                            val brightnessChange =
                                (swipeDistanceY / swipeDistanceThreshold) * 0.01 // Adjust the factor as needed
                            adjustBrightness(brightnessChange.toFloat()) // Adjust brightness
                        }

                    } else if (swipeStartX >= screenWidth / 2 && swipeEndX >= screenWidth / 2) {
                        // Right half of the screen, adjust volume
                        if (deltaY > 0) {
                            val volumeChange =
                                (swipeDistanceY / swipeDistanceThreshold) * 0.01 // Adjust the factor as needed
                            adjustVolume(-volumeChange.toFloat()) // Adjust volume
                        } else {
                            val volumeChange =
                                (swipeDistanceY / swipeDistanceThreshold) * 0.5 // Adjust the factor as needed
                            adjustVolume(volumeChange.toFloat()) // Adjust volume
                        }
                    }
                }
                return true
            }
        })
    }

    override fun onPause() {
        super.onPause()
        pauseVideo()
    }

    override fun onResume() {
        playVideo()
        super.onResume()
    }

    private fun initializeLayout() {
        when(intent.getStringExtra("class")){
            "AllVideos"->{
                playerList = ArrayList()
                playerList.addAll(MainActivity.videoList)
                createPlayer()
            }
            "search"->{
                playerList =ArrayList()
                playerList.addAll(MainActivity.searchList)
                createPlayer()
            }
            "FoldersActivity"->{
                playerList =ArrayList()
                playerList.addAll(FoldersActivity.currentFolderVideoList)
                createPlayer()
            }
            "InternalStorage"->{
                playerList =ArrayList()
                playerList.addAll(MainActivity.internalStorageList)
                createPlayer()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initializeBinding() {
        binding.videoTitle.text = playerList[position].title
        binding.subtitleBtn.setOnClickListener {
            val subtitleTrackList = ArrayList<String>()
            val subtitleSourceList = ArrayList<String>()
            val subtitleList = ArrayList<String>()
            subtitleList.add("0. No Subtitles")
// Iterate through the available subtitle tracks
            for (i in 0 until player.currentTracks.groups.size) {
                val trackGroup = player.currentTracks.groups[i]
                val trackFormat = trackGroup.getTrackFormat(0)
                if (trackFormat.sampleMimeType == MimeTypes.APPLICATION_SUBRIP || trackFormat.sampleMimeType == MimeTypes.TEXT_VTT) {
                    val subtitleLabel = if (trackFormat.label != null) {
                        trackFormat.label.toString()
                    } else {
                        Locale(trackFormat.language.toString()).displayLanguage
                    }
                    subtitleTrackList.add(subtitleLabel)


                    subtitleList.add("${subtitleList.size}.${Locale(trackFormat.language.toString()).displayLanguage} ${trackFormat.label.toString()} ")
                }
            }
            if(subtitleList.size>1) {
                val tempTracks =
                    subtitleList.toArray(arrayOfNulls<CharSequence>(subtitleTrackList.size + 1))

                val alertDialog = MaterialAlertDialogBuilder(this, R.style.alertDialog)
                    .setTitle("Select Subtitle")
                    .setOnCancelListener { playVideo() }
                    .setBackground(ColorDrawable(0x99000000.toInt()))
                    .setItems(tempTracks) { _, position ->
                        playVideo()

                        val selectedSubtitle = tempTracks[position].toString()

                        if (selectedSubtitle.startsWith("0.")) {
                            // "No Subtitles" option selected
                            trackSelector.parameters =
                                DefaultTrackSelector.ParametersBuilder(this)
                                    .setRendererDisabled(C.TRACK_TYPE_VIDEO, true).build()

                            Toast.makeText(this, "Subtitles turned off", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            // Subtitle track selected
                            val selectedSubtitleTrack = subtitleTrackList[position - 1]
                            trackSelector.parameters =
                                DefaultTrackSelector.ParametersBuilder(this)
                                    .setRendererDisabled(C.TRACK_TYPE_VIDEO, false).build()
                            // TODO: Set the selected subtitle track in your video player
                            trackSelector
                                .setParameters(
                                    trackSelector.buildUponParameters()
                                        .setPreferredTextLanguage(selectedSubtitleTrack)
                                )
                            Toast.makeText(
                                this,
                                subtitleList[position] + " selected",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .create()
                    .show()
            }else{
                playVideo()
                Toast.makeText(this, "No subtitle available", Toast.LENGTH_SHORT).show()
            }
        }

        binding.trackSelectionBtn.setOnClickListener {
            val audioTrackList = ArrayList<String>()
            val audioSourceList = ArrayList<String>()
            val audioList = ArrayList<String>()
            audioList.add("0. Default Audio")

            // Iterate through the available audio tracks
            for (i in 0 until player.currentTracks.groups.size) {
                val trackGroup = player.currentTracks.groups[i]
                val trackFormat = trackGroup.getTrackFormat(0)
                if (trackFormat.sampleMimeType == MimeTypes.AUDIO_AAC || trackFormat.sampleMimeType == MimeTypes.AUDIO_MPEG) {
                    val audioLabel = if (trackFormat.label != null) {
                        trackFormat.label.toString()
                    } else {
                        Locale(trackFormat.language.toString()).displayLanguage
                    }
                    audioTrackList.add(audioLabel)
                    audioList.add("${audioList.size}. ${Locale(trackFormat.language.toString()).displayLanguage} ${trackFormat.label.toString()}")
                }
            }

            if (audioList.size > 2) {
                val tempTracks = audioList.toArray(arrayOfNulls<CharSequence>(audioTrackList.size + 1))

                val alertDialog = MaterialAlertDialogBuilder(this, R.style.alertDialog)
                    .setTitle("Select Audio Track")
                    .setOnCancelListener { playVideo() }
                    .setBackground(ColorDrawable(0x99000000.toInt()))
                    .setItems(tempTracks) { _, position ->
                        playVideo()

                        val selectedAudioTrack = tempTracks[position].toString()

                        if (selectedAudioTrack.startsWith("0.")) {
                            // "Default Audio" option selected
                            trackSelector.parameters =
                                DefaultTrackSelector.ParametersBuilder(this)
                                    .setPreferredAudioLanguage(null).build()

                            Toast.makeText(this, "Default Audio selected", Toast.LENGTH_SHORT).show()
                        } else {
                            // Audio track selected
                            val selectedTrackIndex = position - 1
                            if (selectedTrackIndex >= 0 && selectedTrackIndex < audioTrackList.size) {
                                val selectedAudioLabel = audioTrackList[selectedTrackIndex]
                                trackSelector.parameters =
                                    DefaultTrackSelector.ParametersBuilder(this)
                                        .setPreferredAudioLanguage(selectedAudioLabel).build()

                                Toast.makeText(
                                    this,
                                    audioList[position] + " selected",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    .create()
                    .show()
            } else {
                playVideo()
                Toast.makeText(this, "Only default audio track available", Toast.LENGTH_SHORT).show()
            }
        }

        binding.changeOrientationBtn.setOnClickListener {
            isLandscape =!isLandscape
            if(isLandscape){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
            else{
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
        binding.fullScreenBtn.setOnClickListener {
            isFullScreen  = !isFullScreen
            playInFullScreen(isFullScreen)
        }
        binding.backButton.setOnClickListener {
            finish()
        }
        binding.lockBtn.setOnClickListener {
            isLocked =!isLocked
            if(isLocked){
                hideControls()
                binding.lockBtn.setImageResource(R.drawable.lock_icon)
            }
            else{
                showControls()
                binding.lockBtn.setImageResource(R.drawable.open_lock)
            }
        }

        binding.prevButton.setOnClickListener {
            nextPrevVideo(false)
        }
        binding.nextButton.setOnClickListener {
            nextPrevVideo(true)
        }

        binding.playPauseButton.setOnClickListener {
            if (player.isPlaying) {
                pauseVideo()
            } else {
                playVideo()
            }
        }
        binding.playPauseButton1.setOnClickListener {
            if (player.isPlaying) {
                pauseVideo()
            } else {
                playVideo()
            }
        }
        binding.fastForword.setOnClickListener {
            val newduration  = player.currentPosition + 10000
            player.seekTo(newduration)
        }
        binding.rewind.setOnClickListener {
            val newduration  = player.currentPosition - 5000
            player.seekTo(newduration)
        }

        binding.playerView.setOnTouchListener { _, event ->
            if(!isLocked)gestureDetector.onTouchEvent(event)
            else gestureDetector1.onTouchEvent(event)
            true
        }

    }

    private fun createPlayer() {
        trackSelector =DefaultTrackSelector(this)
        binding.videoTitle.text  = playerList[position].title
        binding.videoTitle.isSelected = false
        player = ExoPlayer.Builder(this).setTrackSelector(trackSelector).build()
        binding.playerView.player = player
        val mediaItem = MediaItem.fromUri(playerList[position].artUri)
        player.setMediaItem(mediaItem)
        player.prepare()
        playVideo()
        player.addListener(object: Player.Listener{
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if(playbackState == Player.STATE_ENDED)nextPrevVideo()
            }
        })
        setVisibility()

    }

    private fun playVideo() {
        player.play()
        binding.playPauseButton.setImageResource(R.drawable.pause_icon)
        binding.playPauseButton1.setImageResource(R.drawable.pause_icon)

    }
    private fun pauseVideo(){
        binding.playPauseButton.setImageResource(R.drawable.play_icon)
        binding.playPauseButton1.setImageResource(R.drawable.play_icon)
        player.pause()
    }

    private fun adjustVolume(change: Float) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val newVolume = 0.coerceAtLeast(maxVolume.coerceAtMost((currentVolume + change).toInt()))
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
        val volumePercentage = (newVolume * 100 / maxVolume)
        updateVolumeChangeText(volumePercentage)
        showAndScheduleTextHide()
    }

    private fun adjustBrightness(deltaBrightness: Float) {
        val layoutParams = window.attributes
        val currentBrightness = layoutParams.screenBrightness

        var newBrightness = currentBrightness + deltaBrightness

        if (newBrightness < 0) {
            newBrightness = 0f
        } else if (newBrightness > 1) {
            newBrightness = 1f
        }

        layoutParams.screenBrightness = newBrightness
        window.attributes = layoutParams

        val brightnessPercentage = (newBrightness * 100).toInt()
        updateBrightnessChangeText(brightnessPercentage)
        showAndScheduleTextHide()
    }

    @SuppressLint("SetTextI18n")
    private fun updateBrightnessChangeText(brightnessPercentage: Int) {
        binding.brightnessTV.text = "Brightness: $brightnessPercentage%"
        binding.brightnessTV.visibility = View.VISIBLE
    }

    @SuppressLint("SetTextI18n")
    private fun updateVolumeChangeText(volumePercentage: Int) {
        binding.brightnessTV.text = "Volume: $volumePercentage%"
        binding.brightnessTV.visibility = View.VISIBLE
    }

    private fun showAndScheduleTextHide() {
        binding.brightnessTV.visibility = View.VISIBLE
        textHideHandler.removeCallbacks(textHideRunnable)
        textHideHandler.postDelayed(textHideRunnable, 1000) // Adjust the delay as needed
    }
    private fun setVisibility(){
        runnable = Runnable {
            if(binding.playerView.isControllerVisible){
//                showControls()
            }
            else{
                if(!isLocked)hideControls()
            }
            Handler(Looper.getMainLooper()).postDelayed(runnable, 200)
        }
        Handler(Looper.getMainLooper()).postDelayed(runnable, 1000)
    }

    private fun toggleControlsVisibility() {
        controlsVisible = !controlsVisible
        if (controlsVisible) {
            showControls()
        } else {
            hideControls()
        }
    }
    private fun showControls() {

        // Show the controls (e.g., buttons, progress bar, etc.)

        if(!isLocked){
            binding.topController.visibility = View.VISIBLE
            binding.bottomController.visibility = View.VISIBLE
            binding.lockBtn.visibility = View.VISIBLE
            binding.playPauseButton.visibility = View.VISIBLE
            binding.fastForword.visibility = View.VISIBLE
            binding.rewind.visibility = View.VISIBLE
            binding.playerView.showController()
        }
    }

    private fun hideControls() {
        // Hide the controls (e.g., buttons, progress bar, etc.)
        if(isLocked){
            binding.topController.visibility = View.INVISIBLE
            binding.bottomController.visibility = View.INVISIBLE
            binding.lockBtn.visibility = View.VISIBLE
            binding.playPauseButton.visibility = View.INVISIBLE
            binding.fastForword.visibility = View.INVISIBLE
            binding.rewind.visibility = View.INVISIBLE
            binding.playerView.hideController()
        }
        else{
            binding.topController.visibility = View.INVISIBLE
            binding.bottomController.visibility = View.INVISIBLE
            binding.lockBtn.visibility = View.INVISIBLE
            binding.playPauseButton.visibility = View.INVISIBLE
            binding.fastForword.visibility = View.INVISIBLE
            binding.rewind.visibility = View.INVISIBLE
            binding.playerView.hideController()
        }

    }

    private fun playInFullScreen(enable:Boolean){
        if(enable){
            binding.playerView.resizeMode  = AspectRatioFrameLayout.RESIZE_MODE_FILL
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            binding.fullScreenBtn.setImageResource(R.drawable.fullscreen_exit_icon)
        }
        else{
            binding.playerView.resizeMode  = AspectRatioFrameLayout.RESIZE_MODE_FIT
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            binding.fullScreenBtn.setImageResource(R.drawable.fullscreen_icon)

        }
    }
    private fun nextPrevVideo(isNext:Boolean =true){
        if(isNext){
            setPosition(true)
        }else setPosition(false)
        player.release()
        createPlayer()
    }
    private fun setPosition(isIncrement:Boolean = true){
        if(isIncrement){
            if(playerList.size -1 == position){
                position  =0
            }
            else{
                ++position
            }
        }
        else{
            if(position == 0){
                position  = playerList.size-1
            }
            else{
                --position
            }
        }
    }
    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}




