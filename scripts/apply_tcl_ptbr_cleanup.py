#!/usr/bin/env python3
"""UI-only pt-BR cleanup for TCL Libre after apply_tcl_v22.py.
Do not modify package names, pairing state, navigation machinery or licenses.
"""
from pathlib import Path
import re

ui = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app")
settings = ui / "SettingsScreen.kt"
source = settings.read_text(encoding="utf-8")
# Keep original callback signatures: AppNavGraph still supplies these callbacks.
# Remove only obsolete user-facing navigation entries, not functional routes.
for label in ("Tips & Support", "Support Project"):
    needle = f'                    SettingSubtitle(text = "{label}")'
    at = source.find(needle)
    if at < 0:
        raise RuntimeError(f"Expected settings section not found: {label}")
    # Each section is two LazyColumn item blocks, before the next heading.
    next_heading = source.find('                item {\n                    SettingSubtitle(', at + len(needle))
    if next_heading < 0:
        raise RuntimeError(f"Could not locate next settings section after {label}")
    section_start = source.rfind('                item {\n', 0, at)
    source = source[:section_start] + source[next_heading:]
settings.write_text(source, encoding="utf-8")

translations = {
    "Settings": "Configurações",
    "Remote": "Controle",
    "Remote Controls": "Controles do remoto",
    "Personalize controls, layout, and quick actions": "Personalize os botões, a disposição e os atalhos",
    "Appearance": "Aparência",
    "Color & style": "Cores e estilo",
    "Wallpaper colors, basic colors, and dark theme": "Cores, plano de fundo e tema escuro",
    "Connection": "Conexão",
    "Auto reconnect": "Reconexão automática",
    "Automatically reconnect to the last paired device": "Reconectar automaticamente à última TV vinculada",
    "Done": "Concluir",
    "Cancel": "Cancelar",
    "Save": "Salvar",
    "Back": "Voltar",
    "Close": "Fechar",
    "Search": "Pesquisar",
    "Connected": "Conectado",
    "Disconnected": "Desconectado",
    "Connect": "Conectar",
    "Disconnect": "Desconectar",
    "Refresh": "Atualizar",
    "Retry": "Tentar novamente",
    "Continue": "Continuar",
    "Next": "Próximo",
    "Previous": "Anterior",
    "Add": "Adicionar",
    "Remove": "Remover",
    "Edit": "Editar",
    "Reset": "Restaurar",
    "Default": "Padrão",
    "Theme": "Tema",
    "Dark": "Escuro",
    "Light": "Claro",
    "Volume": "Volume",
    "Channels": "Canais",
    "Keyboard": "Teclado",
    "Applications": "Aplicativos",
    "Favorites": "Favoritos",
    "Devices": "Dispositivos",
    "Select device": "Selecionar dispositivo",
}
changed = {}
for path in sorted(ui.glob("*.kt")):
    if path.name in ("DonationScreen.kt", "TipsSupportScreen.kt"):
        continue # unlinked legacy code is not an active display surface
    before = path.read_text(encoding="utf-8")
    after = before
    for en, pt in translations.items():
        after = after.replace('"' + en + '"', '"' + pt + '"')
    if after != before:
        path.write_text(after, encoding="utf-8")
        changed[path.name] = sum(before.count('"' + en + '"') for en in translations)
assert 'title = "Donation"' not in settings.read_text()
assert 'title = "Support & about"' not in settings.read_text()
assert 'title = "Buscar atualizações"' in settings.read_text()
assert 'TclUpdateManager.check(activity)' in settings.read_text()
print("TCL_PTBR_UI_CLEANUP_READY", changed)
