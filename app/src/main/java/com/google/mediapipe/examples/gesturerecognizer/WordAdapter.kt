package com.google.mediapipe.examples.gesturerecognizer

import android.graphics.Color
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WordAdapter(
    private val wordData: List<Pair<String, List<String>>>,
    var currentWordIndex: Int
) : RecyclerView.Adapter<WordAdapter.WordViewHolder>() {

    private val successfulGestures = mutableSetOf<Pair<Int, Int>>()
    var currentGestureIndex = 0
    var retryCount = 0
    private val gestureStates = mutableMapOf<Pair<Int, Int>, LetterState>()

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

        // Color coding based on current gesture state
        val state = gestureStates[position to currentGestureIndex]
        val backgroundColor = when (state) {
            LetterState.CORRECT -> Color.GREEN
            LetterState.INCORRECT -> Color.RED
            else -> Color.TRANSPARENT
        }

        val spannable = SpannableString(displayName)
        spannable.setSpan(
            BackgroundColorSpan(backgroundColor),
            0, displayName.length,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        with(holder) {
            wordText.text = spannable
            progressText.text = if (position == currentWordIndex) {
                "${currentGestureIndex + 1}/${gestureParts.size}"
            } else ""
        }
    }

    fun getCurrentSequence(): List<String> = wordData[currentWordIndex].second
    fun getCurrentGesture(): String? = getCurrentSequence().getOrNull(currentGestureIndex)

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
