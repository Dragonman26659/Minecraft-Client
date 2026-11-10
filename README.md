# fukntClient

Minecraft client for Fabric 1.21.10

## Charlie + Baritone integration

CharlieBot is the single high-level controller. It owns the GOAP goal stack and delegates all bot locomotion, mining, and block placement to Baritone's public processes:

- Navigation: Baritone custom-goal process.
- Mining: Baritone mine process.
- Placement: Baritone builder process using a one-block `FillSchematic`.
- Rendering: Baritone draws its own calculated path, goal marker, and selection boxes. Charlie intentionally draws no competing path or mining overlay.
- HUD: the HUD module's `Show Bot Plan` setting displays Charlie's active goal stack.

The former Charlie-specific `Pathfinder`, `Miner`, and `Placer` implementations have been removed. Charlie now only sequences goals and observes Baritone process state. Baritone also handles mining drops; Charlie no longer has a separate item-chasing pathfinder.

### Add Baritone

1. Download `baritone-api-fabric-1.16.0.jar` from the official [Baritone v1.16.0 release](https://github.com/cabaletta/baritone/releases/tag/v1.16.0).
2. Put the jar at `libs/baritone-api-fabric-1.16.0.jar`.
3. Reload Gradle in IntelliJ and build the mod.
4. Keep `BARITONE-NOTICE.txt` and comply with Baritone's LGPL-3.0 license when distributing the combined jar.

The existing standalone player movement modules (such as Fly, Speed, Sprint, and Scaffold) are separate client features and have not been rewritten; this change replaces Charlie's bot movement/mining/placing implementations.

### Charlie commands

- `#goto <x> <y> <z>` and `#goto mark <name>`: navigate through Charlie/Baritone.
- `#charliemine <block>` / `#dig <block>`: mine through Baritone's mine process.
- `#stop`: stop Charlie and cancel Baritone's active process.
- Native Baritone commands remain available when they are not intercepted by a FukntClient command.

GOAP still sequences the currently implemented high-level navigate, mine, and place goals. Recipe-driven crafting, smelting, chest inventory memory, and full resource-planning actions remain future work.

## Bundled Baritone dependency

Baritone's official Fabric API build is downloaded automatically by Gradle
(`baritone_version` in `gradle.properties`) and nested in the remapped FukntClient
jar. Fabric Loader loads it as a nested mod and checks the declared `baritone`
dependency in `fabric.mod.json`. No manual `libs/` setup is required.

For Minecraft 1.21.10, the configured Baritone version is 1.16.0.
