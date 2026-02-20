// app/src/main/java/com/example/humsafar/audio/AudioPlayer.kt
// NEW FILE

package com.example.humsafar.audio

import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "AudioPlayer"

/**
 * Decodes base64 audio from the backend and plays it via MediaPlayer.
 *
 * Why temp file instead of data URI:
 *   MediaPlayer.setDataSource(String uri) has a ~1 MB in-memory limit
 *   on some OEM builds. Writing to cacheDir is reliable on all API 29+ devices.
 *
 * Why suspendCancellableCoroutine:
 *   MediaPlayer.OnCompletionListener fires on the main thread asynchronously.
 *   We bridge that callback into a coroutine suspension so the ViewModel
 *   can `await` playback completion naturally.
 */
class AudioPlayer {

    private var mediaPlayer: MediaPlayer? = null

    /**
     * Decodes [base64Audio], writes it to a temp file, and plays it.
     * Suspends until playback completes, errors, or the coroutine is cancelled.
     *
     * @param base64Audio Base64-encoded WAV bytes (from VoiceChatResponse.audioBase64).
     * @param cacheDir    Context.cacheDir — used for the temp file.
     * @param format      File extension, e.g. "wav". Determines MIME on some OEMs.
     */
    suspend fun play(
        base64Audio: String,
        cacheDir:    File,
        format:      String = "wav"
    ) = withContext(Dispatchers.IO) {
        val bytes    = Base64.decode(base64Audio, Base64.DEFAULT)
        val tempFile = File(cacheDir, "tts_${System.currentTimeMillis()}.$format")
        FileOutputStream(tempFile).use { it.write(bytes) }
        Log.d(TAG, "TTS audio written: ${bytes.size} bytes → ${tempFile.name}")

        try {
            suspendCancellableCoroutine { cont ->
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(tempFile.absolutePath)
                    prepare()   // sync — we're on IO dispatcher

                    setOnCompletionListener {
                        Log.i(TAG, "Playback complete")
                        cont.resume(Unit)
                    }
                    setOnErrorListener { _, what, extra ->
                        val msg = "MediaPlayer error what=$what extra=$extra"
                        Log.e(TAG, msg)
                        cont.resumeWithException(RuntimeException(msg))
                        true
                    }

                    start()
                    Log.i(TAG, "Playback started")
                }

                cont.invokeOnCancellation {
                    Log.d(TAG, "Playback cancelled")
                    release()
                }
            }
        } finally {
            release()
            if (tempFile.exists()) tempFile.delete()
        }
    }

    fun stopPlayback() {
        mediaPlayer?.apply { if (isPlaying) stop() }
        release()
    }

    private fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}