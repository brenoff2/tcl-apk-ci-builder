#!/usr/bin/env python3
"""Keep remote transport centered and quick action toolbar anchored to screen bottom."""
from pathlib import Path
p = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app/TclApprovedHomeScreen.kt")
s = p.read_text(encoding="utf-8")
top = """    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .background(Brush.verticalGradient(listOf(TclSurface, TclBg, TclBg)))
    ) {
"""
assert s.count(top) == 1
s=s.replace(top,top+"""        Column(modifier = Modifier.fillMaxSize()) {
""")
scroll="""            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
"""
assert s.count(scroll)==1
s=s.replace(scroll,"""            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
""")
anchor='                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {\n                        val buttonDiameter'
start=s.index(anchor)
end=s.index('                    Spacer(Modifier.height(2.dp))',start)
end+=len('                    Spacer(Modifier.height(2.dp))')
footer=s[start:end].replace('                    Spacer(Modifier.height(2.dp))','')
s=s[:start]+s[end:]
closure='''        }
    }

    if (headsetStatus != null) {'''
assert s.count(closure)==1
new='''        }
        // The bottom actions stay in place even when the TV app list scrolls.
'''+footer+'''        }
    }

    if (headsetStatus != null) {'''
s=s.replace(closure,new)
assert 'weight(1f)' in s and s.count('val buttonDiameter') == 1
p.write_text(s, encoding="utf-8")
print("TCL_STICKY_BOTTOM_ACTIONS_READY")
