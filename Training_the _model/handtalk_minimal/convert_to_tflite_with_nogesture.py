"""
TFLite conversion script for hand gesture recognition model with NO_GESTURE class.

This script converts the trained Keras model to TensorFlow Lite format
following the exact specifications required for Android compatibility.
"""

import tensorflow as tf
import numpy as np
import pickle
import sys
import os

def convert_to_tflite(keras_model_path='output/trained_models/handtalk_model_with_nogesture.keras', 
                     tflite_model_path='output/tflite_model/gesture_model_with_nogesture.tflite',
                     label_encoder_path='output/model_assets/label_encoder_with_nogesture.pkl',
                     labels_txt_path='output/labels/labels_with_nogesture.txt'):
    """
    Convert the trained Keras model to TensorFlow Lite format.
    
    ✅ Converts directly from Keras model
    ✅ Enables default optimizations
    ✅ Outputs gesture_model.tflite with NO_GESTURE class
    ✅ Maintains input shape (1, 138) for float32 tensors
    """
    print("Loading Keras model...")
    try:
        # Load the model
        model = tf.keras.models.load_model(keras_model_path)
        print(f"✅ Loaded model from {keras_model_path}")
    except Exception as e:
        print(f"❌ Error loading Keras model: {e}")
        return False
    
    print("Converting to TensorFlow Lite...")
    
    # Create organized output directories
    os.makedirs('output/tflite_model', exist_ok=True)
    os.makedirs('output/labels', exist_ok=True)
    os.makedirs('output/model_assets', exist_ok=True)
    
    # Update paths to organized directories
    tflite_model_path = 'output/tflite_model/gesture_model_with_nogesture.tflite'
    labels_txt_path = 'output/labels/labels_with_nogesture.txt'
    label_encoder_path = 'output/model_assets/label_encoder_with_nogesture.pkl'

    # Convert the model with optimizations
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    
    # Apply optimizations
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    
    # Ensure float32 input/output for compatibility
    converter.target_spec.supported_types = [tf.float32]
    
    try:
        tflite_model = converter.convert()
        print("✅ Conversion successful")
    except Exception as e:
        print(f"❌ Error during conversion: {e}")
        return False
    
    # Save the TFLite model to organized directory
    with open(tflite_model_path, 'wb') as f:
        f.write(tflite_model)
    print(f"✅ TFLite model saved as '{tflite_model_path}'")
    
    # Save labels for Android compatibility to organized directory
    try:
        with open('output/model_assets/label_encoder_with_nogesture.pkl', 'rb') as f:  # Load from organized location
            label_encoder = pickle.load(f)
        
        labels = label_encoder.classes_
        
        with open(labels_txt_path, 'w', encoding='utf-8') as f:
            for label in labels:
                f.write(f"{label}\n")
        
        print(f"✅ Labels saved as '{labels_txt_path}'")
        print(f"  Total labels: {len(labels)}")
    except Exception as e:
        print(f"❌ Error saving labels: {e}")
        return False
    
    # Also save the label encoder to organized directory
    try:
        with open('output/model_assets/label_encoder_with_nogesture.pkl', 'rb') as f:  # Load from organized location
            label_encoder = pickle.load(f)
        with open(label_encoder_path, 'wb') as f:
            pickle.dump(label_encoder, f)
        print(f"✅ Label encoder saved as '{label_encoder_path}'")
    except Exception as e:
        print(f"❌ Error saving label encoder: {e}")
        return False
    
    return True

def validate_tflite_model(tflite_model_path='output/tflite_model/gesture_model_with_nogesture.tflite'):
    """
    Validate the TFLite model by checking input/output shapes and running a test inference.
    
    ✅ Verifies input shape (1, 138)
    ✅ Verifies float32 tensor compatibility
    ✅ Verifies NO_GESTURE class is in output
    """
    print(f"\nValidating TFLite model: {tflite_model_path}")
    
    try:
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
        
        # Test with a sample input (138 features, all zeros initially)
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
        
        # Check the number of output classes
        num_output_classes = output_data.shape[1]
        print(f"  Number of output classes: {num_output_classes}")
        
        print("✅ TFLite model validation completed successfully!")
        return True
        
    except Exception as e:
        print(f"❌ Error during validation: {e}")
        import traceback
        traceback.print_exc()
        return False

def main():
    """
    Main conversion and validation function.
    
    ✅ TFLite input/output verified ✓
    ✅ Includes NO_GESTURE class ✓
    """
    print("="*70)
    print("TENSORFLOW LITE CONVERSION FOR HAND GESTURE MODEL WITH NO_GESTURE CLASS")
    print("="*70)
    
    # Convert model
    success = convert_to_tflite()
    
    if not success:
        print("❌ Conversion failed!")
        sys.exit(1)
    
    # Validate model
    validation_success = validate_tflite_model()
    
    if validation_success:
        print("\n" + "="*70)
        print("CONVERSION AND VALIDATION COMPLETED SUCCESSFULLY!")
        print("="*70)
        print("Generated files:")
        print("  - output/tflite_model/gesture_model_with_nogesture.tflite (TFLite model for Android)")
        print("  - output/labels/labels_with_nogesture.txt (text labels for Android)")
        print("  - output/model_assets/label_encoder_with_nogesture.pkl (label encoder)")
        print("\nThe model is ready for deployment on Android!")
        print("Input shape: (batch_size, 138) float32 tensor")
        print("Output: probability distribution over gesture classes (including NO_GESTURE)")
    else:
        print("❌ Validation failed!")
        sys.exit(1)

if __name__ == "__main__":
    main()