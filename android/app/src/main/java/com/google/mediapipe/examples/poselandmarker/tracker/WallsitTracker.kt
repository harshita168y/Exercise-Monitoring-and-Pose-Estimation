package com.google.mediapipe.examples.poselandmarker.tracker

import android.util.Log
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.atan2

class WallSitTracker : ExerciseTracker {

    private var startTime: Long = 0
    private var lastFeedbackTime: Long = 0
    private val feedbackInterval = 15_000L // 15 seconds
    private var trackingStarted = false
    private var holdTimeSeconds = 0
    private var elapsedSeconds = 0
    private var lastTimeUpdate = 0L
    private var timerStarted = false

    private val targetAngle = 90.0
    private val lowerThreshold = 80.0
    private val upperThreshold = 100.0
    private var warningCount = 0
    private var lastWarningTime = 0L
    private var lastWarningDisplayTime = 0L

    override fun analyzePose(landmarks: List<NormalizedLandmark>): ExerciseResult {
        val currentTime = System.currentTimeMillis()
        var feedback = ""

        val hipLeft = landmarks[23]
        val kneeLeft = landmarks[25]
        val ankleLeft = landmarks[27]
        val hipRight = landmarks[24]
        val kneeRight = landmarks[26]
        val ankleRight = landmarks[28]

        val leftAngle = calculateAngle(hipLeft, kneeLeft, ankleLeft)
        val rightAngle = calculateAngle(hipRight, kneeRight, ankleRight)
        val avgAngle = (leftAngle + rightAngle) / 2.0

        Log.d("WallSitTracker", "Avg Leg Angle: ${avgAngle.toInt()}°")

        // Don't start unless form is correct
        if (!timerStarted) {
            if (avgAngle in lowerThreshold..upperThreshold) {
                feedback = "Let's begin!"
                timerStarted = true
                trackingStarted = true
                startTime = currentTime
                lastFeedbackTime = currentTime
                warningCount = 0
            } else {
                feedback = "Adjust your position to start"
                return ExerciseResult(
                    reps = 0,
                    stage = "Waiting",
                    feedback = feedback
                )
            }
        }

        // During tracking
        val elapsed = currentTime - startTime
        holdTimeSeconds = (elapsed / 1000).toInt()

        val postureIncorrect = avgAngle < lowerThreshold || avgAngle > upperThreshold

        // Increment warnings once every 5 seconds only if posture is bad
        if (postureIncorrect && currentTime - lastWarningTime >= 5000) {
            warningCount++
            lastWarningTime = currentTime
        }

        // Show posture feedback
        if (postureIncorrect) {
            feedback = if (avgAngle < lowerThreshold) {
                "Sit a little higher"
            } else {
                "Sit a little lower"
            }
        }

        // Show warning count every 30 seconds
        if (currentTime - lastWarningDisplayTime >= 30_000) {
            feedback = "You've had $warningCount posture warnings"
            lastWarningDisplayTime = currentTime
        }

        // Timed encouragement (if no feedback is set)
        if (feedback.isEmpty() && currentTime - lastFeedbackTime >= feedbackInterval) {
            feedback = "Keep going! Time: ${formatTime(holdTimeSeconds)}"
            lastFeedbackTime = currentTime
        }

        return ExerciseResult(
            reps = holdTimeSeconds,
            stage = "Holding",
            feedback = feedback
        )
    }

    private fun calculateAngle(a: NormalizedLandmark, b: NormalizedLandmark, c: NormalizedLandmark): Double {
        val radians = atan2(
            (c.y().toDouble() - b.y().toDouble()),
            (c.x().toDouble() - b.x().toDouble())
        ) - atan2(
            (a.y().toDouble() - b.y().toDouble()),
            (a.x().toDouble() - b.x().toDouble())
        )
        var angle = abs(Math.toDegrees(radians))
        if (angle > 180.0) angle = 360.0 - angle
        return angle
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(minutes, secs)
    }
}
