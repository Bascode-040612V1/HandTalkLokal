import sys
# Fixed tensorflow disablement - proper way
import sys
sys.modules['tensorflow'] = type(sys)('tensorflow')

import mediapipe as mp
import cv2
import os
import numpy as np
import pandas as pd
import json
from datetime import datetime
import time  # Added for timer functionality

# Import logging configuration
from logging_config import setup_logging, get_logger

# Import modules needed for training
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report
import joblib

# Suppress protobuf deprecation warnings
import warnings
warnings.filterwarnings('ignore', category=UserWarning, module='google.protobuf.symbol_database')

# Set up logging
logger = setup_logging()
log_info = logger.info
log_error = logger.error
log_debug = logger.debug
log_warning = logger.warning

# Temporal Smoother - REMOVED for non-smooth tracking
# We're not using smoothing to allow for more responsive tracking
pass

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

def load_and_prepare_data(csv_path="data/gestures_bimanual.csv"):
    """Load and prepare the bimanual gesture data for training"""
    try:
        if not os.path.exists(csv_path):
            error_msg = f"Bimanual data file {csv_path} not found. Please collect bimanual gesture data first."
            log_error(error_msg)
            raise FileNotFoundError(error_msg)
        
        # Load the data
        df = pd.read_csv(csv_path)
        
        # Separate features and labels
        X = df.drop('label', axis=1).values  # Convert to numpy array
        y = df['label'].values  # Convert to numpy array
        
        log_info(f"Bimanual data loaded: {X.shape[0]} samples with {X.shape[1]} features each")
        print(f"Bimanual data loaded: {X.shape[0]} samples with {X.shape[1]} features each")
        # Fixed unique call by converting to Python list
        y_list = y.tolist() if hasattr(y, 'tolist') else list(y)
        unique_labels = list(set(y_list)) if len(y_list) > 0 else []
        log_debug(f"Labels: {unique_labels}")
        print(f"Labels: {unique_labels}")
        
        return X, y
    except Exception as e:
        log_error(f"Error loading data: {str(e)}")
        raise

def train_model_automatically():
    """Automatically train the model after data collection"""
    try:
        print("=== HandTalk Bimanual Sign Language Recognition Model Training ===")
        log_info("Starting automatic HandTalk Bimanual Sign Language Recognition Model Training")
        
        # Load data
        X, y = load_and_prepare_data()
        
        # Split the data
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
        log_info("Data split for training and testing")
        
        # Create and train the model
        model = RandomForestClassifier(n_estimators=100, random_state=42)
        log_info("Training Random Forest classifier...")
        print("Training Random Forest classifier...")
        model.fit(X_train, y_train)
        
        # Evaluate the model
        y_pred = model.predict(X_test)
        accuracy = accuracy_score(y_test, y_pred)
        
        log_info(f"Bimanual model trained with accuracy: {accuracy:.2f}")
        print(f"Bimanual model trained with accuracy: {accuracy:.2f}")
        log_debug("Generating classification report")
        print("\nClassification Report:")
        print(classification_report(y_test, y_pred))
        
        # Save model
        model_filename = "sign_language_model_bimanual.pkl"
        joblib.dump(model, model_filename)
        log_info(f"Bimanual model saved as {model_filename}")
        print(f"Bimanual model saved as {model_filename}")
        
        log_info("Automatic bimanual training completed successfully!")
        print("\n✅ Automatic bimanual training completed successfully!")
        print("Model is ready for real-time bimanual sign language recognition.")
        
    except Exception as e:
        error_msg = f"Error during automatic training: {str(e)}"
        log_error(error_msg)
        print(f"❌ {error_msg}")

