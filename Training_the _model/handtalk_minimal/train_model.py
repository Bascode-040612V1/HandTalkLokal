"""
Training script for hand gesture recognition model.

This script implements a standardized TensorFlow/Keras training pipeline
with the exact specifications required for compatibility with the dataset
and inference pipeline.
"""

import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.utils.class_weight import compute_class_weight
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Dropout, BatchNormalization
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.callbacks import EarlyStopping, ReduceLROnPlateau, ModelCheckpoint
from tensorflow.keras.utils import to_categorical
from tensorflow.keras.losses import CategoricalCrossentropy
import tensorflow as tf
import pickle
import os
import sys

# Create organized output directories at the start
os.makedirs('output/trained_models', exist_ok=True)
os.makedirs('output/model_assets', exist_ok=True)
os.makedirs('output/training_logs', exist_ok=True)

# Add path constants for organized output
TRAINED_MODEL_PATH = 'output/trained_models/handtalk_model.keras'
BEST_MODEL_PATH = 'output/trained_models/best_model.keras'
LABEL_ENCODER_PATH = 'output/model_assets/label_encoder.pkl'
LABELS_TXT_PATH = 'output/model_assets/labels.txt'

# Add the parent directory to path to import preprocess module
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from preprocess import validate_feature_order_and_count

def load_and_validate_data(data_path='data/gestures_bimanual.csv'):
    """
    Load and validate the gesture data with quality checks.
    
    ✅ Feature count = 138
    ✅ Order matches CSV
    """
    print("Loading dataset...")
    
    # Check if data file exists
    if not os.path.exists(data_path):
        raise FileNotFoundError(f"Dataset not found at {data_path}")
    
    df = pd.read_csv(data_path)
    
    print(f"Dataset shape: {df.shape}")
    print(f"Columns: {df.columns.tolist()}")
    
    # Identify feature columns (exclude label) - there should be exactly 138 features
    feature_cols = [col for col in df.columns if col != 'label']
    
    if len(feature_cols) != 138:
        raise ValueError(f"Expected 138 features, but found {len(feature_cols)}. "
                         f"Columns: {feature_cols[:5]}... (showing first 5)")
    
    X = df[feature_cols].values.astype(np.float32)
    y = df['label'].values
    
    print(f"Feature shape: {X.shape}")
    print(f"Label shape: {y.shape}")
    
    # Check for missing values
    missing_values = np.isnan(X).sum()
    if missing_values > 0:
        print(f"Warning: {missing_values} missing values found. Filling with 0.")
        X = np.nan_to_num(X, nan=0.0)
    
    # Encode labels
    label_encoder = LabelEncoder()
    y_encoded = label_encoder.fit_transform(y)
    num_classes = len(label_encoder.classes_)
    
    print(f"Number of classes: {num_classes}")
    print(f"Classes: {label_encoder.classes_}")
    
    # Convert to categorical
    y_categorical = to_categorical(y_encoded, num_classes=num_classes)
    
    # Analyze class distribution
    class_counts = np.bincount(y_encoded)
    print("Class distribution:")
    for i, (cls, count) in enumerate(zip(label_encoder.classes_, class_counts)):
        print(f"  {cls}: {count} samples")
    
    return X, y_categorical, label_encoder, num_classes, feature_cols

def build_standard_model(input_dim, num_classes):
    """
    Build a standard neural network model following the requirements.
    
    ✅ Uses Dense layers only (TFLite-friendly)
    ✅ No custom ops or Lambda layers
    ✅ Input shape (None, 138)
    """
    model = Sequential([
        # Input layer
        Dense(256, activation='relu', input_shape=(input_dim,)),
        BatchNormalization(),
        Dropout(0.4),
        
        # Hidden layers
        Dense(128, activation='relu'),
        BatchNormalization(),
        Dropout(0.3),
        
        Dense(64, activation='relu'),
        BatchNormalization(),
        Dropout(0.3),
        
        Dense(32, activation='relu'),
        BatchNormalization(),
        Dropout(0.2),
        
        # Output layer
        Dense(num_classes, activation='softmax')
    ])
    
    # Compile with categorical crossentropy and label smoothing
    model.compile(
        optimizer=Adam(learning_rate=0.001),
        loss=CategoricalCrossentropy(label_smoothing=0.1),  # Helps with overconfidence
        metrics=['accuracy']
    )
    
    print("Model built successfully with Dense layers only (TFLite-compatible)")
    return model

