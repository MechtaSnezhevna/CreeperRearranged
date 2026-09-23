# Creeper Rearranged

A Minecraft mod that introduces camouflaged variants of creepers.

[中文说明](doc/i18n/README_zh_cn.md)

## New mobs

### Honeeper

A honey-themed creeper variant that wears a bee nest on its head.

**Spawning and creation**
- When a creeper spawns naturally inside a `12*8*12` block area around a bee nest, it spawns as a honeeper instead.
- A naturally spawned honeeper starts with a random honey level: 60% at 0, 25% at 1, 10% at 2, 5% at 3 (full of honey).
- Right-clicking a vanilla creeper while holding an empty bee nest (no bees stored in it) snaps the nest onto its head and converts it into a honeeper.
- Use `/summon creeper_rearranged:honeeper ~ ~ ~ {HoneyLevel:%d}` to summon a honeeper with certain(`%d=0/1/2/3`) honey level.

**Behavior**
- Behaves exactly like a vanilla creeper.
- Pollen-carrying bees are attracted to the nest on its head; when such a bee comes close it loses its pollen and the nest's honey level rises by 1. At honey level 3 the honeeper is full of honey.
- Full-honey explosion: blast damage is halved, hit creatures get Slowness, and every destroyed block has a 30% chance to be replaced with a honey block.

**Harvesting**
- Shears on a full-honey honeeper drop 3 honeycomb; a glass bottle is filled into a honey bottle. Either harvest empties the nest, so bees have to refill it.
- Harvesting does not anger bees.

**Drops**
- Always: an (empty) bee nest and 0-2 gunpowder.
- Full-honey state: 50% chance each for an extra honey block and an extra honeycomb.

**Compatibility**
- With Jade installed, looking at a honeeper shows its honey level (`Honey Level: x/3`, plus "full of honey" at max).

### Endper

An enderman-tainted creeper variant that carries a stolen ender pearl in its chest.

**Spawning**
- When a creeper is about to spawn naturally within 8 blocks of a living enderman, an endper spawns instead.
- In the End, 1/24 of natural enderman spawns are replaced by an endper.
- Spawn with `/summon creeper_rearranged:endper ~ ~ ~` (add `{EndperHasPearl:0b}` for endper with no pearl) or its spawn egg.

**Behavior**
- While calm it only wanders. It never hunts players on its own and never detonates on its own.
- Looking it in the eye provokes it exactly like an enderman: a carved pumpkin hides you; sneaking, invisibility and the amount of armour you wear shrink the provoking distance.
- Once provoked (stared at or hurt) it screams, opens its snarling mouth and sprints at its target, then detonates 0.5 seconds after closing in. Its blast deals 1.5x a vanilla creeper's damage.
- It does not teleport when attacked and takes no damage from water or rain, but it still tries to leave water and get out of the rain.
- Below 3 hearts (of 12) it has a 1/4 chance per setback to teleport away like an enderman and abandon its target - spending its pearl to do so. After that the pearl in its chest is gone and it no longer drops one.

**Drops**
- Always: 0-2 gunpowder.
- While still carrying its pearl: 1 ender pearl.

### Crimper

A creeper variant overgrown with the crimson forest's fungus.

**Spawning**
- Naturally spawns anywhere in the crimson forest.
- Spawn with `/summon creeper_rearranged:crimper ~ ~ ~` or its spawn egg.

**Behavior**
- Behaves exactly like a vanilla creeper.

**Drops**
- Always: 0-2 gunpowder.
- Additionally 0-3 crimson materials, each one picked at random: crimson stem, weeping vines, crimson roots, nether wart block, shroomlight, crimson fungus or crimson nylium.

### Warper

A creeper variant overgrown with the warped forest's fungus.

**Spawning**
- Naturally spawns anywhere in the warped forest.
- Spawn with `/summon creeper_rearranged:warper ~ ~ ~` or its spawn egg.

**Behavior**
- Behaves exactly like a vanilla creeper.
- Endermen are drawn to it: while a living warper is within 16 blocks, an enderman stops wandering randomly and walks to within 5 blocks of it. A warper whose fuse is already burning is ignored - the enderman neither approaches nor flees, it just goes back to wandering.

**Drops**
- Always: 0-2 gunpowder.
- Additionally 0-3 warped materials, each one picked at random: warped fungus, warped stem, warped wart block, warped roots, twisting vines, warped nylium or warped shroomlight.

### Creepop

A creeper variant that drifts through the ocean inside a bubble of TNT.

**Spawning**
- Naturally spawns under water in every ocean biome, with the same spawn rules as the drowned.
- It breathes under water and is not pushed around by currents.
- Spawn with `/summon creeper_rearranged:creepop ~ ~ ~` or its spawn egg.

**Behavior**
- Behaves like a vanilla creeper while it is in water: it hunts players, lights its fuse and detonates.
- Its blast ignores water exactly like the underwater TNT's does: under water it keeps its full radius
  and damage, and the water blocks themselves are never removed.

