"""
DailyWell Asset Generator - ComfyUI API Version
================================================

Uses ComfyUI's local API (port 8188) to generate high-quality assets.
ComfyUI properly loads and runs the Qwen-Image model.

Prerequisites:
1. Start ComfyUI (it runs on localhost:8188)
2. Have Qwen-Image FP8 model loaded in ComfyUI
3. Run this script: python generate_assets_comfyui.py

Asset Specifications for 2026 Premium Health App:
- Habit Icons:        256x256px  (glassmorphic, minimal)
- Coach Avatars:      512x512px  (photorealistic, friendly)
- Achievement Badges: 256x256px  (metallic, premium)
- Backgrounds:        1080x1920px (gradient, atmospheric)
"""

import json
import urllib.request
import urllib.error
import time
import uuid
import os
from pathlib import Path
from typing import Optional
import base64
import io

# ================= PATHS =================
OUTPUT_DIR = Path(r"C:\Users\PC\Desktop\moneygrinder\mobile\PART_2_HEALTH_APPS\03_HABIT_BASED_HEALTH\habit-health\shared\src\androidMain\res\drawable")
COMFYUI_URL = "http://127.0.0.1:8001"

# ================= 2026 PREMIUM ASSET DEFINITIONS =================
# Style: Glassmorphic, Neumorphism 2.0, Liquid Glass (Apple), Premium Health App

