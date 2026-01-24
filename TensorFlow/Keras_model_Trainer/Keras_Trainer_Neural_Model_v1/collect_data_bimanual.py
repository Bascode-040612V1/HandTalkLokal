import cv2
import mediapipe as mp
import numpy as np
import pandas as pd
import os
import time
import math

def normalize_hand_landmarks(landmarks):
    """
    Normalize hand landmarks by:
    1. Translating: Subtract wrist (index 0) from all landmarks so wrist becomes (0,0,0)
    2. Scaling: Divide by distance from wrist to middle finger base (index 9) for size invariance
    """
    if not landmarks or len(landmarks) < 10:
        return [0.0] * 63  # Return zeros if not enough landmarks
    
    # Get wrist coordinates
    wrist_x = landmarks[0].x
    wrist_y = landmarks[0].y
    wrist_z = landmarks[0].z
    
    # Calculate scale factor (distance from wrist to middle finger base)
    middle_finger_base_x = landmarks[9].x
    middle_finger_base_y = landmarks[9].y
    scale_distance = math.sqrt(
        (middle_finger_base_x - wrist_x)**2 + 
        (middle_finger_base_y - wrist_y)**2
    )
    
    if scale_distance == 0:
        scale_distance = 1  # Prevent division by zero
    
    # Normalize each landmark
    normalized_landmarks = []
    for landmark in landmarks:
        norm_x = (landmark.x - wrist_x) / scale_distance
        norm_y = (landmark.y - wrist_y) / scale_distance
        norm_z = (landmark.z - wrist_z) / scale_distance if hasattr(landmark, 'z') else 0.0
        normalized_landmarks.extend([norm_x, norm_y, norm_z])
    
    return normalized_landmarks

def collect_gesture_data():
    # Initialize MediaPipe
    mp_hands = mp.solutions.hands
    mp_pose = mp.solutions.pose
    mp_drawing = mp.solutions.drawing_utils
    
    # Setup MediaPipe components
    hands = mp_hands.Hands(
        static_image_mode=False,
        max_num_hands=2,
        min_detection_confidence=0.7,
        min_tracking_confidence=0.7
    )
    
    pose = mp_pose.Pose(
        static_image_mode=False,
        min_detection_confidence=0.7,
        min_tracking_confidence=0.7
    )
    
    # Get gesture name from user
    gesture_name = input("Enter gesture name (e.g., 'Hello', 'Good', 'Neutral'): ").strip()
    if not gesture_name:
        print("Gesture name cannot be empty!")
        return
    
    # Initialize camera
    cap = cv2.VideoCapture(0)
    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
    
    print(f"Collecting data for gesture: '{gesture_name}'")
    print("Position your hands in the camera view...")
    print("Starting in 3 seconds...")
    time.sleep(3)
    
    collected_frames = 0
    total_frames = 500
    all_data = []
    
    print("Recording started... Perform the gesture continuously.")
    
    while collected_frames < total_frames:
        ret, frame = cap.read()
        if not ret:
            break
        
        frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        
        # Process hands
        hand_results = hands.process(frame_rgb)
        
        # Process pose
        pose_results = pose.process(frame_rgb)
        
        # Prepare feature vector (138 total features)
        features = []
        
        # Process hands (up to 2)
        if hand_results.multi_hand_landmarks:
            for i, hand_landmarks in enumerate(hand_results.multi_hand_landmarks[:2]):  # Max 2 hands
                normalized_landmarks = normalize_hand_landmarks(hand_landmarks.landmark)
                features.extend(normalized_landmarks)
            
            # If only one hand detected, add zeros for the second hand
            if len(hand_results.multi_hand_landmarks) == 1:
                features.extend([0.0] * 63)  # 63 features for missing hand
        else:
            # No hands detected - add zeros for both hands
            features.extend([0.0] * 126)  # 126 features for both hands
        
        # Process pose landmarks (elbows and wrists only: indices 13, 14, 15, 16)
        if pose_results.pose_landmarks:
            pose_landmarks = pose_results.pose_landmarks.landmark
            pose_indices = [13, 14, 15, 16]  # Left elbow, right elbow, left wrist, right wrist
            for idx in pose_indices:
                if idx < len(pose_landmarks):
                    landmark = pose_landmarks[idx]
                    features.extend([landmark.x, landmark.y, landmark.z if hasattr(landmark, 'z') else 0.0])
                else:
                    features.extend([0.0, 0.0, 0.0])  # Add zeros if landmark not available
        else:
            # No pose detected - add zeros for pose landmarks
            features.extend([0.0] * 12)  # 12 features for pose
        
        # Add label
        features.append(gesture_name)
        
        # Store the frame data
        all_data.append(features)
        collected_frames += 1
        
        # Draw landmarks on frame for visualization
        if hand_results.multi_hand_landmarks:
            for hand_landmarks in hand_results.multi_hand_landmarks:
                mp_drawing.draw_landmarks(
                    frame, hand_landmarks, mp_hands.HAND_CONNECTIONS
                )
        
        if pose_results.pose_landmarks:
            # Draw only the specific pose landmarks we care about
            for idx in [13, 14, 15, 16]:
                if idx < len(pose_results.pose_landmarks.landmark):
                    landmark = pose_results.pose_landmarks.landmark[idx]
                    h, w, c = frame.shape
                    cx, cy = int(landmark.x * w), int(landmark.y * h)
                    cv2.circle(frame, (cx, cy), 8, (0, 255, 0), cv2.FILLED)
        
        # Show frame with counter
        cv2.putText(frame, f'Frames: {collected_frames}/{total_frames}', (10, 30),
                   cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
        cv2.putText(frame, f'Gesture: {gesture_name}', (10, 70),
                   cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 0, 0), 2)
        cv2.imshow('Gesture Data Collection', frame)
        
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break
    
    cap.release()
    cv2.destroyAllWindows()
    
    # Create CSV header
    header = []
    
    # Hand landmarks (2 hands × 21 landmarks × 3 coordinates)
    for hand_idx in range(2):
        for landmark_idx in range(21):
            header.extend([
                f'hand{hand_idx}_x{landmark_idx}',
                f'hand{hand_idx}_y{landmark_idx}',
                f'hand{hand_idx}_z{landmark_idx}'
            ])
    
    # Pose landmarks (4 landmarks × 3 coordinates)
    pose_parts = ['left_elbow', 'right_elbow', 'left_wrist', 'right_wrist']
    for part in pose_parts:
        header.extend([f'{part}_x', f'{part}_y', f'{part}_z'])
    
    # Label
    header.append('label')
    
    # Create DataFrame
    df = pd.DataFrame(all_data, columns=header)
    
    # Define output path
    output_path = "sign_language_data.csv"
    
    # Write to CSV
    if os.path.exists(output_path):
        # Append to existing file without header
        df.to_csv(output_path, mode='a', header=False, index=False)
        print(f"Appended {len(all_data)} frames to existing {output_path}")
    else:
        # Create new file with header
        df.to_csv(output_path, mode='w', header=True, index=False)
        print(f"Created new {output_path} with {len(all_data)} frames")
    
    print(f"Data collection completed for gesture: '{gesture_name}'")
    print(f"Total features per frame: {len(header)-1}")

if __name__ == "__main__":
    collect_gesture_data()