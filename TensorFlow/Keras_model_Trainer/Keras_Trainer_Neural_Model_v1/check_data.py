import pandas as pd

# Load the data
df = pd.read_csv('../../../Training_the _model/handtalk_minimal/data/gestures_bimanual.csv')

print(f"Data shape: {df.shape}")
print(f"Number of features: {len([col for col in df.columns if col != 'label'])}")
print(f"Classes: {sorted(df['label'].unique())}")
print(f"\nSamples per class:")
print(df['label'].value_counts().sort_index())
print(f"\nTotal samples: {len(df)}")