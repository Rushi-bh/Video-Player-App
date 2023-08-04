package com.rushibhosale.myvideoplayer1


import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.ContentUris
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rushibhosale.myvideoplayer1.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object{
        @JvmStatic
        lateinit var binding: ActivityMainBinding
            private set
        @JvmStatic
        var selected = 0
        var selectAll =false
        lateinit var videoList:ArrayList<Video>
        lateinit var folderList:ArrayList<Folder>
        lateinit var internalStorageList:ArrayList<Video>
        lateinit var searchList: ArrayList<Video>
        var issearch =false
        var isSelectON =false
        var selectedList:ArrayList<Video> =ArrayList()
        lateinit var selectedFolderList:ArrayList<Folder>
        lateinit var currentFragment: Fragment

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        // Inside your activity's onCreate() method or Application class
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)
        binding =ActivityMainBinding.inflate(layoutInflater)
        setTheme(R.style.coolPinkNav)
        setContentView(binding.root)
        if(requestPermission()){
            internalStorageList = ArrayList()
            folderList  = ArrayList()
            videoList = getAllVideos()
            setfragment(VideoFragment())
        }

        binding.bottomNav.setOnItemSelectedListener {
            when(it.itemId){
                R.id.videos->
                {
                    setfragment(VideoFragment())
                }
                R.id.folders-> {
                    setfragment(FoldersFragment(), id = 2)
                }
            }
            return@setOnItemSelectedListener true
        }
        binding.cancelBtn.setOnClickListener {
            isSelectON = false
            selectedList = ArrayList()
            selected =0
            selectAll = false
            if(currentFragment is VideoFragment)setfragment(VideoFragment())
            else setfragment(FoldersFragment())
            binding.topSelectedLL.visibility = View.GONE
            binding.bottomSelectMenuLl.visibility = View.GONE
            binding.bottomNav.visibility =View.VISIBLE
            supportActionBar?.show()

        }
        binding.selected.text = "${selected}"
