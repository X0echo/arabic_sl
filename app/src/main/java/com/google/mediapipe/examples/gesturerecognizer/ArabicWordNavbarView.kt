package com.google.mediapipe.examples.gesturerecognizer

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ArabicWordNavbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var skipButton: Button
    private lateinit var feedbackView: TextView
    private lateinit var adapter: WordAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var remainingSkips = 3
    private var confidenceThreshold = 0.8f
    private var mediaPlayer: MediaPlayer? = null

    private val arabicWords = listOf(
        "اب", "حب", "شمس", "كتاب", "مدرسة",
        "سيارة", "تفاحة", "جزيرة", "وردة", "سفر"
    )

    init {
        initView()
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.arabic_word_navbar, this, true)
        recyclerView = findViewById(R.id.wordRecyclerView)
        skipButton = findViewById(R.id.skipWordButton)
        feedbackView = findViewById(R.id.feedbackText)

        recyclerView.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        setupAdapter(0)
        setupSkipButton()
        updateSkipButton()
    }

    private fun setupAdapter(startIndex: Int) {
        adapter = WordAdapter(arabicWords, startIndex)
        recyclerView.adapter = adapter
    }

    private fun setupSkipButton() {
        skipButton.setOnClickListener {
            if (remainingSkips > 0) {
                remainingSkips--
                adapter.skipToNextWord()
                updateSkipButton()
                scrollToCurrent()
                timeoutRunnable?.let { it1 -> handler.removeCallbacks(it1) } // Reset timer on skip
            }
        }
    }

    fun onLetterRecognized(letter: String, confidence: Float) {
        val targetLetter = adapter.getCurrentLetter()?.toString() ?: return

        when {
            confidence < confidenceThreshold -> {
                showTemporaryFeedback("ثقة منخفضة: ${(confidence * 100).toInt()}%")
                adapter.markLetterIncorrect()
            }
            letter == targetLetter -> handleCorrectLetter()
            else -> handleIncorrectLetter()
        }
    }

    private fun handleCorrectLetter() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        adapter.markLetterSuccess()
        startTimeoutTimer()
        checkWordCompletion()
    }

    private fun handleIncorrectLetter() {
        adapter.markLetterIncorrect()
        if (adapter.retryCount > 1) {
            showTemporaryFeedback("تسلسل خاطئ! إعادة التعيين...")
            handleWordFailure()
        }
    }

    private fun startTimeoutTimer() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = Runnable {
            showTemporaryFeedback("انتهى الوقت! إعادة التعيين...")
            adapter.resetWord()
        }
        handler.postDelayed(timeoutRunnable!!, 5000)
    }

    private fun checkWordCompletion() {
        if (adapter.currentLetterIndex >= adapter.getCurrentWord().length) {
            playSuccessSound()
            handler.postDelayed({
                if (adapter.currentWordIndex < arabicWords.size - 1) {
                    adapter.skipToNextWord()
                    scrollToCurrent()
                } else {
                    showTemporaryFeedback("أحسنت! اكتملت جميع الكلمات!")
                }
            }, 1500)
        }
    }

    private fun handleWordFailure() {
        adapter.resetWord()
        startTimeoutTimer()
    }

    private fun scrollToCurrent() {
        recyclerView.smoothScrollToPosition(adapter.currentWordIndex)
    }

    private fun updateSkipButton() {
        skipButton.text = "تخطي (${remainingSkips} متبقية)"
        skipButton.isEnabled = remainingSkips > 0
    }

    private fun showTemporaryFeedback(text: String) {
        feedbackView.text = text
        handler.postDelayed({ feedbackView.text = "" }, 2000)
    }

    private fun playSuccessSound() {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(context, R.raw.success).apply {
            start()
            setOnCompletionListener { release() }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
    }
}