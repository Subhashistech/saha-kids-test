package com.saha.kidstest

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private lateinit var state: AppState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        state = AppState(
            store = Store(applicationContext),
            speak = { speak(it) },
            stopSpeaking = { tts?.stop() },
        )
        setContent {
            SahaTheme { App(state) }
        }
    }

    override fun onInit(status: Int) {
        val engine = tts ?: return
        if (status != TextToSpeech.SUCCESS) return
        val result = engine.setLanguage(Locale("en", "IN"))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.US)
        }
        engine.setSpeechRate(0.85f)
        ttsReady = true
    }

    private fun speak(text: String) {
        if (!ttsReady) {
            Toast.makeText(this, "The tablet's voice isn't ready yet. Try again in a moment.", Toast.LENGTH_SHORT).show()
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "question")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
