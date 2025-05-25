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

class ArabicVerbNavbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val recyclerView: RecyclerView
    private val skipButton: Button
    private val feedbackView: TextView
    private lateinit var adapter: VerbAdapter

    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var completionRunnable: Runnable? = null
    private var holdGestureRunnable: Runnable? = null
    private var isHoldingCorrectGesture = false
    private var gestureNeedsReset = false
    private val confidenceThreshold = 0.8f

    private var mediaPlayer: MediaPlayer? = null

    private val verbSequences = listOf(
        "يتيمم" to listOf("يتيمما", "يتيممب"),
        "اصمت" to listOf("اصمت"),
        "يسافر" to listOf("يسافر"),
        "يشرب" to listOf("يشرب"),
        "يشم" to listOf("يشم"),
        "يفكر" to listOf("يفكر"),
        "ينظر" to listOf("ينظر"),
        "يتكلم" to listOf("يتكلم")
    )

    // New flag to track if we are at the end and showing "Redo"
    private var isAtEnd = false

    init {
        LayoutInflater.from(context).inflate(R.layout.arabic_word_navbar, this, true)
        recyclerView = findViewById(R.id.wordRecyclerView)
        skipButton = findViewById(R.id.skipWordButton)
        feedbackView = findViewById(R.id.feedbackText)

        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        setupAdapter(0)
        setupSkipButton()
        updateSkipButton()
    }

    fun setupAdapter(startIndex: Int) {
        adapter = VerbAdapter(verbSequences, startIndex)
        recyclerView.adapter = adapter
    }

    private fun setupSkipButton() {
        skipButton.setOnClickListener {
            if (isAtEnd) {
                // Redo pressed: reset everything and restart
                adapter.currentWordIndex = 0
                adapter.resetSequence()
                scrollToCurrent()
                isAtEnd = false
                updateSkipButton()
                resetTimeout()
                gestureNeedsReset = false
            } else {
                // Skip pressed: move to next verb
                adapter.skipToNextVerb()
                scrollToCurrent()
                resetTimeout()
                gestureNeedsReset = false

                // If reached end, switch button to "Redo"
                if (adapter.currentWordIndex == adapter.itemCount - 1) {
                    isAtEnd = true
                    updateSkipButton()
                }
            }
        }
    }

    fun onVerbRecognized(word: String, confidence: Float) {
        val targetVerb = adapter.getCurrentVerb() ?: return

        if (confidence < confidenceThreshold) {
            cancelHoldTimer()
            showTemporaryFeedback("إشارة غير واضحة")
            return
        }

        if (word == targetVerb) {
            if (!isHoldingCorrectGesture && !gestureNeedsReset) {
                isHoldingCorrectGesture = true
                holdGestureRunnable = Runnable {
                    handleCorrectGesture()
                    isHoldingCorrectGesture = false
                    gestureNeedsReset = true
                }
                handler.postDelayed(holdGestureRunnable!!, 1000)
            }
        } else {
            cancelHoldTimer()
            if (gestureNeedsReset) {
                gestureNeedsReset = false
            }
            handleIncorrectGesture()
        }
    }

    private fun cancelHoldTimer() {
        holdGestureRunnable?.let { handler.removeCallbacks(it) }
        holdGestureRunnable = null
        isHoldingCorrectGesture = false
    }

    private fun handleCorrectGesture() {
        resetTimeout()
        adapter.markVerbSuccess()
        scrollToCurrent()
        playSuccessSound()

        if (adapter.currentVerbIndex == 0) {
            checkSequenceCompletion()
        } else {
            showNextVerbPrompt()
            startTimeoutTimer()
        }
    }

    private fun handleIncorrectGesture() {
        if (adapter.retryCount > 1) {
            handleSequenceFailure()
        }
    }

    private fun checkSequenceCompletion() {
        completionRunnable?.let { handler.removeCallbacks(it) }
        if (adapter.currentWordIndex < verbSequences.size - 1) {
            completionRunnable = Runnable {
                adapter.skipToNextVerb()
                scrollToCurrent()
                gestureNeedsReset = false
            }
            handler.postDelayed(completionRunnable!!, 1000)
        } else {
            showTemporaryFeedback("أحسنت! اكتملت جميع الأفعال!")
            // Mark we are at the end and update button to "Redo"
            isAtEnd = true
            updateSkipButton()
        }
    }

    private fun showNextVerbPrompt() {
        val nextVerb = adapter.getCurrentVerb()
        showTemporaryFeedback("الجزء التالي: $nextVerb")
    }

    private fun handleSequenceFailure() {
        showTemporaryFeedback("!خطأ في التسلسل، إعادة المحاولة")
        adapter.resetSequence()
        startTimeoutTimer()
        gestureNeedsReset = false
    }

    private fun startTimeoutTimer() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = Runnable {
            showTemporaryFeedback("!انتهى الوقت، إعادة التسلسل")
            adapter.resetSequence()
            gestureNeedsReset = false
        }
        handler.postDelayed(timeoutRunnable!!, 5000)
    }

    private fun resetTimeout() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun scrollToCurrent() {
        recyclerView.smoothScrollToPosition(adapter.currentWordIndex)
    }

    private fun updateSkipButton() {
        if (isAtEnd) {
            skipButton.text = "إعادة"  // Redo button text
        } else {
            skipButton.text = "تخطي"  // Skip button text
        }
        skipButton.isEnabled = true
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
