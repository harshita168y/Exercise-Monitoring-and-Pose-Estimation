package com.google.mediapipe.examples.poselandmarker.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import com.google.mediapipe.examples.poselandmarker.R

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun SelectExerciseScreen(
    onExerciseSelected : (String) -> Unit
) {
    val exerciseOptions = listOf("Hammer Curl", "Front Raise","Wall Sitting")
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf(exerciseOptions[0]) }
    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = Color(0xFFEFE1FF)), // light purple background
        contentAlignment = Alignment.Center

    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.bg_exercise_ui),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Exercise Monitoring App",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(text = "Select an Exercise", fontSize = 20.sp, color = Color.Black)

            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = "Choose Exercise",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(40.dp))


            Box {
                TextField(
                    value = selectedOption,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .width(250.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .width(250.dp)
                        .background(Color.White)
                ) {
                    exerciseOptions.forEach { option ->
                        DropdownMenuItem(
                            onClick = {
                                selectedOption = option
                                expanded = false
                            },
                            text = {
                                Text(
                                    text = option,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color.Black
                                )
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { expanded = true }
                )
            }

            Spacer(modifier = Modifier.height(50.dp))

            // Start Session Button
            Button(
                onClick = { onExerciseSelected(selectedOption) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB266FF)),
                shape = RoundedCornerShape(25.dp),
                modifier = Modifier
                    .width(200.dp)
                    .height(50.dp)
            ) {
                Text("Start Session", color = Color.White)
            }
        }
    }
}