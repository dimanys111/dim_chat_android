package com.example.chat.UserUtil

import com.example.chat.ImageUtil.Image
import com.example.chat.MessagUtil.Message
import com.example.chat.MessagUtil.MessagesFragment
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.*

class  User(var username: String,var name: String,var activ:Boolean = false) :
    Serializable {
    var messages_map: SortedMap<Long, Message> = TreeMap()
    var messag_fragment: MessagesFragment? = null
    var images_map: MutableMap<String, Image> = mutableMapOf()
    var avatar = Image("", "", "", 0)

    @Throws(IOException::class)
    private fun writeObject(stream: ObjectOutputStream) {
        stream.writeUTF(username)
        stream.writeUTF(name)
        stream.writeObject(avatar)
        stream.writeObject(messages_map)
        stream.writeObject(images_map)
    }


    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(stream: ObjectInputStream) {
        username = stream.readUTF()
        name = stream.readUTF()
        avatar = stream.readObject() as Image
        messages_map = stream.readObject() as SortedMap<Long, Message>
        images_map = stream.readObject() as MutableMap<String, Image>
    }
}