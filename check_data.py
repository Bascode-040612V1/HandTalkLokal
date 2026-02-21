import pandas as pd

# Read the CSV file
df = pd.read_csv('Training_the _model/handtalk_minimal/data/gestures_bimanual.csv')

print(f"Total samples: {len(df)}")
print("\nSamples per label:")
label_counts = df['label'].value_counts()
print(label_counts)

print("\nLabels in CSV:")
print(sorted(df['label'].unique()))

print("\nLabels in assets/labels.txt:")
with open('app/src/main/assets/labels.txt', 'r') as f:
    asset_labels = [line.strip() for line in f.readlines()]
    print(sorted(asset_labels))