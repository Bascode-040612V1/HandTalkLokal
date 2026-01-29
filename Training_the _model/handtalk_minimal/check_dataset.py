import pandas as pd

# Load the dataset
df = pd.read_csv('data/gestures_bimanual.csv')

print("=== DATASET ANALYSIS ===")
print(f"Total samples: {len(df)}")
print(f"Number of features: {len(df.columns) - 1}")  # -1 for label column
print(f"Feature columns: {list(df.columns[:-1])}")
print()

print("Available gestures:")
gestures = sorted(df['label'].unique())
for i, gesture in enumerate(gestures):
    count = len(df[df['label'] == gesture])
    print(f"  {i+1}. {gesture} ({count} samples)")

print()
print("Current app labels (from assets/labels.txt):")
try:
    with open('../../app/src/main/assets/labels.txt', 'r') as f:
        app_labels = [line.strip() for line in f.readlines() if line.strip()]
    print(app_labels)
except:
    print("Could not read app labels file")

print()
print("Comparison:")
print("- Dataset gestures:", len(gestures))
print("- App labels:", len(app_labels))
print("- Matching gestures:", len(set(gestures) & set(app_labels)))
print("- Missing from app:", set(gestures) - set(app_labels))
print("- Extra in app:", set(app_labels) - set(gestures))