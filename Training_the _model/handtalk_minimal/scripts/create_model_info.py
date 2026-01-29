import joblib
import numpy as np
import os

def create_model_info():
    """Create model information files for Android app"""
    print("Loading model and scaler...")
    
    # Load the trained model and scaler
    model = joblib.load("data/gesture_model.pkl")
    scaler = joblib.load("data/scaler.pkl")
    
    print("Model information:")
    print(f"Number of classes: {len(model.classes_)}")
    print(f"Classes: {list(model.classes_)}")
    print(f"Number of features: {len(model.feature_importances_)}")
    
    # Save class labels to a text file
    labels_path = "data/labels.txt"
    with open(labels_path, 'w') as f:
        for i, label in enumerate(model.classes_):
            f.write(f"{label}\n")
    
    print(f"Labels saved to: {labels_path}")
    
    # Save model information
    info_path = "data/model_info.txt"
    with open(info_path, 'w') as f:
        f.write("Gesture Recognition Model Information\n")
        f.write("====================================\n\n")
        f.write(f"Number of classes: {len(model.classes_)}\n")
        f.write(f"Number of features: {len(model.feature_importances_)}\n")
        f.write(f"Model type: Random Forest\n")
        f.write(f"Number of trees: {model.n_estimators}\n\n")
        f.write("Classes:\n")
        for i, label in enumerate(model.classes_):
            f.write(f"  {i}: {label}\n")
    
    print(f"Model info saved to: {info_path}")
    
    # Create a simple test script to verify the model works
    test_script_path = "scripts/test_model.py"
    with open(test_script_path, 'w') as f:
        f.write('''
import joblib
import numpy as np

def test_model():
    # Load model and scaler
    model = joblib.load("data/gesture_model.pkl")
    scaler = joblib.load("data/scaler.pkl")
    
    # Create a sample input (138 features)
    sample_input = np.random.rand(1, 138)
    
    # Scale the input
    sample_input_scaled = scaler.transform(sample_input)
    
    # Make prediction
    prediction = model.predict(sample_input_scaled)
    probabilities = model.predict_proba(sample_input_scaled)
    
    print(f"Predicted class: {prediction[0]}")
    print(f"Class probabilities: {probabilities[0]}")
    print("Model test completed successfully!")

if __name__ == "__main__":
    test_model()
''')
    
    print(f"Test script created at: {test_script_path}")
    
    print("\nNext steps for Android integration:")
    print("1. The model is currently in scikit-learn format (.pkl files)")
    print("2. For Android, you have two options:")
    print("   a) Use the scikit-learn model with a Python backend (requires Python on Android)")
    print("   b) Convert to TensorFlow Lite (requires retraining in TensorFlow)")
    print("3. For option (b), you would need to:")
    print("   - Retrain your model using TensorFlow/Keras")
    print("   - Convert the TensorFlow model to TFLite format")
    print("   - Update your Android app to use the TFLite model")

if __name__ == "__main__":
    create_model_info()