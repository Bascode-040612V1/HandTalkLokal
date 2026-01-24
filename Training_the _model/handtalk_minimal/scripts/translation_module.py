"""
Translation module for HandTalk application
Supports multiple dialects and languages
"""

import json
import os
from typing import Dict, List, Optional

class TranslationModule:
    def __init__(self, translations_file: str = "translations.json"):
        """
        Initialize the translation module
        
        Args:
            translations_file: Path to the JSON file containing translations
        """
        self.translations_file = translations_file
        self.translations = self._load_translations()
        self.current_dialect = "english"  # Default dialect
    
    def _load_translations(self) -> Dict:
        """
        Load translations from JSON file
        
        Returns:
            Dictionary containing all translations
        """
        default_translations = {
            "english": {
                "Good": "Good",
                "Morning": "Good Morning",
                "Afternoon": "Good Afternoon",
                "Noon": "Noon"
            },
            "filipino": {
                "Good": "Mabuti",
                "Morning": "Magandang Umaga",
                "Afternoon": "Magandang Hapon",
                "Noon": "Tanghali"
            },
            "cebuano": {
                "Good": "Maayo",
                "Morning": "Maayong Buntag",
                "Afternoon": "Maayong Hapon",
                "Noon": "Tanghali"
            },
            "hiligaynon": {
                "Good": "Maopay",
                "Morning": "Maopay nga Aga",
                "Afternoon": "Maopay nga Hapon",
                "Noon": "Tanghali"
            },
            "maranao": {
                "Good": "Maseya",
                "Morning": "Maseya a Pagkaudto",
                "Afternoon": "Maseya a Pagkahapon",
                "Noon": "Tanghali"
            }
        }
        
        if os.path.exists(self.translations_file):
            try:
                with open(self.translations_file, 'r', encoding='utf-8') as f:
                    return json.load(f)
            except Exception as e:
                print(f"Error loading translations file: {e}")
                return default_translations
        else:
            # Create default translations file
            self._save_translations(default_translations)
            return default_translations
    
    def _save_translations(self, translations: Dict) -> None:
        """
        Save translations to JSON file
        
        Args:
            translations: Dictionary containing translations to save
        """
        try:
            with open(self.translations_file, 'w', encoding='utf-8') as f:
                json.dump(translations, f, ensure_ascii=False, indent=2)
        except Exception as e:
            print(f"Error saving translations file: {e}")
    
    def set_dialect(self, dialect: str) -> bool:
        """
        Set the current dialect for translations
        
        Args:
            dialect: The dialect to use for translations
            
        Returns:
            True if dialect is valid, False otherwise
        """
        if dialect.lower() in self.translations:
            self.current_dialect = dialect.lower()
            return True
        return False
    
    def get_available_dialects(self) -> List[str]:
        """
        Get list of available dialects
        
        Returns:
            List of available dialect names
        """
        return list(self.translations.keys())
    
    def translate(self, text: str) -> str:
        """
        Translate text to the current dialect
        
        Args:
            text: Text to translate
            
        Returns:
            Translated text or original text if translation not found
        """
        # Normalize the text (remove extra spaces, etc.)
        normalized_text = text.strip()
        
        # Check if we have translations for the current dialect
        if self.current_dialect in self.translations:
            dialect_translations = self.translations[self.current_dialect]
            
            # Look for exact match
            if normalized_text in dialect_translations:
                return dialect_translations[normalized_text]
            
            # Try case-insensitive match
            for key, value in dialect_translations.items():
                if key.lower() == normalized_text.lower():
                    return value
        
        # Return original text if no translation found
        return text
    
    def add_translation(self, dialect: str, original: str, translation: str) -> None:
        """
        Add a new translation to the database
        
        Args:
            dialect: The dialect for this translation
            original: The original text
            translation: The translated text
        """
        if dialect not in self.translations:
            self.translations[dialect] = {}
        
        self.translations[dialect][original] = translation
        self._save_translations(self.translations)

# Global instance for easy access
translator = TranslationModule()

def get_translator() -> TranslationModule:
    """
    Get the global translator instance
    
    Returns:
        TranslationModule instance
    """
    return translator