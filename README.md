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

## New blocks

### Warped Shroomlight

A warped-forest twin of the shroomlight. It behaves exactly like the vanilla shroomlight - light level 15, hardness 1, mined fastest with a hoe and dropping itself - and only its texture differs.

**How to get it**
- The shroomlights carried by a huge warped fungus are warped shroomlights instead: both the fungi that generate naturally in the warped forest and the ones grown by using bone meal on a warped fungus.

## Client config

Client-side options are stored in `config/creeper_rearranged-client.toml` and can also be edited in game from the mod list's config button.

### Enable Slimmer Models

*Default: enabled.* When enabled, redundant model faces of some creeper variants are culled.
