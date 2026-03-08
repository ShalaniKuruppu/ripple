"""
Check if ddq_model is a valid ZIP/PyTorch file
"""
import zipfile
import os

model_path = r"C:\Users\user\Documents\FYP\ripple\ripple\cellpose backend\cellpose backend\models\Cellpose 3.1\ddq_model"

print(f"File: {model_path}")
print(f"Size: {os.path.getsize(model_path)} bytes ({os.path.getsize(model_path)/1024:.2f} KB)")
print()

# Test if it's a valid ZIP file
print("Testing if it's a valid ZIP file...")
try:
    with zipfile.ZipFile(model_path, 'r') as z:
        print("✓ Valid ZIP file!")
        print(f"  Files in archive: {len(z.namelist())}")
        print(f"  File list:")
        for name in z.namelist():
            info = z.getinfo(name)
            print(f"    - {name} ({info.file_size} bytes)")
except zipfile.BadZipFile as e:
    print(f"✗ NOT a valid ZIP file: {e}")
    print()
    print("This might be a text file or corrupted. Let's check the contents...")
    with open(model_path, 'rb') as f:
        content = f.read()
        print(f"First 200 bytes:")
        print(content[:200])
        print()
        if content.startswith(b'http'):
            print("⚠️ This looks like a URL or text file, not a model!")
        try:
            text = content.decode('utf-8', errors='replace')
            print("File as text:")
            print(text[:500])
        except:
            print("Cannot decode as text")
except Exception as e:
    print(f"✗ Error: {e}")
    import traceback
    traceback.print_exc()
