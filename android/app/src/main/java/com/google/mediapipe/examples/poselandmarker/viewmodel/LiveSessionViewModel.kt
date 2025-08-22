package com.google.mediapipe.examples.poselandmarker.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LiveSessionViewModel : ViewModel() {

    private val _reps = MutableStateFlow(0)
    val reps: StateFlow<Int> = _reps

    private val _stage = MutableStateFlow("")
    val stage: StateFlow<String> = _stage

    private val _feedback = MutableStateFlow("")
    val feedback: StateFlow<String> = _feedback


    val timeInSeconds = MutableStateFlow(0)

    private val _exerciseName = MutableStateFlow("")
    val exerciseName: StateFlow<String> = _exerciseName


    fun updateExerciseStatus(currentReps: Int, currentStage: String, currentFeedback: String) {
        _reps.value = currentReps
        _stage.value = currentStage
        _feedback.value = currentFeedback
    }
    fun setExerciseName(name: String) {
        _exerciseName.value = name
    }
    fun reset() {
        _reps.value = 0
        _stage.value = ""
        _feedback.value = ""
        timeInSeconds.value = 0
   }


}