**Popping it open**
- A sword or a trident (the item tag `creeper_rearranged:creepop_poppers`, which a modpack can extend
  with other mods' items) pricks the bubble open, and so does any arrow or thrown trident: no damage,
  no blast, it simply drops the underwater TNT it was carrying.
- Every other melee attack cannot really hurt it and lights its real fuse instead: it swells for
  1.5 seconds with the white flash and then detonates.
- Flint and steel follows the same water/land rule as the fuse below: a real fuse in water, the
  harmless burst on land.

**Out of water**
- Out of water it can light neither its real fuse nor a target: it stops chasing and drifts through
  the air like a soap bubble in the wind - a 3D random drift with a lot of inertia that rarely
  moves up or down.
- After 15 seconds of that it bursts harmlessly - no blast, no damage, only the underwater TNT
  drop. The last 1.5 seconds it swells *without* the white flash as a warning, and Jade shows the
  seconds it has left.
- Touching water within those 15 seconds refills the timer on the spot, and back in the water it
  hunts like every other creeper variant.
- Rain or snow falling on it pops it at once - that is a harmless burst too.

**Drops**
- Popped open or burst out of water: 1 underwater TNT.
- Killed any other way - fire, lava, suffocation, a fall, the void, another mob, an explosion:
  0-2 gunpowder.

### Cherreeper

A docile cherry-blossom creeper variant that wears a wide pink hat.

**Spawning**
- Naturally spawns in the cherry grove biome, day or night.
- Spawn with `/summon creeper_rearranged:cherreeper ~ ~ ~` or its spawn egg.

**Behavior**
- It is neutral: it never hunts or detonates on its own. Attacking it makes it fight back, and it also turns hostile when you anger bees nearby.
- While calm a wild one randomly alternates between standing and sitting, each state lasting a few seconds; a tamed one keeps the pose its owner last commanded and never switches on its own.

**Taming**
- Right-click with a honey bottle to tame it.
- A tamed cherreeper never attacks the player, even if you hit it. (Don't hit it, you monster!)
- Feed a tamed cherreeper honey bottles to restore 6 HP.
- Right-click a tamed cherreeper to make it sit or stand. A cherreeper you forced to sit stays down and does not move, even when it is hurt.
- A standing tamed cherreeper follows its owner like a cat or dog: it walks back when the owner is more than 10 blocks away, and teleports if the gap grows past 12 blocks.

**Drops**
- Always: 0-2 gunpowder.
- Additionally 0-3 cherry materials, each one picked at random: cherry leaves, cherry log or pink petals.

**Cosmetics**
- Renaming a cherreeper to `color` (case-insensitive) swaps its texture to `cherreeper_color`.

### Phanper

A phantom that carries a block of TNT and turns its dive into a suicide bomb.

**Spawning**
- Each night there is a 1/13 chance that a phanper is summoned high above a random player, like the vanilla phantom spawner.
- Every naturally spawned phantom has a 1/3 chance to be replaced by a phanper.
- Spawn with `/summon creeper_rearranged:phanper ~ ~ ~` or its spawn egg.

**Behavior**
- Like phantoms it circles high in the sky, then dives at you. A dive commits to your position at the moment it starts; if it reaches you it explodes like a creeper (blast radius 3, respects the `mobGriefing` rule).
- Dodge the dive and the phanper cannot explode; it simply flies back into the sky and may try again.
- Unlike phantoms it never burns in daylight, and during the day it does not attack players unless you attack it first; at night it hunts like a phantom.
- Like phantoms it is scared of cats.

**Drops**
- Always: 0-2 gunpowder (more with Looting). It never drops phantom membrane.

### Creepaler

A pale creeper variant from the pale garden that freezes while it is being watched.

**Spawning**
- If a version has no pale garden, half of the naturally spawned creepers in the dark forest will be replaced by a creepaler instead.
- Spawn with `/summon creeper_rearranged:creepaler ~ ~ ~` or its spawn egg.

**Behavior**
- A creepaler sleeps by default, and only wakes when a player watches it from within 12 blocks.
- While a player's gaze is on it, it freezes completely: it cannot move, turn, melee attack, mount anything or be pushed, and it stops all animation. It never disappears in daylight like the creaking.
- Once nobody is watching it and it has no target, an active creepaler falls back asleep; otherwise it sprints at players at about 8 m/s and detonates like every other creeper variant.
- Like the creaking, a player counts as watching when they are in survival or adventure, are not on the creepaler's team, the line between them is free of solid blocks within 32 blocks (glass, stained glass, tinted glass, iron bars and powder snow do not block) and the angle between their gaze and that line is under 60 degrees. A carved pumpkin hides your gaze; a sleeping creepaler only wakes when you are within 12 blocks.

**Drops**
- Always: 0-2 gunpowder (more with Looting).
- Killed during the day (no thunderstorm): 1 gray dye; at night or during a thunderstorm: 1 orange dye.

## New blocks

### Warped Shroomlight

A warped-forest twin of the shroomlight. It behaves exactly like the vanilla shroomlight - light level 15, hardness 1, mined fastest with a hoe and dropping itself - and only its texture differs.

**How to get it**
- The shroomlights carried by a huge warped fungus are warped shroomlights instead: both the fungi that generate naturally in the warped forest and the ones grown by using bone meal on a warped fungus.

### Underwater TNT

A TNT that keeps its full blast power under water; everything else is vanilla TNT. It has the same
instant breaking time, sound and 4 second fuse, it can be ignited by redstone, flint and steel, fire
charges, burning arrows, fire spreading onto it and dispensers, a blast that destroys it primes it
again with a shortened fuse, and it drops itself when mined.

**How to get it**
- There is no crafting recipe: it is creative-tab only (or `/setblock` / `/give`).

**Behavior**
- Its blast treats water as if it was air, so an explosion under water destroys blocks exactly like
  one in the air. The water blocks themselves are never removed, and a waterlogged block only resists
  with its own material instead of vanilla's extra 100 from the water.
- Like vanilla TNT it belongs to `minecraft:enderman_holdable`, so endermen pick it up.

## Client config

Client-side options are stored in `config/creeper_rearranged-client.toml` and can also be edited in game from the mod list's config button.

### Enable Slimmer Models

*Default: enabled.* When enabled, redundant model faces of some creeper variants are culled.
