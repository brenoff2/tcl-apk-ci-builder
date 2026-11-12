#!/usr/bin/env python3
"""Synthetic tests; never contact ADB or a television."""
import importlib.util
import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

MODULE = Path(__file__).parents[1] / 'qa' / 'tcl_adb_apps_probe.py'
spec = importlib.util.spec_from_file_location('tcl_probe', MODULE)
probe = importlib.util.module_from_spec(spec)
spec.loader.exec_module(probe)


class ProbeTests(unittest.TestCase):
    def test_packages_exact_and_deduplicated(self):
        self.assertEqual(probe.packages_from_pm('package:com.netflix.ninja\npackage:com.netflix.ninja\npackage:bad;rm\nnoise'), ['com.netflix.ninja'])

    def test_paths_only_absolute(self):
        self.assertEqual(probe.apk_paths_from_pm('package:/data/app/base.apk\npackage:../evil.apk'), ['/data/app/base.apk'])

    def test_badging(self):
        meta = probe.apk_metadata("application-label:'Netflix'\napplication-icon-160:'res/mipmap-mdpi/icon.png'\nlaunchable-activity: name='Main'\n")
        self.assertEqual(meta['label'], 'Netflix')
        self.assertTrue(meta['has_launchable_activity'])

    def test_icon_exact_and_xml_unsupported(self):
        with tempfile.TemporaryDirectory() as tmp:
            apk, target = Path(tmp)/'base.apk', Path(tmp)/'icon.png'
            with ZipFile(apk, 'w') as z:
                z.writestr('res/mipmap/icon.xml', '<adaptive-icon/>')
                z.writestr('res/mipmap/icon.png', b'\x89PNG\r\n\x1a\nrest')
            self.assertIsNone(probe.export_icon(apk, ['res/mipmap/icon.xml'], target))
            self.assertFalse(target.exists())
            self.assertEqual(probe.export_icon(apk, ['res/mipmap/icon.png'], target), 'res/mipmap/icon.png')
            self.assertTrue(target.exists())


if __name__ == '__main__':
    unittest.main()
