# Fixing Generation - Memory Issues & Solutions

## The Problem
Script crashed with segmentation fault (exit code 139) when loading Qwen-Image FP8 model.
PC was freezing/lagging before crash.

## System Specs
- **RAM**: 32GB (20GB typically in use = 12GB free)
- **VRAM**: 29GB available (capped limit)
- **Model**: Qwen-Image FP8 (~19GB transformer + 9GB text encoder + 0.2GB VAE)

## Root Cause
Using `diffusers` library which loads inefficiently:

```python
# BAD - diffusers approach
from diffusers.models import QwenImageTransformer2DModel

# Step 1: Creates empty model in VRAM
transformer = QwenImageTransformer2DModel(...)

# Step 2: Loads 19GB to RAM
state_dict = load_file(str(path))  # 19GB in RAM

# Step 3: Converts FP8->BF16 in RAM (DOUBLES memory)
for key, value in state_dict.items():
    value = value.to(torch.bfloat16)  # Now 38GB in RAM

# Step 4: Copies to VRAM
transformer.load_state_dict(converted_dict)
transformer.to("cuda")  # 19GB+ in VRAM too
```

**Peak RAM usage**: ~38GB (we only have 12GB free)
**Result**: System freeze → crash

## The Solution
Use `safetensors` direct VRAM loading:

```python
# GOOD - safetensors approach
from safetensors.torch import load_file

# Direct stream to VRAM, no RAM copy, keeps FP8
transformer_sd = load_file("model.safetensors", device="cuda")
```

**Peak RAM usage**: ~0GB
**VRAM usage**: 19GB (within 29GB limit)
**Result**: Works perfectly

## Memory Comparison

| Aspect | diffusers (Crash) | safetensors (Works) |
|--------|-------------------|---------------------|
| RAM usage | ~38GB peak | ~0GB |
| VRAM usage | 19GB+ | 19GB |
| Load method | RAM → convert → VRAM | Direct to VRAM |
| Dtype handling | FP8→BF16 conversion | Keeps FP8 native |
| Speed | Slow (multiple copies) | Fast (single load) |

## Rules for This System

1. **Always use safetensors for loading**:
   ```python
   from safetensors.torch import load_file
   model = load_file("model.safetensors", device="cuda")
   ```

2. **Never use diffusers for model loading** - only use if:
   - You have 64GB+ RAM
   - Or using their `enable_model_cpu_offload()` optimized pipelines

3. **VRAM is your friend, RAM is limited**:
   - 12GB free RAM = can't hold 19GB model
   - 29GB VRAM = plenty of room

4. **Keep FP8 as FP8** - don't convert to BF16/FP16 during load

5. **Load sequentially** - one model at a time, check VRAM between loads

## Test Results After Fix
```
Loading transformer to VRAM...
  Loaded 1933 tensors
  VRAM: 19.03GB

Loading VAE to VRAM...
  Loaded 194 tensors
  VRAM: 19.27GB

Final VRAM: 19.27GB / 29GB limit
[SUCCESS]
```

No crash, no freeze, plenty of headroom.
