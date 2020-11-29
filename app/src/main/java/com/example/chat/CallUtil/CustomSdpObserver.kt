package com.example.chat.CallUtil

import android.util.Log
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription


open class CustomSdpObserver(logTag: String) : SdpObserver {
    private var logTag: String? = this.javaClass.canonicalName

    init {
        this.logTag = this.logTag + " " + logTag
    }

    override fun onCreateSuccess(sessionDescription: SessionDescription) {
        Log.d(logTag, "description created successfully")
    }

    override fun onSetSuccess() {
        Log.d(logTag, "description set successfully")
    }

    override fun onCreateFailure(s: String) {
        Log.d(logTag, "description creation failed")
    }

    override fun onSetFailure(s: String) {
        Log.d(logTag, "description set failed")
    }
}