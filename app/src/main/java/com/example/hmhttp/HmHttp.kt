package com.example.hmhttp

import android.os.Build
import androidx.annotation.RequiresApi
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.net.URL
import java.nio.charset.Charset
import java.util.zip.GZIPInputStream
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory


class HmHttpClient {
    fun newCall(request:Request):RealCall{
        return RealCall(request)
    }

}

class RealCall(val request: Request){
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun execute():Response{
        if (request.url.contains(Regex("\\{\\w+\\}"))) {
            throw IllegalArgumentException("Missing path parameters in URL: ${request.url}")
        }

        val urlObj=URL(request.url)
        println("url ${urlObj}")
        val host=urlObj.host
        println("port ${urlObj.port}")

        val port=if(urlObj.port!=-1)urlObj.port
        else if(urlObj.protocol=="https")443
        else 80                              //https的默认端口是443，http的是80
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
            val input=socket.getInputStream()

            val fullPath=if(path.startsWith("/"))path else "/$path"
            //发送请求行
            writer.println("${request.method} $fullPath?${urlObj.query} HTTP/1.1")
            //发送请求头
            writer.println("Host: $host")
            request.headers.forEach{(key,value)->writer.println("$key: $value")}
            writer.println()
            request.body?.writeTo(writer)
            writer.flush()

            val bufferedReader=input.buffered()
            //读取状态行
            val statusLine = readLineFromStream(bufferedReader) // 自定义方法读取字节流中的行
            val statusCode = statusLine.split(" ")[1].toInt()

            val headers= mutableMapOf<String,MutableList<String>>()
            var line:String
            //读取响应头
            do {
                line = readLineFromStream(bufferedReader)
                if (line.isNotEmpty()) {
                    val (key, value) = line.split(':', limit = 2).map { it.trim() }
                    headers.getOrPut(key) { mutableListOf() }.add(value)
                }
            } while (line.isNotEmpty())


            val contentEncoding=headers["Content-Encoding"]?.firstOrNull()
            val inputStreamDecoder=when(contentEncoding){
                "gzip"->GZIPInputStream(bufferedReader)
                else->bufferedReader
            }
            val transferEncoding = headers["Transfer-Encoding"]?.firstOrNull()
            val isChunked = transferEncoding?.contains("chunked") == true
            //获取字符集
            val contentType=headers["Content-Type"]?.firstOrNull()?:""
            val charsetName=if(contentType.contains("charset=")){
                contentType.split("charset=")[1].split(";")[0].trim()
            }else "UTF-8"

            val charset=try {
                Charset.forName(charsetName)
            }catch (e:Exception){
                Charsets.UTF_8
            }

            // 新增分块编码处理
            val body = if (isChunked) {
                val buffer = ByteArrayOutputStream()
                val chunkedReader = BufferedReader(InputStreamReader(inputStreamDecoder, charset))

                while (true) {
                    val chunkSizeLine = chunkedReader.readLine()?.trim() ?: break
                    val chunkSize = chunkSizeLine.toIntOrNull(16) ?: break
                    if (chunkSize == 0) break

                    val chunk = CharArray(chunkSize)
                    val read = chunkedReader.read(chunk)
                    buffer.write(String(chunk, 0, read).toByteArray(charset))

                    // 跳过块尾部的 \r\n
                    chunkedReader.readLine()
                }
                buffer.toString(charset.name())
            } else {
                inputStreamDecoder.readBytes().toString(charset)
            }

            return Response(statusCode,headers,body)
        }
    }
    /** 从字节流中按行读取（兼容不同换行符） */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun readLineFromStream(input: InputStream): String {
        val buffer = ByteArrayOutputStream()
        var b: Int
        while (true) {
            b = input.read()
            if (b == -1 || b == '\n'.code) break
            if (b != '\r'.code) buffer.write(b)
        }
        return buffer.toString(Charsets.ISO_8859_1)
    }
}

