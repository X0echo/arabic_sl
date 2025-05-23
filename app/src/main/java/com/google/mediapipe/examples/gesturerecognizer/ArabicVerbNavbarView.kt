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
import com.google.mediapipe.examples.gesturerecognizer.databinding.ArabicVerbNavbarBinding

class ArabicVerbNavbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var skipButton: Button
    private lateinit var feedbackView: TextView
    private lateinit var adapter: VerbAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var confidenceThreshold = 0.8f
    private var mediaPlayer: MediaPlayer? = null

    private var completionRunnable: Runnable? = null
    private var holdGestureRunnable: Runnable? = null
    private var isHoldingCorrectGesture = false

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

    private val binding = ArabicVerbNavbarBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    init {
        initView()
    }

    private fun initView() {
        recyclerView = binding.wordRecyclerView
        skipButton = binding.skipWordButton
        feedbackView = binding.feedbackText

        recyclerView.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
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
            adapter.skipToNextVerb()
            scrollToCurrent()
            resetTimeout()
        }
    }

    fun onVerbRecognized(verb: String, confidence: Float) {
        val targetGesture = adapter.getCurrentVerb() ?: return

        if (confidence < confidenceThreshold) {
            cancelHoldTimer()
            handleLowConfidence(confidence)
            return
        }

        if (verb == targetGesture) {
            if (!isHoldingCorrectGesture) {
                isHoldingCorrectGesture = true
                holdGestureRunnable = Runnable {
                    handleCorrectGesture()
                    isHoldingCorrectGesture = false
                }
                handler.postDelayed(holdGestureRunnable!!, 1000)
            }
        } else {
            cancelHoldTimer()
            handleIncorrectGesture()
        }
    }

    private fun cancelHoldTimer() {
        holdGestureRunnable?.let { handler.removeCallbacks(it) }
        holdGestureRunnable = null
        isHoldingCorrectGesture = false
    }

    private fun handleLowConfidence(confidence: Float) {
        showTemporaryFeedback("ثقة منخفضة: ${(confidence * 100).toInt()}%")
    }

    private fun handleCorrectGesture() {
        resetTimeout()
        adapter.markVerbSuccess()
        scrollToCurrent()

        if (adapter.currentVerbIndex == 0) {
            checkSequenceCompletion()
        } else {
            showNextVerbPrompt()
            startTimeoutTimer()
        }
    }

    private fun handleIncorrectGesture() {
        if (adapter.retryCount > 1) handleSequenceFailure()
    }

    private fun checkSequenceCompletion() {
        completionRunnable?.let { handler.removeCallbacks(it) }
        if (adapter.currentWordIndex < verbSequences.size - 1) {
            completionRunnable = Runnable {
                adapter.skipToNextVerb()
                scrollToCurrent()
            }
            handler.postDelayed(completionRunnable!!, 1000)
        } else {
            showTemporaryFeedback("أحسنت! اكتملت جميع الأفعال!")
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
    }

    private fun startTimeoutTimer() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = Runnable {
            showTemporaryFeedback("!انتهى الوقت، إعادة التسلسل")
            adapter.resetSequence()
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
        skipButton.text = "تخطي"
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