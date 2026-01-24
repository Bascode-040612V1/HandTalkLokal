import pandas as pd

# Read the CSV file
df = pd.read_csv(r'Training_the _model/handtalk_minimal/data/gestures_bimanual.csv')

# Get unique labels and sort them
labels = sorted(list(set(df['label'])))
print('Sorted labels:')
for i, label in enumerate(labels):
    print(f'{i}: {label}')

# Also check the current app labels order
print('\nCurrent app labels.txt order:')
with open(r'app/src/main/assets/labels.txt', 'r') as f:
    app_labels = [line.strip() for line in f.readlines()]
    for i, label in enumerate(app_labels):
        print(f'{i}: {label}')