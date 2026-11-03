# Enchant Reveal

Fabric mod for Minecraft 26.3. When you hover over an enchanting table slot, the tooltip lists
**every** enchantment that slot will give, instead of one clue followed by `. . . ?`.

**Singleplayer only** (including a LAN world you host). Vanilla only sends the client the low
16 bits of your enchanting seed. The mod reads the full seed from the built-in singleplayer server
and re-runs vanilla's enchant roll. On a multiplayer server the tooltip stays vanilla.

If the result can't be verified (for example, the server's clue isn't in the computed list),
the mod shows the vanilla tooltip instead of guessing.

No Fabric API required.

## Build

Needs JDK 25+ (`JAVA_HOME=/usr/lib/jvm/java-27-openjdk` here).

```sh
./gradlew build             # runs the game test, then copies the jar into the Prism 26.3 mods folder
./gradlew runClientGameTest # opens a client, enchants a few items, saves tooltip screenshots
```

`runGameTest` enchants 600 items through the real enchanting menu and checks that every result
matches the preview exactly. The client test writes screenshots to
`build/run/clientGameTest/screenshots/`.

## License

MIT. See [LICENSE](LICENSE).