def save_motion_sequence(gesture_name, motion_data):
    """Save hand motion sequence as JSON file"""
    try:
        # Create directory if it doesn't exist
        os.makedirs("data/arm_hand_sequences", exist_ok=True)
        
        # Generate filename with timestamp and sequence number
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        sequence_number = 1
        
        # Check for existing files with the same gesture name to determine sequence number
        import glob
        existing_files = glob.glob(f"data/arm_hand_sequences/gesture_{gesture_name}_seq_*_{timestamp[:8]}*.json")
        if existing_files:
            # Extract sequence numbers and find the highest
            seq_numbers = []
            for file in existing_files:
                try:
                    # Extract sequence number from filename
                    parts = os.path.basename(file).split('_')
                    if len(parts) >= 4:
                        seq_num = int(parts[3])
                        seq_numbers.append(seq_num)
                except:
                    pass
            if seq_numbers:
                sequence_number = max(seq_numbers) + 1
        
        # Format sequence number with leading zeros
        seq_num_formatted = f"{sequence_number:03d}"
        
        # Create filename
        filename = f"data/arm_hand_sequences/gesture_{gesture_name}_seq_{seq_num_formatted}_{timestamp}.json"
        
        # Prepare data for saving
        motion_json = {
            "metadata": {
                "gesture_name": gesture_name,
                "timestamp": timestamp,
                "sequence_number": sequence_number,
                "frame_count": len(motion_data),
                "features_per_frame": len(motion_data[0]) if motion_data else 0
            },
            "frames": motion_data,
            "feature_names": []
        }
        
        # Add feature names (same as CSV header)
        feature_names = []
        # Hand landmarks (both hands)
        for hand in range(2):  # Two hands
            for i in range(21):  # 21 landmarks per hand
                feature_names += [f"hand{hand}_x{i}", f"hand{hand}_y{i}", f"hand{hand}_z{i}"]
        
        # Pose landmarks (hands and arms only) - Updated to remove shoulders
        pose_parts = ["left_elbow", "right_elbow", "left_wrist", "right_wrist"]  # Removed shoulders
        for part in pose_parts:
            feature_names += [f"{part}_x", f"{part}_y", f"{part}_z"]
        
        # Combined label
        feature_names.append("label")
        motion_json["feature_names"] = feature_names
        
        # Save to JSON file
        with open(filename, 'w') as f:
            json.dump(motion_json, f, indent=2)
        
        log_info(f"Saved motion sequence to {filename}")
        print(f"💾 Saved motion sequence to {filename}")
        return filename
        
    except Exception as e:
        log_error(f"Error saving motion sequence: {str(e)}")
        print(f"❌ Error saving motion sequence: {str(e)}")
        return None

# --- SETTINGS ---
gesture_label = input("Enter gesture label for both hands: ")
COMBINED_GESTURE_NAME = gesture_label
SAMPLES = 20  # how many frames to capture per gesture
SAVE_PATH = "data/gestures_bimanual.csv"  # New file for bimanual data

# --- SETUP ---
# Fixed MediaPipe imports using direct module access
import mediapipe.python.solutions.hands as mp_hands
import mediapipe.python.solutions.pose as mp_pose
import mediapipe.python.solutions.drawing_utils as mp_drawing

# MediaPipe settings with high confidence thresholds
hands = mp_hands.Hands(
    static_image_mode=False,  # Set to False for video/live streaming
    max_num_hands=2,
    min_detection_confidence=0.9,  # High detection confidence
    min_tracking_confidence=0.9,   # High tracking confidence
    model_complexity=1  # Use complex model for better accuracy
)

# Pose settings with high confidence thresholds
pose = mp_pose.Pose(
    static_image_mode=False,
    min_detection_confidence=0.9,  # High detection confidence
    min_tracking_confidence=0.9,   # High tracking confidence
    enable_segmentation=False, 
    smooth_landmarks=True,
    model_complexity=1  # Use complex model
)

# Define which pose landmarks to focus on (hands and arms only)
# Including shoulders, elbows, wrists for full arm context (no hips)
ARM_LANDMARKS = [13, 14, 15, 16]  # Elbows, Wrists only (removed shoulders 11, 12)

# Initialize camera with standard settings (30 FPS)
cap = cv2.VideoCapture(0)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)   # Standard resolution
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)  # Standard resolution
cap.set(cv2.CAP_PROP_FPS, 30)            # Force 30 FPS for consistency

# Initialize temporal smoother for tracking stability
# REMOVED: smoother = TemporalSmoother(smoothing_factor=0.4)
# Not using smoothing for more responsive tracking

all_data = []
frame_skip = 1  # Process every frame (no skipping) for better tracking
frame_count = 0

