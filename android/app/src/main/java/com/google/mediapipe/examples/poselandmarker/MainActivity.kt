///*
// * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *       http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */


package com.google.mediapipe.examples.poselandmarker

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.google.mediapipe.examples.poselandmarker.fragment.CameraFragment
import com.google.mediapipe.examples.poselandmarker.ui.SelectExerciseScreen
import com.google.mediapipe.examples.poselandmarker.ui.LiveSessionScreen
import com.google.mediapipe.examples.poselandmarker.viewmodel.LiveSessionViewModel

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExerciseApp()
        }
    }
}


@Composable
fun ExerciseApp() {
    val viewModel: LiveSessionViewModel = viewModel()

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "select") {
        composable("select") {
            SelectExerciseScreen(
                onExerciseSelected = { selectedExercise ->
                    navController.navigate("session/${selectedExercise}")

                }
            )
        }
        composable(
            "session/{exercise}",
            arguments = listOf(navArgument("exercise") { type = NavType.StringType })
        ) { backStackEntry ->
            val exerciseName = backStackEntry.arguments?.getString("exercise") ?: "Hammer Curl"

            val context = LocalContext.current
            val activity = context as AppCompatActivity // 🔹 Move it here

            LiveSessionScreen(

//                exerciseName = exerciseName,


                viewModel = viewModel,
                onEndSession = { navController.popBackStack() },

                onFragmentContainerReady = {
                    Log.d("FragmentCheck", "Injecting CameraFragment")

//                    Handler(Looper.getMainLooper()).post {
//                        val transaction = activity.supportFragmentManager.beginTransaction()
//                        transaction.replace(R.id.fragment_container_camera_compose, CameraFragment())
//                        transaction.commitAllowingStateLoss()
//                    }

                    Handler(Looper.getMainLooper()).postDelayed({
                        val fragment = CameraFragment()

                        viewModel.setExerciseName(exerciseName) // ✅ Set it here

                        val bundle = Bundle()
                        bundle.putString("exercise", exerciseName)
                        fragment.arguments = bundle

                        Log.d("ExerciseCheck", "Sending exercise: $exerciseName")
                        fragment.setViewModel(viewModel)

                        activity.supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container_camera_compose, fragment)
                            .commitAllowingStateLoss()
                    }, 300)

                }
            )

        }



    }

}

