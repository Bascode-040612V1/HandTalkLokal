#!/usr/bin/env python3
"""
Script to view saved gestures in the data file
"""

import os
import pandas as pd

# Import logging configuration
from logging_config import setup_logging, get_logger

# Set up logging
logger = setup_logging()
log_info = logger.info
log_error = logger.error
log_debug = logger.debug

def delete_gesture_by_name(csv_path, gesture_name):
    """Delete all samples for a specific gesture name"""
    try:
        # Read the CSV file
        df = pd.read_csv(csv_path)
        
        # Count samples before deletion
        before_count = len(df)
        gesture_count = len(df[df['label'] == gesture_name])
        
        if gesture_count == 0:
            print(f"No samples found for gesture '{gesture_name}'")
            log_info(f"No samples found for gesture '{gesture_name}' during deletion")
            return False
        
        # Filter out rows with the specified gesture name
        df_filtered = df[df['label'] != gesture_name]
        
        # Count samples after deletion
        after_count = len(df_filtered)
        
        # Save the filtered data back to the CSV file
        df_filtered.to_csv(csv_path, index=False)
        
        print(f"Deleted {gesture_count} samples for gesture '{gesture_name}'")
        print(f"Total samples: {before_count} -> {after_count}")
        log_info(f"Deleted {gesture_count} samples for gesture '{gesture_name}'. Total samples: {before_count} -> {after_count}")
        return True
        
    except Exception as e:
        error_msg = f"Error deleting gesture data: {str(e)}"
        print(error_msg)
        log_error(error_msg)
        return False

def view_saved_gestures_interactive():
    """Display the saved gestures with interactive options"""
    csv_path = "data/gestures_bimanual.csv"
    
    print("=== HANDTALK SAVED GESTURES (INTERACTIVE VIEW) ===")
    print()
    log_info("Starting interactive gesture viewer")
    
    # Check if the file exists
    if not os.path.exists(csv_path):
        error_msg = "❌ No gesture data file found! Please collect some gesture data first."
        print(error_msg)
        log_error("No gesture data file found at %s", csv_path)
        return
    
    try:
        # Load the data
        print(f"Loading gesture data from {csv_path}...")
        log_info("Loading gesture data from %s", csv_path)
        df = pd.read_csv(csv_path)
        
        # Get unique gesture labels
        unique_labels = df['label'].unique()
        
        print()
        success_msg = f"✅ Found {len(df)} gesture samples with {len(unique_labels)} unique gestures"
        print(success_msg)
        log_info(success_msg)
        print()
        print("GESTURE LABELS:")
        print("=" * 50)
        
        # Display each unique gesture label with sample counts
        gesture_list = []
        for i, label in enumerate(unique_labels, 1):
            # Count how many samples we have for this gesture
            count = len(df[df['label'] == label])
            gesture_list.append(label)
            print(f"{i:2d}. {label} ({count} samples)")
        
        print()
        print("OPTIONS:")
        print("=" * 50)
        print("d. Delete a specific gesture")
        print("r. Refresh view")
        print("q. Return to main menu")
        print()
        
        # Interactive loop
        while True:
            choice = input("Enter your choice (number to view details, d/r/q): ").strip().lower()
            
            if choice == 'q':
                log_info("User exited interactive gesture viewer")
                break
            elif choice == 'r':
                # Refresh the view
                log_info("User requested view refresh")
                return view_saved_gestures_interactive()
            elif choice == 'd':
                # Delete a gesture
                try:
                    gesture_choice = input("Enter the number of the gesture to delete: ").strip()
                    gesture_index = int(gesture_choice) - 1
                    
                    if 0 <= gesture_index < len(gesture_list):
                        gesture_name = gesture_list[gesture_index]
                        confirm = input(f"Are you sure you want to delete ALL samples for '{gesture_name}'? (y/N): ").strip().lower()
                        
                        if confirm == 'y':
                            log_info("User confirmed deletion of gesture: %s", gesture_name)
                            if delete_gesture_by_name(csv_path, gesture_name):
                                print("Deletion completed successfully!")
                                print("Refreshing view...")
                                log_info("Deletion successful, refreshing view")
                                return view_saved_gestures_interactive()
                            else:
                                error_msg = "Deletion failed!"
                                print(error_msg)
                                log_error(error_msg)
                        else:
                            print("Deletion cancelled.")
                            log_info("User cancelled deletion of gesture: %s", gesture_name)
                    else:
                        error_msg = "Invalid gesture number!"
                        print(error_msg)
                        log_error(error_msg)
                except ValueError:
                    error_msg = "Invalid input! Please enter a number."
                    print(error_msg)
                    log_error(error_msg)
            else:
                # Try to interpret as a gesture number
                try:
                    gesture_index = int(choice) - 1
                    if 0 <= gesture_index < len(gesture_list):
                        gesture_name = gesture_list[gesture_index]
                        # Show details for this gesture
                        gesture_samples = df[df['label'] == gesture_name]
                        detail_msg = f"\nDetails for '{gesture_name}':\n- Total samples: {len(gesture_samples)}\n- First sample index: {gesture_samples.index[0]}\n- Last sample index: {gesture_samples.index[-1]}"
                        print(detail_msg)
                        log_debug(detail_msg)
                    else:
                        error_msg = "Invalid gesture number!"
                        print(error_msg)
                        log_error(error_msg)
                except ValueError:
                    error_msg = "Invalid choice! Please enter a valid option."
                    print(error_msg)
                    log_error(error_msg)
        
    except Exception as e:
        error_msg = f"❌ Error reading gesture data: {str(e)} Please make sure the data file is properly formatted."
        print(error_msg)
        log_error(error_msg)

