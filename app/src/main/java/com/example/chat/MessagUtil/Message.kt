package com.example.chat.MessagUtil

import com.example.chat.ImageUtil.Image
import com.example.chat.ImageUtil.Record
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

class Message (
    var text_mess: String,
    var time: Date,
    var isMy: Boolean,
    var sender_receiver: String,
    var image: Image? = null
) : Serializable {

    var record : Record?= null
    var isRead: Boolean = false
    var isView: Boolean = false
    var isReceived: Boolean = false
    var isSend: Boolean = false

    @Throws(IOException::class)
    private fun writeObject(stream: ObjectOutputStream) {
        stream.writeUTF(text_mess)
        stream.writeObject(time)
        stream.writeBoolean(isMy)
        stream.writeBoolean(isRead)
        stream.writeBoolean(isSend)
        stream.writeBoolean(isReceived)
        stream.writeBoolean(isView)
        stream.writeUTF(sender_receiver)
        stream.writeObject(image)
        stream.writeObject(record)
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(stream: ObjectInputStream) {
        text_mess = stream.readUTF()
        time = stream.readObject() as Date
        isMy = stream.readBoolean()
        isRead = stream.readBoolean()
        isSend = stream.readBoolean()
        isReceived = stream.readBoolean()
        isView = stream.readBoolean()
        sender_receiver = stream.readUTF()
        var z = stream.readObject()
        if(z!=null){
            image = z as Image
        }
        z = stream.readObject()
        if(z!=null){
            record = z as Record
        }
    }

    companion object{
        val sdf = SimpleDateFormat("HH:mm")
    }
}
