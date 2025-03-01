package com.example.hmhttp

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.net.URL
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory


class HmHttpClient {
    fun newCall(request:Request){

    }
    fun sendRequest(request: Request):Response{
        val urlObj=URL(request.url)
        val host=urlObj.host
        val port=if(urlObj.protocol=="https")443 else 80 //https的默认端口是443，http的是80
        val path=if(urlObj.path.isEmpty()) "/" else urlObj.path

        //HTTP1.1 默认保持连接 reader.readLine()会阻塞读取直到流关闭，所以要主动关闭流
        request.headers.getOrPut("Connection"){"close"}
        val socket=when(urlObj.protocol){
            "https"->{
                SSLSocketFactory.getDefault().createSocket(host,port)as Socket
            }
            else-> Socket(host,port)
        }

        socket.soTimeout=5000
        socket.use{
            val writer=PrintWriter(socket.getOutputStream(),true)
            val reader=BufferedReader(InputStreamReader(socket.getInputStream()))

            val fullPath=if(path.startsWith("/"))path else "/$path"
            //发送请求行
            writer.println("${request.method} $fullPath HTTP/1.1")
            //发送请求头
            writer.println("Host: $host")
            request.headers.forEach{(key,value)->writer.println("$key: $value")}
            writer.println()

            //读取状态行
            val statusLine=reader.readLine()?: throw IOException("No response")
            val statusCode=statusLine.split(" ")[1].toInt()

            val headers= mutableMapOf<String,MutableList<String>>()
            val body=StringBuilder()
            var line:String?=null
            //读取响应头
            while(reader.readLine().also { line=it }?.isNotEmpty()==true){
                val (key,value )=line!!.split(':', limit = 2).map { it.trim() }
                headers.getOrPut(key){ mutableListOf() }.add(value)
            }
            //读取响应体
            while (reader.readLine().also { line=it }?.isNotEmpty()!=null){
                body.append(line).append('\n')
            }

            return Response(statusCode,headers,body.toString())
        }
    }
}
class Request internal constructor(build: Build){
    val url:String= checkNotNull(build.url){"url==null"}
    val method:String=build.method
    val headers:MutableMap<String,String> =build.headers


    open class Build{
        internal var url:String?=null
        internal var method:String
        internal var headers:MutableMap<String,String>
        constructor(){
            method="GET"
            headers= mutableMapOf<String,String>()
        }
        open fun url(url:String):Build=apply { this.url=url }
        open fun method(method:String):Build=apply { this.method=method }
        open fun header(name:String,value:String):Build=apply { this.headers[name]=value}
        open fun build():Request=Request(this)
    }
}
data class Response(
    val statusCode:Int,
    val headers:Map<String,List<String>>,
    val body:String,
)