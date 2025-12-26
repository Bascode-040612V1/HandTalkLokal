"""
Script to retrain the gesture recognition model with current data
"""
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report
import joblib
import os

def load_and_prepare_data(csv_path="data/gestures_bimanual.csv"):
    """Load and prepare the bimanual gesture data for training"""
    try:
        if not os.path.exists(csv_path):
            raise FileNotFoundError(f"Data file {csv_path} not found. Please collect gesture data first.")
        
        # Load the data
        df = pd.read_csv(csv_path)
        
        # Separate features and labels
        X = df.drop('label', axis=1).values  # Convert to numpy array
        y = df['label'].values  # Convert to numpy array
        
        print(f"Data loaded: {X.shape[0]} samples with {X.shape[1]} features each")
        print(f"Expected features: 138 (126 hand + 12 pose landmarks)")  # Updated from 144 to 138
        unique_labels = list(set(y)) if len(y) > 0 else []
        print(f"Labels: {unique_labels}")
        print(f"Number of unique labels: {len(unique_labels)}")
        
        return X, y
    except Exception as e:
        print(f"Error loading data: {str(e)}")
        raise

def train_model():
    """Train the model with current data"""
    try:
        print("=== HandTalk Model Retraining ===")
        
        # Load data
        X, y = load_and_prepare_data()
        
        # Split the data
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
        print(f"Data split: {len(X_train)} training samples, {len(X_test)} testing samples")
        
        # Create and train the model
        model = RandomForestClassifier(
            n_estimators=100, 
            random_state=42,
            # Add parameters to handle class imbalance
            class_weight='balanced'
        )
        print("Training Random Forest classifier...")
        model.fit(X_train, y_train)
        
        # Evaluate the model
        y_pred = model.predict(X_test)
        accuracy = accuracy_score(y_test, y_pred)
        
        print(f"Model trained with accuracy: {accuracy:.2f}")
        print("\nClassification Report:")
        print(classification_report(y_test, y_pred))
        
        # Save model
        model_filename = "sign_language_model_bimanual.pkl"
        joblib.dump(model, model_filename)
        print(f"Model saved as {model_filename}")
        
        print("\n✅ Model retraining completed successfully!")
        print("Model is ready for real-time gesture recognition.")
        
        return model
        
    except Exception as e:
        print(f"Error during training: {str(e)}")
        raise

if __name__ == "__main__":
    train_model()