#!/usr/bin/env python3
"""Add per-TV brand selection and compact, centered brand mark to approved header."""
from pathlib import Path
p = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app/TclApprovedHomeScreen.kt")
s = p.read_text(encoding="utf-8")
anchor = '    // A slim toolbar, not a second oversized connected-status card.\n'
assert s.count(anchor) == 1
prefix = '''    val context = LocalContext.current
    val brands = listOf("TCL", "Sony", "Philips", "Hisense", "Xiaomi", "Google TV", "Android TV", "Outra")
    val prefs = remember(context) {
        context.getSharedPreferences("tcl_tv_brand_selection", android.content.Context.MODE_PRIVATE)
    }
    // Per-device key: the display name is the identifier already exposed by the remote UI.
    // This does not modify the pairing or favorites keys.
    val brandKey = "brand:" + tvName
    var chosenBrand by remember(brandKey) {
        mutableStateOf(prefs.getString(brandKey, null) ?: "")
    }
    var showBrandPicker by remember { mutableStateOf(false) }
    val inferred = remember(tvName) {
        brands.firstOrNull { it != "Outra" && tvName.contains(it, ignoreCase = true) } ?: "Android TV"
    }
    val displayedBrand = chosenBrand.ifEmpty { inferred }
    if (showBrandPicker) {
        AlertDialog(
            onDismissRequest = { showBrandPicker = false },
            title = { Text("Marca da televisão") },
            text = {
                Column {
                    Text("Selecione a marca desta TV. Ela será lembrada nas próximas conexões.")
                    brands.forEach { brand ->
                        TextButton(onClick = {
                            chosenBrand = brand
                            prefs.edit().putString(brandKey, brand).apply()
                            showBrandPicker = false
                        }) { Text(brand) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBrandPicker = false }) { Text("Fechar") }
            }
        )
    }
'''
s=s.replace(anchor,prefix+anchor)
start=s.index('        Row(\n            modifier = Modifier.weight(1f).clickable(onClick = onChooseTv),',s.index('private fun TclApprovedHeader('))
end=s.index('        TclHeaderAction(Icons.Filled.PowerSettingsNew', start)
replacement='''        Row(
            modifier = Modifier.weight(1f).clickable(onClick = onChooseTv),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape)
                .background(if (isConnected) Color(0xFF17DE68) else Color(0xFFFFA427)))
            Text(tvName, color = TclText, fontWeight = FontWeight.Medium,
                fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false))
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Trocar televisão",
                tint = TclMuted, modifier = Modifier.size(12.dp))
        }
        Box(
            modifier = Modifier.width(74.dp).height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(TclSurfaceHigh)
                .clickable { showBrandPicker = true },
            contentAlignment = Alignment.Center
        ) {
            // Text mark avoids licensing/asset misrepresentation; replace with approved assets later.
            Text(displayedBrand, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, color = TclText,
                modifier = Modifier.padding(horizontal = 3.dp))
        }
'''
s=s[:start]+replacement+s[end:]
s=s.replace('contentDescription = "Selecionar TCL"','contentDescription = "Selecionar televisão"')
p.write_text(s,encoding="utf-8")
assert s.count("showBrandPicker = true") == 1
assert "brand:" in s and "onChooseTv" in s
print("TCL_BRAND_PICKER_HEADER_READY")
