package com.example.chat.ImageUtil

import android.graphics.Bitmap
import java.io.*


class Image(var image_src: String, var sha1: String, var url: String, var orientation: Int) :
    Serializable  {
    var bitmap: Bitmap? = null

    @Throws(IOException::class)
    private fun writeObject(stream: ObjectOutputStream) {
        stream.writeUTF(image_src)
        stream.writeUTF(sha1)
        stream.writeUTF(url)
        stream.writeInt(orientation)
    }


    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(stream: ObjectInputStream) {
        image_src = stream.readUTF()
        sha1 = stream.readUTF()
        url = stream.readUTF()
        orientation = stream.readInt()
    }

}

class Record(var record_src: String, var sha1: String, var url: String) :
    Serializable  {

    @Throws(IOException::class)
    private fun writeObject(stream: ObjectOutputStream) {
        stream.writeUTF(record_src)
        stream.writeUTF(sha1)
        stream.writeUTF(url)
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(stream: ObjectInputStream) {
        record_src = stream.readUTF()
        sha1 = stream.readUTF()
        url = stream.readUTF()
    }

}

