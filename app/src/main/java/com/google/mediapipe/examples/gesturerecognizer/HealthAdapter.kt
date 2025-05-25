package com.google.mediapipe.examples.gesturerecognizer

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HealthAdapter(
    private val wordData: List<Pair<String, List<String>>>,
    var currentWordIndex: Int
) : RecyclerView.Adapter<HealthAdapter.WordViewHolder>() {

    private val successfulGestures = mutableSetOf<Pair<Int, Int>>()
    var currentGestureIndex = 0
    var retryCount = 0
    private val gestureStates = mutableMapOf<Pair<Int, Int>, LetterState>()

    private var gestureNeedsReset = false
    private var lastRecognizedGesture: String? = null

    enum class LetterState { PENDING, CORRECT, INCORRECT }

    inner class WordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val wordText: TextView = view.findViewById(R.id.wordText)
        val progressText: TextView = view.findViewById(R.id.progressText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        return WordViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_arabic_word, parent, false)
        )
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val (displayName, gestureParts) = wordData[position]
        val isCurrentWord = position == currentWordIndex
        val allGesturesCompleted = gestureParts.indices.all { index ->
            gestureStates[position to index] == LetterState.CORRECT
        }

        with(holder.wordText) {
            when {
                allGesturesCompleted -> {
                    setBackgroundResource(R.drawable.success_letter_bg)
                    textSize = 18f
                }
                isCurrentWord -> {
                    setBackgroundResource(R.drawable.current_letter_bg)
                    textSize = 20f
                }
                else -> {
                    setBackgroundResource(R.drawable.letter_box_bg)
                    textSize = 18f
                }
            }

            setTextColor(Color.WHITE)
            text = displayName
        }

        holder.progressText.text = if (isCurrentWord) {
            "${currentGestureIndex + 1}/${gestureParts.size}"
        } else {
            ""
        }
    }

    fun getCurrentSequence(): List<String> = wordData[currentWordIndex].second

    fun getCurrentGesture(): String? = getCurrentSequence().getOrNull(currentGestureIndex)

    /**
     * Call this method when a gesture is recognized with given confidence.
     * Prevents repeated success if user holds the same gesture.
     */
    fun onGestureRecognized(word: String, confidence: Float, confidenceThreshold: Float = 0.8f) {
        val currentGesture = getCurrentGesture() ?: return

        if (confidence < confidenceThreshold) {
            // Ignore low confidence gestures or handle elsewhere
            return
        }

        // Reset the flag if new gesture detected (user changed hand sign)
        if (word != lastRecognizedGesture) {
            gestureNeedsReset = false
        }
        lastRecognizedGesture = word

        // If already marked success for this gesture and still holding, ignore
        if (gestureNeedsReset) return

        if (word == currentGesture) {
            markGestureSuccess()
            gestureNeedsReset = true
        } else {
            markGestureIncorrect()
        }
    }

    fun markGestureSuccess() {
        gestureStates[currentWordIndex to currentGestureIndex] = LetterState.CORRECT
        if (currentGestureIndex < getCurrentSequence().lastIndex) {
            currentGestureIndex++
        } else {
            successfulGestures.add(currentWordIndex to currentGestureIndex)
            currentGestureIndex = 0
        }
        retryCount = 0
        notifyItemChanged(currentWordIndex)
    }

    fun markGestureIncorrect() {
        gestureStates[currentWordIndex to currentGestureIndex] = LetterState.INCORRECT
        retryCount++
        notifyItemChanged(currentWordIndex)
    }

    fun resetSequence() {
        currentGestureIndex = 0
        retryCount = 0
        gestureNeedsReset = false
        lastRecognizedGesture = null
        getCurrentSequence().indices.forEach { index ->
            gestureStates[currentWordIndex to index] = LetterState.PENDING
        }
        notifyItemChanged(currentWordIndex)
    }

    fun skipToNextWord() {
        if (currentWordIndex < wordData.size - 1) {
            currentWordIndex++
            resetSequence()
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int = wordData.size
}
