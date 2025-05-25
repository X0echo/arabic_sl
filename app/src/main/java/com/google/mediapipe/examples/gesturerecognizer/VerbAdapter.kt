package com.google.mediapipe.examples.gesturerecognizer

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VerbAdapter(
    private val verbData: List<Pair<String, List<String>>>,
    var currentWordIndex: Int
) : RecyclerView.Adapter<VerbAdapter.VerbViewHolder>() {

    private val successfulVerbs = mutableSetOf<Pair<Int, Int>>()
    var currentVerbIndex = 0
    var retryCount = 0
    private val verbStates = mutableMapOf<Pair<Int, Int>, VerbState>()

    enum class VerbState { PENDING, CORRECT, INCORRECT }

    inner class VerbViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val verbText: TextView = view.findViewById(R.id.wordText)
        val progressText: TextView = view.findViewById(R.id.progressText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerbViewHolder {
        return VerbViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_arabic_word, parent, false)
        )
    }

    override fun onBindViewHolder(holder: VerbViewHolder, position: Int) {
        val (displayName, verbParts) = verbData[position]
        val isCurrentVerb = position == currentWordIndex
        val allVerbsCompleted = verbParts.indices.all { index ->
            verbStates[position to index] == VerbState.CORRECT
        }

        with(holder.verbText) {
            when {
                allVerbsCompleted -> {
                    setBackgroundResource(R.drawable.success_letter_bg)
                    textSize = 18f
                }
                isCurrentVerb -> {
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

        holder.progressText.text = if (isCurrentVerb) {
            "${currentVerbIndex + 1}/${verbParts.size}"
        } else {
            ""
        }
    }

    fun getCurrentSequence(): List<String> = verbData[currentWordIndex].second
    fun getCurrentVerb(): String? = getCurrentSequence().getOrNull(currentVerbIndex)

    fun markVerbSuccess() {
        verbStates[currentWordIndex to currentVerbIndex] = VerbState.CORRECT
        if (currentVerbIndex < getCurrentSequence().lastIndex) {
            currentVerbIndex++
        } else {
            successfulVerbs.add(currentWordIndex to currentVerbIndex)
            currentVerbIndex = 0
        }
        retryCount = 0
        notifyItemChanged(currentWordIndex)
    }

    fun markVerbIncorrect() {
        verbStates[currentWordIndex to currentVerbIndex] = VerbState.INCORRECT
        retryCount++
        notifyItemChanged(currentWordIndex)
    }

    fun resetSequence() {
        currentVerbIndex = 0
        retryCount = 0
        getCurrentSequence().indices.forEach { index ->
            verbStates[currentWordIndex to index] = VerbState.PENDING
        }
        notifyItemChanged(currentWordIndex)
    }

    fun skipToNextVerb() {
        if (currentWordIndex < verbData.size - 1) {
            currentWordIndex++
            resetSequence()
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int = verbData.size
}