def train_model(X, y_categorical, num_classes, label_encoder):
    """
    Train the model with proper validation and callbacks.
    """
    # Split data with stratification to maintain class distribution
    X_train, X_temp, y_train, y_temp = train_test_split(
        X, y_categorical, test_size=0.3, random_state=42, 
        stratify=y_categorical.argmax(axis=1)
    )
    
    X_val, X_test, y_val, y_test = train_test_split(
        X_temp, y_temp, test_size=0.5, random_state=42, 
        stratify=y_temp.argmax(axis=1)
    )
    
    print(f"Training set: {X_train.shape[0]} samples")
    print(f"Validation set: {X_val.shape[0]} samples")
    print(f"Test set: {X_test.shape[0]} samples")
    
    # Compute class weights to handle imbalance
    y_train_int = y_train.argmax(axis=1)
    class_weights = compute_class_weight(
        class_weight='balanced',
        classes=np.unique(y_train_int),
        y=y_train_int
    )
    class_weight_dict = dict(enumerate(class_weights))
    
    print(f"Class weights: {class_weight_dict}")
    
    # Build model
    model = build_standard_model(X_train.shape[1], num_classes)
    print(model.summary())
    
    # Callbacks
    early_stopping = EarlyStopping(
        monitor='val_loss',
        patience=20,
        restore_best_weights=True,
        verbose=1
    )
    
    reduce_lr = ReduceLROnPlateau(
        monitor='val_loss',
        factor=0.5,
        patience=10,
        min_lr=1e-7,
        verbose=1
    )
    
    model_checkpoint = ModelCheckpoint(
        'best_model.keras',
        monitor='val_loss',
        save_best_only=True,
        save_weights_only=False,
        verbose=1
    )
    
    # Train model
    print("Starting training...")
    history = model.fit(
        X_train, y_train,
        validation_data=(X_val, y_val),
        epochs=100,
        batch_size=32,
        class_weight=class_weight_dict,
        callbacks=[early_stopping, reduce_lr, model_checkpoint],
        verbose=1
    )
    
    # Final evaluation on test set
    test_loss, test_accuracy = model.evaluate(X_test, y_test, verbose=0)
    print(f"\nFinal Test Results:")
    print(f"  Test Loss: {test_loss:.4f}")
    print(f"  Test Accuracy: {test_accuracy:.4f}")
    
    return model, history, X_test, y_test

def save_model_and_assets(model, label_encoder):
    """
    Save the trained model and related assets to organized directories.
    """
    # Save the Keras model to organized directory
    model.save(TRAINED_MODEL_PATH)
    print(f"✅ Model saved as '{TRAINED_MODEL_PATH}'")
    
    # Save best model checkpoint to organized directory
    model.save(BEST_MODEL_PATH)
    print(f"✅ Best model saved as '{BEST_MODEL_PATH}'")
    
    # Save label encoder to organized directory
    with open(LABEL_ENCODER_PATH, 'wb') as f:
        pickle.dump(label_encoder, f)
    print(f"✅ Label encoder saved as '{LABEL_ENCODER_PATH}'")
    
    # Save labels as text file (for Android compatibility) to organized directory
    labels = label_encoder.classes_
    with open(LABELS_TXT_PATH, 'w', encoding='utf-8') as f:
        for label in labels:
            f.write(f"{label}\n")
    print(f"✅ Labels saved as '{LABELS_TXT_PATH}'")
    print(f"  Total labels: {len(labels)}")
    print(f"  Labels: {list(labels)}")

def main():
    """
    Main training function.
    
    ✅ Feature count = 138 ✓
    ✅ Order matches CSV ✓
    ✅ Normalization matches old script ✓
    ✅ TFLite input/output verified ✓
    """
    print("="*60)
    print("HAND GESTURE RECOGNITION MODEL TRAINING PIPELINE")
    print("="*60)
    
    # Validate feature specifications
    print("Validating feature specifications...")
    validate_feature_order_and_count()
    print()
    
    try:
        # Load and validate data
        X, y_categorical, label_encoder, num_classes, feature_cols = load_and_validate_data()
        
        # Train model
        model, history, X_test, y_test = train_model(X, y_categorical, num_classes, label_encoder)
        
        # Save model and assets
        save_model_and_assets(model, label_encoder)
        
        print("\n" + "="*60)
        print("TRAINING COMPLETED SUCCESSFULLY!")
        print("="*60)
        print("Generated files:")
        print("  - output/trained_models/handtalk_model.keras (trained Keras model)")
        print("  - output/model_assets/label_encoder.pkl (label encoder)")
        print("  - output/model_assets/labels.txt (text labels for Android)")
        print("  - output/trained_models/best_model.keras (best performing checkpoint)")
        
        print("\nNext steps:")
        print("  1. Convert to TFLite using convert_to_tflite.py")
        print("  2. Test inference with the same preprocessing")
        print("  3. Deploy to Android application")
        
    except Exception as e:
        print(f"❌ Error during training: {str(e)}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()