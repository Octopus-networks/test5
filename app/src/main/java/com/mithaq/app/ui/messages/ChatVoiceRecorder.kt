package com.mithaq.app.ui.messages

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Minimal voice-note recorder for the chat composer. Records AAC audio in an MPEG-4 container
 * to the app cache, then hands the file to the caller for upload. Device-local only.
 */
class ChatVoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startMs: Long = 0L

    val isRecording: Boolean get() = recorder != null

    /** Starts recording. Returns true on success. */
    fun start(): Boolean {
        if (recorder != null) return false
        return try {
            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            @Suppress("DEPRECATION")
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }
            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setAudioEncodingBitRate(64000)
            rec.setAudioSamplingRate(44100)
            rec.setOutputFile(file.absolutePath)
            rec.prepare()
            rec.start()
            recorder = rec
            outputFile = file
            startMs = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            cleanup()
            false
        }
    }

    /** Stops and returns (file, durationMs), or null if the clip was too short or failed. */
    fun stop(): Pair<File, Long>? {
        val rec = recorder ?: return null
        val file = outputFile
        recorder = null
        return try {
            rec.stop()
            rec.release()
            val durationMs = System.currentTimeMillis() - startMs
            if (file != null && file.exists() && file.length() > 0L && durationMs > 700L) {
                outputFile = null
                Pair(file, durationMs)
            } else {
                file?.delete()
                outputFile = null
                null
            }
        } catch (e: Exception) {
            try { rec.release() } catch (_: Exception) {}
            file?.delete()
            outputFile = null
            null
        }
    }

    /** Cancels and discards the current recording. */
    fun cancel() {
        val rec = recorder
        recorder = null
        if (rec != null) {
            try { rec.stop() } catch (_: Exception) {}
            try { rec.release() } catch (_: Exception) {}
        }
        outputFile?.delete()
        outputFile = null
    }

    private fun cleanup() {
        try { recorder?.release() } catch (_: Exception) {}
        recorder = null
        outputFile?.delete()
        outputFile = null
    }
}
