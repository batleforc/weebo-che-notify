# weebo-bridge-notify

<p align="center">
  <img src="icon.png" width="140" alt="Logo Weebo Bridge Notify — cloche de notification">
</p>

Extension code-oss / VS Code pour workspace [Eclipse Che](https://eclipse.dev/che/) : un pont de notifications entre le pod et l'utilisateur.

N'importe quel process du pod (script, CI locale, hook d'une IA — Claude Code, Codex, Gemini CLI...) écrit une ligne dans `~/.ide-notify`, et l'extension :

1. affiche une popup native dans l'IDE (en bas à droite) ;
2. optionnellement, relaie vers une **notification OS** via l'API Notification du navigateur (pont OS).

Basé sur le template [weebo-base](https://github.com/batleforc/weebo-base).

## Usage

```bash
# Format brut : niveau|message (niveaux : info, warn, error)
echo "info|Build terminé ✅" >> ~/.ide-notify

# Ou via le binaire fourni
bin/ide-notify warn "Attention, il se passe un truc"

# Depuis un hook Claude Code (payload JSON sur stdin)
echo '{"hook_event_name":"Notification","message":"Claude a besoin de toi"}' | bin/ide-notify

# Avec des call to action (boutons dans la popup IDE)
bin/ide-notify info "Build terminé ✅" --action-url "Voir la CI=https://..." --action-shell "Relancer=task build"
```

- `info` et `warn` s'effacent seuls, `error` reste affichée jusqu'à fermeture.
- Variables d'env : `IDE_NOTIFY_FILE` (fichier cible), `IDE_NOTIFY_LEVEL` (niveau par défaut en mode stdin).

## Brancher une IA (Claude Code, Codex, Gemini CLI...)

Le binaire `bin/ide-notify` accepte le JSON des agents **sur stdin** (hooks Claude Code) ou **en argument** (notify Codex CLI), déduit le niveau (`warn` pour les demandes de permission) et préfixe le message avec la source. Exemple pour Claude Code (`~/.claude/settings.json`) :

```json
{
  "hooks": {
    "Notification": [
      {
        "hooks": [
          { "type": "command", "command": "node /projects/weeboBridgeNotify/bin/ide-notify 2>/dev/null || true" }
        ]
      }
    ]
  }
}
```

Toutes les intégrations (Claude Code, Codex CLI, outils sans hook dédié, scripts, Taskfile) sont détaillées avec exemples dans **[docs/integrations.md](docs/integrations.md)**.

## Pont notifications OS (navigateur)

Les webviews de che-code sont servies depuis la même origine que l'IDE : une webview peut donc utiliser l'API `Notification` de Chrome avec la permission du site.

Le pont vit dans une vue du **panneau du bas** (onglet « Bridge Notify », à côté du terminal) — pas d'onglet d'éditeur.

1. `F1` → `Weebo Bridge Notify: Ouvrir le pont notifications OS (navigateur)` (ou cliquer l'onglet « Bridge Notify » du panneau).
2. Cliquer sur « Activer les notifications navigateur » et accepter la permission Chrome.
3. La vue peut ensuite être masquée et le panneau fermé : le relais continue en arrière-plan (`retainContextWhenHidden`).

Avec le setting `weeboBridgeNotify.osBridge.autoOpen`, la vue est résolue silencieusement à chaque démarrage puis le panneau est refermé : rien de visible, le pont tourne quand même.

## Tasks

```bash
task vsix:build     # Package l'extension en dist/weebo-bridge-notify.vsix
task vsix:install   # Build + installe dans code-oss (recharger la fenêtre ensuite)
task vsix:test      # Envoie une notification de test
task vsix:publish   # Publie sur le registre Open VSX perso (OVSX_REGISTRY_URL, OVSX_PAT)
task lint           # Vérifie la syntaxe des sources
task audit          # Audit sécurité (dépendances + secrets)
```

## Settings

| Setting | Défaut | Description |
|---|---|---|
| `weeboBridgeNotify.file` | `~/.ide-notify` | Fichier surveillé |
| `weeboBridgeNotify.pollInterval` | `1000` | Intervalle de surveillance (ms) |
| `weeboBridgeNotify.osBridge.autoOpen` | `false` | Rouvre le pont OS au démarrage |
