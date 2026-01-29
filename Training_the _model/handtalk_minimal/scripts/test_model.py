
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
