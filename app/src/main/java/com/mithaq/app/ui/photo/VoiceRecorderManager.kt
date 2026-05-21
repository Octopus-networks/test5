package com.mithaq.app.ui.photo

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

class VoiceRecorderManager(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null

    fun startRecording(outputFile: File) {
        try {
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setOutputFile(outputFile.absolutePath)
            rec.prepare()
            rec.start()
            recorder = rec
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun stopRecording() {
        try {
            recorder?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            recorder?.release()
            recorder = null
        }
    }

    fun startPlaying(audioPath: String, onComplete: () -> Unit) {
        try {
            stopPlaying()
            val mp = MediaPlayer()
            mp.setDataSource(audioPath)
            mp.prepare()
            mp.setOnCompletionListener {
                onComplete()
                stopPlaying()
            }
            mp.start()
            player = mp
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun stopPlaying() {
        try {
            player?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            player?.release()
            player = null
        }
    }
}
