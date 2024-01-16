package com.example.chat


import android.os.Handler
import android.view.View
import android.widget.Toast
import com.example.chat.CallUtil.CallFragment
import com.example.chat.MessagUtil.MessagesFragment
import com.example.chat.SearchUtil.SearchFragment
import com.example.chat.UserUtil.MyUser
import com.example.chat.UserUtil.UsersFragment
import com.example.chat.UserUtil.UsersFragment.Companion.usersFragment
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketException
import com.neovisionaries.ws.client.WebSocketFrame

import org.json.JSONObject

class WebSocketListener : WebSocketAdapter() {

    override fun onConnected(
        websocket: WebSocket?,
        headers: MutableMap<String, MutableList<String>>?
    ) {
        super.onConnected(websocket, headers)
        websocket?.sendText(JSONObject().put("client_event","ok").toString())

        websocket?.sendText(JSONObject()
            .put("login",JSONObject()
                .put("username", MyUser.username)
                .put("password", MyUser.password)
            ).toString())
        MyUser.is_login = true
        MyUser.send_all()

        MainActivity.runOnUiThread(Runnable{
            if(usersFragment!=null && usersFragment!!.isVisible){
                MainActivity.activity?.activityMainBinding?.appBarMain?.ivActivUser?.visibility= View.VISIBLE
            }
        })
    }

    override fun onBinaryMessage(websocket: WebSocket?, binary: ByteArray?) {
        super.onBinaryMessage(websocket, binary)
        MessagesFragment.binary_file=binary
    }

    override fun onTextMessage(websocket: WebSocket?, text: String?) {
        super.onTextMessage(websocket, text)
        output(text!!)
    }

    override fun onTextMessage(websocket: WebSocket?, data: ByteArray?) {
        super.onTextMessage(websocket, data)
        output(data.toString())
    }

    override fun onDisconnected(
        websocket: WebSocket?,
        serverCloseFrame: WebSocketFrame?,
        clientCloseFrame: WebSocketFrame?,
        closedByServer: Boolean
    ) {
        super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer)
        MyUser.ws = null
        MyUser.is_login = false
        MainActivity.runOnUiThread(Runnable{
            if(usersFragment!=null && usersFragment!!.isVisible){
                MainActivity.activity?.activityMainBinding?.appBarMain?.ivActivUser?.visibility= View.GONE
            }
        })
        Handler().postDelayed({
            MyUser.start()
        }, 5000)
    }

    override fun onError(websocket: WebSocket?, cause: WebSocketException?) {
        super.onError(websocket, cause)
        websocket?.disconnect()
        MyUser.ws = null
        MyUser.is_login = false
        MainActivity.runOnUiThread(Runnable{
            if(usersFragment!=null && usersFragment!!.isVisible){
                MainActivity.activity?.activityMainBinding?.appBarMain?.ivActivUser?.visibility= View.GONE
            }
        })
        Handler().postDelayed({
            MyUser.start()
        }, 5000)
    }

    private fun output(txt: String) {
        val jo = JSONObject(txt)

        val has = { str: String, f: (String) -> Unit ->
            if(jo.has(str)) {
                f(jo.get(str).toString())
            }
        }

        val has_obj = { str: String, f: (JSONObject) -> Unit ->
            if(jo.has(str)) {
                f(jo.getJSONObject(str))
            }
        }

        has_obj("login") {
            MyUser.login(it)
        }

        has_obj("registr") {
            MyUser.registr(it)
        }

        has("server_event") {
            MainActivity.runOnUiThread(Runnable{
                Toast.makeText(MyApplication.appContext, it, Toast.LENGTH_SHORT).show()
            })
        }

        has_obj("del_mess") {
            MessagesFragment.del_mess(it)
        }

        has_obj("is_send_mess") {
            MessagesFragment.is_send_mess(it)
        }

        has_obj("is_messag_received") {
            MessagesFragment.is_messag_received(it)
        }

        has_obj("messag") {
            MessagesFragment.receive_messag(it)
        }

        has_obj("set_find_users") {
            SearchFragment.set_find_users(it)
        }

        has_obj("is_messag_read") {
            MessagesFragment.is_messag_read(it)
        }

        has_obj("is_messag_read_receiver") {
            MessagesFragment.is_messag_read_receiver(it)
        }

        has_obj("set_user_avatar") {
            UsersFragment.set_user_avatar(it)
        }

        has_obj("offer_send") {
            CallFragment.offer_send(it)
        }

        has_obj("answer_send") {
            CallFragment.answer_send(it)
        }

        has_obj("iceCandidate_send") {
            CallFragment.iceCandidate_send(it)
        }

        has_obj("call_close_send") {
            CallFragment.call_close_send(it)
        }

        has_obj("find_random_user") {
            RandomFragment.find_random_user(it)
        }

        has_obj("send_user_activ") {
            UsersFragment.send_user_activ(it)
        }

        has_obj("random_chat_close") {
            RandomFragment.random_chat_close(it)
        }
    }

}