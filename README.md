# Exercise-Monitoring-and-Pose-Estimation
An Android app for real-time exercise monitoring using MediaPipe BlazePose. Tracks Hammer Curl, Wall Sit, and Front Arm Raise with rep counting, stage detection, and instant audio feedback to improve form and prevent injuries.

The main goal is to make exercise tracking more accurate, convenient, and engaging without depending on expensive wearables or trainers, especially for home workouts.

> Features

Real-time posture recognition powered by BlazePose

Rep counting with stage detection (up/down phases)

Voice-based real-time corrective feedback

Biomechanical validation using joint angles and rules

Optimized for responsiveness on Android devices

> Exercises Implemented

Hammer Curl – reliable tracking with posture deviation alerts

Wall Sit – consistent detection with stability checks

Front Arm Raise – functional, though rapid movements can reduce accuracy

> Evaluation

Hammer Curl and Wall Sit tracking showed high reliability and consistency

Front Arm Raise worked well for moderate speed, but had minor limitations with fast movements

BlazePose ran efficiently on mobile without noticeable latency, even though initial concerns were about performance and buffering

Some limitations observed in dim lighting conditions, but overall robust for standard workout settings

> Contribution & Insight

This project shows how pose estimation and biomechanics can be effectively combined for accurate rep detection and improved user engagement. Careful threshold selection proved key to maximizing both accuracy and responsiveness.