ASSETS = {
    # ===== HABIT ICONS (256x256) - Glassmorphic Minimal Style =====
    "habit_rest": {
        "filename": "habit_rest.png",
        "width": 256, "height": 256,
        "prompt": "minimalist sleep icon app, frosted glass circular button, soft indigo purple gradient background, elegant crescent moon with small stars, subtle outer glow, premium iOS health app design, perfectly centered, vector art style, clean crisp edges, soft shadows, no text",
        "negative": "realistic, photo, text, words, blurry, low quality, complex, busy",
    },
    "habit_hydrate": {
        "filename": "habit_hydrate.png",
        "width": 256, "height": 256,
        "prompt": "minimalist water drop icon app, frosted glass circular button, soft cyan aqua gradient background, elegant water droplet with reflection highlight, premium iOS health app design, perfectly centered, vector art style, clean crisp edges, soft shadows, no text",
        "negative": "realistic, photo, text, words, blurry, low quality, complex, busy",
    },
    "habit_move": {
        "filename": "habit_move.png",
        "width": 256, "height": 256,
        "prompt": "minimalist running exercise icon app, frosted glass circular button, soft coral orange gradient background, elegant running person silhouette with motion lines, premium iOS health app design, perfectly centered, vector art style, clean crisp edges, soft shadows, no text",
        "negative": "realistic, photo, text, words, blurry, low quality, complex, busy",
    },
    "habit_nourish": {
        "filename": "habit_nourish.png",
        "width": 256, "height": 256,
        "prompt": "minimalist healthy food leaf icon app, frosted glass circular button, soft green lime gradient background, elegant leaf or apple shape, premium iOS health app design, perfectly centered, vector art style, clean crisp edges, soft shadows, no text",
        "negative": "realistic, photo, text, words, blurry, low quality, complex, busy",
    },
    "habit_calm": {
        "filename": "habit_calm.png",
        "width": 256, "height": 256,
        "prompt": "minimalist meditation lotus icon app, frosted glass circular button, soft lavender purple gradient background, elegant lotus flower zen symbol, premium iOS health app design, perfectly centered, vector art style, clean crisp edges, soft shadows, no text",
        "negative": "realistic, photo, text, words, blurry, low quality, complex, busy",
    },
    "habit_connect": {
        "filename": "habit_connect.png",
        "width": 256, "height": 256,
        "prompt": "minimalist social heart connection icon app, frosted glass circular button, soft warm peach amber gradient background, two overlapping hearts symbol, premium iOS health app design, perfectly centered, vector art style, clean crisp edges, soft shadows, no text",
        "negative": "realistic, photo, text, words, blurry, low quality, complex, busy",
    },
    "habit_unplug": {
        "filename": "habit_unplug.png",
        "width": 256, "height": 256,
        "prompt": "minimalist digital detox power button icon app, frosted glass circular button, soft cool gray blue gradient background, elegant power off symbol, premium iOS health app design, perfectly centered, vector art style, clean crisp edges, soft shadows, no text",
        "negative": "realistic, photo, text, words, blurry, low quality, complex, busy",
    },

    # ===== ACHIEVEMENT BADGES (256x256) - Premium Metallic Style =====
    "badge_streak_7": {
        "filename": "badge_streak_7.png",
        "width": 256, "height": 256,
        "prompt": "premium bronze medal badge, shiny metallic bronze gold gradient, number 7 in elegant font center, laurel wreath border, achievement reward icon, gaming app badge, 3D glossy finish, premium quality, centered composition, transparent background style",
        "negative": "text except number, blurry, flat, matte, low quality",
    },
    "badge_streak_30": {
        "filename": "badge_streak_30.png",
        "width": 256, "height": 256,
        "prompt": "premium silver medal badge, shiny metallic silver chrome gradient, number 30 in elegant font center, laurel wreath border with ribbon, achievement reward icon, gaming app badge, 3D glossy finish, premium quality, centered composition",
        "negative": "text except number, blurry, flat, matte, low quality",
    },
    "badge_streak_100": {
        "filename": "badge_streak_100.png",
        "width": 256, "height": 256,
        "prompt": "premium gold medal badge, shiny metallic gold gradient with sparkles, number 100 in elegant font center, ornate laurel wreath border with ribbon, diamond accents, ultimate achievement icon, gaming app badge, 3D glossy finish, premium quality, centered",
        "negative": "text except number, blurry, flat, matte, low quality",
    },
    "badge_first_habit": {
        "filename": "badge_first_habit.png",
        "width": 256, "height": 256,
        "prompt": "premium achievement badge, green emerald gradient, single checkmark symbol in center, star burst behind, first milestone celebration icon, gaming app badge, 3D glossy metallic finish, premium quality, centered composition",
        "negative": "text, words, blurry, flat, matte, low quality",
    },
    "badge_perfect_week": {
        "filename": "badge_perfect_week.png",
        "width": 256, "height": 256,
        "prompt": "premium achievement badge, purple amethyst gradient, seven small stars arranged in circle, crown on top, perfect week celebration icon, gaming app badge, 3D glossy metallic finish, premium quality, centered composition",
        "negative": "text, words, blurry, flat, matte, low quality",
    },
    "badge_early_bird": {
        "filename": "badge_early_bird.png",
        "width": 256, "height": 256,
        "prompt": "premium achievement badge, warm sunrise orange yellow gradient, cute bird silhouette with sun rays, early morning theme, gaming app badge, 3D glossy metallic finish, premium quality, centered composition",
        "negative": "text, words, blurry, flat, matte, low quality, realistic bird",
    },
    "badge_night_owl": {
        "filename": "badge_night_owl.png",
        "width": 256, "height": 256,
        "prompt": "premium achievement badge, deep midnight blue purple gradient, cute owl silhouette with crescent moon and stars, nighttime theme, gaming app badge, 3D glossy metallic finish, premium quality, centered composition",
        "negative": "text, words, blurry, flat, matte, low quality, realistic owl",
    },
    "badge_comeback": {
        "filename": "badge_comeback.png",
        "width": 256, "height": 256,
        "prompt": "premium achievement badge, phoenix rising gradient orange red gold, phoenix bird rising from flames symbol, comeback triumph theme, gaming app badge, 3D glossy metallic finish, premium quality, centered composition",
        "negative": "text, words, blurry, flat, matte, low quality",
    },

    # ===== COACH AVATARS (512x512) - Friendly Approachable Style =====
    "coach_sam": {
        "filename": "coach_sam.png",
        "width": 512, "height": 512,
        "prompt": "professional friendly health coach portrait, warm smile, approachable expression, neutral background, soft studio lighting, diverse representation, modern casual professional attire, headshot composition, high quality portrait photography style, clean background",
        "negative": "cartoon, anime, distorted, ugly, blurry, low quality",
    },
    "coach_alex": {
        "filename": "coach_alex.png",
        "width": 512, "height": 512,
        "prompt": "professional energetic fitness coach portrait, confident smile, motivating expression, neutral background, bright studio lighting, athletic build, modern sporty attire, headshot composition, high quality portrait photography style, clean background",
        "negative": "cartoon, anime, distorted, ugly, blurry, low quality",
    },
    "coach_dana": {
        "filename": "coach_dana.png",
        "width": 512, "height": 512,
        "prompt": "professional calm mindfulness coach portrait, serene peaceful smile, zen expression, neutral background, soft natural lighting, calming presence, comfortable modern attire, headshot composition, high quality portrait photography style, clean background",
        "negative": "cartoon, anime, distorted, ugly, blurry, low quality",
    },
    "coach_grace": {
        "filename": "coach_grace.png",
        "width": 512, "height": 512,
        "prompt": "professional nurturing wellness coach portrait, gentle warm smile, caring expression, neutral background, soft golden lighting, welcoming presence, elegant casual attire, headshot composition, high quality portrait photography style, clean background",
        "negative": "cartoon, anime, distorted, ugly, blurry, low quality",
    },

    # ===== BACKGROUNDS (1080x1920) - Premium Gradient Atmospheric =====
    "bg_dashboard": {
        "filename": "bg_dashboard.png",
        "width": 1080, "height": 1920,
        "prompt": "abstract premium app background, soft gradient from deep teal to mint green, subtle glassmorphic blur layers, gentle light rays from top, minimalist atmospheric, premium health wellness app aesthetic, no objects just gradient atmosphere, mobile wallpaper",
        "negative": "objects, people, text, busy, complex, sharp edges",
    },
    "bg_insights": {
        "filename": "bg_insights.png",
        "width": 1080, "height": 1920,
        "prompt": "abstract premium app background, soft gradient from deep purple to soft lavender, subtle aurora borealis effect, gentle light particles, minimalist atmospheric, premium analytics insights aesthetic, no objects just gradient atmosphere, mobile wallpaper",
        "negative": "objects, people, text, busy, complex, sharp edges",
    },
    "bg_settings": {
        "filename": "bg_settings.png",
        "width": 1080, "height": 1920,
        "prompt": "abstract premium app background, soft gradient from slate gray to soft silver blue, subtle geometric patterns very faint, minimalist atmospheric, premium settings configuration aesthetic, no objects just gradient atmosphere, mobile wallpaper",
        "negative": "objects, people, text, busy, complex, sharp edges, colorful",
    },
    "bg_profile": {
        "filename": "bg_profile.png",
        "width": 1080, "height": 1920,
        "prompt": "abstract premium app background, soft gradient from warm peach to soft coral pink, subtle organic flowing shapes, gentle warmth, minimalist atmospheric, premium personal profile aesthetic, no objects just gradient atmosphere, mobile wallpaper",
        "negative": "objects, people, text, busy, complex, sharp edges",
    },
}


