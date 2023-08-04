package com.rushibhosale.myvideoplayer1

import android.net.Uri

data class Video(val id:String, var title:String, val duration: Long = 0, val folderName:String = "internal storage",
                 val size:String, var path:String, var artUri: Uri
){}
