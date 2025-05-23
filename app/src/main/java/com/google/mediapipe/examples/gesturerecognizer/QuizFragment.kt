package com.google.mediapipe.examples.gesturerecognizer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.storage.FirebaseStorage
import com.google.mediapipe.examples.gesturerecognizer.fragment.ResultFragment

class QuizFragment : Fragment() {

    private var currentHiddenIndex: Int? = null
    private lateinit var videoView: VideoView
    private lateinit var optionImages: List<ImageView>
    private lateinit var optionTexts: List<TextView>
    private lateinit var optionLayouts: List<LinearLayout>
    private lateinit var hintButton: Button
    private lateinit var storage: FirebaseStorage

    data class Option(val imageRes: Int, val label: String)

    data class Question(
        val videoPath: String,
        val options: List<Option>,
        val correctIndex: Int,
        val mute: Boolean = false
    )

    private var questions: List<Question> = listOf()
    private var currentIndex = 0
    private var score = 0
    private var hasAnswered = false
    private var hintUsed = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_quiz, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        storage = FirebaseStorage.getInstance()

        videoView = view.findViewById(R.id.videoView)
        hintButton = view.findViewById(R.id.hintButton)

        optionLayouts = listOf(
            view.findViewById(R.id.option1),
            view.findViewById(R.id.option2),
            view.findViewById(R.id.option3)
        )

        optionImages = listOf(
            view.findViewById(R.id.optionImage1),
            view.findViewById(R.id.optionImage2),
            view.findViewById(R.id.optionImage3)
        )

        optionTexts = listOf(
            view.findViewById(R.id.optionText1),
            view.findViewById(R.id.optionText2),
            view.findViewById(R.id.optionText3)
        )

        questions = sampleQuestions.shuffled().take(20)
        loadQuestion()

