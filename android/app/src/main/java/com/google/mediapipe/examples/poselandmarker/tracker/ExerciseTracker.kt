package com.google.mediapipe.examples.poselandmarker.tracker

import com.google.mediapipe.tasks.components.containers.Landmark

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

interface ExerciseTracker {
    fun analyzePose(landmarks: List<NormalizedLandmark>): ExerciseResult
    fun requiresFullBody(): Boolean {
        return true
    }


}

