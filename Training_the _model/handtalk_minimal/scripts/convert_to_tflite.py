"""
Script to convert the trained scikit-learn model to TensorFlow Lite format
"""
import pandas as pd
import numpy as np
import joblib
import tensorflow as tf
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
import os

def load_data(csv_path="data/gestures_bimanual.csv"):
    """Load and prepare the bimanual gesture data"""
    try:
        if not os.path.exists(csv_path):
            raise FileNotFoundError(f"Data file {csv_path} not found. Please collect gesture data first.")
        
        # Load the data
        df = pd.read_csv(csv_path)
        
        # Separate features and labels
        X = df.drop('label', axis=1).values
        y = df['label'].values
        
        # Get unique labels
        labels = list(set(y))
        print(f"Labels: {labels}")
        
        return X, y, labels
    except Exception as e:
        print(f"Error loading data: {str(e)}")
        raise

def train_tensorflow_model():
    """Train a TensorFlow model equivalent to the scikit-learn model"""
    print("=== Converting to TensorFlow Lite ===")
    
    # Load data
    X, y, labels = load_data()
    
    # Create label to index mapping
    label_to_index = {label: idx for idx, label in enumerate(labels)}
    y_indices = np.array([label_to_index[label] for label in y])
    
    # Split the data
    X_train, X_test, y_train, y_test = train_test_split(X, y_indices, test_size=0.2, random_state=42)
    
    # Create a simple neural network model
    model = tf.keras.Sequential([
        tf.keras.layers.Dense(128, activation='relu', input_shape=(X.shape[1],)),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(64, activation='relu'),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(len(labels), activation='softmax')
    ])
    
    model.compile(
        optimizer='adam',
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )
    
    # Train the model
    print("Training TensorFlow model...")
    model.fit(X_train, y_train, epochs=50, validation_data=(X_test, y_test), verbose=1)
    
    # Evaluate the model
    loss, accuracy = model.evaluate(X_test, y_test, verbose=0)
    print(f"TensorFlow model accuracy: {accuracy:.2f}")
    
    # Save the model
    model.save("gesture_model_tf.h5")
    print("TensorFlow model saved as gesture_model_tf.h5")
    
    # Convert to TensorFlow Lite
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()
    
    with open("gesture_model.tflite", "wb") as f:
        f.write(tflite_model)
    
    print("TensorFlow Lite model saved as gesture_model.tflite")
    
    # Save labels to a file
    with open("labels.txt", "w") as f:
        for label in labels:
            f.write(f"{label}\n")
    
    print("Labels saved as labels.txt")
    
    return model, labels

if __name__ == "__main__":
    train_tensorflow_model()