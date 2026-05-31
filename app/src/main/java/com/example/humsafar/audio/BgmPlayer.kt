// app/src/main/java/com/example/humsafar/audio/BgmPlayer.kt
//
// Lightweight looping background-music player for game screens (quiz, minigames).
// Resolves the raw resource by NAME at runtime so the app still compiles and
// simply stays silent if no audio file has been dropped into res/raw yet.
//
// To enable music: add a royalty-free loop as app/src/main/res/raw/quiz_bgm.mp3
// (or pass a different resName).

package com.example.humsafar.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

class BgmPlayer(private val context: Context) {

    private var player: MediaPlayer? = null

    /** Start looping the named raw resource at a gentle volume. Safe to call twice. */
    fun start(resName: String = "quiz_bgm", volume: Float = 0.45f) {
        if (player != null) return
        val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
        if (resId == 0) {
            Log.i("BgmPlayer", "No res/raw/$resName found — playing silently.")
            return
        }
        runCatching {
            player = MediaPlayer.create(context, resId)?.apply {
                isLooping = true
                setVolume(volume, volume)
                start()
            }
        }.onFailure { Log.w("BgmPlayer", "BGM start failed: ${it.message}") }
    }

    fun stop() {
        runCatching {
            player?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        }
        player = null
    }
}
