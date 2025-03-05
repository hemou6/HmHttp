package com.example.hmhttp

data class Response(
    val statusCode:Int,
    val headers:Map<String,List<String>>,
    val body:String,
)