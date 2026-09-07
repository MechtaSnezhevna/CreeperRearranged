# Creeper Rearranged

A Minecraft mod that introduces camouflaged variants of creepers.

## New mobs

### Honeeper

A honey-themed creeper variant that wears a bee nest on its head.

**Spawning and creation**
- When a creeper spawns naturally inside a `12*8*12` block area around a bee nest, it spawns as a honeeper instead.
- Right-clicking a vanilla creeper while holding an empty bee nest (no bees stored in it) snaps the nest onto its head and converts it into a honeeper.

**Behavior**
- Behaves exactly like a vanilla creeper.
- Every time a bee flies over its head it fills up a little; after 5 bee visits it reaches the full-honey state.
- Full-honey explosion: blast damage is halved, hit creatures get Slowness, and the outer rim of the destroyed blocks is replaced with honey blocks while the inner crater stays air.

**Drops**
- Always: a bee nest and 0-2 gunpowder.
- Full-honey state: 50% chance each for an extra honey block and an extra honeycomb.

**Assets**
- Rendered with GeckoLib (required mod dependency):
  - model `assets/creeper_rearranged/geo/entity/honeeper.geo.json`
  - texture `assets/creeper_rearranged/textures/entity/honeeper.png`
  - animations `assets/creeper_rearranged/animations/entity/honeeper.animation.json` (`idle`/`move`/`idleh`/`moveh`)
- The full-honey state switches to the `idleh`/`moveh` animation variants (honey layer pushed forward).
- Re-export from Blockbench (GeckoLib format) and replace the files at the same paths - no Java change needed unless the animation names change (they are constants in `Honeeper`).