def view_saved_gestures_basic():
    """Display the saved gestures using basic file operations"""
    csv_path = "data/gestures_bimanual.csv"
    
    print("=== HANDTALK SAVED GESTURES (BASIC VIEW) ===")
    print()
    log_info("Starting basic gesture viewer")
    
    # Check if the file exists
    if not os.path.exists(csv_path):
        error_msg = "❌ No gesture data file found! Please collect some gesture data first."
        print(error_msg)
        log_error("No gesture data file found at %s", csv_path)
        return
    
    try:
        # Read the file line by line
        with open(csv_path, 'r', encoding='utf-8') as file:
            lines = file.readlines()
        
        if not lines:
            error_msg = "❌ Gesture data file is empty!"
            print(error_msg)
            log_error(error_msg)
            return
            
        # Get the header (first line)
        header = lines[0].strip().split(',')
        expected_fields = len(header)
        label_column_index = -1  # Label is typically the last column
        
        # Find the label column index
        for i, col in enumerate(header):
            if col.strip().lower() == 'label':
                label_column_index = i
                break
        
        if label_column_index == -1:
            label_column_index = -1  # Default to last column
        
        loading_msg = f"Loading gesture data from {csv_path}... Total lines in file: {len(lines)}. Expected fields per line: {expected_fields}."
        print(f"Loading gesture data from {csv_path}...")
        print(f"Total lines in file: {len(lines)}")
        print(f"Expected fields per line: {expected_fields}")
        log_info(loading_msg)
        print()
        
        # Extract unique labels from all rows (skip header)
        labels = []
        valid_rows = 0
        malformed_rows = 0
        
        for i, line in enumerate(lines[1:], 1):  # Skip header
            columns = line.strip().split(',')
            if len(columns) == expected_fields:
                valid_rows += 1
                if len(columns) > abs(label_column_index):
                    label = columns[label_column_index].strip()
                    if label and label not in labels:
                        labels.append(label)
            else:
                malformed_rows += 1
        
        found_msg = f"✅ Found {valid_rows} valid gesture samples with {len(labels)} unique gestures"
        print(found_msg)
        if malformed_rows > 0:
            warning_msg = f"⚠️  Skipped {malformed_rows} malformed rows"
            print(warning_msg)
            logger.warning(warning_msg)
        log_info(found_msg)
        print()
        print("GESTURE LABELS:")
        print("=" * 50)
        
        # Display each unique gesture label
        for i, label in enumerate(labels, 1):
            print(f"{i:2d}. {label}")
            
        print()
        print("FEATURE INFORMATION:")
        print("=" * 50)
        feature_msg = f"Columns per row: {len(header)}. Label column: '{header[label_column_index]}'."
        print(f"Columns per row: {len(header)}")
        print(f"Label column: '{header[label_column_index]}'")
        # Updated to reflect 12 pose landmarks instead of 18
        print("Hand landmarks: 126 features (2 hands × 21 landmarks × 3 coordinates)")
        print("Pose landmarks: 12 features (4 points × 3 coordinates)")  # Updated from 18 to 12
        print("Label: 1 feature")
        log_debug(feature_msg)
        
    except Exception as e:
        error_msg = f"❌ Error reading gesture data: {str(e)} Please make sure the data file is properly formatted."
        print(error_msg)
        log_error(error_msg)

