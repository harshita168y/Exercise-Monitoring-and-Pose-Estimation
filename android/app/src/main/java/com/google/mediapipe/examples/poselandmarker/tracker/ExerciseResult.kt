package com.google.mediapipe.examples.poselandmarker.tracker

data class ExerciseResult(
    val reps: Int,
    val stage: String,
    val feedback: String,
    val badLandmarkIndices: Set<Int> = emptySet()


)