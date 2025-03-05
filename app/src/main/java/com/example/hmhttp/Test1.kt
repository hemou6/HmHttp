package com.example.hmhttp

import android.os.Build
import androidx.annotation.RequiresApi


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun main(){
    val body=JsonBody.Builder()
        .add("Path",1)
        .build()
    val request = Request.Builder()
        .url("http://123.56.29.69:8080/common/test/{num}")
        .method("POST")
        .path("num","1")
        .build()
    val response = HmHttpClient().newCall(request).execute()
    println("response ${response.body}")

}