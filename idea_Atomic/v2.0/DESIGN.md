# Design System Document: The Cinematic Controller

## 1. Overview & Creative North Star

### Creative North Star: "The Obsidian Command"
This design system is built to transform a mobile device into a high-end, tactile command center. We are moving away from the "flat app" aesthetic toward a **Hyper-Physical Dark Mode**. By blending deep, charcoal surfaces with high-energy "Electric Orange" accents and glassmorphism, we create an experience that feels like a premium physical remote control carved from obsidian.

The system breaks the standard template through **tonal depth** and **intentional asymmetry**. We rely on a "Scale of Light" rather than lines. Elements are not merely placed on a grid; they are "milled" out of the surface or "floated" above it using sophisticated layering and backdrop blurs.

---

## 2. Colors

The color palette is anchored in absolute darkness to minimize light bleed in a home theater environment, using `primary` accents to drive the eye toward core navigation.

### The Palette
- **Background (`#0e0e0e`)**: The foundation. A deep, ink-black that makes the device hardware disappear.
- **Primary (`#ff906a`) & Primary Fixed (`#ff794a`)**: Our "Electric Orange." Used sparingly for high-impact navigation and active states.
- **Secondary (`#e4e2e1`)**: Reserved for high-contrast labels and primary iconography.
- **Surface Tiers**:
    - `surface_container_lowest` (#000000): For "inset" tactile wells.
    - `surface_container_highest` (#262626): For elevated, glass-like interaction points.

### The "No-Line" Rule
**Explicit Instruction:** Designers are prohibited from using 1px solid borders to define sections. Boundaries must be defined through background shifts. For example, a playback control bar should use `surface_container_low` against a `surface` background. The transition is the boundary.

### Signature Textures: Glass & Gradient
To achieve a "bespoke" feel, secondary controls must utilize **Glassmorphism**. 
- Use `surface_container_highest` at 40-60% opacity with a `backdrop-blur` of 20px. 
- Main CTAs (like the Navigation Wheel) should utilize a subtle radial gradient transitioning from `primary` to `primary_container` to create a 3D "haptic" dome effect.

---

## 3. Typography

The system utilizes **Inter** for its neutral, technical precision. The hierarchy is designed to be legible from a distance, mimicking an editorial layout.

- **Display (Large/Medium)**: Used for "Now Playing" titles or device names. Set with tight letter-spacing (-0.02em) to feel authoritative.
- **Headline (Small)**: For primary navigational headers. Clean, bold, and unapologetic.
- **Title (Medium)**: The workhorse for button labels.
- **Body & Label**: Reserved for metadata (resolution, timestamps). Use `on_surface_variant` (#adaaaa) to keep secondary info from competing with primary controls.

The interplay between `display-lg` and `label-sm` creates a "High-End Editorial" contrast—combining massive, bold commands with tiny, precise technical data.

---

## 4. Elevation & Depth

Hierarchy is achieved through **Tonal Layering**, mimicking physical materials.

### The Layering Principle
- **Level 0 (Base)**: `surface` (#0e0e0e).
- **Level 1 (Submerged)**: Use `surface_container_lowest` (#000000) with an inner shadow to create "wells" for buttons, making them feel like physical cutouts.
- **Level 2 (Floating)**: Use `surface_container_high` (#201f1f) for standard buttons.
- **Level 3 (Overlay)**: Glassmorphic elements for secondary navigation that floats above the main remote interface.

### Ambient Shadows
For floating elements, shadows must be "Atmospheric." 
- **Blur**: 40px - 60px.
- **Opacity**: 8% of `#000000`.
- **Note**: Never use pure grey shadows on orange elements; tint the shadow with a hint of `primary_dim` to simulate natural light dispersion.

### The Ghost Border
If a container requires more definition (e.g., in high-glare environments), use a **Ghost Border**: `outline_variant` (#484847) at **15% opacity**. This provides a hint of a "specular edge" without the dated look of a solid stroke.

---

## 5. Components

### The Navigation Wheel (Signature Component)
The core of the system.
- **Style**: A massive `primary` circle. 
- **Center "OK"**: An inner circle using `surface_container_highest` with a glass-blur, creating a "lens" effect. 
- **Haptics**: On press, the gradient should invert to simulate physical travel.

### Buttons (Tactile Variants)
- **Primary**: Solid `primary_fixed`. High-contrast `on_primary_fixed` (#000000) text. 
- **Secondary (Glass)**: `surface_container_highest` at 50% opacity + backdrop blur. These should feel like frosted glass tiles.
- **Active State**: Use `primary_dim` with a subtle outer glow (0px 0px 12px) of the same color.

### Lists & Channel Grids
- **Rules**: No dividers. Use **Spacing 8** (1.75rem) to separate categories. 
- **Selection**: Do not use a checkbox. Use a `surface_bright` background shift and a `primary` vertical accent bar (4px width) on the left edge.

### Additional Components: The "Tactile Volume Slider"
Instead of a thin line, use a wide `surface_container_low` track. The "thumb" should be a large, glassmorphic rectangle that "magnifies" the track behind it.

---

## 6. Do's and Don'ts

### Do:
- **Use "Breathing Room"**: Use `spacing-16` and `spacing-20` to separate functional clusters (e.g., D-pad vs. Media Controls).
- **Embrace Asymmetry**: Align small labels to the far right while headlines sit far left to create a premium, non-standard layout.
- **Focus on Tactility**: Ensure every button has a distinct `pressed` state that looks "pushed in" (using inner shadows).

### Don't:
- **No 1px Lines**: Never use a solid grey line to separate the bottom nav from the content. Use a background shift to `surface_container_lowest`.
- **No Pure White**: Avoid `#ffffff` for secondary text; use `on_surface_variant` to prevent eye strain.
- **No Flat Orange**: Avoid using the accent color as a flat fill for large areas without a subtle gradient or "inner glow" to provide depth.