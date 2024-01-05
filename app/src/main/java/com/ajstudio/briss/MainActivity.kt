package com.ajstudio.briss

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnRepeat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.ajstudio.briss.databinding.ActivityMainBinding
import com.ajstudio.briss.model.BreatheMethod
import com.ajstudio.briss.tools.carouselview.CarouselScrollListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private val TAG = "MainActivity"
    private val methods = listOf(
        BreatheMethod(
            "", 4, 4, 4, 4, "", "", "", ""
        ),
        BreatheMethod(
            "Box Breathing", 4, 4, 4, 4,
            "A four-sided approach in which each “side” is a step with the same amount of completion time.",
            "Exhale slowly to a count of four, hold the empty lungs for a four-count, inhale slowly to a count of four, hold the air in your lungs for a count of four.",
            "Practice multiple times a day.",
            "It can help reduce stress, anxiety, and even curb panic attacks."
        ),
        BreatheMethod(
            "4-7-8 Breathing", 4, 7, 8, 0,
            "A technique that involves breathing in for 4 seconds, holding the breath for 7 seconds, and exhaling for 8 seconds.",
            "Empty the lungs of air, breathe in quietly through the nose for 4 seconds, hold the breath for a count of 7 seconds, exhale forcefully through the mouth, making a “whoosh” sound for 8 seconds.",
            "Practice consistently one to two times a day.",
            "It may help reduce anxiety and help people get to sleep."
        ),
        BreatheMethod(
            "Pursed-Lip Breathing", 2, 0, 4, 0,
            "Particularly useful for people living with shortness of breath from chronic obstructive pulmonary disease (COPD), asthma, or other lung conditions.",
            "Sit or stand with your neck and shoulders relaxed. With your mouth closed, inhale through your nostrils for a count of two. Exhale slowly and steadily through your mouth for four seconds, puckering your lips as if blowing a kiss.",
            "Practice using this breath 4 to 5 times a day when you begin.",
            "It can help get the diaphragm working and increase the amount of oxygen entering the body."
        ),
        BreatheMethod(
            "Simple Deep Breathing", 5, 0, 5, 0,
            "The goal is to breathe deeply into your belly without forcing it to fill with air.",
            "Breathe in gently through your nose, counting from one to five. Exhale slowly through your nose, counting from one to five again.",
            "Practice multiple times a day.",
            "It can help reduce stress, anxiety, and even curb panic attacks."
        )
    )

    private var sessionStarted = false
    private var selectedMethod = methods[0]
    private var duration: Long = 120000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.method = selectedMethod

        scaleView(2f, .95f, 1000L, binding.progressBar)

        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)

        changeMethodInformation()
        binding.carouselView.apply {
            binding.carouselView.hideIndicator(true)
            size = methods.size
            setCarouselViewListener { view, position ->
                (view as TextView).text = methods[position].name
                view.alpha = 1f
            }
            carouselScrollListener = object : CarouselScrollListener {
                override fun onScrollStateChanged(
                    recyclerView: RecyclerView,
                    newState: Int,
                    position: Int
                ) {
                    changeMethodInformation()
                    selectedMethod = methods[position]
                    binding.method = selectedMethod
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {}
            }
            show()
            this.currentItem = 2
        }
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED)
                    binding.carouselView.visibility = View.GONE
                else if (!sessionStarted) binding.carouselView.visibility = View.VISIBLE

            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (sessionStarted) return
                val x = 1 - slideOffset
                binding.replaceWithName.methodName.alpha = slideOffset
                binding.carouselView.alpha = x
                if (slideOffset < .82) binding.fadeOut.alpha = slideOffset

            }
        }
        )
        binding.stateVisualizer.setOnClickListener {
            if (!sessionStarted) {
                sessionStarted = true
                startSession()
            }else{
                stopSession()
            }
        }
    }

    private lateinit var visualizerScope:CoroutineScope
    private fun startSession() {
        visualizerScope = CoroutineScope(Dispatchers.Main)
        binding.replaceWithName.methodName.alpha = 1f
        binding.carouselView.visibility = View.GONE

        RotateAnimation(100f,0f,.5f,.5f).apply {
            duration=150
        }
        ObjectAnimator.ofFloat(
            binding.progressBar,"rotation",-90f,0f
        ).apply {
            duration=450
            start()
        }

        scaleView(.95f, 1f, 250L, binding.progressBar)
        scaleView(1f, 0f, 150L, binding.duration)
        scaleView(1f, .6f, 320)

        // End Session Timer
        visualizerScope.launch {
            binding.progressBar.progressMax = duration.toFloat()
            while (true) {
                runOnUiThread {
                    binding.progressBar.progress += 100
                }
                delay(100)
            }
        }
        suspend fun holdCounter(time: Int) {
            binding.holdCounterHolder.alpha = 1f
            val counters = listOf(
                binding.holdTimerCounter1, binding.holdTimerCounter2, binding.holdTimerCounter3
            )
            for (counter in counters) {
                counter.alpha = 0f
            }
            for (counter in counters) {
                runOnUiThread {
                    ObjectAnimator.ofFloat(
                        counter, "alpha", 0f, 1f
                    ).apply {
                        duration = (time * 1000) / 3L
                        start()
                    }
                }
                delay(((time * 1000) / 3L)-450)
            }

            for (counter in counters) {
                runOnUiThread {
                    ObjectAnimator.ofFloat(
                        counter, "alpha", 1f, 0f
                    ).apply {
                        duration = 250
                        start()
                    }
                }
                delay(250)
            }

        }
        visualizerScope.launch {
            delay(320)
            while (true) {
                //inhale
                switchStateText(R.string.inahleStateText)
                scaleView(.6f, 1f, selectedMethod.inhaleSeconds * 1000L)
                delay(selectedMethod.inhaleSeconds * 1000L)
                //inhale hold
                if (selectedMethod.inhaleHoldSeconds != 0) {
                    switchStateText(R.string.holdStateText)
                    holdCounter(
                        selectedMethod.inhaleHoldSeconds
                    )
                }
                //exhale
                switchStateText(R.string.exhaleStateText)
                scaleView(1f, .6f, selectedMethod.exhaleSeconds * 1000L)
                ObjectAnimator.ofFloat(
                    binding.exhaleFinalPositionPlaceholder, "alpha", 0f, .4f
                ).apply {
                    repeatMode = ObjectAnimator.REVERSE
                    repeatCount = 1
                    duration = 1500L
                    start()
                }
                delay(selectedMethod.exhaleSeconds * 1000L)
                //exhale hold
                if (selectedMethod.exhaleHoldSeconds != 0) {
                    switchStateText(R.string.holdStateText)
                    holdCounter(
                        selectedMethod.exhaleHoldSeconds
                    )
                }
                if (binding.progressBar.progress >= binding.progressBar.progressMax) {
                    stopSession()
                }
            }
        }
    }

    private fun switchStateText(newTextResID:Int){
        ObjectAnimator.ofFloat(
            binding.mainDisplayText,"alpha",1f,0f
        ).apply {
            duration=300
            repeatCount=1
            repeatMode=ObjectAnimator.REVERSE
            doOnRepeat {
                binding.mainDisplayText.text=getString(newTextResID)
            }
            start()
        }
    }

    private fun stopSession() {
        try {
            visualizerScope.cancel()
        }catch (_:IllegalStateException){}

        ObjectAnimator.ofFloat(
            binding.progressBar,"rotation",0f,360f
        ).apply {
            duration=500
            start()
        }
        ObjectAnimator.ofFloat(
            binding.holdCounterHolder,"alpha",1f,0f
        ).apply {
            duration=200
            start()
        }
        binding.replaceWithName.methodName.alpha = 0f
        binding.carouselView.visibility = View.VISIBLE
        binding.progressBar.setProgressWithAnimation(0f,200)//progress=0f
        scaleView(1f, .95f, 190L, binding.progressBar)
        scaleView(0f, 1f, 300L, binding.duration)
        scaleView(.6f, 1f, 320)

        switchStateText(R.string.sessionStartTip)

        sessionStarted = false
    }

    private fun scaleView(
        startScale: Float,
        endScale: Float,
        duration: Long,
        view: View = binding.stateVisualizer
    ) {
        val anim: Animation = ScaleAnimation(
            startScale, endScale,
            startScale, endScale,
            Animation.RELATIVE_TO_SELF, .5f,
            Animation.RELATIVE_TO_SELF, .5f
        )
        anim.fillAfter = true
        anim.duration = duration
        view.startAnimation(anim)
    }

    private fun changeMethodInformation() {
        binding.replaceWithName.methodName.text = selectedMethod.name
        binding.inhaleSeconds.text = selectedMethod.inhaleSeconds.toString()
        binding.exhaleSeconds.text = selectedMethod.exhaleSeconds.toString()

        selectedMethod.inhaleHoldSeconds.apply {
            if (this == 0) {
                binding.inhaleHoldIcon.visibility = View.GONE
                binding.inhaleHoldSeconds.visibility = View.GONE
            } else {
                binding.inhaleHoldSeconds.text = this.toString()
                binding.inhaleHoldIcon.visibility = View.VISIBLE
                binding.inhaleHoldSeconds.visibility = View.VISIBLE
            }
        }
        selectedMethod.exhaleHoldSeconds.apply {
            if (this == 0) {
                binding.exhaleHoldIcon.visibility = View.GONE
                binding.exhaleHoldSeconds.visibility = View.GONE
            } else {
                binding.exhaleHoldIcon.visibility = View.VISIBLE
                binding.exhaleHoldSeconds.visibility = View.VISIBLE
                binding.exhaleHoldSeconds.text = this.toString()
            }
        }
    }
}