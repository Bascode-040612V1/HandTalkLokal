import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from sklearn.decomposition import PCA
from sklearn.manifold import TSNE
from sklearn.preprocessing import StandardScaler
import warnings
warnings.filterwarnings('ignore')

def load_and_validate_dataset(data_path='sign_language_data.csv'):
    """
    Load and validate the gesture dataset
    """
    print("Loading dataset...")
    df = pd.read_csv(data_path)
    
    print(f"Dataset shape: {df.shape}")
    
    # Identify feature columns and label
    feature_cols = [col for col in df.columns if col != 'label']
    label_col = 'label'
    
    X = df[feature_cols].values.astype(np.float32)
    y = df[label_col].values
    
    print(f"Feature columns: {len(feature_cols)}")
    print(f"Labels: {len(np.unique(y))} classes")
    print(f"Classes: {sorted(np.unique(y))}")
    
    # Basic statistics
    print(f"\nBasic Statistics:")
    print(f"Mean values range: [{X.mean(axis=0).min():.4f}, {X.mean(axis=0).max():.4f}]")
    print(f"Std values range: [{X.std(axis=0).min():.4f}, {X.std(axis=0).max():.4f}]")
    print(f"Min values range: [{X.min(axis=0).min():.4f}, {X.min(axis=0).max():.4f}]")
    print(f"Max values range: [{X.max(axis=0).min():.4f}, {X.max(axis=0).max():.4f}]")
    
    # Check for anomalies
    print(f"\nChecking for anomalies...")
    print(f"NaN values: {np.isnan(X).sum()}")
    print(f"Infinite values: {np.isinf(X).sum()}")
    
    # Check for extreme values
    extreme_mask = np.abs(X) > 10  # Values that are unusually large
    extreme_count = extreme_mask.sum()
    print(f"Extreme values (>10): {extreme_count}")
    
    return df, X, y, feature_cols

def visualize_class_distribution(y):
    """
    Visualize class distribution
    """
    unique_labels, counts = np.unique(y, return_counts=True)
    
    plt.figure(figsize=(12, 6))
    bars = plt.bar(range(len(unique_labels)), counts)
    plt.title('Class Distribution in Dataset')
    plt.xlabel('Class Index')
    plt.ylabel('Sample Count')
    plt.xticks(range(len(unique_labels)), unique_labels, rotation=45)
    
    # Add value labels on bars
    for bar, count in zip(bars, counts):
        plt.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 0.1, 
                 str(count), ha='center', va='bottom')
    
    plt.tight_layout()
    plt.savefig('class_distribution.png', dpi=300, bbox_inches='tight')
    plt.show()
    
    print(f"\nClass balance analysis:")
    min_count = counts.min()
    max_count = counts.max()
    avg_count = counts.mean()
    
    print(f"Minimum samples per class: {min_count}")
    print(f"Maximum samples per class: {max_count}")
    print(f"Average samples per class: {avg_count:.1f}")
    print(f"Balance ratio: {min_count/max_count:.2f}")

def visualize_feature_correlations(X, feature_cols):
    """
    Visualize feature correlations to identify redundant features
    """
    # Sample a subset for correlation analysis (too many features to visualize all)
    n_samples = min(1000, X.shape[0])  # Use at most 1000 samples
    sample_indices = np.random.choice(X.shape[0], n_samples, replace=False)
    X_sample = X[sample_indices]
    
    # Calculate correlations for a subset of features
    n_features_to_analyze = min(50, X.shape[1])  # Analyze first 50 features
    X_subset = X_sample[:, :n_features_to_analyze]
    
    corr_matrix = np.corrcoef(X_subset.T)
    
    plt.figure(figsize=(12, 10))
    mask = np.triu(np.ones_like(corr_matrix, dtype=bool))
    sns.heatmap(corr_matrix, mask=mask, cmap='coolwarm', center=0,
                square=True, fmt='.2f', cbar_kws={"shrink": .8})
    plt.title(f'Correlation Matrix (First {n_features_to_analyze} Features)')
    plt.tight_layout()
    plt.savefig('feature_correlations.png', dpi=300, bbox_inches='tight')
    plt.show()

