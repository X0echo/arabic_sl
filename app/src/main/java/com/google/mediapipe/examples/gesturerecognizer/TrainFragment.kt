package com.google.mediapipe.examples.gesturerecognizer


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.mediapipe.examples.gesturerecognizer.fragment.LetterCameraFragment

class TrainFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_train, container, false)

        val lettersButton = view.findViewById<MaterialButton>(R.id.lettersButton)
        val numbersButton = view.findViewById<MaterialButton>(R.id.numbersButton)
        val challengesButton = view.findViewById<MaterialButton>(R.id.challengesButton)

        lettersButton.setOnClickListener {
            // Replace with LettersPreviewFragment
            replaceFragment(LetterCameraFragment())
        }

        numbersButton.setOnClickListener {
            // Replace with NumbersPreviewFragment

        }

        challengesButton.setOnClickListener {
            // Replace with ChallengesPreviewFragment

        }

        return view
    }

    private fun replaceFragment(fragment: Fragment) {
        // Use parentFragmentManager to access the activity's fragment manager
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment) // Replace current fragment
            .addToBackStack(null) // Add transaction to back stack (optional)
            .commit()
    }
}