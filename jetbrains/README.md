# weebo-bridge-notify — plugin JetBrains

Plugin JetBrains (IntelliJ IDEA, PyCharm, WebStorm, GoLand...) équivalent à l'extension code-oss / VS Code située à la racine du repo. Écrit en Kotlin avec l'[IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html).

Il partage le même canal `~/.ide-notify` et embarque le même CLI `bin/ide-notify` (copié au build) : toute intégration branchée pour l'extension VS Code (hooks Claude Code, Codex CLI, scripts...) fonctionne telle quelle ici.

- Chaque ligne du fichier devient une notification native (balloon) — `info`/`warn` s'effacent seules, `error` reste affichée.
- Call to action en boutons de notification : `url`, `file` (+ `line`), `shell` (terminal intégré), `copy`, `command` (id d'action IDE).
- Le CLI `ide-notify` est installé dans `~/.local/bin` au démarrage (désactivable).
- Réglages : `Settings → Tools → Weebo Bridge Notify` ; test via `Tools → Weebo Bridge Notify: Envoyer une notification de test`.
- Compatibilité : IDE JetBrains **2026.1 et plus** (`since-build 261`).

Build (JDK 21 requis, ex. `mise use java@temurin-21`), depuis la racine du repo :

```bash
task jetbrains:build   # Package le plugin en dist/weebo-bridge-notify-<version>.zip
task jetbrains:verify  # Plugin Verifier (compatibilité API JetBrains)
task jetbrains:test    # Envoie une notification de test (même canal)
```

Installation : `Settings → Plugins → ⚙️ → Install Plugin from Disk`.

Pour la présentation complète (usage, intégrations IA, format du fichier), voir le [readme racine](../readme.md) et [docs/integrations.md](../docs/integrations.md).
