package com.example.chat.MediaPlayer



interface PlaybackInfoListener {

    annotation class State {
        companion object {
            var INVALID = -1
            var PLAYING = 0
            var PAUSED = 1
            var RESET = 2
            var COMPLETED = 3
        }
    }

    fun onDurationChanged(duration: Int) {}
    fun onPositionChanged(position: Int) {}
    fun onStateChanged(@State state: Int) {}
    fun onPlaybackCompleted() {}

}