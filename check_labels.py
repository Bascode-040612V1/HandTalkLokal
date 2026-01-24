import pandas as pd

# Read the CSV file
df = pd.read_csv(r'c:\Users\obuma\Documents\0001_HandTalk_Local\App_Development\Hand_Talk_Lokal\Training_the _model\handtalk_minimal\data\gestures_bimanual.csv')

# Get unique labels
unique_labels = df['label'].unique()
print("Unique labels in training data:")
for label in unique_labels:
    count = len(df[df['label'] == label])
    print(f"  {label}: {count} samples")

print(f"\nTotal samples: {len(df)}")
print(f"Total unique labels: {len(unique_labels)}")