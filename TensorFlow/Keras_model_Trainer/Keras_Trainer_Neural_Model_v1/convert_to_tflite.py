import tensorflow as tf
import numpy as np
import pickle

def convert_model_to_tflite(keras_model_path='improved_gesture_model.h5', 
                          tflite_model_path='gesture_model.tflite',
                          label_encoder_path='label_encoder.pkl',
                          labels_txt_path='labels.txt'):
    """
    Convert the trained Keras model to TensorFlow Lite format
    """
    print("Loading Keras model...")
    model = tf.keras.models.load_model(keras_model_path)
    
    print("Converting to TensorFlow Lite...")
    
    # Convert the model
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    
    # Optimize the model
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    
    # Optionally, you can specify representative dataset for quantization
    def representative_data_gen():
        # Generate dummy data with the same shape as your input
        for _ in range(100):
            yield [np.random.random((1, 138)).astype(np.float32)]  # 138 features
    
    # Uncomment the next line if you want to use quantization
    # converter.representative_dataset = representative_data_gen
    
    # Ensure the model uses float32 inputs/outputs for better compatibility
    converter.target_spec.supported_types = [tf.float32]
    
    tflite_model = converter.convert()
    
    # Save the TFLite model
    with open(tflite_model_path, 'wb') as f:
        f.write(tflite_model)
    
    print(f"TFLite model saved as '{tflite_model_path}'")
    
    # Load label encoder and save labels to txt file
    print("Saving labels...")
    with open(label_encoder_path, 'rb') as f:
        label_encoder = pickle.load(f)
    
    labels = label_encoder.classes_
    
    with open(labels_txt_path, 'w', encoding='utf-8') as f:
        for label in labels:
            f.write(f"{label}\n")
    
    print(f"Labels saved as '{labels_txt_path}'")
    print(f"Total labels: {len(labels)}")
    print(f"Labels: {list(labels)}")

def validate_tflite_model(tflite_model_path='gesture_model.tflite'):
    """
    Validate the TFLite model by running a sample inference
    """
    print("Validating TFLite model...")
    
    # Load TFLite model and allocate tensors
    interpreter = tf.lite.Interpreter(model_path=tflite_model_path)
    interpreter.allocate_tensors()
    
    # Get input and output tensors
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    
    print(f"Input shape: {input_details[0]['shape']}")
    print(f"Output shape: {output_details[0]['shape']}")
    
    # Test with a sample input
    input_shape = input_details[0]['shape']
    input_data = np.random.random(input_shape).astype(np.float32)
    
    interpreter.set_tensor(input_details[0]['index'], input_data)
    interpreter.invoke()
    
    # Get the output
    output_data = interpreter.get_tensor(output_details[0]['index'])
    print(f"Sample output shape: {output_data.shape}")
    print(f"Sample output (first 5 values): {output_data[0][:5]}")
    
    # Check that output sums to approximately 1 (softmax)
    output_sum = np.sum(output_data[0])
    print(f"Output sum (should be ~1.0): {output_sum:.4f}")
    
    print("TFLite model validation completed successfully!")

def main():
    """
    Main function to convert model to TFLite and validate
    """
    try:
        convert_model_to_tflite()
        validate_tflite_model()
        print("\nConversion and validation completed successfully!")
        print("\nTo use in Android app:")
        print("1. Copy 'gesture_model.tflite' to app/src/main/assets/")
        print("2. Copy 'labels.txt' to app/src/main/assets/")
        print("3. The model is now ready to be used in the Android app")
    except Exception as e:
        print(f"Error during conversion: {str(e)}")

if __name__ == "__main__":
    main()