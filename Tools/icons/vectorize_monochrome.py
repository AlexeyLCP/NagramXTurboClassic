#!/usr/bin/env python3
# Vectorize monochrome.png -> Android VectorDrawable monochrome.xml (108x108 dp)
# Preserves semi-transparent fire via multi-level fillAlpha layers (1.0, 0.45, 0.25)
import os, sys
import numpy as np
from PIL import Image
from skimage.measure import find_contours, approximate_polygon

HERE = os.path.dirname(os.path.abspath(__file__))
SOURCES_DIR = os.path.join(HERE, "sources")

CANVAS_SIZE = 108.0
FILL = 0.54 * 0.89  # 0.4806 safe-zone fill ratio

def vectorize_theme(slug):
    mono_path = os.path.join(SOURCES_DIR, slug, "monochrome.png")
    if not os.path.exists(mono_path):
        return None
    im = Image.open(mono_path).convert("RGBA")
    alpha = np.array(im)[:, :, 3]
    
    ys, xs = np.where(alpha > 30)
    if len(xs) == 0:
        return None
    min_x, max_x = xs.min(), xs.max()
    min_y, max_y = ys.min(), ys.max()
    core_w = max_x - min_x
    core_h = max_y - min_y
    side = max(core_w, core_h)
    
    target_size = CANVAS_SIZE * FILL
    scale = target_size / side
    off_x = (CANVAS_SIZE - core_w * scale) / 2.0 - min_x * scale
    off_y = (CANVAS_SIZE - core_h * scale) / 2.0 - min_y * scale
    
    def get_svg_path(binary_mask, tol=1.0):
        contours = find_contours(binary_mask, 0.5)
        paths = []
        for c in contours:
            if len(c) < 6: continue
            pts = approximate_polygon(c, tolerance=tol)
            if len(pts) < 4: continue
            cmds = []
            for i, (y, x) in enumerate(pts):
                vx = x * scale + off_x
                vy = y * scale + off_y
                pfx = "M" if i == 0 else "L"
                cmds.append(f"{pfx}{vx:.2f},{vy:.2f}")
            cmds.append("Z")
            paths.append(" ".join(cmds))
        return " ".join(paths)
        
    p1 = get_svg_path((alpha >= 35).astype(float), tol=1.2)
    p2 = get_svg_path((alpha >= 110).astype(float), tol=1.0)
    p3 = get_svg_path((alpha >= 205).astype(float), tol=0.8)
    
    xml = f"""<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Plume / outer flame glow -->
    <path
        android:fillColor="#FFFFFF"
        android:fillAlpha="0.25"
        android:fillType="evenOdd"
        android:pathData="{p1}" />
    <!-- Mid-flame body -->
    <path
        android:fillColor="#FFFFFF"
        android:fillAlpha="0.45"
        android:fillType="evenOdd"
        android:pathData="{p2}" />
    <!-- Solid origami plane & turbine -->
    <path
        android:fillColor="#FFFFFF"
        android:fillAlpha="1.0"
        android:fillType="evenOdd"
        android:pathData="{p3}" />
</vector>
"""
    out_file = os.path.join(SOURCES_DIR, slug, "monochrome.xml")
    with open(out_file, "w") as f:
        f.write(xml)
    return len(xml)

def main():
    themes = sorted([d for d in os.listdir(SOURCES_DIR) if os.path.isdir(os.path.join(SOURCES_DIR, d)) and not d.startswith(".")])
    for t in themes:
        sz = vectorize_theme(t)
        sys.stdout.write(f"[{t:12s}] -> monochrome.xml ({sz} bytes)\n")
    sys.stdout.write("done\n")

if __name__ == "__main__":
    main()
