# Icon theme prompts

Source of truth for the art in `tools/icons/sources/`. Three-step generation pipeline; the whole file is English (project language).

## Pipeline

1. **Step 1 — full render** (new chat, attach the reference). Common skeleton — substitute the three theme fragments from the table:
```text
A 1024x1024 3D illustration. BACKGROUND: {background}. FOREGROUND OBJECT: {plane}. GEOMETRY & THRUST: The jet turbine MUST be mounted strictly centered along the central bottom crease (symmetry axis) of the paper plane. A bright, vibrant, dense and highly luminous {flame} MUST shoot prominently backward out of the turbine nozzle, fully visible. EXACT FRAMING: The entire plane together with its full fire exhaust fits cleanly inside the central 60% of the canvas. NO borders, NO frames, NO text.
```
2. **Step 2 — alpha mask** (same chat):
```text
Generate a grayscale ALPHA MASK of this exact image. Pure white (#FFFFFF) where the paper plane and turbine are. The fire jet: white where dense, fading through grays to black at the faint tips — alpha, not color. Pure black background. PIXEL-ALIGNED with the original: same position and scale, no redraw, no shift. Output ONLY the mask.
```
3. **Step 3 — clean background** (new chat, attach the step-1 result):
```text
A 1024x1024 illustration. Extract the background style and colors from the reference image. STRICTLY NO objects: no plane, no turbine, no fire. ONLY the empty background texture, edge-to-edge, full-bleed. NO borders, NO rounded corners.
```

## Themes (18)

| Slug | Name | Background | Plane | Flame |
|---|---|---|---|---|
| turbo | Turbo | playful cartoon sky, puffy clouds, soft light | rounded 3D cartoon paper plane | cartoon flame trail, soft edges |
| sky | Sky | clear blue sky, cirrus clouds, daylight | brushed aluminum, sun highlights | golden fire jet |
| sunset | Sunset | dramatic sunset, purple-crimson clouds | metal with warm rim reflections | blazing sunset fire |
| blue_night | Blue Night | deep midnight-blue gradient, cool rim light | polished steel, chrome bevels | hot white core + orange-amber |
| halloween | Halloween | dark spooky atmosphere | dark enchanted plane | violet/purple mystical |
| glass | Glass | soft studio gradient, caustics | frosted crystal glass, refraction, dispersion | glowing energy |
| paper_box | Paper Box | warm craft cardboard studio | corrugated kraft cardboard, sandy tones | warm golden |
| paper_fire | Paper Fire | dark background, embers, warm rim light | natural white origami paper | roaring fire, hot core |
| comix_blue | Comix Blue | comic book: blue Ben-Day dots, speed lines | bold ink outlines, cel shading | stylized comic blue fire |
| comix_purple | Comix Purple | graphic novel, purple-magenta palette | inked outlines, dramatic shading | stylized comic energy flame |
| carbon | Carbon | dark graphite carbon pattern | matte black carbon weave | golden-amber |
| gold | Gold (hidden) | dark warm studio, cinematic light | vintage warm craft with golden accents | golden fire jet |
| matrix | Matrix (hidden) | green code rain, cyber terminal | cyber stealth, emerald circuit traces | emerald digital code fire |
| neon | Neon (hidden) | cyberpunk neon, synthwave purple/cyan | glowing neon edges | electric neon plasma |
| space | Space (hidden) | deep space, stars, nebula | dark star metal, sharp geometry | hyper-thrust plasma |
| hexagon | Hexagon (hidden) | blueprint/CAD: dark cyan grid paper | precision schematic metal | laser energy jet |
| pixel | Pixel (hidden) | retro 16-bit pixel sky, dithering | voxel/pixel-art plane | pixelated fire exhaust |
| glitch | Glitch (hidden) | dark digital glitch, aberration, scanlines | pixel-glitch vaporwave plane | glitching digital fire |

Note: the roster used to include gold ×3 variants plus separate Neon+/Pixel Sky/Pixel Glitch themes — trimmed to the current 18 (see `LauncherIconController`).
