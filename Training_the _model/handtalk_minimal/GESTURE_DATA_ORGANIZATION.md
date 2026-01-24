# Gesture Data Organization and Collection Guide

## Overview
This document explains how the HandTalk gesture recognition system organizes and manages gesture data to prevent mismatches between gestures and words.

## Key Improvements

### 1. Centralized Gesture Registry
- **File:** `gesture_registry.py`
- **Purpose:** Tracks all collected gestures in a centralized registry
- **Benefits:**
  - Prevents duplicate gesture collections
  - Provides a single source of truth for all gesture data
  - Enables easy management and verification of collected data

### 2. Improved Sequence Numbering System
- **File:** `collect_data_bimanual.py` (updated)
- **Purpose:** Ensures consistent and predictable numbering across all gesture collections
- **Benefits:**
  - Each gesture gets a unique sequence number regardless of date
  - Prevents sequence number conflicts
  - Makes data organization clearer

### 3. Gesture Management Tool
- **File:** `manage_gestures.py`
- **Purpose:** Provides utilities to manage, view, and verify gesture collections
- **Features:**
  - View all registered gestures
  - Check for potential duplicates
  - View detailed information about specific gestures
  - Clean up unregistered files

## File Structure

```
data/
├── arm_hand_sequences/
│   ├── gesture_Hello_seq_001_20260120_163632.json
│   ├── gesture_Hello_seq_002_20260120_163845.json
│   └── ...
├── gestures_bimanual.csv
└── gesture_registry.json
```

## How It Works

### 1. Data Collection Process
When collecting gesture data:
1. The system checks the registry for existing sequences of the same gesture
2. Assigns the next available sequence number (regardless of date)
3. Saves the data with a consistent naming convention
4. Registers the new collection in the gesture registry

### 2. Naming Convention
Files follow the pattern: `gesture_{name}_seq_{number}_{timestamp}.json`
- `{name}`: The gesture name (e.g., "Hello", "Good Morning")
- `{number}`: Sequence number (001, 002, etc.) - unique per gesture
- `{timestamp}`: Date and time of collection (YYYYMMDD_HHMMSS)

### 3. Registry Structure
The registry (`data/gesture_registry.json`) contains:
- List of all gestures collected
- For each gesture:
  - First and last collection dates
  - Total number of collections
  - Details of each sequence collected

## Using the Management Tools

### View All Registered Gestures
```bash
python manage_gestures.py
```

### Check for Potential Issues
The system can detect:
- Potential duplicate gesture names (similar spellings)
- Unregistered files that exist but aren't in the registry
- Missing files referenced in the registry

### Clean Up Operations
- Identify and register untracked gesture files
- Detect and warn about potential duplicates

## Benefits of This Approach

1. **Prevents Mismatches:** Clear linkage between gesture names and their data
2. **Scalable Organization:** Easy to add new gestures without conflicts
3. **Quality Control:** Tools to verify and maintain data integrity
4. **Traceability:** Complete history of when and how each gesture was collected
5. **Maintenance:** Easy to identify and fix issues with collected data

## Best Practices

1. **Always use the management tools** to verify your data
2. **Check for duplicates** before adding new gestures
3. **Review the registry** regularly to ensure data integrity
4. **Maintain consistent naming** for gesture labels
5. **Document any manual changes** to the data structure

## Troubleshooting

If you encounter issues with gesture data:
1. Run the management tool to check for problems
2. Verify that all files are properly registered
3. Check for duplicate or conflicting names
4. Use the cleanup functions to resolve inconsistencies