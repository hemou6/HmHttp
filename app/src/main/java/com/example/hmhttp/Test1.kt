package com.example.hmhttp

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.gson.Gson


data class Data(val img:String,val key:String)
data class ApiResponse(val code:Int,val msg:String?,val data: Data)

const val host="http://123.56.29.69:8080"

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun main(){
//    val request = Request.Builder()
//        .url(host+"/common/captcha")
//        .build()
//    val response = HmHttpClient().newCall(request).execute()
//    println("response ${response.body}")
//    val json= Gson().fromJson(response.body,ApiResponse::class.java)
//    println("img "+json.data.img)
//    println("key "+json.data.key)

    val body=JsonBody.Builder()
        .add("username","hemou")
        .add("password","123456")
        .add("captcha","c5Q2V")
        .add("key","abe7b67b-0784-45ee-85ec-22f56316b234")
        .build()
    val request1=Request.Builder()
        .url(host+"/user/enroll")
        .post(body)
        .build()
    val response1=HmHttpClient().newCall(request1).execute()
    println("response1 ${response1.body}")
}