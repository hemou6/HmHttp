package com.example.hmhttp

fun main(){
    val getRequest = Request.Build()
        .url("https://www.baidu.com")
        .build()
    val getResponse = HmHttpClient().sendRequest(getRequest)
    println("GET Response: ${getResponse.statusCode}\n${getResponse.body}")
}