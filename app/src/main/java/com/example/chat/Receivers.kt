package com.example.chat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.chat.UserUtil.MyUser
import org.json.JSONObject

class AlarmTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        MainActivity.init()
        //MainActivity.alarm_timer()
        MyUser.start()
        MyUser.send_webSocket_(JSONObject().put("ping", "ping").toString())
    }
}

class MyBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            MainActivity.init()
            MainActivity.alarm_timer()
            MyUser.start()
        }
    }
}