# FukntClient

A Fabric client utility mod for **Minecraft 1.21.10**, built with Java 21 and Fabric Loom. FukntClient includes modular movement, player, combat and visual features, a chat-command system, and Charlie: an experimental autonomous assistant built around Baritone.

> **Status:** Charlie's current release is a working integration foundation, not a fully autonomous survival agent. The GOAP planner can construct dependency plans and inspect the player's inventory; Baritone performs navigation and mining. Chest-coordinate discovery and a separate low-latency PvP controller are available. Automatic chest looting, recipe execution, furnace operation, and full plan-to-action orchestration are still being developed and are not represented as complete features.

## Requirements

- Minecraft **1.21.10**
- Fabric Loader **0.19.1 or newer**
- Fabric API matching Minecraft 1.21.10
- Java **21** (JDK for development and the Minecraft `runClient` runtime; Java 26 is not supported for this project)
- Internet access on the first Gradle configuration/build so Gradle can retrieve Minecraft, Fabric dependencies, and Baritone

## Build from source

### Windows

```bat
gradlew.bat build
```

### Linux / macOS

```bash
chmod +x gradlew
./gradlew build
```

The output mod jar is written to `build/libs/` (use the remapped jar for a Fabric installation). Gradle automatically downloads the matching Baritone Fabric API release into the project's local `libs/` directory before Loom inspects file dependencies. You do **not** need to manually download or copy Baritone.

If the first build fails during dependency setup:

1. Confirm `java -version` reports Java 21 when using the terminal.
2. In IntelliJ IDEA, set **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JVM** to a JDK 21 installation. Also set the Minecraft run configuration to use Java 21 if it overrides the Gradle launcher.
3. This project configures Gradle's Java toolchain and the Loom `runClient` Java launcher to use Java 21. Install a full JDK 21 (not just a JRE) so Gradle can find it.
4. Confirm GitHub, Maven Central and `maven.fabricmc.net` are reachable.
5. Delete a partial `libs/baritone-api-fabric-*.jar` or `.part` file, then rerun the build.
6. If Loom has cached a corrupt Minecraft download, close the IDE and retry with `gradlew --refresh-dependencies build` after checking network access.

The build deliberately avoids absolute paths such as `C:\\Users\\...`. The Baritone artifact is downloaded to a project-relative path so the project can be built on different machines.

## Installing

1. Build the project, or obtain a release jar.
2. Put the remapped FukntClient jar in the `mods` folder of the Fabric 1.21.10 instance.
3. Launch with Java 21 and a compatible Fabric Loader/Fabric API installation.
4. Baritone is nested as a Fabric mod dependency; do not install a second copy of Baritone alongside FukntClient.

Baritone is licensed under LGPL-3.0. Keep `BARITONE-NOTICE.txt` with redistributed builds and comply with the upstream license and source-offer requirements when distributing the combined artifact. Upstream project: [cabaletta/baritone](https://github.com/cabaletta/baritone).

## Charlie

Charlie is designed as a high-level planner and delegates movement/mining to Baritone. Its architecture separates:

- **Survival GOAP:** a symbolic action planner that reasons about prerequisite resources and tool tiers. For example, `mine diamonds` inserts an iron-tier pickaxe prerequisite before the diamond-ore mining action.
- **World/resource observations:** inventory counts are read from the player's inventory; nearby loaded chest blocks can be discovered without force-loading chunks.
- **Action backends:** Baritone provides pathing and block mining. Crafting, smelting, and chest transfer actions are planned but their complete GUI/inventory execution backend is not yet implemented.
- **Combat controller:** a separate per-client-tick tactical loop, intentionally independent from survival replanning so it does not wait for GOAP to rebuild a plan.

### Commands

| Command | Description |
|---|---|
| `#charlie plan mine diamonds` | Generate a dependency plan for mining diamonds using current inventory state. |
| `#charlie plan craft iron pickaxe` | Generate a prerequisite plan for an iron pickaxe. |
| `#charlie chests` | List nearby loaded chest/trapped-chest coordinates (up to 16 blocks horizontally). Does not open or loot them yet. |
| `#charlie pvp` | Enable the fast combat controller and select the nearest player. |
| `#charlie pvp PlayerName` | Enable the fast combat controller for a named player. |
| `#charlie stop` | Stop Charlie's combat and survival tasks and cancel Baritone pathing. |
| `#goto <x> <y> <z>` | Navigate to coordinates through Baritone. |
| `#goto mark <name>` | Navigate to a saved FukntClient mark. |
| `#charliemine <block>` / `#dig <block>` | Start Baritone's mine process for a block type. |
| `#stop` | Stop Charlie and cancel Baritone's active process. |

Examples:

```text
#charlie plan mine diamonds
#charlie plan craft iron pickaxe
#charlie chests
#charlie pvp Steve
#charlie stop
#charliemine diamond_ore
#goto 120 64 -300
```

### GOAP roadmap

1. Expand item/recipe knowledge from the live recipe manager.
2. Implement chest interaction, container scanning, item transfer, and storage memory.
3. Implement crafting-table and furnace action executors with menu-state verification.
4. Add plan replanning after each action, failure recovery, resource reservations, and a visible plan queue.
5. Improve combat with target prediction, defensive movement, weapon selection, and configurable combat profiles.

## Project structure

- `src/main/java` – common mod initialization and mixins.
- `src/client/java/plog/fukntclient/client` – client initializer, modules, settings, commands, managers, GUI and rendering.
- `src/client/java/plog/charlie` – Charlie controller, Baritone bridge, GOAP planner and combat controller.
- `src/main/resources` – Fabric mod metadata and mixin configurations.
- `gradle/` – Gradle wrapper.

## Troubleshooting

- **`Failed to compute checksum` / missing Baritone JAR:** the build now downloads the artifact during project configuration, before Loom checks file dependencies. Ensure the download completed and retry. Remove an incomplete jar in `libs/` if necessary.
- **Baritone unavailable in-game:** check that the final jar contains `META-INF/jars/baritone-api-fabric-1.16.0.jar` and that `fabric.mod.json` declares the nested jar and `baritone` dependency.
- **`runClient` exits with `-1` / `NTSTATUS 0xFFFFFFFF`:** check the JVM path in the error. If it points to Java 26 (for example, `temurin-26`), install JDK 21 and select it under IntelliJ's Gradle JVM setting. The project now requests a Java 21 toolchain for compilation and a Java 21 launcher for `runClient`; restart the IDE after changing the JDK. If it still exits, inspect `run/crash-reports/` and `run/logs/latest.log`—the Gradle exit code alone does not identify the native or mod crash.
- **Wrong Java version:** Minecraft 1.21.10 requires Java 21; configure both the terminal and IDE Gradle JVM to Java 21.
- **Command not recognised:** use the `#` prefix and check `#fhelp` for FukntClient commands. Native Baritone commands remain available when not intercepted by FukntClient.

## Contributing

Keep client-only Minecraft interactions in the client source set. Keep planning logic separate from Minecraft side effects where practical, and verify every action against live world/inventory state before marking it complete. Avoid hardcoded local filesystem paths and document any new dependencies and licensing requirements.
