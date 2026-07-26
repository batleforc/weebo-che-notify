const vscode = require('vscode');
const fs = require('fs');
const os = require('os');
const path = require('path');

let position = 0;
let watchedFile = null;
let panel = null;

function config() {
  return vscode.workspace.getConfiguration('weeboBridgeNotify');
}

function notifyFile() {
  const custom = config().get('file');
  return custom && custom.trim() ? custom : path.join(os.homedir(), '.ide-notify');
}

function forwardToBridge(level, message, actions) {
  if (panel) panel.webview.postMessage({ type: 'notify', level, message, actions: actions || [] });
}

// Exécute un call to action attaché à une notification.
// Types supportés : url, file (+ line), command (VS Code), shell (terminal visible), copy.
function runAction(action) {
  if (!action || typeof action !== 'object') return;
  try {
    if (action.type === 'url' && action.url) {
      vscode.env.openExternal(vscode.Uri.parse(action.url, true));
    } else if (action.type === 'file' && action.path) {
      const opts = {};
      if (Number.isInteger(action.line) && action.line > 0) {
        const pos = new vscode.Position(action.line - 1, 0);
        opts.selection = new vscode.Range(pos, pos);
      }
      vscode.commands.executeCommand('vscode.open', vscode.Uri.file(action.path), opts);
    } else if (action.type === 'command' && action.command) {
      vscode.commands.executeCommand(action.command, ...(Array.isArray(action.args) ? action.args : []));
    } else if (action.type === 'shell' && action.command) {
      const term = vscode.window.createTerminal({ name: action.label || 'weebo-bridge-notify' });
      term.show(false);
      term.sendText(action.command);
    } else if (action.type === 'copy' && action.text !== undefined) {
      vscode.env.clipboard.writeText(String(action.text));
      vscode.window.setStatusBarMessage('weebo-bridge-notify : copié dans le presse-papier ✅', 3000);
    } else {
      vscode.window.showWarningMessage('weebo-bridge-notify : action inconnue ou incomplète (' + (action.type || '?') + ')');
    }
  } catch (e) {
    vscode.window.showErrorMessage('weebo-bridge-notify : action « ' + (action.label || action.type) + ' » échouée : ' + e.message);
  }
}

function showLine(line) {
  let level = 'info';
  let msg = line;
  let actions = [];
  const trimmed = line.trimStart();
  if (trimmed.startsWith('{')) {
    // Ligne JSON : {level, message, actions: [{label, type, ...}]}
    try {
      const obj = JSON.parse(trimmed);
      level = LEVELS_RE.test(obj.level) ? obj.level : 'info';
      msg = typeof obj.message === 'string' ? obj.message : '';
      actions = Array.isArray(obj.actions) ? obj.actions.filter((a) => a && typeof a.label === 'string') : [];
    } catch (e) {
      // pas du JSON valide : affiché comme texte brut
    }
  } else {
    const m = line.match(/^(info|warn|error)\|([\s\S]*)$/);
    if (m) {
      level = m[1];
      msg = m[2];
    }
  }
  if (!msg.trim() && actions.length === 0) return;
  const show = level === 'error' ? vscode.window.showErrorMessage
    : level === 'warn' ? vscode.window.showWarningMessage
    : vscode.window.showInformationMessage;
  show(msg, ...actions.map((a) => a.label)).then((choice) => {
    const picked = actions.find((a) => a.label === choice);
    if (picked) runAction(picked);
  });
  forwardToBridge(level, msg, actions);
}

const LEVELS_RE = /^(info|warn|error)$/;

function startWatching() {
  stopWatching();
  watchedFile = notifyFile();
  try {
    if (!fs.existsSync(watchedFile)) fs.writeFileSync(watchedFile, '');
    position = fs.statSync(watchedFile).size;
  } catch (e) {
    vscode.window.showErrorMessage('weebo-bridge-notify: impossible d\'initialiser ' + watchedFile + ' : ' + e.message);
    return;
  }
  const interval = config().get('pollInterval') || 1000;
  fs.watchFile(watchedFile, { interval }, (curr) => {
    if (curr.size < position) position = 0; // fichier tronqué/recréé
    if (curr.size <= position) return;
    let fd;
    try {
      fd = fs.openSync(watchedFile, 'r');
      const buf = Buffer.alloc(curr.size - position);
      fs.readSync(fd, buf, 0, buf.length, position);
      position = curr.size;
      buf.toString('utf8').split('\n').filter(Boolean).forEach(showLine);
    } catch (e) {
      // lecture ratée : on retentera au prochain tick sans avancer position
    } finally {
      if (fd !== undefined) fs.closeSync(fd);
    }
  });
}

function stopWatching() {
  if (watchedFile) fs.unwatchFile(watchedFile);
  watchedFile = null;
}

