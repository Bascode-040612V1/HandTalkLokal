import tensorflow as tf

# Load the Keras model
print("Loading Keras model...")
model = tf.keras.models.load_model('improved_gesture_model.h5')

# Convert to TensorFlow Lite
print("Converting to TensorFlow Lite...")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

# Save the TFLite model
with open('gesture_model.tflite', 'wb') as f:
    f.write(tflite_model)

print("TFLite model converted successfully!")
print("Model size:", len(tflite_model), "bytes")