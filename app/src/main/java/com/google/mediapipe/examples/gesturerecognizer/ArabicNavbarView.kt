package com.google.mediapipe.examples.gesturerecognizer

import android.media.MediaPlayer
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.os.Handler
import android.os.Looper

class ArabicNavbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var skipButton: Button
    private lateinit var adapter: ArabicLetterAdapter

    private val arabicLetterNames = listOf(
        "ا", "ب", "ت", "ث", "ج", "ح", "خ",
        "د", "ذ", "ر", "ز", "س", "ش", "ص",
        "ض", "ط", "ظ", "ع", "غ", "ف", "ق",
        "ك", "ل", "م", "ن", "ه", "و", "ي",
        "لا", "ة", "إ", "ئ", "ال", "none"
    )

    private val handler = Handler(Looper.getMainLooper())
    private var skipRunnable: Runnable? = null
    private var isSuccessHandled = false

    init {
        initView()
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.arabic_navbar, this, true)
        recyclerView = findViewById(R.id.letterRecyclerView)
        skipButton = findViewById(R.id.skipButton)

        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, true)
        setupAdapter(0)

        skipButton.setOnClickListener {
            cancelPendingSkip()
            if (isLastLetter()) {
                setupAdapter(0)
                skipButton.text = "تخطي"  // Arabic for "Skip"
            } else {
                adapter.skipToNext()
                updateSkipButtonText()
            }
            isSuccessHandled = false
            scrollToCurrent()
        }
    }

    private fun setupAdapter(startIndex: Int) {
        adapter = ArabicLetterAdapter(arabicLetterNames, startIndex)
        recyclerView.adapter = adapter
        updateSkipButtonText()
    }

    private fun scrollToCurrent() {
        recyclerView.smoothScrollToPosition(adapter.getCurrentIndex())
    }

    private fun getCurrentIndex(): Int = arabicLetterNames.indexOf(adapter.getCurrentLetter())

    private fun isLastLetter(): Boolean = getCurrentIndex() == arabicLetterNames.size - 1

    private fun updateSkipButtonText() {
        skipButton.text = if (isLastLetter()) "اعادة المحاوله" else "تخطي"  // "Redo" : "Skip"
    }

    fun getCurrentLetter(): String = adapter.getCurrentLetter()

    fun onLetterRecognized(letter: String, confidence: Float) {
        val currentLetter = adapter.getCurrentLetter()
        if (!isSuccessHandled && letter == currentLetter && confidence >= 0.8f) {
            isSuccessHandled = true
            cancelPendingSkip()
            adapter.markCurrentLetterSuccess()
            playSuccessSound()
            scheduleSkipToNext()
        }
    }

    private fun scheduleSkipToNext() {
        skipRunnable = Runnable {
            adapter.skipToNext()
            isSuccessHandled = false
            scrollToCurrent()
            updateSkipButtonText()
        }
        handler.postDelayed(skipRunnable!!, 1000)
    }

    private fun cancelPendingSkip() {
        skipRunnable?.let {
            handler.removeCallbacks(it)
            skipRunnable = null
        }
    }

    private fun playSuccessSound() {
        val mediaPlayer = MediaPlayer.create(context, R.raw.success)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener { it.release() }
    }
}