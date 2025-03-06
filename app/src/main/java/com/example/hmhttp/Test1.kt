package com.example.hmhttp

import android.os.Build
import androidx.annotation.RequiresApi


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun main(){
    val body=JsonBody.Builder()
        .add("Path",1)
        .build()
    val request = Request.Builder()
        .url("http://shanhe.kim/api/youxi/wzyyb.php")
        .query("msg","花木兰")
        .build()
    val response = HmHttpClient().newCall(request).execute()
    println("response ${response.body}")
}