def view_saved_gestures_advanced():
    """Display the saved gestures using pandas (advanced view)"""
    csv_path = "data/gestures_bimanual.csv"
    
    print("=== HANDTALK SAVED GESTURES (ADVANCED VIEW) ===")
    print()
    log_info("Starting advanced gesture viewer")
    
    # Check if the file exists
    if not os.path.exists(csv_path):
        error_msg = "❌ No gesture data file found! Please collect some gesture data first."
        print(error_msg)
        log_error("No gesture data file found at %s", csv_path)
        return
    
    try:
        # Load the data
        print(f"Loading gesture data from {csv_path}...")
        log_info("Loading gesture data from %s", csv_path)
        
        # Read CSV with error handling
        try:
            df = pd.read_csv(csv_path)
        except Exception as e:
            error_msg = f"❌ Error parsing CSV file: {str(e)} Attempting to fix the file..."
            print(error_msg)
            logger.error(error_msg)
            # Try to read with error correction
            try:
                df = pd.read_csv(csv_path, on_bad_lines='skip')
            except:
                # Fallback to basic read
                df = pd.read_csv(csv_path)
        
        # Get unique gesture labels
        unique_labels = df['label'].unique()
        
        print()
        success_msg = f"✅ Found {len(df)} gesture samples with {len(unique_labels)} unique gestures"
        print(success_msg)
        log_info(success_msg)
        print()
        print("GESTURE LABELS:")
        print("=" * 50)
        
        # Display each unique gesture label with sample counts
        for i, label in enumerate(unique_labels, 1):
            # Count how many samples we have for this gesture
            count = len(df[df['label'] == label])
            print(f"{i:2d}. {label} ({count} samples)")
        
        print()
        print("FEATURE INFORMATION:")
        print("=" * 50)
        feature_info = f"Total features per sample: {len(df.columns) - 1}. Hand landmarks: 126 features (2 hands × 21 landmarks × 3 coordinates). Pose landmarks: 12 features (4 points × 3 coordinates). Label: 1 feature."
        print(f"Total features per sample: {len(df.columns) - 1}")  # -1 for the label column
        print(f"Hand landmarks: 126 features (2 hands × 21 landmarks × 3 coordinates)")
        print(f"Pose landmarks: 12 features (4 points × 3 coordinates)")  # Updated from 18 to 12
        print(f"Label: 1 feature")
        log_debug(feature_info)
        
        print()
        print("Interactive options:")
        print("- Run this script directly to access interactive features")
        print("- From main menu: python view_gestures.py")
        
    except ImportError:
        error_msg = "❌ Pandas not available, switching to basic view..."
        print(error_msg)
        logger.warning(error_msg)
        print()
        view_saved_gestures_basic()
    except Exception as e:
        error_msg = f"❌ Error reading gesture data: {str(e)} Please make sure the data file is properly formatted. You can try running the fix_csv.py script to repair the file."
        print(error_msg)
        log_error(error_msg)

def view_saved_gestures():
    """Main function to view saved gestures"""
    try:
        import pandas as pd
        log_info("Using advanced gesture viewer (pandas available)")
        view_saved_gestures_advanced()
    except ImportError:
        logger.warning("Pandas not available, using basic gesture viewer")
        view_saved_gestures_basic()

if __name__ == "__main__":
    try:
        import os
        # Check if pandas is available for interactive features
        try:
            import pandas as pd
            # If run directly, offer interactive mode
            log_info("Running interactive gesture viewer")
            view_saved_gestures_interactive()
        except ImportError:
            # Fall back to basic view if pandas not available
            logger.warning("Pandas not available, falling back to basic viewer")
            view_saved_gestures()
    except Exception as e:
        error_msg = f"❌ Unexpected error: {str(e)}"
        print(error_msg)
        log_error(error_msg)