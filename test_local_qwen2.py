"""
Test loading local Qwen-Image FP8 - attempt 2 with config specification.
"""

import torch
from pathlib import Path
from safetensors.torch import load_file

MODEL_DIR = Path(r"D:\Models\qwen_image_fp8")

print("Testing direct safetensors loading...")

# Check file structure
transformer_path = MODEL_DIR / "qwen_image_2512_fp8_e4m3fn.safetensors"
print(f"\nLoading weights from: {transformer_path}")
print("This may take a moment for 19GB file...")

# Load with safetensors
state_dict = load_file(str(transformer_path))

print(f"\nLoaded {len(state_dict)} tensors")
print("\nFirst 20 key names:")
for i, key in enumerate(list(state_dict.keys())[:20]):
    shape = state_dict[key].shape
    dtype = state_dict[key].dtype
    print(f"  {key}: {shape} ({dtype})")

print("\n...checking for model structure clues...")
# Look for patterns
prefixes = set()
for key in state_dict.keys():
    parts = key.split('.')
    if len(parts) > 1:
        prefixes.add(parts[0])

print(f"\nTop-level prefixes: {prefixes}")
