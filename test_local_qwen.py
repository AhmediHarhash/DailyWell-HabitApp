"""
Test loading local Qwen-Image FP8 safetensors with diffusers.
No downloads - uses D:\Models\qwen_image_fp8 files only.
"""

import torch
from pathlib import Path

# Local model files
MODEL_DIR = Path(r"D:\Models\qwen_image_fp8")
TEXT_ENCODER_DIR = Path(r"D:\Models\text-encoders\split_files\text_encoders")

print("=" * 60)
print("Testing Local Qwen-Image FP8 Loading")
print("=" * 60)

# List available files
print("\nLocal model files:")
for f in MODEL_DIR.iterdir():
    size_mb = f.stat().st_size / (1024*1024)
    print(f"  {f.name}: {size_mb:.1f} MB")

print(f"\nText encoder files:")
for f in TEXT_ENCODER_DIR.iterdir():
    size_mb = f.stat().st_size / (1024*1024)
    print(f"  {f.name}: {size_mb:.1f} MB")

# Try loading transformer
print("\n" + "=" * 60)
print("Attempting to load QwenImageTransformer2DModel from single file...")
print("=" * 60)

try:
    from diffusers.models import QwenImageTransformer2DModel

    transformer_path = MODEL_DIR / "qwen_image_2512_fp8_e4m3fn.safetensors"
    print(f"\nLoading: {transformer_path}")

    transformer = QwenImageTransformer2DModel.from_single_file(
        str(transformer_path),
        torch_dtype=torch.bfloat16
    )
    print("SUCCESS: Transformer loaded!")
    print(f"  Parameters: {sum(p.numel() for p in transformer.parameters()):,}")

except Exception as e:
    print(f"FAILED: {type(e).__name__}: {e}")

# Try loading VAE
print("\n" + "=" * 60)
print("Attempting to load AutoencoderKLQwenImage from single file...")
print("=" * 60)

try:
    from diffusers.models import AutoencoderKLQwenImage

    vae_path = MODEL_DIR / "qwen_image_vae.safetensors"
    print(f"\nLoading: {vae_path}")

    vae = AutoencoderKLQwenImage.from_single_file(
        str(vae_path),
        torch_dtype=torch.bfloat16
    )
    print("SUCCESS: VAE loaded!")

except Exception as e:
    print(f"FAILED: {type(e).__name__}: {e}")

print("\n" + "=" * 60)
print("Test complete")
print("=" * 60)
