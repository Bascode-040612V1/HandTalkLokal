"""
Gesture Registry Module
Manages the collection and tracking of gestures to prevent mismatches between gestures and words
"""

import json
import os
from datetime import datetime
from typing import Dict, List, Optional


class GestureRegistry:
    def __init__(self, registry_file: str = "data/gesture_registry.json"):
        """
        Initialize the gesture registry
        
        Args:
            registry_file: Path to the JSON file storing the gesture registry
        """
        self.registry_file = registry_file
        self.registry = self._load_registry()
    
    def _load_registry(self) -> Dict:
        """
        Load the gesture registry from file or create a default one
        
        Returns:
            Dictionary containing the gesture registry
        """
        default_registry = {
            "created_at": datetime.now().isoformat(),
            "last_updated": datetime.now().isoformat(),
            "gestures": {}
        }
        
        # Create data directory if it doesn't exist
        os.makedirs(os.path.dirname(self.registry_file), exist_ok=True)
        
        if os.path.exists(self.registry_file):
            try:
                with open(self.registry_file, 'r', encoding='utf-8') as f:
                    return json.load(f)
            except Exception as e:
                print(f"Error loading registry file: {e}")
                return default_registry
        else:
            # Create default registry
            self._save_registry(default_registry)
            return default_registry
    
    def _save_registry(self, registry: Dict) -> None:
        """
        Save the gesture registry to file
        
        Args:
            registry: The registry dictionary to save
        """
        registry["last_updated"] = datetime.now().isoformat()
        try:
            with open(self.registry_file, 'w', encoding='utf-8') as f:
                json.dump(registry, f, ensure_ascii=False, indent=2)
        except Exception as e:
            print(f"Error saving registry file: {e}")
    
    def register_gesture(self, gesture_name: str, file_path: str, sequence_number: int, 
                         frame_count: int, collection_date: str) -> bool:
        """
        Register a new gesture in the registry
        
        Args:
            gesture_name: Name of the gesture
            file_path: Path to the gesture data file
            sequence_number: Sequence number of this gesture collection
            frame_count: Number of frames in the gesture
            collection_date: Date when the gesture was collected
            
        Returns:
            True if registration was successful, False otherwise
        """
        if gesture_name not in self.registry["gestures"]:
            self.registry["gestures"][gesture_name] = {
                "first_added": collection_date,
                "last_added": collection_date,
                "total_collections": 0,
                "sequences": []
            }
        
        # Add this sequence to the gesture
        sequence_info = {
            "sequence_number": sequence_number,
            "file_path": file_path,
            "frame_count": frame_count,
            "collection_date": collection_date,
            "size_bytes": os.path.getsize(file_path) if os.path.exists(file_path) else 0
        }
        
        self.registry["gestures"][gesture_name]["sequences"].append(sequence_info)
        self.registry["gestures"][gesture_name]["total_collections"] += 1
        self.registry["gestures"][gesture_name]["last_added"] = collection_date
        
        # Update the registry file
        self._save_registry(self.registry)
        return True
    
    def get_gesture_info(self, gesture_name: str) -> Optional[Dict]:
        """
        Get information about a specific gesture
        
        Args:
            gesture_name: Name of the gesture to look up
            
        Returns:
            Dictionary with gesture information or None if not found
        """
        return self.registry["gestures"].get(gesture_name)
    
    def get_all_gestures(self) -> List[str]:
        """
        Get a list of all registered gesture names
        
        Returns:
            List of gesture names
        """
        return list(self.registry["gestures"].keys())
    
    def get_gesture_sequences(self, gesture_name: str) -> List[Dict]:
        """
        Get all sequences for a specific gesture
        
        Args:
            gesture_name: Name of the gesture
            
        Returns:
            List of sequence information dictionaries
        """
        gesture_info = self.get_gesture_info(gesture_name)
        if gesture_info:
            return gesture_info["sequences"]
        return []
    
    def update_gesture_stats(self, gesture_name: str) -> bool:
        """
        Update statistics for a gesture based on current files
        
        Args:
            gesture_name: Name of the gesture to update
            
        Returns:
            True if update was successful, False otherwise
        """
        if gesture_name not in self.registry["gestures"]:
            return False
        
        # Recalculate total collections based on actual sequences
        sequences = self.get_gesture_sequences(gesture_name)
        self.registry["gestures"][gesture_name]["total_collections"] = len(sequences)
        
        # Update last added date to the latest sequence
        if sequences:
            latest_date = max(seq["collection_date"] for seq in sequences)
            self.registry["gestures"][gesture_name]["last_added"] = latest_date
        
        self._save_registry(self.registry)
        return True
    
    def remove_gesture(self, gesture_name: str) -> bool:
        """
        Remove a gesture from the registry
        
        Args:
            gesture_name: Name of the gesture to remove
            
        Returns:
            True if removal was successful, False otherwise
        """
        if gesture_name in self.registry["gestures"]:
            del self.registry["gestures"][gesture_name]
            self._save_registry(self.registry)
            return True
        return False
    
    def get_registry_summary(self) -> Dict:
        """
        Get a summary of the entire registry
        
        Returns:
            Dictionary with registry summary information
        """
        total_gestures = len(self.registry["gestures"])
        total_sequences = sum(len(info["sequences"]) for info in self.registry["gestures"].values())
        
        return {
            "created_at": self.registry["created_at"],
            "last_updated": self.registry["last_updated"],
            "total_gestures": total_gestures,
            "total_sequences": total_sequences,
            "gestures": self.registry["gestures"]
        }


# Global instance for easy access
gesture_registry = GestureRegistry()


def get_gesture_registry() -> GestureRegistry:
    """
    Get the global gesture registry instance
    
    Returns:
        GestureRegistry instance
    """
    return gesture_registry


def add_gesture_to_registry(gesture_name: str, file_path: str, sequence_number: int, 
                           frame_count: int, collection_date: str) -> bool:
    """
    Convenience function to add a gesture to the registry
    
    Args:
        gesture_name: Name of the gesture
        file_path: Path to the gesture data file
        sequence_number: Sequence number of this gesture collection
        frame_count: Number of frames in the gesture
        collection_date: Date when the gesture was collected
        
    Returns:
        True if addition was successful, False otherwise
    """
    registry = get_gesture_registry()
    return registry.register_gesture(gesture_name, file_path, sequence_number, 
                                     frame_count, collection_date)


def get_gesture_info(gesture_name: str) -> Optional[Dict]:
    """
    Convenience function to get information about a gesture
    
    Args:
        gesture_name: Name of the gesture to look up
        
    Returns:
        Dictionary with gesture information or None if not found
    """
    registry = get_gesture_registry()
    return registry.get_gesture_info(gesture_name)


def get_all_registered_gestures() -> List[str]:
    """
    Convenience function to get all registered gesture names
    
    Returns:
        List of gesture names
    """
    registry = get_gesture_registry()
    return registry.get_all_gestures()


def print_registry_summary():
    """
    Print a human-readable summary of the gesture registry
    """
    registry = get_gesture_registry()
    summary = registry.get_registry_summary()
    
    print("=== Gesture Registry Summary ===")
    print(f"Created at: {summary['created_at']}")
    print(f"Last updated: {summary['last_updated']}")
    print(f"Total gestures: {summary['total_gestures']}")
    print(f"Total sequences: {summary['total_sequences']}")
    print("\nRegistered gestures:")
    
    for gesture_name, info in summary['gestures'].items():
        print(f"  - {gesture_name}: {info['total_collections']} sequences "
              f"(added {info['first_added']})")


if __name__ == "__main__":
    # Test the registry functionality
    print_registry_summary()