def check_comfyui_running():
    """Check if ComfyUI is running on localhost:8188"""
    try:
        req = urllib.request.Request(f"{COMFYUI_URL}/system_stats")
        with urllib.request.urlopen(req, timeout=5) as response:
            return response.status == 200
    except:
        return False


def get_workflow_template():
    """
    Returns Qwen-Image workflow using UNETLoader + CLIPLoader + VAELoader
    """
    workflow = {
        "1": {
            "class_type": "UNETLoader",
            "inputs": {
                "unet_name": "qwen_image_2512_fp8_e4m3fn.safetensors",
                "weight_dtype": "fp8_e4m3fn"
            }
        },
        "2": {
            "class_type": "CLIPLoader",
            "inputs": {
                "clip_name": "umt5_xxl_fp8_e4m3fn_scaled.safetensors",
                "type": "wan"
            }
        },
        "3": {
            "class_type": "VAELoader",
            "inputs": {
                "vae_name": "qwen_image_vae.safetensors"
            }
        },
        "4": {
            "class_type": "CLIPTextEncode",
            "inputs": {
                "clip": ["2", 0],
                "text": "prompt here"
            }
        },
        "5": {
            "class_type": "EmptySD3LatentImage",
            "inputs": {
                "batch_size": 1,
                "height": 512,
                "width": 512
            }
        },
        "6": {
            "class_type": "KSampler",
            "inputs": {
                "cfg": 4.0,
                "denoise": 1.0,
                "latent_image": ["5", 0],
                "model": ["1", 0],
                "negative": ["4", 0],
                "positive": ["4", 0],
                "sampler_name": "euler",
                "scheduler": "simple",
                "seed": 0,
                "steps": 28
            }
        },
        "7": {
            "class_type": "VAEDecode",
            "inputs": {
                "samples": ["6", 0],
                "vae": ["3", 0]
            }
        },
        "8": {
            "class_type": "SaveImage",
            "inputs": {
                "filename_prefix": "DailyWell",
                "images": ["7", 0]
            }
        }
    }
    return workflow


def queue_prompt(prompt_workflow):
    """Queue a prompt to ComfyUI and return the prompt_id"""
    p = {"prompt": prompt_workflow}
    data = json.dumps(p).encode('utf-8')
    req = urllib.request.Request(f"{COMFYUI_URL}/prompt", data=data)
    req.add_header('Content-Type', 'application/json')

    with urllib.request.urlopen(req) as response:
        result = json.loads(response.read())
        return result.get('prompt_id')


def get_history(prompt_id):
    """Get the execution history for a prompt"""
    req = urllib.request.Request(f"{COMFYUI_URL}/history/{prompt_id}")
    with urllib.request.urlopen(req) as response:
        return json.loads(response.read())


def get_image(filename, subfolder, folder_type):
    """Get an image from ComfyUI output"""
    url = f"{COMFYUI_URL}/view?filename={filename}&subfolder={subfolder}&type={folder_type}"
    req = urllib.request.Request(url)
    with urllib.request.urlopen(req) as response:
        return response.read()


