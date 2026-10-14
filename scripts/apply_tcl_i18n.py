#!/usr/bin/env python3
"""Add 15 locale resources and connect active settings labels to Android locale."""
from pathlib import Path
from xml.sax.saxutils import escape

def android_xml(value):
    return escape(value).replace("'", chr(92) + "'")
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
    body.extend(f'    <string name="tcl_{key}">{android_xml(value)}</string>' for key, value in zip(KEYS, values))
    body.append("</resources>")
    (folder / "tcl_localization.xml").write_text("\n".join(body) + "\n", encoding="utf-8")
# Android requires default values for every generated resource identifier.
default_folder = res / "values"
default_folder.mkdir(parents=True, exist_ok=True)
default_body = ['<?xml version="1.0" encoding="utf-8"?>', '<resources>']
default_body.extend(f'    <string name="tcl_{key}">{android_xml(value)}</string>' for key, value in zip(KEYS, DATA["en"]))
default_body.append("</resources>")
(default_folder / "tcl_localization.xml").write_text("\\n".join(default_body) + "\\n", encoding="utf-8")
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

# Additional active settings descriptions, localized without changing callbacks.
DESCRIPTIONS = {
"pt-rBR": ["Personalize os botões, a disposição e os atalhos", "Cores, plano de fundo e tema escuro", "Sobre, patrocinadores e informações de suporte", "Apoie o desenvolvimento e mantenha o app sem anúncios", "Reconectar automaticamente à última TV vinculada"],
"en": ["Customize controls, layout and shortcuts", "Wallpaper, colors and dark theme", "About and support information", "Support development and keep the app ad-free", "Reconnect to the last paired TV automatically"],
"es": ["Personaliza controles, diseño y accesos directos", "Fondo, colores y tema oscuro", "Información y ayuda", "Apoya el desarrollo sin anuncios", "Reconectar automáticamente a la última TV"],
"fr": ["Personnaliser les commandes et raccourcis", "Fond, couleurs et thème sombre", "Informations et assistance", "Soutenez le développement sans publicité", "Reconnexion automatique au dernier téléviseur"],
"de": ["Steuerung und Kurzbefehle anpassen", "Hintergrund, Farben und dunkles Design", "Informationen und Hilfe", "Werbefreie Entwicklung unterstützen", "Automatisch mit dem letzten TV verbinden"],
"it": ["Personalizza comandi e scorciatoie", "Sfondo, colori e tema scuro", "Informazioni e assistenza", "Sostieni lo sviluppo senza pubblicità", "Riconnetti automaticamente l'ultima TV"],
"zh-rCN": ["自定义控件、布局和快捷方式", "壁纸、颜色和深色主题", "关于与支持信息", "支持无广告开发", "自动重新连接上次配对的电视"],
"hi": ["नियंत्रण, लेआउट और शॉर्टकट बदलें", "वॉलपेपर, रंग और डार्क थीम", "जानकारी और सहायता", "विज्ञापन-मुक्त विकास में सहयोग दें", "पिछले टीवी से स्वतः दोबारा जुड़ें"],
"ar": ["تخصيص التحكم والتخطيط والاختصارات", "الخلفية والألوان والمظهر الداكن", "المعلومات والدعم", "ادعم التطوير دون إعلانات", "إعادة الاتصال بالتلفاز الأخير تلقائيًا"],
"ru": ["Настройка кнопок и ярлыков", "Фон, цвета и тёмная тема", "Информация и поддержка", "Поддержите разработку без рекламы", "Автоматически подключаться к последнему ТВ"],
"ja": ["操作やショートカットをカスタマイズ", "壁紙、色、ダークテーマ", "情報とサポート", "広告なしの開発を支援", "前回のテレビに自動再接続"],
"ko": ["조작과 바로가기 맞춤 설정", "배경, 색상 및 어두운 테마", "정보 및 지원", "광고 없는 개발 후원", "마지막 TV에 자동 재연결"],
"bn": ["নিয়ন্ত্রণ ও শর্টকাট সাজান", "ওয়ালপেপার, রং ও ডার্ক থিম", "তথ্য ও সহায়তা", "বিজ্ঞাপনমুক্ত উন্নয়নে সহায়তা", "শেষ টিভিতে স্বয়ংক্রিয় পুনঃসংযোগ"],
"in": ["Sesuaikan kontrol dan pintasan", "Wallpaper, warna, dan tema gelap", "Informasi dan bantuan", "Dukung pengembangan tanpa iklan", "Hubungkan ulang otomatis ke TV terakhir"],
"tr": ["Kontrolleri ve kısayolları özelleştir", "Duvar kağıdı, renkler ve koyu tema", "Bilgi ve destek", "Reklamsız geliştirmeyi destekle", "Son TV'ye otomatik bağlan"]
}
description_keys = ["remote_description", "appearance_description", "support_description", "donation_description", "auto_reconnect_description"]
for locale, values in DESCRIPTIONS.items():
    file = res / ("values-" + locale) / "tcl_localization.xml"
    source_xml = file.read_text(encoding="utf-8")
    extra = "".join(f'    <string name="tcl_{key}">{android_xml(value)}</string>\n' for key, value in zip(description_keys, values))
    source_xml = source_xml.replace("</resources>", extra + "</resources>")
    file.write_text(source_xml, encoding="utf-8")
default_file = res / "values" / "tcl_localization.xml"
default_xml = default_file.read_text(encoding="utf-8")
default_extra = "".join(f'    <string name="tcl_{key}">{android_xml(value)}</string>\\n' for key, value in zip(description_keys, DESCRIPTIONS["en"]))
default_file.write_text(default_xml.replace("</resources>", default_extra + "</resources>"), encoding="utf-8")
source = settings.read_text(encoding="utf-8")
for key, value in zip(description_keys, DESCRIPTIONS["pt-rBR"]):
    # The short support description is not translated by the earlier compatibility script.
    original_en = "About, sponsor, and project support details"
    if key == "support_description":
        original = original_en
    else:
        original = value
    source = source.replace('"' + original + '"', 'context.getString(com.hari.androidtvremote.R.string.tcl_' + key + ')')
settings.write_text(source, encoding="utf-8")
