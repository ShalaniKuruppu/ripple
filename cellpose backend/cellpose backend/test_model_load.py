"""
Test if PyTorch can load the ddq_model file
"""
import os
import sys

print("Testing model loading...")
print(f"Python: {sys.version}")
print()

# Test 1: Check if file exists
model_path = r"C:\Users\user\Documents\FYP\ripple\ripple\cellpose backend\cellpose backend\models\Cellpose 3.1\ddq_model"
print(f"Model path: {model_path}")
print(f"File exists: {os.path.exists(model_path)}")
print(f"File size: {os.path.getsize(model_path) if os.path.exists(model_path) else 'N/A'} bytes")
print()

# Test 2: Try to load with PyTorch
try:
    import torch
    print(f"PyTorch version: {torch.__version__}")
    print("Attempting to load model with torch.load()...")
    
    # Try loading
    state_dict = torch.load(model_path, map_location=torch.device("cpu"))
    print(f"✓ SUCCESS! Model loaded successfully")
    print(f"  Keys in state dict: {list(state_dict.keys())[:5]}...")  # Show first 5 keys
    
except Exception as e:
    print(f"✗ FAILED: {e}")
    import traceback
    traceback.print_exc()
    print()
    
    # Test 3: Try with forward slashes
    print("Trying with forward slashes...")
    model_path_forward = model_path.replace('\\', '/')
    print(f"Path: {model_path_forward}")
    try:
        state_dict = torch.load(model_path_forward, map_location=torch.device("cpu"))
        print(f"✓ SUCCESS with forward slashes!")
    except Exception as e2:
        print(f"✗ Also failed: {e2}")
        print()
        
        # Test 4: Try opening as binary file first
        print("Trying to open as binary file...")
        try:
            with open(model_path, 'rb') as f:
                print(f"✓ Can open file for reading")
                # Read first few bytes
                header = f.read(20)
                print(f"  First 20 bytes: {header}")
                # Try loading from file handle
                f.seek(0)
                state_dict = torch.load(f, map_location=torch.device("cpu"))
                print(f"✓ SUCCESS loading from file handle!")
        except Exception as e3:
            print(f"✗ Failed: {e3}")
            import traceback
            traceback.print_exc()
