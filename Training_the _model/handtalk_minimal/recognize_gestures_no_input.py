"""
Non-interactive version of the gesture recognition script for testing
"""
import sys
import mediapipe as mp
import cv2
import numpy as np
import pandas as pd
import joblib
import os
from datetime import datetime

# Import logging configuration
from logging_config import setup_logging, get_logger

# Import modules needed for training
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report
import joblib

# Import translation module
from translation_module import get_translator

# Suppress protobuf deprecation warnings
import warnings
warnings.filterwarnings('ignore', category=UserWarning, module='google.protobuf.symbol_database')

# Set up logging
logger = setup_logging()
log_info = logger.info
log_error = logger.error
log_debug = logger.debug
log_warning = logger.warning

def load_model(model_path="sign_language_model_bimanual.pkl"):
    """Load the trained bimanual model"""
    try:
        if not os.path.exists(model_path):
            error_msg = f"Bimanual model file {model_path} not found. Please train a bimanual model first."
            log_error(error_msg)
            raise FileNotFoundError(error_msg)
        
        model = joblib.load(model_path)
        log_info(f"Bimanual model loaded from {model_path}")
        print(f"Bimanual model loaded from {model_path}")
        
        return model
    except Exception as e:
        log_error(f"Error loading model: {str(e)}")
        raise

def extract_features(hand_results, pose_results):
    """Extract features from MediaPipe results in the same format as training data"""
    frame_data = []
    
    # Extract hand landmarks (both hands if present)
    if hasattr(hand_results, 'multi_hand_landmarks') and hand_results.multi_hand_landmarks:
        for hand_landmarks in hand_results.multi_hand_landmarks:
            # Extract hand landmark coordinates
            for lm in hand_landmarks.landmark:
                frame_data += [lm.x, lm.y, lm.z]
        # If only one hand is detected, we still need to pad the data
        if len(hand_results.multi_hand_landmarks) == 1:
            # Add zero padding for the missing hand
            frame_data += [0.0] * 63  # 21 landmarks * 3 coordinates each
    else:
        # If no hands detected, add zero padding for both hands
        frame_data += [0.0] * 126  # 2 hands * 21 landmarks * 3 coordinates each
        
    # Extract pose landmarks (hands and arms only)
    if hasattr(pose_results, 'pose_landmarks') and pose_results.pose_landmarks:
        # Extract pose landmark coordinates (focus on upper body landmarks)
        # Pose landmarks: 0-32 (33 total), focusing on:
        # Elbows: 13, 14
        # Wrists: 15, 16
        pose_coords = []
        for i, lm in enumerate(pose_results.pose_landmarks.landmark):
            # Focus on hands and arms landmarks only
            if i in [13, 14, 15, 16]:
                pose_coords += [lm.x, lm.y, lm.z]
        # Ensure we have exactly 12 pose coordinates (4 points * 3 coordinates x,y,z)
        if len(pose_coords) >= 12:
            frame_data += pose_coords[:12]
        else:
            # Add zero padding if we don't have all pose landmarks
            frame_data += [0.0] * 12
    else:
        # Add zero padding if no pose detected
        frame_data += [0.0] * 12
    
    # Ensure we have exactly 138 features (126 hand + 12 pose)
    if len(frame_data) > 138:
        frame_data = frame_data[:138]
    elif len(frame_data) < 138:
        frame_data += [0.0] * (138 - len(frame_data))
    
    # Final validation
    if len(frame_data) != 138:
        log_warning(f"Feature count mismatch: expected 138, got {len(frame_data)}")
        
    return np.array(frame_data).reshape(1, -1)

def draw_text_with_background(frame, text, position, font, font_scale, text_color, thickness, bg_color=(128, 128, 128), padding=5):
    """Draw text with a background rectangle for better visibility"""
    # Get text size
    text_size = cv2.getTextSize(text, font, font_scale, thickness)[0]
    
    # Calculate background rectangle coordinates
    x, y = position
    bg_coords = (
        (x - padding, y - text_size[1] - padding),
        (x + text_size[0] + padding, y + padding)
    )
    
    # Draw background rectangle
    cv2.rectangle(frame, bg_coords[0], bg_coords[1], bg_color, -1)
    
    # Draw text
    cv2.putText(frame, text, position, font, font_scale, text_color, thickness)

