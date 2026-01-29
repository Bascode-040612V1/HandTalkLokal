"""
Quick TFLite conversion script for the trained model with NO_GESTURE class.
"""

import tensorflow as tf
import numpy as np
import pickle
import os

def convert_to_tflite():
    # Load the trained model
    model = tf.keras.models.load_model('output/trained_models/gesture_model_quick.keras')
    
    # Convert to TFLite
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_types = [tf.float32]
    
    tflite_model = converter.convert()
    
    # Save TFLite model
    os.makedirs('../../app/src/main/assets', exist_ok=True)
    with open('../../app/src/main/assets/gesture_model.tflite', 'wb') as f:
        f.write(tflite_model)
    
    print("✅ TFLite model saved to app/src/main/assets/gesture_model.tflite")
    
    # Copy labels to app assets
    with open('output/model_assets/labels_quick.txt', 'r') as src:
        labels_content = src.read()
    
    with open('../../app/src/main/assets/labels.txt', 'w') as dst:
        dst.write(labels_content)
    
    print("✅ Labels copied to app/src/main/assets/labels.txt")
    
    # Validate the model
    interpreter = tf.lite.Interpreter(model_content=tflite_model)
    interpreter.allocate_tensors()
    
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    
    print(f"Input shape: {input_details[0]['shape']}")
    print(f"Output shape: {output_details[0]['shape']}")
    
    # Test inference
    test_input = np.random.random((1, 138)).astype(np.float32)
    interpreter.set_tensor(input_details[0]['index'], test_input)
    interpreter.invoke()
    output = interpreter.get_tensor(output_details[0]['index'])
    
    print(f"Test output shape: {output.shape}")
    print(f"Number of classes: {output.shape[1]}")
    print(f"Output sum: {np.sum(output[0]):.4f}")
    
    print("✅ Model validation successful!")

if __name__ == "__main__":
    convert_to_tflite()