def visualize_clusters(X, y):
    """
    Use PCA and t-SNE to visualize data clusters
    """
    # Standardize the data
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)
    
    # PCA visualization
    pca = PCA(n_components=2)
    X_pca = pca.fit_transform(X_scaled)
    
    plt.figure(figsize=(12, 5))
    
    plt.subplot(1, 2, 1)
    scatter = plt.scatter(X_pca[:, 0], X_pca[:, 1], c=pd.Categorical(y).codes, cmap='tab10', alpha=0.6)
    plt.title(f'PCA Visualization\nExplained Variance Ratio: {pca.explained_variance_ratio_.sum():.3f}')
    plt.xlabel('PC1')
    plt.ylabel('PC2')
    plt.colorbar(scatter)
    
    # t-SNE visualization (sample for performance)
    n_samples_tsne = min(1000, X_scaled.shape[0])
    indices_tsne = np.random.choice(X_scaled.shape[0], n_samples_tsne, replace=False)
    
    X_tsne_scaled = X_scaled[indices_tsne]
    y_tsne = y[indices_tsne]
    
    tsne = TSNE(n_components=2, random_state=42, perplexity=30)
    X_tsne = tsne.fit_transform(X_tsne_scaled)
    
    plt.subplot(1, 2, 2)
    scatter = plt.scatter(X_tsne[:, 0], X_tsne[:, 1], c=pd.Categorical(y_tsne).codes, cmap='tab10', alpha=0.6)
    plt.title('t-SNE Visualization (Sampled)')
    plt.xlabel('Component 1')
    plt.ylabel('Component 2')
    plt.colorbar(scatter)
    
    plt.tight_layout()
    plt.savefig('data_clusters.png', dpi=300, bbox_inches='tight')
    plt.show()

def identify_problematic_samples(df, X, y, feature_cols, threshold=2.5):
    """
    Identify potentially mislabeled samples using statistical outliers
    """
    print(f"\nIdentifying potentially problematic samples...")
    
    # Calculate mean and std for each class
    unique_labels = np.unique(y)
    outlier_indices = []
    
    for label in unique_labels:
        class_mask = (y == label)
        class_data = X[class_mask]
        
        if len(class_data) < 2:  # Skip classes with too few samples
            continue
            
        # Calculate mean and std for this class
        class_mean = np.mean(class_data, axis=0)
        class_std = np.std(class_data, axis=0)
        
        # Avoid division by zero
        class_std = np.where(class_std == 0, 1e-8, class_std)
        
        # Calculate z-scores for samples in this class
        class_samples = X[y == label]
        z_scores = np.abs((class_samples - class_mean) / class_std)
        
        # Find samples with high average z-score
        avg_z_scores = np.mean(z_scores, axis=1)
        class_indices = np.where(y == label)[0]
        
        for i, avg_z in enumerate(avg_z_scores):
            if avg_z > threshold:
                outlier_indices.append(class_indices[i])
    
    print(f"Found {len(outlier_indices)} potentially problematic samples")
    
    if len(outlier_indices) > 0:
        outlier_df = df.iloc[outlier_indices]
        print("\nSample of potentially problematic entries:")
        print(outlier_df.head(10))
        
        # Save problematic samples
        outlier_df.to_csv('problematic_samples.csv', index=False)
        print(f"\nPotentially problematic samples saved to 'problematic_samples.csv'")
    
    return outlier_indices

def main():
    """
    Main function to run dataset validation
    """
    try:
        df, X, y, feature_cols = load_and_validate_dataset()
        
        # Run validations
        visualize_class_distribution(y)
        visualize_feature_correlations(X, feature_cols)
        visualize_clusters(X, y)
        identify_problematic_samples(df, X, y, feature_cols)
        
        print("\nDataset validation completed!")
        print("Check the generated plots and reports for insights:")
        print("- class_distribution.png: Shows how balanced your classes are")
        print("- feature_correlations.png: Shows correlations between features")
        print("- data_clusters.png: Shows how data clusters in 2D space")
        print("- problematic_samples.csv: Samples that might be mislabeled")
        
        print(f"\nRecommendations based on validation:")
        print("1. If classes are imbalanced, consider collecting more data for underrepresented classes")
        print("2. If there are many outliers, review and correct potentially mislabeled samples")
        print("3. If clusters overlap significantly, consider collecting more distinctive gestures")
        print("4. If feature correlations are too high, consider feature selection techniques")
        
    except Exception as e:
        print(f"Error during validation: {str(e)}")

if __name__ == "__main__":
    main()