def main():
    """Main function for real-time gesture recognition"""
    print("=== HandTalk Real-time Gesture Recognition ===")
    print()
    
    # Initialize translator with default dialect
    translator = get_translator()
    translator.set_dialect("spanish")  # Set default dialect for testing
    
    print(f"Using dialect: {translator.current_dialect.capitalize()}")
    
    # Load the trained model
    try:
        model = load_model()
        # Print model information for debugging
        print(f"Model classes: {model.classes_}")
        print(f"Number of classes: {len(model.classes_)}")
    except Exception as e:
        print(f"❌ Failed to load model: {str(e)}")
        print("   Please make sure you have trained a model first.")
        return
    
    # Setup MediaPipe
    import mediapipe.python.solutions.hands as mp_hands
    import mediapipe.python.solutions.pose as mp_pose
    import mediapipe.python.solutions.drawing_utils as mp_drawing
    
    hands = mp_hands.Hands(
        static_image_mode=False,
        max_num_hands=2,
        min_detection_confidence=0.7,  # Reduced for better detection
        min_tracking_confidence=0.7,   # Reduced for better tracking
        model_complexity=1  # Use complex model for better accuracy
    )
    
    pose = mp_pose.Pose(
        static_image_mode=False,
        min_detection_confidence=0.7,  # Reduced for better detection
        min_tracking_confidence=0.7,   # Reduced for better tracking
        enable_segmentation=False,
        smooth_landmarks=True,
        model_complexity=1  # Use complex model
    )
    
    # Setup camera
    cap = cv2.VideoCapture(0)
    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
    cap.set(cv2.CAP_PROP_FPS, 30)            # Force 30 FPS for consistency
    
    print("Starting real-time gesture recognition...")
    print("Show your hands to the camera to begin recognizing gestures.")
    print(f"Current dialect: {translator.current_dialect.capitalize()}")
    print("Press 'q' to quit.")
    print()
    
    min_confidence = 0.3  # Lowered confidence threshold to improve recognition
    
    try:
        while True:
            ret, frame = cap.read()
            if not ret:
                print("Failed to read frame from camera")
                break
            
            frame = cv2.flip(frame, 1)
            rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            
            # Process hands and pose
            hand_results = hands.process(rgb)
            pose_results = pose.process(rgb)
            
            # Recognize gesture
            multi_hand_landmarks = getattr(hand_results, 'multi_hand_landmarks', None)
            if multi_hand_landmarks:
                try:
                    # Extract features
                    features = extract_features(hand_results, pose_results)
                    
                    # Make prediction
                    prediction_proba = model.predict_proba(features)[0]
                    max_proba = np.max(prediction_proba)
                    
                    # Define confidence thresholds
                    high_confidence_threshold = 0.7
                    medium_confidence_threshold = 0.4
                    
                    if max_proba >= high_confidence_threshold:
                        # High confidence - display the gesture
                        predicted_class = model.classes_[np.argmax(prediction_proba)]
                        
                        # Display prediction
                        display_text = f"Gesture: {predicted_class}"
                        translated_text = translator.translate(predicted_class)
                        confidence_text = f"Confidence: {max_proba:.2f} (High)"
                        
                        # Draw predictions on frame
                        draw_text_with_background(frame, display_text, (10, 30),
                                                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)  # Green
                        draw_text_with_background(frame, f"Translation: {translated_text}", (10, 60),
                                                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 255), 2)
                        draw_text_with_background(frame, confidence_text, (10, 90),
                                                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)  # Green
                    elif max_proba >= medium_confidence_threshold:
                        # Medium confidence - show as unrecognized
                        confidence_text = f"Confidence: {max_proba:.2f} (Medium)"
                        display_text = "Gesture: Unrecognized (Medium Confidence)"
                        
                        # Draw medium confidence message
                        draw_text_with_background(frame, display_text, (10, 30),
                                                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 255), 2)  # Yellow
                        draw_text_with_background(frame, confidence_text, (10, 60),
                                                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 255), 2)  # Yellow
                    else:
                        # Low confidence - show as unrecognized
                        confidence_text = f"Confidence: {max_proba:.2f} (Low)"
                        display_text = "Gesture: Unrecognized (Low Confidence)"
                        
                        # Draw low confidence message
                        draw_text_with_background(frame, display_text, (10, 30),
                                                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 255), 2)  # Red
                        draw_text_with_background(frame, confidence_text, (10, 60),
                                                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 255), 2)  # Red
                except Exception as e:
                    log_error(f"Error during prediction: {str(e)}")
                    draw_text_with_background(frame, "Prediction error", (10, 30),
                                            cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 255), 2)
            
            # Display instructions
            draw_text_with_background(frame, "Real-time Gesture Recognition", (10, frame.shape[0] - 30),
                                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 1)
            draw_text_with_background(frame, "Press 'q' to quit", (10, frame.shape[0] - 10),
                                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 1)
            
            # Show frame
            cv2.imshow("HandTalk - Real-time Gesture Recognition", frame)
            
            # Exit on 'q' key press
            if cv2.waitKey(1) & 0xFF == ord('q'):
                break
                
    except KeyboardInterrupt:
        print("\nRecognition interrupted by user")
    except Exception as e:
        log_error(f"Error in main recognition loop: {str(e)}")
        print(f"Error in recognition: {str(e)}")
    finally:
        # Cleanup
        cap.release()
        cv2.destroyAllWindows()
        hands.close()
        pose.close()

if __name__ == "__main__":
    main()