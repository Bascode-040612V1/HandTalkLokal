"""
Script to convert JSON gesture sequences to CSV format for training
"""

import os
import json
import pandas as pd
from pathlib import Path


def load_json_gesture_sequence(json_file_path):
    """
    Load a single JSON gesture sequence file and extract features
    """
    with open(json_file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    frames = data.get('frames', [])
    gesture_name = data.get('metadata', {}).get('gesture_name', '')
    
    features_list = []
    
    for frame in frames:
        # Each frame is an array with 139 elements (138 features + 1 label)
        if isinstance(frame, list) and len(frame) >= 139:
            # Extract the first 138 features and add the gesture name
            features = frame[:138]  # 138 features
            label = frame[138]      # The label is the 139th element
            features_list.append(features + [label])
        else:
            print(f"Warning: Frame format unexpected in {json_file_path}")
    
    return features_list


def convert_json_sequences_to_csv():
    """
    Convert all JSON gesture sequences to a single CSV file
    """
    json_dir = Path("data/arm_hand_sequences")
    output_csv = "data/gestures_bimanual.csv"
    
    if not json_dir.exists():
        print(f"Directory {json_dir} does not exist!")
        return
    
    all_features = []
    gesture_files = list(json_dir.glob("gesture_*.json"))
    
    print(f"Found {len(gesture_files)} gesture files to process...")
    
    for json_file in gesture_files:
        print(f"Processing {json_file.name}...")
        try:
            features = load_json_gesture_sequence(json_file)
            all_features.extend(features)
            print(f"  Added {len(features)} frames from {json_file.name}")
        except Exception as e:
            print(f"  Error processing {json_file.name}: {str(e)}")
    
    if not all_features:
        print("No features were extracted from the JSON files!")
        return
    
    print(f"Total frames processed: {len(all_features)}")
    
    # Create DataFrame with proper column names
    feature_columns = []
    
    # Hand features (126 features)
    for hand_idx in range(2):  # 2 hands
        for landmark_idx in range(21):  # 21 landmarks per hand
            for coord in ['x', 'y', 'z']:  # 3 coordinates
                feature_columns.append(f'hand{hand_idx}_landmark{landmark_idx}_{coord}')
    
    # Pose features (12 features)
    pose_parts = ['left_elbow', 'right_elbow', 'left_wrist', 'right_wrist']
    for part in pose_parts:
        for coord in ['x', 'y', 'z']:
            feature_columns.append(f'{part}_{coord}')
    
    # Add label column
    feature_columns.append('label')
    
    # Create the dataframe - ensure we have the right number of columns
    df = pd.DataFrame(all_features)
    df.columns = feature_columns  # Assign column names separately
    
    # Save to CSV
    df.to_csv(output_csv, index=False)
    print(f"CSV file saved as {output_csv}")
    print(f"Dataset shape: {df.shape}")
    
    # Print summary
    print("\nGesture distribution:")
    gesture_counts = df['label'].value_counts()
    for gesture, count in gesture_counts.items():
        print(f"  {gesture}: {count} samples")


def create_organized_directories():
    """
    Create organized output directories if they don't exist
    """
    directories = [
        'output/tflite_model',
        'output/labels',
        'output/model_assets',
        'output/trained_models',
        'output/training_logs'
    ]
    
    for directory in directories:
        os.makedirs(directory, exist_ok=True)
        print(f"Ensured directory exists: {directory}")


if __name__ == "__main__":
    print("=== Converting JSON sequences to CSV format ===")
    
    # Create organized directories
    create_organized_directories()
    
    # Convert JSON sequences to CSV
    convert_json_sequences_to_csv()
    
    print("\n=== Conversion completed! ===")
    print("You can now run run_complete_pipeline.bat to train the model and convert to TFLite.")