def wait_for_completion(prompt_id, timeout=300):
    """Wait for a prompt to complete, return the output images"""
    start_time = time.time()
    while time.time() - start_time < timeout:
        history = get_history(prompt_id)
        if prompt_id in history:
            outputs = history[prompt_id].get('outputs', {})
            if outputs:
                # Find the SaveImage node output
                for node_id, node_output in outputs.items():
                    if 'images' in node_output:
                        return node_output['images']
        time.sleep(1)
    return None


def generate_asset_comfyui(asset_name: str, asset_config: dict, seed: Optional[int] = None) -> bool:
    """Generate a single asset using ComfyUI API"""
    print(f"\nGenerating: {asset_name}")
    print(f"  Size: {asset_config['width']}x{asset_config['height']}")

    # Get workflow template
    workflow = get_workflow_template()

    # Update workflow with asset-specific settings
    workflow["5"]["inputs"]["width"] = asset_config["width"]
    workflow["5"]["inputs"]["height"] = asset_config["height"]
    workflow["4"]["inputs"]["text"] = asset_config["prompt"]

    # Set seed
    if seed is None:
        seed = hash(asset_name) % (2**32)
    workflow["6"]["inputs"]["seed"] = seed

    try:
        # Queue the prompt
        prompt_id = queue_prompt(workflow)
        print(f"  Queued: {prompt_id}")

        # Wait for completion
        images = wait_for_completion(prompt_id)

        if images:
            # Get the first image
            img_info = images[0]
            img_data = get_image(img_info['filename'], img_info.get('subfolder', ''), img_info['type'])

            # Save to output directory
            output_path = OUTPUT_DIR / asset_config["filename"]
            output_path.parent.mkdir(parents=True, exist_ok=True)

            with open(output_path, 'wb') as f:
                f.write(img_data)

            print(f"  -> {asset_config['filename']}")
            return True
        else:
            print(f"  [ERROR] Timeout waiting for image")
            return False

    except Exception as e:
        print(f"  [ERROR] {e}")
        return False


def generate_all():
    """Generate all assets"""
    print("=" * 60)
    print("DailyWell Asset Generator - ComfyUI API")
    print("=" * 60)

    # Check if ComfyUI is running
    if not check_comfyui_running():
        print("\n[ERROR] ComfyUI is not running!")
        print("Please start ComfyUI first (localhost:8188)")
        print("\nSteps:")
        print("1. Open ComfyUI")
        print("2. Load your Qwen-Image workflow")
        print("3. Run this script again")
        return

    print("\nComfyUI connected!")

    # Ensure output directory exists
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    success_count = 0
    total = len(ASSETS)

    for asset_name, asset_config in ASSETS.items():
        if generate_asset_comfyui(asset_name, asset_config):
            success_count += 1

    print("\n" + "=" * 60)
    print(f"Done: {success_count}/{total} assets generated")
    print(f"Output: {OUTPUT_DIR}")
    print("=" * 60)


def print_workflow_instructions():
    """Print instructions for setting up ComfyUI workflow"""
    print("""
================================================================================
COMFYUI WORKFLOW SETUP INSTRUCTIONS
================================================================================

Before running this script, you need to set up ComfyUI:

1. START COMFYUI
   - Run ComfyUI.exe or your startup script
   - Wait for it to load (opens browser to localhost:8188)

2. LOAD QWEN-IMAGE MODEL
   - In ComfyUI, create a workflow with:
     * CheckpointLoaderSimple -> Load qwen_image_2512_fp8_e4m3fn.safetensors
     * CLIPTextEncode (positive prompt)
     * CLIPTextEncode (negative prompt)
     * EmptyLatentImage
     * KSampler
     * VAEDecode
     * SaveImage

3. EXPORT WORKFLOW AS API
   - File -> Export (API Format)
   - This gives you the exact JSON format needed
   - Update get_workflow_template() in this script with your exported workflow

4. RUN THIS SCRIPT
   python generate_assets_comfyui.py

================================================================================
""")


if __name__ == "__main__":
    import sys

    if len(sys.argv) > 1:
        cmd = sys.argv[1].lower()
        if cmd == "help":
            print_workflow_instructions()
        elif cmd == "check":
            if check_comfyui_running():
                print("ComfyUI is running!")
            else:
                print("ComfyUI is NOT running. Start it first.")
        elif cmd == "generate":
            generate_all()
        else:
            print(f"Unknown command: {cmd}")
            print("Usage: python generate_assets_comfyui.py [help|check|generate]")
    else:
        # Default: try to generate
        generate_all()
