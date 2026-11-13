#!/usr/bin/env python3
"""Finish the approved flush-graphite skin on shared settings widgets.

Runs after apply_anytv_brand.py; no dimensions, navigation or callbacks change.
The common Modifier.anyTvCutGraphite lives in TclApprovedHomeScreen.kt.
"""
from pathlib import Path

target = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app/SettingComponents.kt")
source = target.read_text(encoding="utf-8")

# Shared settings hub cards, retaining their exact layout, padding and handlers.
old_group = """.background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(24.dp)
                )"""
new_group = """.anyTvCutGraphite(RoundedCornerShape(24.dp))"""
assert source.count(old_group) == 1, "Settings group renderer changed"
source = source.replace(old_group, new_group, 1)

# Settings icons follow the same material/cut, not elevated secondary tint.
old_icon = """.clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),"""
new_icon = """.anyTvCutGraphite(RoundedCornerShape(12.dp)),"""
assert source.count(old_icon) == 1, "Settings icon renderer changed"
source = source.replace(old_icon, new_icon, 1)

# Existing SettingItemRow remains a full-width row with the same hit target.
# Add only a shape/rim; do not change the clickable Surface or its children.
old_row = """.padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,"""
new_row = """.padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                .anyTvCutGraphite(RoundedCornerShape(18.dp)),
            verticalAlignment = Alignment.CenterVertically,"""
assert source.count(old_row) == 1, "Settings row renderer changed"
source = source.replace(old_row, new_row, 1)

target.write_text(source, encoding="utf-8")
print("ANYTV_SETTINGS_SHARED_GRAPHITE_CUT_APPLIED")
