import json
import os

# Check gesture registry
registry_path = r"c:\Users\obuma\Documents\0001_HandTalk_Local\App_Development\Hand_Talk_Lokal\Training_the _model\handtalk_minimal\data\gesture_registry.json"
if os.path.exists(registry_path):
    with open(registry_path, 'r') as f:
        data = json.load(f)
    gestures = list(data['gestures'].keys())
    print("Gestures in registry:")
    print(sorted(gestures))
    print(f"Total: {len(gestures)} gestures")
else:
    print("Registry file not found")

# Check training labels file
labels_path = r"c:\Users\obuma\Documents\0001_HandTalk_Local\App_Development\Hand_Talk_Lokal\Training_the _model\handtalk_minimal\labels.txt"
if os.path.exists(labels_path):
    with open(labels_path, 'r') as f:
        labels = [line.strip() for line in f.readlines() if line.strip()]
    print("\nLabels in training file:")
    print(labels)
    print(f"Total: {len(labels)} labels")
else:
    print("Training labels file not found")

# Check Android labels file
android_labels_path = r"c:\Users\obuma\Documents\0001_HandTalk_Local\App_Development\Hand_Talk_Lokal\app\src\main\assets\labels.txt"
if os.path.exists(android_labels_path):
    with open(android_labels_path, 'r') as f:
        android_labels = [line.strip() for line in f.readlines() if line.strip()]
    print("\nLabels in Android app:")
    print(android_labels)
    print(f"Total: {len(android_labels)} labels")
else:
    print("Android labels file not found")