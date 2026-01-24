"""
Gesture Management Script
Provides utilities for managing gestures, viewing collected data, and preventing mismatches
"""

import os
import json
from gesture_registry import get_gesture_registry, print_registry_summary, get_all_registered_gestures
from collections import defaultdict


def list_all_gestures():
    """List all registered gestures with their collection counts"""
    registry = get_gesture_registry()
    gestures = registry.get_all_gestures()
    
    if not gestures:
        print("No gestures registered yet.")
        return
    
    print("=== Registered Gestures ===")
    for gesture in sorted(gestures):
        info = registry.get_gesture_info(gesture)
        if info:
            print(f"- {gesture}: {info['total_collections']} collections "
                  f"(first: {info['first_added'][:10]}, last: {info['last_added'][:10]})")
        else:
            print(f"- {gesture}: Info unavailable")


def view_gesture_details(gesture_name):
    """View detailed information about a specific gesture"""
    registry = get_gesture_registry()
    info = registry.get_gesture_info(gesture_name)
    
    if not info:
        print(f"No information found for gesture: {gesture_name}")
        return
    
    print(f"\n=== Details for Gesture: {gesture_name} ===")
    print(f"First added: {info['first_added']}")
    print(f"Last added: {info['last_added']}")
    print(f"Total collections: {info['total_collections']}")
    
    sequences = registry.get_gesture_sequences(gesture_name)
    print(f"\nSequences ({len(sequences)}):")
    for seq in sequences:
        print(f"  - Seq #{seq['sequence_number']:03d}: {seq['frame_count']} frames, "
              f"collected {seq['collection_date'][:10]}, file: {seq['file_path']}")


def check_for_duplicates():
    """Check for potential duplicate gesture names"""
    registry = get_gesture_registry()
    gestures = registry.get_all_gestures()
    
    # Group gestures by similarity (case-insensitive)
    gesture_groups = defaultdict(list)
    for gesture in gestures:
        normalized = gesture.lower().strip()
        gesture_groups[normalized].append(gesture)
    
    duplicates_found = False
    print("\n=== Duplicate Check ===")
    for normalized, original_forms in gesture_groups.items():
        if len(original_forms) > 1:
            duplicates_found = True
            print(f"Potential duplicates for '{normalized}': {', '.join(original_forms)}")
    
    if not duplicates_found:
        print("No potential duplicates found.")


def clean_up_unused_files():
    """Remove gesture files that are not registered in the registry"""
    registry = get_gesture_registry()
    registered_files = set()
    
    # Collect all registered file paths
    for gesture_name in registry.get_all_gestures():
        sequences = registry.get_gesture_sequences(gesture_name)
        for seq in sequences:
            registered_files.add(seq['file_path'])
    
    # Find all gesture files in the directory
    gesture_dir = "data/arm_hand_sequences"
    if not os.path.exists(gesture_dir):
        print(f"Gesture directory does not exist: {gesture_dir}")
        return
    
    all_files = set()
    for file in os.listdir(gesture_dir):
        if file.endswith('.json') and file.startswith('gesture_'):
            all_files.add(os.path.join(gesture_dir, file))
    
    # Find unregistered files
    unregistered_files = all_files - registered_files
    
    if unregistered_files:
        print(f"\n=== Unregistered Files Found ({len(unregistered_files)}) ===")
        for file in sorted(unregistered_files):
            print(f"  - {file}")
        
        response = input("\nWould you like to register these files? (y/N): ")
        if response.lower() == 'y':
            for file_path in unregistered_files:
                # Extract gesture name and sequence from filename
                filename = os.path.basename(file_path)
                parts = filename.split('_')
                
                if len(parts) >= 4 and parts[0] == 'gesture' and parts[2] == 'seq':
                    gesture_name = '_'.join(parts[1:-2])  # Everything between 'gesture' and 'seq'
                    try:
                        sequence_num = int(parts[-2])
                        
                        # Read the file to get frame count
                        with open(file_path, 'r') as f:
                            data = json.load(f)
                            frame_count = len(data.get('frames', []))
                        
                        # Register the gesture
                        registry.register_gesture(
                            gesture_name=gesture_name,
                            file_path=file_path,
                            sequence_number=sequence_num,
                            frame_count=frame_count,
                            collection_date=data.get('metadata', {}).get('collection_date', 'Unknown')
                        )
                        print(f"  ✅ Registered: {gesture_name} (seq #{sequence_num:03d})")
                    except ValueError:
                        print(f"  ❌ Could not parse sequence number from: {filename}")
                    except Exception as e:
                        print(f"  ❌ Error registering {filename}: {str(e)}")
                else:
                    print(f"  ❌ Could not parse filename: {filename}")
    else:
        print("\n=== Clean Status ===")
        print("All gesture files are properly registered in the registry.")


def main():
    """Main function to manage gestures"""
    print("=== HandTalk Gesture Manager ===")
    print("1. View registry summary")
    print("2. List all gestures")
    print("3. View details for a specific gesture")
    print("4. Check for duplicates")
    print("5. Clean up unregistered files")
    print("6. Exit")
    
    while True:
        try:
            choice = input("\nEnter your choice (1-6): ").strip()
            
            if choice == '1':
                print_registry_summary()
            elif choice == '2':
                list_all_gestures()
            elif choice == '3':
                gesture_name = input("Enter gesture name to view details: ").strip()
                if gesture_name:
                    view_gesture_details(gesture_name)
                else:
                    print("Gesture name cannot be empty.")
            elif choice == '4':
                check_for_duplicates()
            elif choice == '5':
                clean_up_unused_files()
            elif choice == '6':
                print("Exiting gesture manager...")
                break
            else:
                print("Invalid choice. Please enter 1-6.")
        except KeyboardInterrupt:
            print("\n\nExiting gesture manager...")
            break
        except Exception as e:
            print(f"An error occurred: {str(e)}")


if __name__ == "__main__":
    main()