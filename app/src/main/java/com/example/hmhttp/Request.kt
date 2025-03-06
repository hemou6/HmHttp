package com.example.hmhttp

import com.google.gson.Gson
import java.io.PrintWriter
import java.net.URLEncoder


class Request internal constructor(build: Builder){
    val url:String= checkNotNull(build.url){"url==null"}
    val method:String=build.method
    val headers:MutableMap<String,String> =build.headers
    val body:RequestBody?=build.body

    class Builder{
        internal var url:String?=null
        internal var method:String
        internal var headers:MutableMap<String,String>
        internal var body:RequestBody?=null
        internal val pathParam= mutableMapOf<String,String>()
        internal val queryParams= mutableMapOf<String,Any>()
        constructor(){
            method="GET"
            headers= mutableMapOf<String,String>("Content-Type" to "application/json; charset=utf-8")
        }
        fun url(url:String):Builder=apply { this.url=url }

        fun method(method:String):Builder=apply { this.method=method }

        fun header(name:String,value:String):Builder=apply { this.headers[name]=value}

        fun path(name:String,value: String):Builder=apply { this.pathParam[name]=value }

        fun query(name: String,value: Any):Builder=apply {
            when(val current=queryParams[name]){
                null->queryParams[name]=value
                is Collection<*>->queryParams[name]=current+value
                else ->queryParams[name]= listOf(current,value)
            }
        }

        fun post(body:RequestBody):Builder=apply{
            method="POST"
            this.body=body
            header("Content-Type",body.contentType()!!)
            header("Content-Length",body.contentLength().toString())
        }

        fun build():Request=this.let{
            buildUrl()
            Request(this)
        }

        private fun buildUrl(){
            pathParam.forEach{(key,value)->
                url=url?.replace("{$key}",value)
            }

            if (queryParams.isEmpty()) return

            val baseUrl = if (url!!.contains("?")) url else "$url?"
            val encodedParams = queryParams.flatMap { (key, value) ->
                when (value) {
                    is Collection<*> -> value.map { Pair(key, it.toString()) }
                    else -> listOf(Pair(key, value.toString()))
                }
            }.joinToString("&") { (k, v) ->
                "${encode(k)}=${encode(v)}"
            }

            url="$baseUrl$encodedParams"
        }
        private fun encode(str: String): String {
            return URLEncoder.encode(str, "UTF-8")
                .replace("+", "%20") // 更规范的编码替换
        }
    }
}
abstract class RequestBody{
    abstract fun contentType():String?
    abstract fun contentLength():Long
    abstract fun writeTo(output: PrintWriter)
}
class JsonBody internal constructor(
    data:Map<String,Any>
) : RequestBody() {
    val data=data
    val size:Int get() = data.size

    fun size()=size

    override fun contentLength():Long{
        val bytes=Gson().toJson(data).toByteArray(Charsets.UTF_8)
        return bytes.size.toLong()
    }

    override fun contentType()="application/json; charset=utf-8"

    override fun writeTo(output: PrintWriter) {
        Gson().toJson(data,output)
    }
    class Builder{
        private val data= mutableMapOf<String,Any>()
        fun add(name:String,value:Any):Builder=apply {
            data.put(name,value)
        }
        fun build():JsonBody=JsonBody(data)
    }
}