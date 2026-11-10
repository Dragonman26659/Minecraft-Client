Baritone is not committed to this repository.

The Gradle build automatically downloads the official Baritone Fabric API release
matching baritone_version in gradle.properties into this directory, then nests it
inside the FukntClient mod jar. Fabric Loader discovers the nested mod through
the jars entry and enforces the baritone dependency in fabric.mod.json.

Minecraft 1.21.10 uses Baritone 1.16.0. Do not place a second Baritone jar in
the game's mods folder when using the bundled FukntClient jar.
