// app/src/main/java/com/example/humsafar/audio/AudioRecorder.kt
// NEW FILE

package com.example.humsafar.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TAG = "AudioRecorder"

/**
 * Records PCM audio from the microphone and returns WAV-encoded bytes.
 *
 * Design decisions:
 *   • 16 kHz / Mono / Int16 — optimal for Sarvam STT; higher rates
 *     add bandwidth with zero STT quality gain.
 *   • WAV (not MP3/OGG) — no device codec required; Sarvam accepts it natively.
 *   • AudioRecord (not MediaRecorder) — gives raw PCM for WAV header injection;
 *     MediaRecorder writes to file only and cannot stream.
 *   • Suspend function — capture loop runs on Dispatchers.IO; caller stays reactive.
 */
class AudioRecorder {

    private val sampleRate    = 16_000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val encoding      = AudioFormat.ENCODING_PCM_16BIT

    // OS minimum buffer; we clamp to at least 4 KB
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encoding)
        .coerceAtLeast(4096)

    @Volatile private var audioRecord: AudioRecord? = null
    @Volatile private var isRecording  = false

    /**
     * Starts recording. Suspends until [stopRecording] is called.
     * Returns complete WAV bytes ready for multipart upload.
     *
     * Caller must hold RECORD_AUDIO permission before invoking this.
     */
    @SuppressLint("MissingPermission")
    suspend fun startRecording(): ByteArray = withContext(Dispatchers.IO) {
        val pcm    = ByteArrayOutputStream()
        val buffer = ByteArray(bufferSize)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate, channelConfig, encoding, bufferSize
        ).also { rec ->
            if (rec.state != AudioRecord.STATE_INITIALIZED)
                throw IOException("AudioRecord init failed — RECORD_AUDIO permission missing?")
            rec.startRecording()
            isRecording = true
            Log.i(TAG, "Recording started — ${sampleRate}Hz mono, bufferSize=$bufferSize")
        }

        try {
            while (isActive && isRecording) {
                val n = audioRecord!!.read(buffer, 0, buffer.size)
                if (n > 0) pcm.write(buffer, 0, n)
            }
        } finally {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            Log.i(TAG, "Recording stopped — PCM bytes=${pcm.size()}")
        }

        buildWav(pcm.toByteArray())
    }

    /** Thread-safe stop signal. Safe to call from any thread. */
    fun stopRecording() {
        isRecording = false
    }

    val isCurrentlyRecording: Boolean get() = isRecording

    // ── WAV header (44 bytes, RIFF/PCM little-endian) ─────────────────────
    private fun buildWav(pcm: ByteArray): ByteArray {
        val channels     = 1
        val bitsPerSample = 16
        val byteRate     = sampleRate * channels * bitsPerSample / 8
        val blockAlign   = channels * bitsPerSample / 8

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + pcm.size)           // file size – 8
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)                       // PCM chunk size
            putShort(1)                      // AudioFormat = PCM
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(bitsPerSample.toShort())
            put("data".toByteArray())
            putInt(pcm.size)
        }.array()

        return header + pcm
    }
}