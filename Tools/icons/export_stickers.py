# Telegram sticker export: sources/<theme>/final.png -> webp 512x512, rounded corners.
# Static stickers: exactly 512 on one side, <=64KB, image/webp.

import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "stickers")
SIDE = 512
SIZE_LIMIT = 64 * 1024

# LauncherIconController order (our themes), gold pinned last per user call
ORDER = [
    "turbo", "sky", "sunset", "blue_night", "halloween",
    "paper_box", "paper_fire", "carbon",
    "matrix", "neon", "space", "hexagon", "pixel", "glitch", "gold",
]


def rounded_mask(radius_ratio):
    r = int(SIDE * radius_ratio)
    mask = Image.new("L", (SIDE, SIDE), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, SIDE - 1, SIDE - 1], radius=r, fill=255)
    return mask


def export(theme, radius_ratio):
    src = Image.open(os.path.join(HERE, "sources", theme, "final.png")).convert("RGBA")
    im = src.resize((SIDE, SIDE), Image.LANCZOS)
    im.putalpha(rounded_mask(radius_ratio))
    os.makedirs(OUT, exist_ok=True)
    idx = ORDER.index(theme) + 1
    path = os.path.join(OUT, f"{idx}_{theme}.webp")
    lo, hi, best = 1, 100, None
    while lo <= hi:
        q = (lo + hi) // 2
        im.save(path, "WEBP", quality=q, method=6)
        if os.path.getsize(path) <= SIZE_LIMIT:
            best, lo = q, q + 1
        else:
            hi = q - 1
    if best is not None:
        im.save(path, "WEBP", quality=best, method=6)
    else:
        # noisy art does not compress: soften grain and retry
        from PIL import ImageFilter
        soft = im.filter(ImageFilter.GaussianBlur(0.6))
        soft.putalpha(rounded_mask(radius_ratio))
        soft.save(path, "WEBP", quality=90, method=6)
    return path, os.path.getsize(path)


if __name__ == "__main__":
    themes = sys.argv[1].split(",") if len(sys.argv) > 1 else ["turbo"]
    radius_ratio = float(sys.argv[2]) if len(sys.argv) > 2 else 0.125
    for theme in themes:
        path, size = export(theme, radius_ratio)
        sys.stdout.write(f"[{theme}] {path} {size / 1024:.1f}KB r={radius_ratio}\n")
