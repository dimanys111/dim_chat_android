package com.example.chat.MediaPlayer

interface PlayerAdapter {
    fun loadMedia(path: String)
    fun release()
    val isPlaying: Boolean

    fun play()
    fun reset()
    fun pause()
    fun seekTo(position: Int)
}
