package com.google.mediapipe.examples.poselandmarker.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import com.google.mediapipe.examples.poselandmarker.R
import com.google.mediapipe.examples.poselandmarker.fragment.CameraFragment
import com.google.mediapipe.examples.poselandmarker.viewmodel.LiveSessionViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@Composable
fun LiveSessionScreen(
    viewModel: LiveSessionViewModel,
    onFragmentContainerReady: () -> Unit,
    onEndSession: () -> Unit

) {

//    val exerciseName by viewModel.feedback.collectAsState()
    val exerciseName by viewModel.exerciseName.collectAsState()

    val reps by viewModel.reps.collectAsState()
    val stage by viewModel.stage.collectAsState()
    val feedback by viewModel.feedback.collectAsState()
    val rawTime by viewModel.timeInSeconds.collectAsState()


    val formattedTime = String.format("%02d:%02d", rawTime / 60, rawTime % 60)
//    LaunchedEffect(Unit) {
//        onFragmentContainerReady()
//    }


    Box(modifier = Modifier.fillMaxSize()) {

        val context = LocalContext.current
        val activity = context as AppCompatActivity

        // 🔹 Background image
        Image(
            painter = painterResource(id = R.drawable.bg_exercise_ui),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // 🔹 Exercise Title
            Text(
                text = exerciseName.replaceFirstChar { it.uppercase() },
                fontSize = 22.sp,
                color = Color.Black
            )
            AndroidView(
                factory = { context ->
                    FrameLayout(context).apply {
                        id = R.id.fragment_container_camera_compose

                        post {
                            onFragmentContainerReady()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp) //
            )


            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isWallSit = exerciseName.equals("wall sitting", ignoreCase = true)

                val timeFormatted = remember(reps) {
                    val minutes = reps / 60
                    val seconds = reps % 60
                    String.format("%02d:%02d", minutes, seconds)
                }

                if (isWallSit) {
                    Text("Time: $timeFormatted", fontSize = 18.sp, color = Color.Black)
                } else {
                    Text("Reps: $reps", fontSize = 18.sp, color = Color.Black)
                }

                Text("Stage: $stage", fontSize = 16.sp, color = Color.Black)
                Text("Feedback: $feedback", fontSize = 16.sp, color = Color.Black)

                Spacer(modifier = Modifier.weight(1f)) // Push button to bottom

                Button(
                    onClick = onEndSession,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("End Session")
                }
            }
        }
    }
}