function bridgeHtml() {
  return `<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline';">
  <title>Weebo Bridge Notify — pont OS</title>
  <style>
    body { font-family: var(--vscode-font-family); color: var(--vscode-foreground); padding: 1rem; }
    button { padding: 0.4rem 0.8rem; cursor: pointer; }
    #status { margin-top: 0.8rem; }
    .hint { opacity: 0.7; font-size: 0.9em; margin-top: 1rem; }
  </style>
</head>
<body>
  <h3>Pont notifications OS</h3>
  <p>Ce panneau relaie les notifications de l'IDE vers le navigateur (API Notification).
  Il doit rester ouvert (même en arrière-plan) pour que le relais fonctionne.</p>
  <button id="grant">Activer les notifications navigateur</button>
  <div id="status"></div>
  <p class="hint">Si la permission est bloquée, autorisez les notifications pour ce site
  dans Chrome : icône 🔒 dans la barre d'adresse → Autorisations du site → Notifications.</p>
  <script>
    const vscodeApi = acquireVsCodeApi();
    const statusEl = document.getElementById('status');
    function refresh() {
      if (!('Notification' in window)) {
        statusEl.textContent = 'API Notification indisponible dans cette webview.';
        return;
      }
      const p = Notification.permission;
      statusEl.textContent = 'Permission navigateur : ' + p +
        (p === 'granted' ? ' ✅ — les notifications OS sont actives.' : '');
    }
    document.getElementById('grant').addEventListener('click', async () => {
      if (!('Notification' in window)) { refresh(); return; }
      await Notification.requestPermission();
      refresh();
      if (Notification.permission === 'granted') {
        new Notification('Weebo Bridge Notify', { body: 'Notifications OS activées ✅' });
      }
    });
    window.addEventListener('message', (event) => {
      const d = event.data;
      if (!d || d.type !== 'notify') return;
      if (!('Notification' in window) || Notification.permission !== 'granted') return;
      const prefix = { info: 'ℹ️', warn: '⚠️', error: '❌' }[d.level] || '';
      const actions = Array.isArray(d.actions) ? d.actions : [];
      const body = d.message + (actions.length ? '\\n👆 Cliquer : ' + actions[0].label : '');
      const n = new Notification((prefix ? prefix + ' ' : '') + 'Weebo Bridge Notify', { body });
      n.onclick = () => {
        window.focus();
        if (actions.length) vscodeApi.postMessage({ type: 'invokeAction', action: actions[0] });
        n.close();
      };
    });
    refresh();
  </script>
</body>
</html>`;
}

function openBridge(context, preserveFocus) {
  if (panel) {
    panel.reveal(undefined, true);
    return;
  }
  panel = vscode.window.createWebviewPanel(
    'weeboBridgeNotifyBridge',
    'Weebo Bridge Notify — pont OS',
    { viewColumn: vscode.ViewColumn.Beside, preserveFocus: !!preserveFocus },
    { enableScripts: true, retainContextWhenHidden: true }
  );
  panel.webview.html = bridgeHtml();
  panel.webview.onDidReceiveMessage((msg) => {
    if (msg && msg.type === 'invokeAction') runAction(msg.action);
  }, null, context.subscriptions);
  panel.onDidDispose(() => { panel = null; }, null, context.subscriptions);
}

function activate(context) {
  startWatching();

  context.subscriptions.push(
    vscode.commands.registerCommand('weeboBridgeNotify.test', () => {
      const file = notifyFile();
      fs.appendFileSync(file, 'info|🔔 Notification de test weebo-bridge-notify (commande weeboBridgeNotify.test)\n');
    }),
    vscode.commands.registerCommand('weeboBridgeNotify.openOsBridge', () => openBridge(context, false)),
    vscode.workspace.onDidChangeConfiguration((e) => {
      if (e.affectsConfiguration('weeboBridgeNotify.file') || e.affectsConfiguration('weeboBridgeNotify.pollInterval')) {
        startWatching();
      }
    }),
    { dispose: stopWatching }
  );

  if (config().get('osBridge.autoOpen')) {
    openBridge(context, true);
  }

  if (!context.globalState.get('weeboBridgeNotify.welcomed')) {
    context.globalState.update('weeboBridgeNotify.welcomed', true);
    vscode.window
      .showInformationMessage(
        'weebo-bridge-notify actif — les lignes ajoutées à ~/.ide-notify apparaîtront ici.',
        'Activer les notifications OS'
      )
      .then((choice) => {
        if (choice) vscode.commands.executeCommand('weeboBridgeNotify.openOsBridge');
      });
  }
}

function deactivate() {
  stopWatching();
}

module.exports = { activate, deactivate };
