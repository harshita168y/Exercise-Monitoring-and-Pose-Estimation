package com.google.mediapipe.examples.poselandmarker.tracker

import android.util.Log
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.*

class FrontRaiseTracker : ExerciseTracker {

    private enum class Stage { WAITING_UP, MOVING_DOWN }

    private var stage: Stage = Stage.WAITING_UP
    private var lastFeedbackTime = System.currentTimeMillis()
    private var lastVelocityFeedbackTime = System.currentTimeMillis()
    private var repCount = 0
    private var waitingToStart = true
    private var hasReachedUp = false

    // Speed detection
    private var lastWristY: Float? = null
    private var lastYTime: Long = 0
    private var prevLeftWristY = 0f
    private var prevRightWristY = 0f
    private var prevTimestamp = System.currentTimeMillis()

    private var lastSpeedFeedbackTime = 0L
    private val velocityCooldown = 3000L
    private val velocityThreshold = 0.07f  // Adjustable threshold based on log observations
    private var badFormDetected = false
    private var resetAfterBadForm = false


    private val feedbackCooldown = 3000L // 3 seconds

    override fun analyzePose(landmarks: List<NormalizedLandmark>): ExerciseResult {
        val currentTime = System.currentTimeMillis()
        var feedback = ""

        val shoulderLeft = landmarks[11]
        val elbowLeft = landmarks[13]
        val wristLeft = landmarks[15]

        val shoulderRight = landmarks[12]
        val elbowRight = landmarks[14]
        val wristRight = landmarks[16]

        // Vertical positioning
        val avgShoulderY = (shoulderLeft.y() + shoulderRight.y()) / 2f
        val avgWristY = (wristLeft.y() + wristRight.y()) / 2f

        // Elbow angles
        val leftElbowAngle = calculateAngle(shoulderLeft, elbowLeft, wristLeft)
        val rightElbowAngle = calculateAngle(shoulderRight, elbowRight, wristRight)
        val leftBent = leftElbowAngle < 150
        val rightBent = rightElbowAngle < 150

        Log.d("FrontRaiseTracker", "Stage=$stage, wristY=$avgWristY, shoulderY=$avgShoulderY")
        Log.d("FrontRaiseTracker", "LeftElbow=${leftElbowAngle.toInt()}°, RightElbow=${rightElbowAngle.toInt()}°")

        // Step 1: Speed feedback
        val timeDelta = (currentTime - prevTimestamp).coerceAtLeast(1)
        val leftSpeed = abs(wristLeft.y() - prevLeftWristY) / timeDelta * 1000
        val rightSpeed = abs(wristRight.y() - prevRightWristY) / timeDelta * 1000

        Log.d("FrontRaiseSpeedDebug", "Left speed = %.7f | Right speed = %.7f".format(leftSpeed, rightSpeed))

// Update previous values
        prevLeftWristY = wristLeft.y()
        prevRightWristY = wristRight.y()
        prevTimestamp = currentTime

        // Step 2: Initial start
        if (waitingToStart) {
            feedback = "Let's begin"
            waitingToStart = false
            lastFeedbackTime = currentTime
            return ExerciseResult(repCount, "Down", feedback)
        }

        // Step 3: Elbow check with staged feedback
        if ((leftBent || rightBent) && (currentTime - lastFeedbackTime > feedbackCooldown)) {
            feedback = "Don't bend your elbows"
            lastFeedbackTime = currentTime

            // Queue stage-related feedback after 1.2 sec delay
            return ExerciseResult(repCount, stage.name, feedback)
        }


        // Step 4: Rep tracking stages
        when (stage) {
            Stage.WAITING_UP -> {
                if (avgWristY < avgShoulderY - 0.05f) {
                    feedback = "Move your arms down"
                    stage = Stage.MOVING_DOWN
                    hasReachedUp = true
                    lastFeedbackTime = currentTime
                } else {
                    if (currentTime - lastFeedbackTime > feedbackCooldown && feedback.isEmpty()) {
                        feedback = "Move your arms up"
                        lastFeedbackTime = currentTime
                    }
                }
            }

            Stage.MOVING_DOWN -> {
                if (avgWristY > avgShoulderY + 0.08f) {
                    if (hasReachedUp) {
                        repCount++
                        feedback = "Rep $repCount completed"
                        stage = Stage.WAITING_UP
                        hasReachedUp = false
                        lastFeedbackTime = currentTime
                    }
                }
            }
        }

        return ExerciseResult(
            reps = repCount,
            stage = stage.name,
            feedback = feedback
        )
    }

    private fun calculateAngle(a: NormalizedLandmark, b: NormalizedLandmark, c: NormalizedLandmark): Double {
        val radians = atan2(c.y() - b.y(), c.x() - b.x()) -
                atan2(a.y() - b.y(), a.x() - b.x())
        var angle = abs(radians * 180.0 / Math.PI)
        if (angle > 180.0) angle = 360.0 - angle
        return angle
    }
}
