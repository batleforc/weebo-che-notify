# Intégrations — brancher une IA (ou n'importe quel outil) sur weebo-bridge-notify

Le principe est toujours le même : **tout ce qui peut exécuter une commande shell peut notifier**. Il suffit d'écrire une ligne `niveau|message` dans `~/.ide-notify`, directement ou via `bin/ide-notify` qui sait aussi parser les payloads JSON des agents.

```bash
# Les trois formes équivalentes
echo "info|Message" >> ~/.ide-notify
ide-notify info "Message"
echo '{"message": "Message"}' | ide-notify
```

Le CLI `ide-notify` est installé automatiquement dans `~/.local/bin` (présent dans le PATH) à l'activation de l'extension ; si l'extension n'a pas encore été activée, le chemin complet `node /projects/weeboBridgeNotify/bin/ide-notify` reste utilisable à la place.

`bin/ide-notify` accepte le JSON **sur stdin** (style hooks Claude Code) ou **en argument** (style notify Codex CLI), et déduit le niveau (`warn` pour les demandes de permission/approbation) et la source. Voir l'en-tête du script pour les variables d'env (`IDE_NOTIFY_FILE`, `IDE_NOTIFY_LEVEL`, `IDE_NOTIFY_SOURCE`).

## Claude Code (testé ✅)

Dans `~/.claude/settings.json`, brancher les hooks voulus — le payload JSON arrive sur stdin :

```json
{
  "hooks": {
    "Notification": [
      {
        "hooks": [
          { "type": "command", "command": "ide-notify 2>/dev/null || true" }
        ]
      }
    ],
    "Stop": [
      {
        "hooks": [
          { "type": "command", "command": "ide-notify 2>/dev/null || true" }
        ]
      }
    ]
  }
}
```

- `Notification` : demandes de permission, besoin d'input → niveau `warn` automatique.
- `Stop` : fin de tour → « Claude Code [Stop] : tâche terminée ».
- Tout futur hook fonctionnera tel quel : l'événement (`hook_event_name`) est repris dans le message.

## OpenAI Codex CLI

Codex passe le payload JSON **en argument** (pas sur stdin). Dans `~/.codex/config.toml` :

```toml
notify = ["ide-notify"]
```

Le script détecte le champ `type` (ex. `agent-turn-complete`) et préfixe le message avec « Codex ». Vérifier la syntaxe exacte dans la doc Codex de votre version.

## Gemini CLI, OpenCode, aider, et les autres

Ces outils évoluent vite ; plutôt que de documenter une syntaxe qui sera périmée, la méthode :

1. Chercher dans la doc de l'outil un mécanisme *hook*, *notify*, *event* ou *plugin* capable d'exécuter une commande.
2. Le pointer sur `ide-notify` :
   - payload JSON sur stdin ou en argument → géré automatiquement ;
   - sinon, passer le message en arguments : `ide-notify warn "..."`.
3. Si le payload a des champs exotiques, définir `IDE_NOTIFY_SOURCE="MonOutil"` pour un préfixe propre, ou ajouter le mapping dans `notifyFromPayload()` (`bin/ide-notify`).

Exemple générique en fin de script/wrapper :

```bash
mon-agent run "$PROMPT"; code=$?
if [ $code -eq 0 ]; then
  ide-notify info "MonAgent : terminé ✅"
else
  ide-notify error "MonAgent : échec (exit $code)"
fi
```

## Scripts, CI locale, Taskfile

```yaml
# Taskfile.yaml — notifier la fin d'un build long
tasks:
  build:notify:
    cmds:
      - defer: ide-notify info "Build terminé"
      - task: build
```

```bash
# N'importe quel long process
cargo build --release && echo "info|cargo build OK ✅" >> ~/.ide-notify || echo "error|cargo build KO ❌" >> ~/.ide-notify
```

## Call to action

Une notification peut porter des boutons d'action, affichés dans la popup IDE. Via `bin/ide-notify` (options répétables, format `"Label=valeur"`) :

```bash
ide-notify info "Build terminé ✅" \
  --action-url   "Voir la CI=https://ci.example.com/run/42" \
  --action-file  "Ouvrir le log=/tmp/build.log:120" \
  --action-shell "Relancer=task build" \
  --action-cmd   "Recharger l'IDE=workbench.action.reloadWindow" \
  --action-copy  "Copier le tag=v1.2.3"
```

Ou en écrivant directement une ligne JSON dans `~/.ide-notify` (les agents peuvent aussi mettre un champ `actions` dans leur payload JSON, il est transmis tel quel) :

```json
{"level":"warn","message":"PR prête à relire","actions":[{"label":"Ouvrir la PR","type":"url","url":"https://github.com/..."}]}
```

Types d'action : `url` (ouvre le lien), `file` (`path` + `line` optionnelle), `command` (commande VS Code + `args`), `shell` (lance `command` dans un terminal visible), `copy` (`text` → presse-papier).

Côté notification OS : les boutons ne sont pas affichables (limite de l'API Notification hors service worker), mais **cliquer sur la notification refocalise l'IDE et déclenche la première action** — elle sert donc de call to action par défaut.

## Format du fichier ~/.ide-notify

- Une notification par ligne : `info|...`, `warn|...` ou `error|...` (sans préfixe = `info`), ou une ligne JSON `{"level","message","actions"}` pour les call to action.
- `info` et `warn` s'effacent seules dans l'IDE ; `error` reste jusqu'à fermeture.
- Le fichier peut être tronqué/supprimé sans casser l'extension (elle se resynchronise).
- Avec le pont OS ouvert (commande `Weebo Bridge Notify: Ouvrir le pont notifications OS` + permission Chrome accordée), chaque ligne part aussi en notification OS.
