package com.rushibhosale.myvideoplayer1

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.rushibhosale.myvideoplayer1.databinding.ActivityFoldersBinding
import java.io.File

class FoldersActivity : AppCompatActivity() {

    companion object{
        lateinit var currentFolderVideoList:ArrayList<Video>
        lateinit var adapter:VideoAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        val binding = ActivityFoldersBinding.inflate(layoutInflater)
        setTheme(R.style.coolPinkNav)
        setContentView(binding.root)
        val position = intent.getIntExtra("position", 0)
        val currentfolderid =MainActivity.folderList[position].id
        if(currentfolderid!="internal Storage") currentFolderVideoList  =getAllVideos(currentfolderid)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = MainActivity.folderList[position].folderName
        binding.videoFARV.setHasFixedSize(true)
        binding.videoFARV.setItemViewCacheSize(10)
        binding.videoFARV.layoutManager  =LinearLayoutManager(this@FoldersActivity)
        if(MainActivity.folderList[position].id!="internal Storage"){
            adapter = VideoAdapter(this@FoldersActivity, currentFolderVideoList, isFolder = true)
            binding.videoFARV.adapter = adapter
        }
        else{
            adapter = VideoAdapter(this@FoldersActivity, MainActivity.internalStorageList, isFolder = true, isInternalStorage = true)
            binding.videoFARV.adapter = adapter
            }
    }
    override fun onBackPressed() {
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // Handle the back button press
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        adapter.onResult(requestCode, resultCode)
    }
    private fun getAllVideos(currentfolderidFun: String): ArrayList<Video> {
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
}