//        binding.totalSelected.text  = if(currentFragment is VideoFragment) "${videoList.size} Selected" else "${folderList.size} selected"
        binding.check.setOnClickListener {
            selectAll = !selectAll
            if(selectAll){
                selectedList = ArrayList()
                selectedFolderList = ArrayList()
                if(currentFragment is VideoFragment) selectedList.addAll(videoList)
                else selectedFolderList.addAll(folderList)

            }else{
                selectedList = ArrayList()
                selectedFolderList =ArrayList()
            }
            selected = if(currentFragment is VideoFragment) selectedList.size else selectedFolderList.size
            if(currentFragment is VideoFragment)setfragment(VideoFragment())
            else setfragment(FoldersFragment())
            binding.selected.text = "${selected}"
        }
        binding.infoBtn.setOnClickListener {
            showFileInfoAlertDialog()
        }
        binding.shareBtn.setOnClickListener {
            shareSelectedFiles()
        }
        binding.deleteBtn.setOnClickListener {
            if(selected!=0)deleteSelectedVideos()
            else{
                Toast.makeText(this, "First select to delete", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun deleteSelectedVideos() {
        var selectedFiles:ArrayList<Video> = ArrayList()
        if(currentFragment is VideoFragment) selectedFiles = selectedList
        else{
            for(folder in selectedFolderList){
                val currentFolderVideos =selectedFolderVideos(folder.id)
                selectedFiles.addAll(currentFolderVideos)
            }
        }
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){
            val uriList = arrayListOf<Uri>()
            for (video in selectedFiles){
                var uri =Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, video.id)
                uriList.add(uri)
            }
            val pi =MediaStore.createDeleteRequest(this.contentResolver, uriList)
            (this as Activity).startIntentSenderForResult(pi.intentSender, 123, null, 0,0,0,null)
        }else{
            for(video in selectedFiles){
                var file =File(video.path)
                if (file.exists()){
                    if(file.delete()){
                        Toast.makeText(this, "File deleted Successfully", Toast.LENGTH_SHORT).show()
                        // After deleting the videos, you can clear the selected list

                    }else{
                        Toast.makeText(this, "Unable to delete file try after some time", Toast.LENGTH_SHORT).show()
                        break
                    }
                }
            }
            selectedList.clear()
            selected = 0

            // Update the UI accordingly
            if(currentFragment is VideoFragment){
                folderList = ArrayList()
                internalStorageList =ArrayList()
                videoList =getAllVideos()
                (currentFragment as VideoFragment).updateVideoList(videoList)
            }
            isSelectON =false
            if(currentFragment is VideoFragment)setfragment(VideoFragment())else setfragment(FoldersFragment())
            binding.topSelectedLL.visibility = View.GONE
            binding.bottomSelectMenuLl.visibility = View.GONE
            binding.bottomNav.visibility = View.VISIBLE
            supportActionBar?.show()
        }
    }

    private fun shareSelectedFiles() {
        val fileUris = ArrayList<Uri>()
        var selectedFiles:ArrayList<Video> = ArrayList()
        if(currentFragment is VideoFragment) {
            selectedFiles = selectedList
        }else{
            for(folder in selectedFolderList){
                val currentFolderVideos =selectedFolderVideos(folder.id)
                selectedFiles.addAll(currentFolderVideos)
            }
        }
        for (video in selectedFiles) {
            val file = File(video.path)
            val uri = FileProvider.getUriForFile(this, "com.rushibhosale.myvideoplayer1.fileprovider", file)
            fileUris.add(uri)
        }

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "video/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)
        }

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "Share files"))
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.select->{
                isSelectON = true
                selectedList =ArrayList()
                selectedFolderList = ArrayList()
                selected =0
                if(currentFragment is VideoFragment)setfragment(VideoFragment())
                else setfragment(FoldersFragment())
                binding.selected.text = "${selected}"
                binding.topSelectedLL.visibility = View.VISIBLE
                binding.bottomSelectMenuLl.visibility = View.VISIBLE
                binding.bottomNav.visibility =View.GONE
                supportActionBar?.hide()
            }
        }
        return super.onOptionsItemSelected(item)
    }
    private fun showFileInfoAlertDialog() {
        val alertDialog = MaterialAlertDialogBuilder(this)
            .setTitle(getinfoTitle())
            .setMessage(getFilesInfoMessage())
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        alertDialog.setOnShowListener {
            alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)?.apply {
                setBackgroundColor(Color.BLACK) // Set the background color directly to black
                setTextColor(Color.WHITE)
            }
        }
        alertDialog.show()
    }
    private fun getinfoTitle(): String {
        val builder =StringBuilder()
        if(currentFragment is VideoFragment){
            builder.append("Selected Videos")
        }else{
            builder.append("Selected Folders")
        }

        return builder.toString()
    }

    private fun getFilesInfoMessage(): String {
        val builder1 =StringBuilder()
        var size:Float =0F
        val builder = StringBuilder()
        if(currentFragment is VideoFragment)
        {
            if(selectedList.size>0) {
                for (file in selectedList) {
                    val fileSizeInBytes = file.size
                    val fileSizeInMB = fileSizeInBytes.toDouble() / (1024 * 1024)
                    builder.append("File Name: ${file.title}\n")
                        .append("File Size: ${String.format("%.2f", fileSizeInMB)} MB\n")
                        .append("File Path: ${file.path}\n\n")
                    size  = (size + fileSizeInMB).toFloat()
                }
            }else{
                builder.append("No files Selected")
                return builder.toString()
            }
            if(selectedList.size>1){
                builder1.append("Total Files: ${selectedList.size}\n").append("Total size: ${size}MB\n\n")
                builder1.append(builder)
                return builder1.toString()
            }
            return builder.toString()
        }
        else{
            if(selectedFolderList.size>0){
                for(folder in selectedFolderList){
                    val videoList = selectedFolderVideos(folder.id)
                    var currentFolderSize =0f
                    for (video in videoList){
                        val videoSizeInMB =video.size.toDouble() / (1024 * 1024)
                        currentFolderSize= (videoSizeInMB + currentFolderSize).toFloat()
                    }
                    size =(size+currentFolderSize).toFloat()
                    builder.append("Folder name:${folder.folderName}\n").append("Folder size: ${currentFolderSize}MB\n\n")
                }
            }
            else{
                builder.append("No folders selected")
                return builder.toString()
            }
            if(selectedFolderList.size>1){
                builder1.append("Total folders selected: ${selectedFolderList.size}\n").append("Total folder sizes: ${size}MB\n\n")
                builder1.append(builder)
                return builder1.toString()
            }
            return builder.toString()
        }
    }

    private fun setfragment(fragment: Fragment, id:Int =1) {
        currentFragment =fragment
        supportActionBar?.title = if( id==1 ){"All Videos"}else{"All Folders"}
        binding.totalSelected.text  = if(currentFragment is VideoFragment) "${videoList.size} Selected" else "${folderList.size} selected"
        val transaction =supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentFL, fragment)
        transaction.disallowAddToBackStack()
        transaction.commit()
    }

    private fun requestPermission():Boolean{
        if(Build.VERSION.SDK_INT<29){
            if (ActivityCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED ){
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE),
                    13
                )
            }
            return true
        }
        else{
            if(ActivityCompat.checkSelfPermission(this, READ_MEDIA_VIDEO)!=PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(READ_MEDIA_VIDEO),
                    13
                )
            }
            return true
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 123 && resultCode == Activity.RESULT_OK){
            // After deleting the videos, you can clear the selected list
            if(currentFragment is VideoFragment){
                folderList = ArrayList()
                internalStorageList = ArrayList()
                videoList =getAllVideos()
                (currentFragment as VideoFragment).updateVideoList(videoList)
            }
            else{
                folderList = ArrayList()
                internalStorageList = ArrayList()
                videoList =getAllVideos()
                (currentFragment as FoldersFragment).updateFolderList(folderList)
            }
            selectedList.clear()
            selectedFolderList.clear()
            selected = 0

            // Update the UI accordingly
            isSelectON =false
            if(currentFragment is VideoFragment)setfragment(VideoFragment())
            else setfragment(FoldersFragment())
            binding.topSelectedLL.visibility = View.GONE
            binding.bottomSelectMenuLl.visibility = View.GONE
            binding.bottomNav.visibility = View.VISIBLE
            supportActionBar?.show()
        }
        else if(currentFragment is VideoFragment)(currentFragment as VideoFragment).adapter.onResult(requestCode, resultCode)

    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == 13){
            if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
                internalStorageList =ArrayList()
                folderList  = ArrayList()
                videoList = getAllVideos()
                setfragment(VideoFragment())
            }
            else{
                if(Build.VERSION.SDK_INT<=30){
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE),
                        13
                    )
                }
                else{
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(READ_MEDIA_VIDEO),
                        13
                    )
                }
            }
        }
    }
    private fun selectedFolderVideos(currentfolderidFun: String): ArrayList<Video> {
        if(currentfolderidFun =="internal Storage"){
            return internalStorageList
        }
        val tempVideoList = ArrayList<Video>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED
        )
        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )
        cursor?.use {
            val titleColumnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val idColumnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val durationColumnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val folderNameColumnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val folderIdColumnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            val sizeColumnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val pathColumnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            while (it.moveToNext()) {
                val titleC = it.getString(titleColumnIndex)
                val idC = it.getString(idColumnIndex)
                val durationC = it.getLong(durationColumnIndex)
                val folderNameC = it.getString(folderNameColumnIndex)
                val folderidC = it.getString(folderIdColumnIndex)
                val sizeC = it.getString(sizeColumnIndex)
                val pathC = it.getString(pathColumnIndex)
                val fileC = File(pathC)
                val artUriC = Uri.fromFile(fileC)
                val video:Video
                if (folderidC == currentfolderidFun){
                    video = Video(
                        id = idC,
                        title  =titleC,
                        duration =durationC,
                        size = sizeC,
                        folderName = folderNameC,
                        path = pathC,
                        artUri = artUriC
                    )
                    if (fileC.exists()){
                        tempVideoList.add(video)
                    }
                }
            }
        }
        return tempVideoList
    }
    private fun getAllVideos(): ArrayList<Video> {
        val tempFolderList= ArrayList<String>()
        val tempVideoList = ArrayList<Video>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED
        )
        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )
        cursor?.use {
            val titleColumnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val idColumnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val durationColumnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val folderNameColumnIndex =
                it.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val folderIdColumnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            val sizeColumnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val pathColumnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val dateAddedColumnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

            while (it.moveToNext()) {
                val titleC = it.getString(titleColumnIndex)
                val idC = it.getString(idColumnIndex)
                val durationC = it.getLong(durationColumnIndex)
                val folderNameC = it.getString(folderNameColumnIndex)
                val folderidC = it.getString(folderIdColumnIndex)
                val sizeC = it.getString(sizeColumnIndex)
                val pathC = it.getString(pathColumnIndex)
                val fileC = File(pathC)
                val artUriC = Uri.fromFile(fileC)
                val video:Video
                if (folderNameC!=null){
                    video = Video(
                        id = idC,
                        title  =titleC,
                        duration =durationC,
                        size = sizeC,
                        folderName = folderNameC,
                        path = pathC,
                        artUri = artUriC
                    )
                    if (fileC.exists()){
                        tempVideoList.add(video)
                    }
                }
                else{
                    video = Video(
                        id = idC,
                        title  =titleC,
                        duration =durationC,
                        size = sizeC,
                        path = pathC,
                        artUri = artUriC
                    )
                    if (fileC.exists()){
                        internalStorageList.add(video)
                        tempVideoList.add(video)
                    }
                }
                if(!tempFolderList.contains(folderidC)){
                    tempFolderList.add(folderidC)
                    if(folderNameC!=null){
                        folderList.add(Folder(id = folderidC, folderName = folderNameC))
                    }
                    else{
                        folderList.add(Folder())
                    }
                }
            }
        }
        return tempVideoList
    }
}



