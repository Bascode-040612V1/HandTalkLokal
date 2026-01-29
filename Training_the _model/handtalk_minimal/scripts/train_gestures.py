import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, accuracy_score
import joblib
import os

def load_data(csv_path):
    """Load and prepare the gesture data"""
    print("Loading data...")
    df = pd.read_csv(csv_path)
    
    # Separate features and labels
    X = df.drop('label', axis=1)
    y = df['label']
    
    print(f"Data shape: {X.shape}")
    print(f"Number of classes: {len(y.unique())}")
    print(f"Classes: {y.unique()}")
    
    return X, y

def prepare_data(X, y, test_size=0.2, random_state=42):
    """Split and scale the data"""
    print("Preparing data...")
    
    # Split the data
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=test_size, random_state=random_state, stratify=y
    )
    
    # Scale the features
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)
    
    print(f"Training set size: {X_train.shape[0]}")
    print(f"Test set size: {X_test.shape[0]}")
    
    return X_train_scaled, X_test_scaled, y_train, y_test, scaler

def train_model(X_train, y_train):
    """Train the gesture recognition model"""
    print("Training model...")
    
    # Create and train the model
    model = RandomForestClassifier(
        n_estimators=100,
        random_state=42,
        max_depth=10,
        min_samples_split=5,
        min_samples_leaf=2
    )
    
    model.fit(X_train, y_train)
    print("Model training completed!")
    
    return model

def evaluate_model(model, X_test, y_test):
    """Evaluate the trained model"""
    print("Evaluating model...")
    
    # Make predictions
    y_pred = model.predict(X_test)
    
    # Calculate accuracy
    accuracy = accuracy_score(y_test, y_pred)
    print(f"Test Accuracy: {accuracy:.4f}")
    
    # Detailed classification report
    print("\nClassification Report:")
    print(classification_report(y_test, y_pred))
    
    return accuracy

def save_model(model, scaler, model_path, scaler_path):
    """Save the trained model and scaler"""
    print("Saving model and scaler...")
    
    joblib.dump(model, model_path)
    joblib.dump(scaler, scaler_path)
    
    print(f"Model saved to: {model_path}")
    print(f"Scaler saved to: {scaler_path}")

def main():
    # File paths
    data_dir = "data"
    csv_file = os.path.join(data_dir, "gestures_bimanual.csv")
    model_file = os.path.join(data_dir, "gesture_model.pkl")
    scaler_file = os.path.join(data_dir, "scaler.pkl")
    
    # Check if data file exists
    if not os.path.exists(csv_file):
        print(f"Error: Data file not found at {csv_file}")
        print("Please ensure you have collected gesture data first.")
        return
    
    # Load data
    X, y = load_data(csv_file)
    
    # Prepare data
    X_train, X_test, y_train, y_test, scaler = prepare_data(X, y)
    
    # Train model
    model = train_model(X_train, y_train)
    
    # Evaluate model
    accuracy = evaluate_model(model, X_test, y_test)
    
    # Save model and scaler
    save_model(model, scaler, model_file, scaler_file)
    
    print(f"\nTraining completed successfully!")
    print(f"Model accuracy: {accuracy:.4f}")
    print(f"Model saved to: {model_file}")

if __name__ == "__main__":
    main()