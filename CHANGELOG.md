# Changelog

All notable changes to this project will be documented in this file. See [conventional commits](https://www.conventionalcommits.org/) for commit guidelines.

Ce fichier est maintenu par [Cocogitto](https://github.com/cocogitto/cocogitto) (`cog bump`) à partir de l'historique de commits ; les versions ci-dessous, antérieures au premier commit, ont été rédigées à la main.

- - -
## [v0.4.0](https://github.com/batleforc/weebo-che-notify/compare/e0c2a73c4cac7ef4cbb2b3cc9f8b23c572333413..v0.4.0) - 2026-07-27
#### Features
- add to commit type - ([e90f7d2](https://github.com/batleforc/weebo-che-notify/commit/e90f7d28901098f6f1682bae3749c0cd516b4d9e)) - Max Batleforc
- plugin JetBrains équivalent à l'extension VS Code - ([e0c2a73](https://github.com/batleforc/weebo-che-notify/commit/e0c2a73c4cac7ef4cbb2b3cc9f8b23c572333413)) - Max Batleforc, Claude Fable 5
#### Doc
- readme jetbrains/ dédié et cas d'usage admin plateforme - ([eb64eb0](https://github.com/batleforc/weebo-che-notify/commit/eb64eb0d5ad8b899fe648a75afc79ae7c8354473)) - Max Batleforc, Claude Fable 5
#### Miscellaneous Chores
- (**release**) prépare la release 0.4.0 - ([f2940eb](https://github.com/batleforc/weebo-che-notify/commit/f2940eb90a38b180752ef3e01ab73c36fe69f2ea)) - Max Batleforc, Claude Fable 5

- - -


## [0.3.0] - 2026-07-26

### Features

- **(cli)** le CLI `ide-notify` est installé automatiquement dans `~/.local/bin` (PATH) à l'activation de l'extension (désactivable via `installCli`) — tout outil peut notifier avec `ide-notify ...` sans connaître le chemin de l'extension
- **(os-bridge)** le pont OS n'est plus un onglet d'éditeur mais une vue webview dans le panneau du bas (onglet « Bridge Notify », à côté du terminal) ; grâce à `retainContextWhenHidden`, le relais continue vue masquée ou panneau fermé
- **(os-bridge)** avec `osBridge.autoOpen`, la vue s'ouvre au démarrage pour armer le pont ; l'option `osBridge.autoClose` (désactivée par défaut) referme ensuite le panneau pour un fonctionnement sans interface visible

## [0.2.0] - 2026-07-26

### Features

- **(actions)** support des call to action sur les notifications : boutons dans la popup IDE (`url`, `file`, `command`, `shell`, `copy`), options `--action-*` dans `bin/ide-notify`, champ `actions` accepté dans les payloads JSON des agents
- **(os-bridge)** le clic sur la notification OS refocalise l'IDE et déclenche la première action
- **(icon)** icône de l'extension (cloche + pastille, générée par script node sans dépendance)

## [0.1.0] - 2026-07-26

### Features

- **(extension)** surveillance de `~/.ide-notify` : chaque ligne `info|`, `warn|` ou `error|` devient une popup native dans code-oss
- **(os-bridge)** pont webview même origine que che-code relayant les notifications vers l'API Notification du navigateur (vraies notifications OS depuis un workspace Eclipse Che)
- **(ide-notify)** binaire acceptant messages texte en arguments, JSON sur stdin (hooks Claude Code) ou JSON en argument (notify Codex CLI), avec détection du niveau et de la source
- **(docs)** `docs/integrations.md` : branchement de Claude Code, Codex CLI, autres IA, scripts et Taskfile
- **(template)** structure basée sur weebo-base : Taskfile, cocogitto, mise, devfile, hooks git
