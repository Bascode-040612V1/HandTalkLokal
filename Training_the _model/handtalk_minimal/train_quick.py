"""
Quick training script for hand gesture recognition model with NO_GESTURE class.

This script trains a model quickly with fewer epochs for immediate use.
"""

import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.utils.class_weight import compute_class_weight
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Dropout, BatchNormalization
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.callbacks import EarlyStopping, ReduceLROnPlateau
from tensorflow.keras.utils import to_categorical
from tensorflow.keras.losses import CategoricalCrossentropy
import tensorflow as tf
import pickle
import os

# Create organized output directories
os.makedirs('output/trained_models', exist_ok=True)
os.makedirs('output/model_assets', exist_ok=True)

def load_and_validate_data():
    """Load the expanded dataset with NO_GESTURE class."""
    data_path = '../../datasets/gestures_bimanual_expanded.csv'
    
    df = pd.read_csv(data_path)
    
    print(f"Dataset shape: {df.shape}")
    print(f"Number of classes: {df['label'].nunique()}")
    print(f"Classes: {sorted(df['label'].unique())}")
    
    # Identify feature columns (exclude label) - there should be exactly 138 features
    feature_cols = [col for col in df.columns if col != 'label']
    
    X = df[feature_cols].values.astype(np.float32)
    y = df['label'].values
    
    # Encode labels
    label_encoder = LabelEncoder()
    y_encoded = label_encoder.fit_transform(y)
    num_classes = len(label_encoder.classes_)
    
    # Convert to categorical
    y_categorical = to_categorical(y_encoded, num_classes=num_classes)
    
    return X, y_categorical, label_encoder, num_classes, feature_cols

def build_simple_model(input_dim, num_classes):
    """Build a simpler model for quicker training."""
    model = Sequential([
        Dense(128, activation='relu', input_shape=(input_dim,)),
        BatchNormalization(),
        Dropout(0.3),
        
        Dense(64, activation='relu'),
        BatchNormalization(),
        Dropout(0.3),
        
        Dense(32, activation='relu'),
        Dropout(0.2),
        
        Dense(num_classes, activation='softmax')
    ])
    
    model.compile(
        optimizer=Adam(learning_rate=0.001),
        loss=CategoricalCrossentropy(label_smoothing=0.1),
        metrics=['accuracy']
    )
    
    return model

def main():
    print("Loading data...")
    X, y_categorical, label_encoder, num_classes, feature_cols = load_and_validate_data()
    
    # Split data
    X_train, X_test, y_train, y_test = train_test_split(
        X, y_categorical, test_size=0.2, random_state=42, 
        stratify=y_categorical.argmax(axis=1)
    )
    
    print(f"Training samples: {X_train.shape[0]}")
    print(f"Test samples: {X_test.shape[0]}")
    print(f"Number of classes: {num_classes}")
    
    # Build model
    model = build_simple_model(X_train.shape[1], num_classes)
    
    # Quick training with fewer epochs
    print("Starting quick training...")
    model.fit(
        X_train, y_train,
        validation_split=0.2,
        epochs=10,  # Reduced for quick training
        batch_size=32,
        verbose=1
    )
    
    # Evaluate
    test_loss, test_accuracy = model.evaluate(X_test, y_test, verbose=0)
    print(f"Test Accuracy: {test_accuracy:.4f}")
    
    # Save model and labels
    model.save('output/trained_models/gesture_model_quick.keras')
    
    # Save labels
    labels = label_encoder.classes_
    with open('output/model_assets/labels_quick.txt', 'w') as f:
        for label in labels:
            f.write(f"{label}\n")
    
    # Save label encoder
    with open('output/model_assets/label_encoder_quick.pkl', 'wb') as f:
        pickle.dump(label_encoder, f)
    
    print("Quick training completed!")
    print(f"Model saved with {num_classes} classes including NO_GESTURE")

if __name__ == "__main__":
    main()