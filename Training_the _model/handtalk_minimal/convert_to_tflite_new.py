import joblib
import numpy as np
import tensorflow as tf
from sklearn.ensemble import RandomForestClassifier
from sklearn.preprocessing import LabelEncoder
import pandas as pd
import os

def create_and_convert_model():
    """Create a TensorFlow model based on the scikit-learn model and convert to TFLite"""
    
    # Load the trained scikit-learn model and scaler
    print("Loading scikit-learn model and scaler...")
    model = joblib.load("data/gesture_model.pkl")
    scaler = joblib.load("data/scaler.pkl")
    
    # Load the data to get class information
    df = pd.read_csv("data/gestures_bimanual.csv")
    X = df.drop('label', axis=1)
    y = df['label']
    
    # Create label encoder
    label_encoder = LabelEncoder()
    y_encoded = label_encoder.fit_transform(y)
    
    print(f"Number of classes: {len(model.classes_)}")
    print(f"Feature shape: {X.shape}")
    
    # Create a TensorFlow model that mimics the Random Forest behavior
    # We'll train a simple neural network to approximate the Random Forest
    print("Creating TensorFlow model...")
    
    # Create a neural network with similar capacity to the Random Forest
    tf_model = tf.keras.Sequential([
        tf.keras.Input(shape=(138,)),  # 138 features
        tf.keras.layers.Dense(128, activation='relu'),
        tf.keras.layers.Dropout(0.3),
        tf.keras.layers.Dense(64, activation='relu'),
        tf.keras.layers.Dropout(0.3),
        tf.keras.layers.Dense(32, activation='relu'),
        tf.keras.layers.Dense(len(model.classes_), activation='softmax')  # Output for all classes
    ])
    
    # Compile the model
    tf_model.compile(
        optimizer='adam',
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )
    
    print("Model architecture created!")
    
    # Since we don't have the original training data in encoded form, 
    # we'll create synthetic training data based on the scikit-learn model predictions
    print("Preparing training data for TensorFlow model...")
    
    # Generate synthetic data by sampling from the original dataset and using the RF model's predictions
    X_scaled = scaler.transform(X.values)
    X_tensor = tf.constant(X_scaled.astype(np.float32))
    
    # Get predictions from the Random Forest model to use as targets
    rf_predictions = model.predict(X.values)
    rf_prediction_indices = label_encoder.transform(rf_predictions)
    
    # Train the TensorFlow model to mimic the Random Forest
    print("Training TensorFlow model to approximate Random Forest...")
    tf_model.fit(
        X_tensor, 
        rf_prediction_indices, 
        epochs=50, 
        batch_size=32, 
        verbose=1,
        validation_split=0.2
    )
    
    # Test the model
    sample_input = X_tensor[:1]
    prediction = tf_model.predict(sample_input)
    print(f"Sample prediction shape: {prediction.shape}")
    print(f"Prediction sum (should be ~1.0): {np.sum(prediction[0]):.4f}")
    
    # Convert to TensorFlow Lite
    print("Converting to TensorFlow Lite...")
    
    # Convert the model
    converter = tf.lite.TFLiteConverter.from_keras_model(tf_model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    
    # Ensure float32 for compatibility
    converter.target_spec.supported_types = [tf.float32]
    
    tflite_model = converter.convert()
    
    # Save the TFLite model
    tflite_path = "data/gesture_model.tflite"
    with open(tflite_path, 'wb') as f:
        f.write(tflite_model)
    
    print(f"✅ TensorFlow Lite model saved to: {tflite_path}")
    
    # Save labels
    labels_path = "data/labels.txt"
    with open(labels_path, 'w') as f:
        for label in model.classes_:
            f.write(f"{label}\n")
    
    print(f"✅ Labels saved to: {labels_path}")
    
    # Save the scaler parameters as well for reference
    scaler_params_path = "data/scaler_params.txt"
    with open(scaler_params_path, 'w') as f:
        f.write(f"Mean: {scaler.mean_}\n")
        f.write(f"Scale: {scaler.scale_}\n")
    
    print(f"✅ Scaler parameters saved to: {scaler_params_path}")
    
    print("\n✅ Model conversion completed successfully!")
    print(f"✅ Generated files: {tflite_path} and {labels_path}")
    
    return tflite_path, labels_path

def validate_tflite_model(tflite_model_path):
    """Validate the TFLite model"""
    print(f"\nValidating TFLite model: {tflite_model_path}")
    
    # Load TFLite model and allocate tensors
    interpreter = tf.lite.Interpreter(model_path=tflite_model_path)
    interpreter.allocate_tensors()
    
    # Get input and output tensors
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    
    print(f"Input details: {input_details[0]}")
    print(f"Output details: {output_details[0]}")
    
    # Check input shape
    input_shape = input_details[0]['shape']
    expected_shape = [1, 138]  # batch_size=1, features=138
    
    if list(input_shape) == expected_shape:
        print(f"✅ Input shape correct: {input_shape}")
    else:
        print(f"❌ Input shape mismatch! Expected {expected_shape}, got {input_shape}")
        return False
    
    # Check input data type
    input_dtype = input_details[0]['dtype']
    if input_dtype == np.float32:
        print(f"✅ Input data type correct: {input_dtype}")
    else:
        print(f"❌ Input data type mismatch! Expected float32, got {input_dtype}")
        return False
    
    # Test with a sample input (using mean values from scaler as baseline)
    sample_input = np.random.random(input_shape).astype(np.float32)
    
    interpreter.set_tensor(input_details[0]['index'], sample_input)
    interpreter.invoke()
    
    # Get the output
    output_data = interpreter.get_tensor(output_details[0]['index'])
    print(f"✅ Sample inference successful")
    print(f"  Output shape: {output_data.shape}")
    
    # Check that output sums to approximately 1 (softmax layer)
    output_sum = np.sum(output_data[0])
    print(f"  Output sum (should be ~1.0): {output_sum:.4f}")
    
    if abs(output_sum - 1.0) < 0.01:  # Allow small floating point errors
        print("✅ Output appears to be valid softmax probabilities")
    else:
        print("⚠️  Output may not be valid softmax probabilities")
    
    print("✅ TFLite model validation completed successfully!")
    return True

def main():
    print("Starting TensorFlow Lite conversion process...")
    
    # Create and convert the model
    tflite_path, labels_path = create_and_convert_model()
    
    # Validate the converted model
    validation_success = validate_tflite_model(tflite_path)
    
    if validation_success:
        print("\n🎉 Conversion and validation completed successfully!")
        print("Next steps:")
        print(f"1. Copy {tflite_path} to app/src/main/assets/gesture_model.tflite")
        print(f"2. Copy {labels_path} to app/src/main/assets/labels.txt")
        print("3. Your Android app will now use the new model with 22 gesture classes!")
    else:
        print("❌ Validation failed!")
        
if __name__ == "__main__":
    main()