print(f"\nRecording bimanual gesture:")
print(f"Gesture label: '{gesture_label}' (both hands)")
print(f"Samples to collect: {SAMPLES}")
print("Show your hands and arms when ready...")
print("Press 's' to start recording.")
print("NOTE: Make sure this gesture is one you want to train the model to recognize!")

# Log the start of the program
log_info(f"Starting data collection for bimanual gesture: '{gesture_label}' (both hands)")
logger.info(f"Starting data collection for bimanual gesture: '{gesture_label}' (both hands)")

recording = False
count = 0
countdown_active = False
countdown_start_time = 0

try:
    while True:
        ret, frame = cap.read()
        if not ret:
            logger.warning("Failed to read frame from camera")  # Using standard logging
            break
        
        frame_count += 1
        # Process every frame (no skipping) for better tracking
        if frame_count % frame_skip != 0:
            continue
            
        frame = cv2.flip(frame, 1)
        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        
        # Process hands
        hand_results = hands.process(rgb)
        
        # Process pose (wrists and elbows only - no face mapping)
        pose_results = pose.process(rgb)

        # Handle countdown timer
        if countdown_active:
            # Calculate elapsed time
            elapsed_time = time.time() - countdown_start_time
            remaining_time = max(0, 5 - int(elapsed_time))
            
            # Display countdown on screen
            draw_text_with_background(frame, f"Recording starts in {remaining_time} seconds", (10, 50),
                                    cv2.FONT_HERSHEY_SIMPLEX, 1.5, (0, 0, 255), 3, (0, 0, 0))
            draw_text_with_background(frame, "Get ready with your gesture", (10, 100),
                                    cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 255), 2)
            
            # Check if countdown is finished
            if elapsed_time >= 5:
                countdown_active = False
                recording = True
                count = 0
                all_data = []
                print("🎥 Recording started... perform the gesture now.")

        # Draw hand landmarks with distinction for left/right
        # Use getattr with default None to safely access attributes
        multi_hand_landmarks = getattr(hand_results, 'multi_hand_landmarks', None)
        multi_handedness = getattr(hand_results, 'multi_handedness', None)
        
        if multi_hand_landmarks:
            for idx, hand_landmarks in enumerate(multi_hand_landmarks):
                # Use MediaPipe's handedness detection instead of positional guessing
                color = (255, 0, 0)  # Default to blue (left hand)
                hand_side = "Left"   # Default to left hand
                
                # Get the handedness confidence if available
                if multi_handedness and idx < len(multi_handedness):
                    handedness = multi_handedness[idx]
                    classification = getattr(handedness, 'classification', [])
                    if classification and len(classification) > 0:
                        if classification[0].label == "Right":
                            color = (0, 255, 0)  # Green for right hand
                            hand_side = "Right"
                        else:  # "Left"
                            color = (255, 0, 0)  # Blue for left hand
                            hand_side = "Left"
                else:
                    # Fallback to positional method if handedness not available
                    h, w, c = frame.shape
                    landmarks = getattr(hand_landmarks, 'landmark', [])
                    if landmarks and len(landmarks) > 0:
                        wrist_x = landmarks[0].x * w
                        
                        # Left side of screen is right hand (due to flip), right side is left hand
                        if wrist_x < w/2:
                            color = (0, 255, 0)  # Green for right hand
                            hand_side = "Right"
                        else:
                            color = (255, 0, 0)  # Blue for left hand
                            hand_side = "Left"
                
                # Draw raw landmarks instead of smoothed ones
                h, w, c = frame.shape  # Initialize h,w,c before use
                landmarks = getattr(hand_landmarks, 'landmark', [])
                if landmarks:
                    for lm_idx, lm in enumerate(landmarks):
                        cx, cy = int(lm.x * w), int(lm.y * h)
                        cv2.circle(frame, (cx, cy), 4, color, cv2.FILLED)
                        
                        # Draw connections between landmarks
                        hand_connections = getattr(mp_hands, 'HAND_CONNECTIONS', [])
                        if hand_connections:
                            for connection in hand_connections:
                                start_idx, end_idx = connection
                                if start_idx < len(landmarks) and end_idx < len(landmarks):
                                    start_lm = landmarks[start_idx]
                                    end_lm = landmarks[end_idx]
                                    start_x, start_y = int(start_lm.x * w), int(start_lm.y * h)
                                    end_x, end_y = int(end_lm.x * w), int(end_lm.y * h)
                                    cv2.line(frame, (start_x, start_y), (end_x, end_y), color, 2)
                    
                    # Label the hand with background for better visibility
                    if len(landmarks) > 0:
                        cx, cy = int(landmarks[0].x * w), int(landmarks[0].y * h)
                        draw_text_with_background(frame, hand_side, (cx, cy-20), 
                                                cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 1, (0, 0, 0))

        # Draw pose landmarks for arm tracking visualization
        if pose_results:
            pose_landmarks = getattr(pose_results, 'pose_landmarks', None)
            if pose_landmarks:
                # Draw only the key upper body landmarks we're tracking
                h, w, c = frame.shape  # Initialize h,w,c before use
                landmarks = getattr(pose_landmarks, 'landmark', [])
                if landmarks:
                    for i, lm in enumerate(landmarks):
                        if i in ARM_LANDMARKS:  # Our key landmarks
                            cx, cy = int(lm.x * w), int(lm.y * h)
                            # Different colors for different body parts
                            if i in [13, 14]:  # Elbows
                                color = (0, 255, 255)  # Yellow
                            elif i in [15, 16]:  # Wrists
                                color = (255, 0, 255)  # Magenta
                            else:
                                color = (128, 128, 128)  # Default gray
                            cv2.circle(frame, (cx, cy), 6, color, cv2.FILLED)

        # Collect data when recording
        if recording:
            try:
                frame_data = []
                
                # Collect hand landmarks (both hands if present)
                multi_hand_landmarks = getattr(hand_results, 'multi_hand_landmarks', None)
                if multi_hand_landmarks:
                    for hand_landmarks in multi_hand_landmarks:
                        # Extract raw hand landmark coordinates (no smoothing)
                        landmarks = getattr(hand_landmarks, 'landmark', [])
                        for lm in landmarks:
                            frame_data += [lm.x, lm.y, lm.z]
                    # If only one hand is detected, we still need to pad the data
                    if len(multi_hand_landmarks) == 1:
                        # Add zero padding for the missing hand
                        frame_data += [0.0] * 63  # 21 landmarks * 3 coordinates each
                else:
                    # If no hands detected, add zero padding for both hands
                    frame_data += [0.0] * 126  # 2 hands * 21 landmarks * 3 coordinates each
                    
                # Collect pose landmarks (hands and arms only)
                if pose_results:
                    pose_landmarks = getattr(pose_results, 'pose_landmarks', None)
                    if pose_landmarks:
                        # Extract pose landmark coordinates (focus on upper body landmarks)
                        pose_coords = []
                        landmarks = getattr(pose_landmarks, 'landmark', [])
                        if landmarks:
                            for i, lm in enumerate(landmarks):
                                # Focus on hands and arms landmarks only
                                if i in ARM_LANDMARKS:
                                    pose_coords += [lm.x, lm.y, lm.z]
                        # Ensure we have exactly 12 pose coordinates (4 points * 3 coordinates)
                        if len(pose_coords) >= 12:
                            frame_data += pose_coords[:12]  # Take only first 12 (x,y,z for 4 points)
                        else:
                            # Add zero padding if we don't have all pose landmarks
                            frame_data += [0.0] * 12
                    else:
                        # Add zero padding if no pose detected
                        frame_data += [0.0] * 12  # 4 key pose landmarks * 3 coordinates each
                else:
                    # Add zero padding if no pose detected
                    frame_data += [0.0] * 12  # 4 key pose landmarks * 3 coordinates each
                    
                # Add combined gesture label
                frame_data.append(COMBINED_GESTURE_NAME)
                all_data.append(frame_data)


                count += 1
                # Recording indicator (without red overlay)
                draw_text_with_background(frame, "RECORDING", (10, 50),
                                        cv2.FONT_HERSHEY_SIMPLEX, 1.5, (0, 0, 255), 3, (0, 0, 0))
                cv2.putText(frame, f"{count}/{SAMPLES}", (10, 100),
                            cv2.FONT_HERSHEY_SIMPLEX, 1.5, (0, 0, 255), 3)
                cv2.putText(frame, f"Gesture: {gesture_label}", (10, 150),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 255), 2)

                if count >= SAMPLES:
                    recording = False
                    log_info(f"Successfully collected {SAMPLES} samples for gesture: '{gesture_label}'")
                    print(f"✅ Collected {SAMPLES} samples for gesture: '{gesture_label}'")
                    print("💡 Remember to retrain the model to recognize this new gesture!")
                    break
            except Exception as e:
                log_error(f"Error during data collection: {str(e)}")
                print(f"❌ Error during data collection: {str(e)}")
        else:
            # Display instructions
            draw_text_with_background(frame, f"Gesture: {COMBINED_GESTURE_NAME} (both hands)", (10, 30),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
            draw_text_with_background(frame, f"Samples: {count}/{SAMPLES}", (10, 60),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
            
            # Add positioning guide
            h, w = frame.shape[:2]
            center_x = w // 2
            # Draw vertical center line to help with hand positioning
            cv2.line(frame, (center_x, 0), (center_x, h), (128, 128, 128), 1)
            draw_text_with_background(frame, "Keep left hand on right side, right hand on left side", (10, h-20),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 1, (0, 0, 0))
            
            if not recording and not countdown_active:
                draw_text_with_background(frame, "Press 's' to START recording", (10, 90),
                                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 255), 2)
            elif countdown_active:
                # Countdown display is handled above
                pass
            else:
                draw_text_with_background(frame, "Recording... Keep hands steady!", (10, 90),
                                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 255), 2)

        cv2.imshow("HandTalk Enhanced - Bimanual Data Collector", frame)
        key = cv2.waitKey(1) & 0xFF

        if key == ord('s') and not recording and not countdown_active:  # start
            # Add 5-second countdown timer before recording
            print("Starting recording in 5 seconds...")
            countdown_active = True
            countdown_start_time = time.time()
        elif key == ord('q'):  # quit
            break

