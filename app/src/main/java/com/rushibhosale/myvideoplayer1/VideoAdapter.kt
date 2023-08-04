package com.rushibhosale.myvideoplayer1
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rushibhosale.myvideoplayer1.databinding.MoreOptionsLayoutBinding
import com.rushibhosale.myvideoplayer1.databinding.RenameFieldBinding
import com.rushibhosale.myvideoplayer1.databinding.VideoItemViewBinding
import java.io.File
import java.lang.StringBuilder

class VideoAdapter(private val context:Context, private var videoList: ArrayList<Video>, private val isFolder:Boolean= false, private  val isInternalStorage:Boolean =false):RecyclerView.Adapter<VideoAdapter.MyHolder>() {
    var newPosition =0
    private lateinit var dialogRF: androidx.appcompat.app.AlertDialog
    class MyHolder(binding:VideoItemViewBinding):RecyclerView.ViewHolder(binding.root){
        var title = binding.videoName
        var folderName = binding.folderName
        var duration = binding.duration
        var image  =binding.videoImage
        val checkbox =binding.checkbox
        val moreBtn =binding.moreBtn
        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(VideoItemViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val isSelectOn = MainActivity.isSelectON
        val mainBinding = MainActivity.binding
        val video = videoList[position]
        holder.checkbox.visibility = if (isSelectOn) View.VISIBLE else View.GONE
        holder.checkbox.setImageResource(if(MainActivity.selectAll)R.drawable.checked_box else R.drawable.empty_check_box)
        holder.moreBtn.visibility =if (!isSelectOn) View.VISIBLE else View.GONE
        holder.title.text = video.title
        holder.folderName.text = video.folderName
        holder.duration.text = DateUtils.formatElapsedTime(video.duration/1000)
        Glide.with(context).asBitmap().load(video.artUri).centerCrop().apply(
            RequestOptions().placeholder(R.mipmap.ic_launcher)).into(holder.image)
        holder.root.setOnClickListener {
            if(!MainActivity.isSelectON){
                if(!isInternalStorage) {
                    when{
                        isFolder->{
                            sendIntent(pos = position, ref ="FoldersActivity")
                        }
                        MainActivity.issearch->{
                            sendIntent(pos = position, ref = "search")
                        }
                        else->{
                            sendIntent(pos =position, ref ="AllVideos")
                        }
                    }
                }
                else{
                    sendIntent(pos =position, ref = "InternalStorage")
                }
            }else{
                if(video in MainActivity.selectedList){
                    MainActivity.selectedList.remove(video)
                    MainActivity.selected -=1
                    mainBinding.selected.text = "${MainActivity.selected}"
                    holder.checkbox.setImageResource(R.drawable.empty_check_box)
                }
                else{
                    MainActivity.selectedList.add(video)
                    MainActivity.selected +=1
                    mainBinding.selected.text = "${MainActivity.selected}"
                    holder.checkbox.setImageResource(R.drawable.checked_box)
                }
            }
        }
        holder.moreBtn.setOnClickListener {
            newPosition = position
            val customDialog = LayoutInflater.from(context).inflate(R.layout.more_options_layout,holder.root, false)
            val bindingMF  = MoreOptionsLayoutBinding.bind(customDialog)
            val dialog = MaterialAlertDialogBuilder(context)
                .setView(customDialog)
                .create()
            dialog.show()

            bindingMF.renameBtnMOL.setOnClickListener {
                dialog.dismiss()
                renameVideo(video)
            }
            bindingMF.deleteBtnMOL.setOnClickListener {
                dialog.dismiss()
                deleteVideo(video)
            }
            bindingMF.shareBtnMOL.setOnClickListener {
                dialog.dismiss()
                shareVideo(video)
            }
            bindingMF.infoBtnMOL.setOnClickListener {
                dialog.dismiss()
                infoVideo(video)
            }

        }
    }

    private fun infoVideo(video: Video) {
            val alertDialog = MaterialAlertDialogBuilder(context)
                .setTitle(video.title)
                .setMessage(getFilesInfoMessage(video))
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

    private fun getFilesInfoMessage(video: Video): String {
        var builder =StringBuilder()
        val sizeinBytes =video.size
        val sizeinMBs = sizeinBytes.toDouble()/(1024*1024)

        builder.append("Video Name: ${video.title}\n")
            .append("Folder name: ${video.folderName}\n")
            .append("Video size: ${String.format("%.2f", sizeinMBs)}MB\n")
            .append("Video duration: ${DateUtils.formatElapsedTime(video.duration/1000)}\n")
            .append("Video path: ${video.path}\n")
        return builder.toString()
    }

    private fun shareVideo(video: Video) {
        val fileUris = ArrayList<Uri>()
        val file = File(video.path)
        val uri = FileProvider.getUriForFile(context, "com.rushibhosale.myvideoplayer1.fileprovider", file)
        fileUris.add(uri)
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "video/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, "Share files"))
    }

    private fun deleteVideo(video: Video) {
        requestDeleteR(position = newPosition)
    }

    private fun renameVideo(video: Video) {
        requestWriteR()
    }

    private fun sendIntent(pos: Int, ref: String) {
        PlayerActivity.position = pos
        val intent = Intent(context, PlayerActivity::class.java)
        intent.putExtra("class", ref)
        startActivity(context, intent, null)
    }

    override fun getItemCount(): Int {
        return videoList.size
    }
    @SuppressLint("NotifyDataSetChanged")
    public fun updateList(updatedList:ArrayList<Video>){
        videoList = updatedList
        notifyDataSetChanged()
    }
    private fun requestDeleteR(position: Int){
        //list of videos to delete
        val uriList: List<Uri> = listOf(Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoList[position].id))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            //requesting for delete permission
            val pi = MediaStore.createDeleteRequest(context.contentResolver, uriList)
            (context as Activity).startIntentSenderForResult(pi.intentSender, 124,
                null, 0, 0, 0, null)
        }
        else{
            //for devices less than android 11
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

        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateDeleteUI(position: Int){
        when{
            isFolder -> {
                FoldersActivity.currentFolderVideoList.removeAt(position)
                notifyDataSetChanged()
            }
            else -> {
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
            (context as Activity).startIntentSenderForResult(pi.intentSender, 125,
                null, 0, 0, 0, null)
        }else renameFunction(newPosition)
    }

    private fun renameFunction(position: Int){
        val customDialogRF = LayoutInflater.from(context).inflate(R.layout.rename_field, null, false)
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
        dialogRF.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.RED)
        dialogRF.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.BLACK)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateRenameUI(position: Int, newName: String, newFile: File){
        when{
            isFolder -> {
                FoldersActivity.currentFolderVideoList[position].title = newName
                FoldersActivity.currentFolderVideoList[position].path = newFile.path
                FoldersActivity.currentFolderVideoList[position].artUri = Uri.fromFile(newFile)
                notifyItemChanged(position)
            }
            else -> {
                MainActivity.videoList[position].title = newName
                MainActivity.videoList[position].path = newFile.path
                MainActivity.videoList[position].artUri = Uri.fromFile(newFile)
                notifyItemChanged(position)
            }
        }
    }

    fun onResult(requestCode: Int, resultCode: Int) {
        when(requestCode){
            124 -> {
                if(resultCode == Activity.RESULT_OK) updateDeleteUI(newPosition)
            }
            125->{
                if(resultCode == Activity.RESULT_OK) renameFunction(position = newPosition)
            }
        }
    }


}