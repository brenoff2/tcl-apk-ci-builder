#!/usr/bin/env python3
"""Center the complete D-pad/volume/channel stage between playback and footer.

Keep the display hierarchy constrained to the actual viewport height:
  toolbar + installed apps + playback
  equally shared free space / D-pad / free space
  fixed bottom action row
No verticalScroll on the center stage: weighted children inside a scrollable
column are unconstrained and are not centered against the bottom toolbar.
"""
from pathlib import Path

p = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app/TclApprovedHomeScreen.kt")
source = p.read_text(encoding="utf-8")
top = """    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF11141B), TclBg, TclBg)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
"""
head_marker = "                    TclApprovedHeader("
stage_marker = """                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        // The old 178dp cap"""
footer_marker = """                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val buttonDiameter"""
spacer_marker = "                    Spacer(Modifier.height(2.dp))"
end_marker = "\n    if (headsetStatus != null) {"

assert source.count(top) == 1
start = source.index(top)
head_start = source.index(head_marker, start)
stage_start = source.index(stage_marker, head_start)
footer_start = source.index(footer_marker, stage_start)
spacer_start = source.index(spacer_marker, footer_start)
end = source.index(end_marker, spacer_start)
assert source.count(footer_marker) == 1
header = source[head_start:stage_start]
stage = source[stage_start:footer_start]
footer = source[footer_start:spacer_start]

# Bound the D-pad to the actual free viewport height, then center the complete
# D-pad and both rockers as one unit. The 28% OK ring remains a large tap target.
stage = stage.replace(
    ".coerceAtLeast(0.dp).coerceAtMost(304.dp)",
    ".coerceAtLeast(0.dp).coerceAtMost(304.dp).coerceAtMost(stageHeight)"
)
assert "coerceAtMost(stageHeight)" in stage

layout = top + """        val outerWidth = maxWidth
        val outerHeight = maxHeight
        val compactHeight = outerHeight < 760.dp
        val tightHeight = outerHeight < 650.dp
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(
                        horizontal = if (outerWidth < 360.dp) 10.dp else 14.dp,
                        vertical = if (tightHeight) 6.dp else 12.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(
                    if (tightHeight) 9.dp else if (compactHeight) 13.dp else 17.dp
                )
            ) {
""" + header + """            }
            // The gap above the stage is exactly the gap below it, regardless
            // of screen height and the height of the installed-app strip.
            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val stageHeight = maxHeight
""" + stage + """            }
            Box(modifier = Modifier.fillMaxWidth()
                .padding(
                    horizontal = if (outerWidth < 360.dp) 10.dp else 14.dp,
                    vertical = if (tightHeight) 6.dp else 12.dp
                )) {
""" + footer + """            }
        }
    }
"""
updated = source[:start] + layout + source[end:]
assert updated.count("TclApprovedHeader(") == 2
assert updated.count("TclResponsivePad(") == 1
assert updated.count("val buttonDiameter") == 1
assert "modifier = Modifier.weight(1f).fillMaxWidth()" in updated
assert "coerceAtMost(stageHeight)" in updated
p.write_text(updated, encoding="utf-8")
print("ANYTV_CENTERED_REMOTE_STAGE_WITH_STICKY_FOOTER_OK")