except Exception as e:
    log_error(f"Critical error in main loop: {str(e)}")
    print(f"❌ Critical error: {str(e)}")

cap.release()
cv2.destroyAllWindows()

# --- SAVE DATA ---
if all_data:
    try:
        df = pd.DataFrame(all_data)
        os.makedirs("data", exist_ok=True)
        
        # Create header for enhanced data
        header = []
        
        # Hand landmarks (both hands)
        for hand in range(2):  # Two hands
            for i in range(21):  # 21 landmarks per hand
                header += [f"hand{hand}_x{i}", f"hand{hand}_y{i}", f"hand{hand}_z{i}"]
        
        # Pose landmarks (hands and arms only)
        pose_parts = ["left_elbow", "right_elbow", "left_wrist", "right_wrist"]  # Removed shoulders
        for part in pose_parts:
            header += [f"{part}_x", f"{part}_y", f"{part}_z"]  # Fixed: Added missing _y
        
        # Combined label
        header.append("label")
        

        if not os.path.exists(SAVE_PATH):
            df.to_csv(SAVE_PATH, index=False, header=header)
        else:
            df.to_csv(SAVE_PATH, mode='a', index=False, header=False)
        
        log_info(f"Saved enhanced bimanual data to {SAVE_PATH}")
        print(f"💾 Saved enhanced bimanual data to {SAVE_PATH}")
        print(f"📊 Collected data for {len(all_data)} frames with {len(header)} features each")
        
        # Save motion sequence as JSON
        json_filename = save_motion_sequence(COMBINED_GESTURE_NAME, all_data)
        if json_filename:
            print(f"🎬 Saved motion sequence as JSON: {json_filename}")
        
        # Automatically train the model after data collection
        print("\n🤖 Automatically training the model with new data...")
        try:
            train_model_automatically()
            print("✅ Model trained and saved successfully!")
        except Exception as e:
            log_error(f"Error during automatic model training: {str(e)}")
            print(f"❌ Error during automatic model training: {str(e)}")
            print("⚠️  You may need to manually train the model using the batch file option.")
        
    except Exception as e:
        log_error(f"Error saving data: {str(e)}")
        print(f"❌ Error saving data: {str(e)}")
else:
    log_info("No data collected to save")  # Changed from warning to info
    print("⚠️ No data collected to save")
