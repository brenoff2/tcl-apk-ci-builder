#!/usr/bin/env python3
"""Isolated desktop-side QA probe. Never pairs devices or installs/uninstalls APKs."""
import argparse
import json
import re
import subprocess
import tempfile
import zipfile
from pathlib import Path

PACKAGE = re.compile(r"^[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+$")
ICON = re.compile(r"^application-icon(?:-\d+)?:'([^']+)'$", re.MULTILINE)
LABEL = re.compile(r"^application-label:'([^']*)'$", re.MULTILINE)
LAUNCHABLE = re.compile(r"^launchable-activity:", re.MULTILINE)


def packages_from_pm(output):
    return sorted({line[8:] for line in output.splitlines()
                   if line.startswith('package:') and PACKAGE.fullmatch(line[8:])})


def apk_paths_from_pm(output):
    return [line[8:] for line in output.splitlines()
            if line.startswith('package:') and line[8:].startswith('/') and '\n' not in line[8:]]


def apk_metadata(badging):
    label = LABEL.search(badging)
    icons = ICON.findall(badging)
    return {'label': label.group(1) if label else None,
            'icon_candidates': list(reversed(icons)),
            'has_launchable_activity': bool(LAUNCHABLE.search(badging))}


def export_icon(apk_path, candidates, destination):
    """Only exact file-backed image resources; no false placeholder or XML claims."""
    with zipfile.ZipFile(apk_path) as archive:
        for item in candidates:
            if not item.startswith('res/') or '..' in Path(item).parts:
                continue
            if Path(item).suffix.lower() not in ('.png', '.webp'):
                continue
            try:
                info = archive.getinfo(item)
            except KeyError:
                continue
            if info.file_size > 2_000_000:
                continue
            data = archive.read(info)
            if (item.endswith('.png') and not data.startswith(b'\x89PNG\r\n\x1a\n')) or (item.endswith('.webp') and data[8:12] != b'WEBP'):
                continue
            destination.write_bytes(data)
            return item
    return None


def run(args, timeout=20):
    return subprocess.run(args, capture_output=True, text=True, check=True, timeout=timeout).stdout


def main():
    parser = argparse.ArgumentParser(description='AnyTV TCL ADB QA: installed inventory, original icon samples and explicit launch')
    parser.add_argument('--serial', required=True, help='Exact previously authorized adb device serial, e.g. IP:PORT')
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--inspect', action='append', default=[], help='Package to inspect/pull, repeatable; no package = inventory only')
    parser.add_argument('--launch', help='Package to launch with Android TV LEANBACK_LAUNCHER')
    parser.add_argument('--confirm-launch', action='store_true', help='Explicitly authorize launch on connected TV')
    args = parser.parse_args()
    if not args.serial.strip() or args.serial.startswith('-'):
        parser.error('Exact non-empty ADB serial required')
    if any(not PACKAGE.fullmatch(pkg) for pkg in args.inspect + ([args.launch] if args.launch else [])):
        parser.error('Invalid package name')
    if args.launch and not args.confirm_launch:
        parser.error('--launch requires --confirm-launch')
    if not args.launch and args.confirm_launch:
        parser.error('--confirm-launch requires --launch')
    state = run(['adb', '-s', args.serial, 'get-state']).strip()
    if state != 'device':
        raise RuntimeError('ADB_DEVICE_NOT_AUTHORIZED')
    installed = packages_from_pm(run(['adb', '-s', args.serial, 'shell', 'pm', 'list', 'packages']))
    args.output.mkdir(parents=True, exist_ok=True)
    records = [{'package': pkg, 'installed': True, 'label': None,
                'icon_status': 'NOT_INSPECTED', 'launch_status': 'NOT_TESTED'} for pkg in installed]
    index = {r['package']: r for r in records}
    for pkg in args.inspect:
        if pkg not in index:
            raise RuntimeError('PACKAGE_NOT_INSTALLED: ' + pkg)
        record = index[pkg]
        paths = apk_paths_from_pm(run(['adb', '-s', args.serial, 'shell', 'pm', 'path', pkg]))
        if not paths:
            record['icon_status'] = 'APK_PATH_UNAVAILABLE'
            continue
        with tempfile.TemporaryDirectory(prefix='anytv-tcl-adb-') as tmp:
            for position, remote_path in enumerate(paths):
                local = Path(tmp) / f'{position}.apk'
                run(['adb', '-s', args.serial, 'pull', remote_path, str(local)], timeout=90)
                badging = run(['aapt', 'dump', 'badging', str(local)], timeout=20)
                info = apk_metadata(badging)
                if info['label']:
                    record['label'] = info['label']
                if info['icon_candidates']:
                    extension = '.webp' if info['icon_candidates'][0].endswith('.webp') else '.png'
                    target = args.output / (pkg + extension)
                    if export_icon(local, info['icon_candidates'], target):
                        record['icon_status'] = 'EXTRACTED'
                        record['icon_file'] = target.name
                        break
            if record['icon_status'] == 'NOT_INSPECTED':
                record['icon_status'] = 'UNSUPPORTED_RESOURCE_OR_SPLIT'
    if args.launch:
        if args.launch not in index:
            raise RuntimeError('PACKAGE_NOT_INSTALLED: ' + args.launch)
        outcome = run(['adb', '-s', args.serial, 'shell', 'monkey', '-p', args.launch,
                       '-c', 'android.intent.category.LEANBACK_LAUNCHER', '1'], timeout=30)
        index[args.launch]['launch_status'] = 'COMMAND_SENT_NEEDS_VISUAL_CONFIRMATION' if 'Events injected: 1' in outcome else 'FAILED_OR_UNVERIFIED'
    result = {'schema': 1, 'transport': 'ADB_AUTHORIZED_EXISTING',
              'real_tv_visual_verification': False, 'apps': records}
    (args.output / 'inventory.json').write_text(json.dumps(result, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print(json.dumps({'packages_found': len(records), 'icons_extracted': sum(r['icon_status']=='EXTRACTED' for r in records),
                      'physical_qa': 'PENDING'}, ensure_ascii=False))


if __name__ == '__main__':
    main()