        val scaleAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.click_scale)

        optionLayouts.forEachIndexed { index, layout ->
            layout.setOnClickListener {
                if (hasAnswered) return@setOnClickListener
                hasAnswered = true

                val isCorrect = index == questions[currentIndex].correctIndex

                if (isCorrect) {
                    layout.setBackgroundColor(
                        ContextCompat.getColor(requireContext(), android.R.color.holo_green_light)
                    )
                    score++
                } else {
                    layout.setBackgroundColor(
                        ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
                    )
                    optionLayouts[questions[currentIndex].correctIndex].setBackgroundColor(
                        ContextCompat.getColor(requireContext(), android.R.color.holo_green_light)
                    )
                }

                layout.startAnimation(scaleAnim)

                layout.postDelayed({
                    currentIndex++
                    if (currentIndex < questions.size) {
                        loadQuestion()
                    } else {
                        showScoreFragment()
                    }
                }, 1000)
            }
        }

        hintButton.setOnClickListener {
            if (hintUsed) return@setOnClickListener
            hintUsed = true
            hintButton.isEnabled = false

            val question = questions[currentIndex]
            val wrongAnswers = optionLayouts.indices.filter { it != question.correctIndex }
            currentHiddenIndex = wrongAnswers.random()
            optionLayouts[currentHiddenIndex!!].visibility = View.GONE
        }
    }

    private fun loadQuestion() {
        hasAnswered = false
        hintUsed = false
        hintButton.isEnabled = true
        currentHiddenIndex = null
        val question = questions[currentIndex]

        optionLayouts.forEach {
            it.visibility = View.VISIBLE
            it.alpha = 1f
            it.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
        }

        loadVideoFromFirebase(question.videoPath)

        question.options.forEachIndexed { i, option ->
            optionImages[i].setImageResource(option.imageRes)
            optionTexts[i].text = option.label
        }
    }

    private fun loadVideoFromFirebase(videoPath: String) {
        // Arrêter l’ancienne lecture
        videoView.stopPlayback()
        videoView.setVideoURI(null)
        videoView.setMediaController(null) // pas de contrôles visibles

        // Référencer le fichier Firebase Storage
        val storageRef = storage.reference.child(videoPath)

        // Télécharger l’URL de la vidéo
        storageRef.downloadUrl.addOnSuccessListener { uri ->
            // Ne change PAS les dimensions ici → le layout XML s’en charge

            // Charger la nouvelle vidéo
            videoView.setVideoURI(uri)
            videoView.setOnPreparedListener { mp ->
                mp.isLooping = true
                if (questions[currentIndex].mute) {
                    mp.setVolume(0f, 0f)
                }
                videoView.start()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Erreur de chargement de la vidéo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showScoreFragment() {
        val resultFragment = ResultFragment.newInstance(score, questions.size)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, resultFragment)
            .addToBackStack(null)
            .commit()
    }

    private val sampleQuestions = listOf(
        Question(
            videoPath = "sign_videos/حكم.mp4",
            options = listOf(
                Option(R.drawable.arbitre, "حكم"),
                Option(R.drawable.gardien, "حارس"),
                Option(R.drawable.joueur, "لاعب")
            ),
            correctIndex = 0,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/رياضيات.mp4",
            options = listOf(
                Option(R.drawable.phyzique, "فيزياء"),
                Option(R.drawable.histoire, "تاريخ"),
                Option(R.drawable.math, "رياضيات")
            ),
            correctIndex = 2,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/سبورة.mp4",
            options = listOf(
                Option(R.drawable.tableau, "سبورة"),
                Option(R.drawable.stylo, "قلم"),
                Option(R.drawable.gomme, "ممحاة")
            ),
            correctIndex = 0,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/ضربة جزاء.mp4",
            options = listOf(
                Option(R.drawable.stade, "ملعب"),
                Option(R.drawable.penalty, "ضربة جزاء"),
                Option(R.drawable.goal, "مرمى")
            ),
            correctIndex = 1,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/مدرسة.mp4",
            options = listOf(
                Option(R.drawable.hospital, "مستشفى"),
                Option(R.drawable.stade, "ملعب"),
                Option(R.drawable.ecole, "مدرسة")
            ),
            correctIndex = 2,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/معلم.mp4",
            options = listOf(
                Option(R.drawable.prof, "معلم"),
                Option(R.drawable.paintre, "رسام"),
                Option(R.drawable.filmer, "مصور")
            ),
            correctIndex = 0,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/ملعب.mp4",
            options = listOf(
                Option(R.drawable.goal, "مرمى"),
                Option(R.drawable.stade, "ملعب"),
                Option(R.drawable.ecole, "مدرسة")
            ),
            correctIndex = 1,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/يكتب.mp4",
            options = listOf(
                Option(R.drawable.reflichir, "يفكر"),
                Option(R.drawable.ecrire, "يكتب"),
                Option(R.drawable.filmer, "يصور")
            ),
            correctIndex = 1,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/طرد.mp4",
            options = listOf(
                Option(R.drawable.ballon, "كرة"),
                Option(R.drawable.arbitre, "حكم"),
                Option(R.drawable.carte_rouge, "طرد")
            ),
            correctIndex = 2,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/مرحبا.mp4",
            options = listOf(
                Option(R.drawable.hi, "مرحبا"),
                Option(R.drawable.bien, "بخير"),
                Option(R.drawable.hmd, "الحمد لله")
            ),
            correctIndex = 0,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/كيف حالك.mp4",
            options = listOf(
                Option(R.drawable.how, "كيف حالك"),
                Option(R.drawable.bien, "بخير"),
                Option(R.drawable.hi, "مرحبا")
            ),
            correctIndex = 0,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/سؤال.mp4",
            options = listOf(
                Option(R.drawable.exam, "اختبار"),
                Option(R.drawable.reponse, "جواب"),
                Option(R.drawable.question, "سؤال")
            ),
            correctIndex = 2,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/أسد.mp4", // Chemin dans Firebase Storage
            options = listOf(
                Option(R.drawable.lion, "الأسد"),
                Option(R.drawable.tigre, "نمر"),
                Option(R.drawable.chat, "قطة")
            ),
            correctIndex = 0,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/أزرق.mp4",
            options = listOf(
                Option(R.drawable.orange, "البرتقالي"),
                Option(R.drawable.yellow, "أصفر"),
                Option(R.drawable.blue, "أزرق")
            ),
            correctIndex = 2,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/عنكبوت.mp4",
            options = listOf(
                Option(R.drawable.spider, "عنكبوت"),
                Option(R.drawable.chat, "قطة"),
                Option(R.drawable.khoufach, "خفاش")
            ),
            correctIndex = 0,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/ينام.mp4",
            options = listOf(
                Option(R.drawable.work, "يعمل"),
                Option(R.drawable.sleep, "ينام"),
                Option(R.drawable.grimper, "تسلق")
            ),
            correctIndex = 1,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/يخيط.mp4",
            options = listOf(
                Option(R.drawable.rire, "ضحك"),
                Option(R.drawable.ykheyet, "يخيط"),
                Option(R.drawable.reflichir, "يفكر")
            ),
            correctIndex = 1,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/بنفسجي.mp4",
            options = listOf(
                Option(R.drawable.blue, "أزرق"),
                Option(R.drawable.gris, "رمادي"),
                Option(R.drawable.purpul, "البنفسجي")
            ),
            correctIndex = 2,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/سمك.mp4",
            options = listOf(
                Option(R.drawable.poisson, "سمكة"),
                Option(R.drawable.boma, "بومة"),
                Option(R.drawable.torture, "سلحفاة")
            ),
            correctIndex = 0,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/أزرق سماوي.mp4",
            options = listOf(
                Option(R.drawable.green, "اخضر"),
                Option(R.drawable.cyan, "أزرق سماوي"),
                Option(R.drawable.orange, "برتقالي")
            ),
            correctIndex = 1,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/بني.mp4",
            options = listOf(
                Option(R.drawable.orange, "برتقالي"),
                Option(R.drawable.brown, "بني"),
                Option(R.drawable.yellow, "أصفر")
            ),
            correctIndex = 1,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/نسر.mp4",
            options = listOf(
                Option(R.drawable.boma, "بومة"),
                Option(R.drawable.nasr, "نسر"),
                Option(R.drawable.bata, "بطة")
            ),
            correctIndex = 1,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/ثعبان.mp4",
            options = listOf(
                Option(R.drawable.girafe, "زرافة"),
                Option(R.drawable.serpent, "ثعبان"),
                Option(R.drawable.cheval, "حصان")
            ),
            correctIndex = 1,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/جدة.mp4",
            options = listOf(
                Option(R.drawable.grandmother, "جدة"),
                Option(R.drawable.grandfather, "جد"),
                Option(R.drawable.fils, "ولد")
            ),
            correctIndex = 0,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/برتقالي.mp4",
            options = listOf(
                Option(R.drawable.red, "أحمر"),
                Option(R.drawable.gris, "رمادي"),
                Option(R.drawable.orange, "برتقالي")
            ),
            correctIndex = 2,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/زرافة.mp4",
            options = listOf(
                Option(R.drawable.girafe, "زرافة"),
                Option(R.drawable.elephant, "فيل"),
                Option(R.drawable.lapin, "أرنب")
            ),
            correctIndex = 0,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/يصور.mp4",
            options = listOf(
                Option(R.drawable.photo, "يصور"),
                Option(R.drawable.rire, "يضحك"),
                Option(R.drawable.work, "يعمل")
            ),
            correctIndex = 0,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/فيل.mp4",
            options = listOf(
                Option(R.drawable.piroce, "ببغاء"),
                Option(R.drawable.cheval, "حصان"),
                Option(R.drawable.elephant, "فيل")
            ),
            correctIndex = 2,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/نحلة.mp4",
            options = listOf(
                Option(R.drawable.bata, "نسر"),
                Option(R.drawable.abaille, "نحلة"),
                Option(R.drawable.piroce, "ببغاء")
            ),
            correctIndex = 1,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/يفتح.mp4",
            options = listOf(
                Option(R.drawable.reflichir, "يفكر"),
                Option(R.drawable.play, "يلعب"),
                Option(R.drawable.open, "يفتح")
            ),
            correctIndex = 1,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/أصفر.mp4",
            options = listOf(
                Option(R.drawable.red, "أحمر"),
                Option(R.drawable.blue, "أزرق"),
                Option(R.drawable.yellow, "أصفر")
            ),
            correctIndex = 2,
            mute = true
        ),
        Question(
            videoPath = "sign_videos/خفاش.mp4",
            options = listOf(
                Option(R.drawable.khoufach, "خفاش"),
                Option(R.drawable.bata, "بطة"),
                Option(R.drawable.nasr, "نسر")
            ),
            correctIndex = 0,
            mute = true
        ),
        )
}
