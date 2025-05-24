package com.google.mediapipe.examples.gesturerecognizer

import android.content.Context
import android.graphics.Rect
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ArabicEducationNavbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var skipButton: Button
    private lateinit var feedbackView: TextView
    private lateinit var adapter: EducationAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var confidenceThreshold = 0.8f
    private var mediaPlayer: MediaPlayer? = null

    private var completionRunnable: Runnable? = null
    private var holdGestureRunnable: Runnable? = null
    private var isHoldingCorrectGesture = false

    private val educationSequences = listOf(
        "جامع" to listOf("جامعا", "جامعب"),
        "جواب" to listOf("جواب"),
        "عربية" to listOf("عربية"),
        "لغة" to listOf("لغة"),
        "ممتاز" to listOf("ممتاز"),
    )

    init {
        initView()
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.arabic_education_navbar, this, true)
        recyclerView = findViewById(R.id.educationRecyclerView)
        skipButton = findViewById(R.id.skipEducationButton)
        feedbackView = findViewById(R.id.feedbackText)

        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.addItemDecoration(HorizontalSpaceItemDecoration(24))
        setupAdapter(0)
        setupSkipButton()
        updateSkipButton()
    }

    fun setupAdapter(startIndex: Int) {
        adapter = EducationAdapter(educationSequences, startIndex)
        recyclerView.adapter = adapter
    }

    private fun setupSkipButton() {
        skipButton.setOnClickListener {
            adapter.skipToNextWord()
            scrollToCurrent()
            resetTimeout()
        }
    }

    fun onEducationWordRecognized(word: String, confidence: Float) {
        val targetGesture = adapter.getCurrentGesture() ?: return

        if (confidence < confidenceThreshold) {
            cancelHoldTimer()
            handleLowConfidence()
            return
        }

        if (word == targetGesture) {
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

    private fun handleLowConfidence() {
        showTemporaryFeedback("إشارة غير واضحة")
    }

    private fun handleCorrectGesture() {
        resetTimeout()
        adapter.markGestureSuccess()

        if (adapter.isSequenceCompleted()) {
            checkSequenceCompletion()
        } else {
            showNextGesturePrompt()
            startTimeoutTimer()
        }
    }

    private fun handleIncorrectGesture() {
        if (adapter.retryCount > 1) handleSequenceFailure()
    }

    private fun checkSequenceCompletion() {
        completionRunnable?.let { handler.removeCallbacks(it) }
        if (adapter.currentWordIndex < educationSequences.size - 1) {
            completionRunnable = Runnable {
                adapter.skipToNextWord()
                scrollToCurrent()
            }
            handler.postDelayed(completionRunnable!!, 1000)
        } else {
            showTemporaryFeedback("أحسنت! اكتملت جميع الكلمات التعليمية!")
        }
    }

    private fun showNextGesturePrompt() {
        val nextGesture = adapter.getCurrentGesture()
        showTemporaryFeedback("الجزء التالي: $nextGesture")
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

    class HorizontalSpaceItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            if (position != parent.adapter?.itemCount?.minus(1)) {
                outRect.right = space
            }
        }
    }
}