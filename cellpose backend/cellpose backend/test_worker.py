"""
Test script to diagnose worker.py issues
"""
import sys
import os
import json

print("=" * 60)
print("CELLPOSE WORKER DIAGNOSTIC SCRIPT")
print("=" * 60)

# 1. Check Python version
print(f"\n1. Python Version: {sys.version}")

# 2. Check current working directory
print(f"\n2. Current Working Directory: {os.getcwd()}")

# 3. Check script directory
script_dir = os.path.dirname(os.path.abspath(__file__))
print(f"\n3. Script Directory: {script_dir}")

# 4. Check if required packages are installed
print("\n4. Checking Required Packages:")
required_packages = ['cellpose', 'cv2', 'numpy', 'PIL']
for pkg in required_packages:
    try:
        if pkg == 'cv2':
            import cv2
            print(f"   ✓ opencv-python (cv2): {cv2.__version__}")
        elif pkg == 'cellpose':
            import cellpose
            print(f"   ✓ cellpose: installed")
        elif pkg == 'numpy':
            import numpy as np
            print(f"   ✓ numpy: {np.__version__}")
        elif pkg == 'PIL':
            from PIL import Image
            print(f"   ✓ Pillow (PIL): {Image.__version__}")
    except ImportError as e:
        print(f"   ✗ {pkg}: NOT INSTALLED - {e}")

# 5. Check models directory
models_dir = os.path.join(script_dir, "models")
print(f"\n5. Models Directory: {models_dir}")
print(f"   Exists: {os.path.exists(models_dir)}")
if os.path.exists(models_dir):
    print(f"   Contents: {os.listdir(models_dir)}")

# 6. Test image reading with a dummy file
print("\n6. Testing Image I/O:")
try:
    import tempfile
    import numpy as np
    import cv2
    from PIL import Image
    
    # Create a test image
    test_array = np.random.randint(0, 255, (100, 100, 3), dtype=np.uint8)
    
    # Save with PIL
    pil_img = Image.fromarray(test_array)
    temp_file = tempfile.NamedTemporaryFile(suffix='.png', delete=False)
    temp_path = temp_file.name
    temp_file.close()
    
    pil_img.save(temp_path)
    print(f"   ✓ Saved test image to: {temp_path}")
    
    # Try to read with cv2
    img = cv2.imread(temp_path)
    if img is not None:
        print(f"   ✓ Successfully read image with cv2: shape {img.shape}")
    else:
        print(f"   ✗ Failed to read image with cv2")
    
    # Clean up
    os.unlink(temp_path)
    print(f"   ✓ Cleaned up test file")
    
except Exception as e:
    print(f"   ✗ Error during image test: {e}")
    import traceback
    traceback.print_exc()

# 7. Test command-line argument parsing
print("\n7. Testing Command-Line Argument Parsing:")
print(f"   sys.argv: {sys.argv}")

# 8. Check if we can import and use Cellpose models
print("\n8. Testing Cellpose Model Import:")
try:
    from cellpose import models
    print("   ✓ Successfully imported cellpose.models")
    
    # Try to create a model instance (this might download the model)
    print("   Attempting to create Cellpose model (may download ~200MB)...")
    print("   NOTE: This is normal for first-time setup")
    
except Exception as e:
    print(f"   ✗ Error importing/using Cellpose: {e}")
    import traceback
    traceback.print_exc()

print("\n" + "=" * 60)
print("DIAGNOSTIC COMPLETE")
print("=" * 60)
