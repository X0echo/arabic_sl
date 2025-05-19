// 1. Updated WordAdapter.kt
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
    private val words: List<String>,
    var currentWordIndex: Int
) : RecyclerView.Adapter<WordAdapter.WordViewHolder>() {

    private val successfulWords = mutableSetOf<Int>()
    var currentLetterIndex = 0
    var retryCount = 0
    private val letterStates = mutableMapOf<Pair<Int, Int>, LetterState>()

    enum class LetterState { PENDING, CORRECT, INCORRECT }

    inner class WordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val wordText: TextView = view.findViewById(R.id.wordText)
        val progressText: TextView = view.findViewById(R.id.progressText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_arabic_word, parent, false)
        return WordViewHolder(view)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val word = words[position]
        val spannable = SpannableString(word)

        // Apply letter backgrounds
        word.forEachIndexed { index, _ ->
            val state = letterStates[position to index] ?: LetterState.PENDING
            val color = when (state) {
                LetterState.CORRECT -> Color.GREEN
                LetterState.INCORRECT -> Color.RED
                LetterState.PENDING -> Color.TRANSPARENT
            }
            spannable.setSpan(
                BackgroundColorSpan(color),
                index,
                index + 1,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        holder.wordText.text = spannable
        holder.progressText.text = if (position == currentWordIndex) {
            "${currentLetterIndex + 1}/${word.length}"
        } else ""
    }

    fun getCurrentWord(): String = words[currentWordIndex]
    fun getCurrentLetter(): Char? = getCurrentWord().getOrNull(currentLetterIndex)

    fun markLetterSuccess() {
        letterStates[currentWordIndex to currentLetterIndex] = LetterState.CORRECT
        currentLetterIndex++
        retryCount = 0
        notifyItemChanged(currentWordIndex)
    }

    fun markLetterIncorrect() {
        letterStates[currentWordIndex to currentLetterIndex] = LetterState.INCORRECT
        retryCount++
        notifyItemChanged(currentWordIndex)
    }

    fun resetWord() {
        currentLetterIndex = 0
        retryCount = 0
        words[currentWordIndex].forEachIndexed { index, _ ->
            letterStates[currentWordIndex to index] = LetterState.PENDING
        }
        notifyItemChanged(currentWordIndex)
    }

    fun skipToNextWord() {
        if (currentWordIndex < words.size - 1) {
            currentWordIndex++
            resetWord()
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int = words.size
}