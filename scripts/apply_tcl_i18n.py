#!/usr/bin/env python3
"""Add 15 locale resources and connect active settings labels to Android locale."""
from pathlib import Path
from xml.sax.saxutils import escape
import json
DATA = json.loads("{\"pt-rBR\":[\"Configurações\",\"Controle\",\"Controles do remoto\",\"Aparência\",\"Cores e estilo\",\"Dicas e suporte\",\"Suporte e informações\",\"Apoie o projeto\",\"Doação\",\"Conexão\",\"Reconexão automática\"],\"en\":[\"Settings\",\"Remote\",\"Remote controls\",\"Appearance\",\"Colors and style\",\"Tips and support\",\"Support and about\",\"Support the project\",\"Donation\",\"Connection\",\"Auto reconnect\"],\"es\":[\"Configuración\",\"Control remoto\",\"Controles remotos\",\"Apariencia\",\"Colores y estilo\",\"Ayuda y consejos\",\"Soporte e información\",\"Apoya el proyecto\",\"Donación\",\"Conexión\",\"Reconexión automática\"],\"fr\":[\"Paramètres\",\"Télécommande\",\"Commandes\",\"Apparence\",\"Couleurs et style\",\"Conseils et aide\",\"Assistance et informations\",\"Soutenir le projet\",\"Don\",\"Connexion\",\"Reconnexion automatique\"],\"de\":[\"Einstellungen\",\"Fernbedienung\",\"Fernbedienungstasten\",\"Darstellung\",\"Farben und Stil\",\"Tipps und Hilfe\",\"Hilfe und Informationen\",\"Projekt unterstützen\",\"Spende\",\"Verbindung\",\"Automatisch verbinden\"],\"it\":[\"Impostazioni\",\"Telecomando\",\"Comandi\",\"Aspetto\",\"Colori e stile\",\"Suggerimenti e supporto\",\"Supporto e informazioni\",\"Sostieni il progetto\",\"Donazione\",\"Connessione\",\"Riconnessione automatica\"],\"zh-rCN\":[\"设置\",\"遥控器\",\"遥控功能\",\"外观\",\"颜色和样式\",\"提示与支持\",\"支持与信息\",\"支持项目\",\"捐赠\",\"连接\",\"自动重新连接\"],\"hi\":[\"सेटिंग\",\"रिमोट\",\"रिमोट नियंत्रण\",\"दिखावट\",\"रंग और शैली\",\"सुझाव और सहायता\",\"सहायता और जानकारी\",\"परियोजना का समर्थन करें\",\"दान\",\"कनेक्शन\",\"स्वतः दोबारा कनेक्ट\"],\"ar\":[\"الإعدادات\",\"جهاز التحكم\",\"أزرار التحكم\",\"المظهر\",\"الألوان والنمط\",\"نصائح ودعم\",\"الدعم والمعلومات\",\"ادعم المشروع\",\"تبرع\",\"الاتصال\",\"إعادة الاتصال تلقائيًا\"],\"ru\":[\"Настройки\",\"Пульт\",\"Управление\",\"Оформление\",\"Цвета и стиль\",\"Советы и помощь\",\"Поддержка и сведения\",\"Поддержать проект\",\"Пожертвование\",\"Подключение\",\"Автоподключение\"],\"ja\":[\"設定\",\"リモコン\",\"リモコン操作\",\"外観\",\"色とスタイル\",\"ヒントとサポート\",\"サポートと情報\",\"プロジェクトを支援\",\"寄付\",\"接続\",\"自動再接続\"],\"ko\":[\"설정\",\"리모컨\",\"리모컨 제어\",\"모양\",\"색상 및 스타일\",\"팁 및 지원\",\"지원 및 정보\",\"프로젝트 후원\",\"기부\",\"연결\",\"자동 재연결\"],\"bn\":[\"সেটিংস\",\"রিমোট\",\"রিমোট নিয়ন্ত্রণ\",\"চেহারা\",\"রং ও স্টাইল\",\"পরামর্শ ও সহায়তা\",\"সহায়তা ও তথ্য\",\"প্রকল্পে সহায়তা করুন\",\"অনুদান\",\"সংযোগ\",\"স্বয়ংক্রিয় পুনঃসংযোগ\"],\"in\":[\"Pengaturan\",\"Remote\",\"Kontrol remote\",\"Tampilan\",\"Warna dan gaya\",\"Tips dan bantuan\",\"Dukungan dan info\",\"Dukung proyek\",\"Donasi\",\"Koneksi\",\"Sambung ulang otomatis\"],\"tr\":[\"Ayarlar\",\"Uzaktan kumanda\",\"Kumanda kontrolleri\",\"Görünüm\",\"Renkler ve stil\",\"İpuçları ve destek\",\"Destek ve bilgiler\",\"Projeyi destekle\",\"Bağış\",\"Bağlantı\",\"Otomatik yeniden bağlan\"]}")
KEYS = ["settings","remote","remote_controls","appearance","colors","tips","support","support_project","donation","connection","auto_reconnect"]
res = Path("source/app/src/main/res")
for locale, values in DATA.items():
    if len(values) != len(KEYS):
        raise RuntimeError("Missing localized label: " + locale)
    folder = res / ("values-" + locale)
    folder.mkdir(parents=True, exist_ok=True)
    body = ['<?xml version="1.0" encoding="utf-8"?>', '<resources>']
    body.extend(f'    <string name="tcl_{key}">{escape(value)}</string>' for key, value in zip(KEYS, values))
    body.append("</resources>")
    (folder / "tcl_localization.xml").write_text("\n".join(body) + "\n", encoding="utf-8")
settings = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app/SettingsScreen.kt")
source = settings.read_text(encoding="utf-8")
from_locale = DATA["pt-rBR"]
for key, value in zip(KEYS, from_locale):
    original = '"' + value + '"'
    translated = 'context.getString(com.hari.androidtvremote.R.string.tcl_' + key + ')'
    if original not in source:
        raise RuntimeError("Expected active settings label missing: " + key)
    source = source.replace(original, translated)
settings.write_text(source, encoding="utf-8")
assert 'tcl_donation' in source and 'tcl_support_project' in source
print("TCL_I18N_15_SETTINGS_LABELS_READY")
