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
import matplotlib.pyplot as plt
import seaborn as sns
from sklearn.metrics import classification_report, confusion_matrix
import warnings
warnings.filterwarnings('ignore')

def load_and_preprocess_data(data_path='sign_language_data.csv'):
    """
    Load and preprocess the gesture data with quality checks
    """
    print("Loading dataset...")
    df = pd.read_csv(data_path)
    
    print(f"Dataset shape: {df.shape}")
    print(f"Columns: {df.columns.tolist()}")
    
    # Identify feature columns (exclude label)
    feature_cols = [col for col in df.columns if col != 'label']
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
    
    return X, y_categorical, label_encoder, num_classes

def build_improved_model(input_dim, num_classes):
    """
    Build an improved neural network model for gesture recognition
    """
    model = Sequential([
        # Input layer
        Dense(512, activation='relu', input_shape=(input_dim,)),
        BatchNormalization(),
        Dropout(0.3),
        
        # Hidden layers
        Dense(256, activation='relu'),
        BatchNormalization(),
        Dropout(0.3),
        
        Dense(128, activation='relu'),
        BatchNormalization(),
        Dropout(0.2),
        
        Dense(64, activation='relu'),
        BatchNormalization(),
        Dropout(0.2),
        
        # Output layer
        Dense(num_classes, activation='softmax')
    ])
    
    # Compile with focal loss to handle class imbalance
    model.compile(
        optimizer=Adam(learning_rate=0.001),
        loss=CategoricalCrossentropy(label_smoothing=0.1),  # Label smoothing
        metrics=['accuracy']
    )
    
    return model

def train_model_with_validation(X, y_categorical, num_classes, label_encoder):
    """
    Train the model with proper validation and callbacks
    """
    # Split data
    X_train, X_temp, y_train, y_temp = train_test_split(
        X, y_categorical, test_size=0.3, random_state=42, stratify=y_categorical.argmax(axis=1)
    )
    
    X_val, X_test, y_val, y_test = train_test_split(
        X_temp, y_temp, test_size=0.5, random_state=42, stratify=y_temp.argmax(axis=1)
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
    model = build_improved_model(X_train.shape[1], num_classes)
    print(model.summary())
    
    # Callbacks
    early_stopping = EarlyStopping(
        monitor='val_loss',
        patience=20,
        restore_best_weights=True
    )
    
    reduce_lr = ReduceLROnPlateau(
        monitor='val_loss',
        factor=0.5,
        patience=10,
        min_lr=1e-7
    )
    
    # Train model
    history = model.fit(
        X_train, y_train,
        validation_data=(X_val, y_val),
        epochs=100,
        batch_size=32,
        class_weight=class_weight_dict,
        callbacks=[early_stopping, reduce_lr],
        verbose=1
    )
    
    # Evaluate on test set
    test_loss, test_accuracy = model.evaluate(X_test, y_test, verbose=0)
    print(f"Test Accuracy: {test_accuracy:.4f}")
    
    # Detailed evaluation
    y_pred = model.predict(X_test)
    y_pred_classes = np.argmax(y_pred, axis=1)
    y_true_classes = np.argmax(y_test, axis=1)
    
    # Classification report
    print("\nClassification Report:")
    print(classification_report(y_true_classes, y_pred_classes, 
                              target_names=label_encoder.classes_))
    
    # Confusion matrix
    cm = confusion_matrix(y_true_classes, y_pred_classes)
    
    # Plot confusion matrix
    plt.figure(figsize=(12, 10))
    sns.heatmap(cm, annot=True, fmt='d', cmap='Blues',
                xticklabels=label_encoder.classes_,
                yticklabels=label_encoder.classes_)
    plt.title('Confusion Matrix')
    plt.xlabel('Predicted Label')
    plt.ylabel('True Label')
    plt.xticks(rotation=45)
    plt.yticks(rotation=0)
    plt.tight_layout()
    plt.savefig('confusion_matrix.png', dpi=300, bbox_inches='tight')
    plt.show()
    
    # Identify problematic gesture pairs
    print("\nAnalyzing problematic gesture pairs:")
    for i in range(len(cm)):
        for j in range(len(cm[0])):
            if i != j and cm[i][j] > 0:  # Misclassified pairs
                true_label = label_encoder.classes_[i]
                pred_label = label_encoder.classes_[j]
                count = cm[i][j]
                print(f"  {true_label} -> {pred_label}: {count} misclassifications")
    
    return model, history

def plot_training_history(history):
    """
    Plot training history
    """
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(12, 4))
    
    # Plot accuracy
    ax1.plot(history.history['accuracy'], label='Training Accuracy')
    ax1.plot(history.history['val_accuracy'], label='Validation Accuracy')
    ax1.set_title('Model Accuracy')
    ax1.set_xlabel('Epoch')
    ax1.set_ylabel('Accuracy')
    ax1.legend()
    
    # Plot loss
    ax2.plot(history.history['loss'], label='Training Loss')
    ax2.plot(history.history['val_loss'], label='Validation Loss')
    ax2.set_title('Model Loss')
    ax2.set_xlabel('Epoch')
    ax2.set_ylabel('Loss')
    ax2.legend()
    
    plt.tight_layout()
    plt.savefig('training_history.png', dpi=300, bbox_inches='tight')
    plt.show()

def main():
    """
    Main function to run the complete training pipeline
    """
    # Load and preprocess data
    X, y_categorical, label_encoder, num_classes = load_and_preprocess_data()
    
    # Train model
    model, history = train_model_with_validation(X, y_categorical, num_classes, label_encoder)
    
    # Plot training history
    plot_training_history(history)
    
    # Save the model
    model.save('improved_gesture_model.h5')
    print("Model saved as 'improved_gesture_model.h5'")
    
    # Save label encoder classes
    import pickle
    with open('label_encoder.pkl', 'wb') as f:
        pickle.dump(label_encoder, f)
    print("Label encoder saved as 'label_encoder.pkl'")
    
    print("\nTraining completed successfully!")
    print("Next steps:")
    print("1. Convert the saved model to TensorFlow Lite format")
    print("2. Test the model on the Android app")
    print("3. Monitor for any remaining misclassifications")

if __name__ == "__main__":
    main()