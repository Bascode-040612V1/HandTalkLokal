import numpy as np
import joblib
import sys

# Load the model
print("Loading model...")
model = joblib.load('c:\\Users\\obuma\\Documents\\0001_HandTalk_Local\\App_Development\\Hand_Talk_Lokal\\Training_the _model\\handtalk_minimal\\data\\gesture_model.pkl')
print(f"Model loaded successfully. Number of classes: {len(model.classes_)}")
print(f"Classes: {model.classes_}")

# Test with random input
print("\nTesting with random input...")
test_input = np.random.rand(1, 138).astype(np.float32)
prediction = model.predict_proba(test_input)
print(f"Prediction shape: {prediction.shape}")
print(f"Probabilities (first 5): {prediction[0][:5]}")
print(f"Max probability: {np.max(prediction[0])}")
print(f"Predicted class: {model.classes_[np.argmax(prediction[0])]}")

# Test with zeros (no hands detected)
print("\nTesting with zeros (no hands)...")
test_input_zeros = np.zeros((1, 138), dtype=np.float32)
prediction_zeros = model.predict_proba(test_input_zeros)
print(f"Prediction shape: {prediction_zeros.shape}")
print(f"Max probability: {np.max(prediction_zeros[0])}")
print(f"Predicted class: {model.classes_[np.argmax(prediction_zeros[0])]}")

# Test with ones (all features max)
print("\nTesting with ones (all features max)...")
test_input_ones = np.ones((1, 138), dtype=np.float32)
prediction_ones = model.predict_proba(test_input_ones)
print(f"Prediction shape: {prediction_ones.shape}")
print(f"Max probability: {np.max(prediction_ones[0])}")
print(f"Predicted class: {model.classes_[np.argmax(prediction_ones[0])]}")

print("\nTest completed.")