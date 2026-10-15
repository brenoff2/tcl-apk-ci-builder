"""Fail-closed documentary context verification; never proves OTA or device execution."""
from __future__ import annotations
import argparse
from pathlib import Path
import re

FIELDS = ('repository','branch','observed_commit','observed_at_utc','build_status',
          'signature_status','ota_status','device_test_status','next_action')
REQUIRED = ('AGENTS.md','docs/dev-context/STATUS.md','docs/dev-context/DECISIONS.md',
            'docs/dev-context/CHANGELOG_WORK.md','docs/ANYTV_RELEASE_SIGNING_AND_UPDATES.md',
            'docs/CONTRATO_FUNCIONAL_TCL.md')

def _extract(text: str) -> tuple[dict[str,str],list[str]]:
    match = re.search(r'<!--\s*context-state\s*\n(.*?)\nend-context-state\s*-->',text,re.S)
    if match is None:
        return {},['missing context-state block']
    data,errors={},[]
    for line in match.group(1).splitlines():
        if not line.strip(): continue
        if '=' not in line:
            errors.append('malformed context-state line');continue
        key,value=(v.strip() for v in line.split('=',1))
        if key in data: errors.append(f'duplicate field: {key}')
        elif not value: errors.append(f'empty field: {key}')
        else: data[key]=value
    return data,errors

def parse_status(text: str) -> dict[str,str]:
    data,errors=_extract(text)
    if errors: raise ValueError('; '.join(errors))
    return data

def validate_context(root: Path, *, expected_branch: str | None=None,
                     current_sha: str | None=None, strict_map: bool=False) -> list[str]:
    root=Path(root)
    errors=[]
    for rel in REQUIRED:
        if not (root/rel).is_file(): errors.append(f'missing required file: {rel}')
    path=root/'docs/dev-context/STATUS.md'
    if not path.is_file(): return errors
    text=path.read_text(encoding='utf-8')
    data,err=_extract(text)
    errors.extend(err)
    for field in FIELDS:
        if not data.get(field): errors.append(f'missing context field: {field}')
    for field in sorted(set(data)-set(FIELDS)):
        errors.append(f'unknown context field: {field}')
    if data.get('repository')!='brenoff2/tcl-apk-ci-builder': errors.append('repository mismatch')
    if expected_branch and data.get('branch')!=expected_branch:
        errors.append(f'branch mismatch: expected {expected_branch}; recorded {data.get("branch")}')
    sha=data.get('observed_commit','')
    if not re.fullmatch(r'[a-f0-9]{40}',sha): errors.append('invalid observed_commit SHA')
    if current_sha and sha!=current_sha: errors.append('observed_commit stale')
    if not re.fullmatch(r'\d{4}-\d\d-\d\dT\d\d:\d\d:\d\dZ',data.get('observed_at_utc','')):
        errors.append('invalid observed_at_utc')
    for field in ('build_status','signature_status','ota_status','device_test_status'):
        m=re.search(r'^\s*-\s*'+chr(96)+re.escape(field)+chr(96)+r'\s*:\s*(.+)$',text,re.M)
        if m is None:
            errors.append(f'{field}: evidence record missing')
            continue
        proof=m.group(1).strip()
        unverified=re.match(r'^UNVERIFIED(?:$|[,;:. ])',proof) is not None
        if not ('https://github.com/' in proof or (unverified and 'UNVERIFIED' in data.get(field,''))):
            errors.append(f'{field}: evidence must link to GitHub or explicitly be UNVERIFIED')
    agents=root/'AGENTS.md'
    if agents.is_file():
        a=agents.read_text(encoding='utf-8')
        for source in ('docs/dev-context/STATUS.md','docs/ANYTV_RELEASE_SIGNING_AND_UPDATES.md','docs/CONTRATO_FUNCIONAL_TCL.md'):
            if source not in a: errors.append('AGENTS.md omits authoritative source: '+source)
    if strict_map:
        path=root/'docs/dev-context/CODEBASE_MAP.md'
        if not path.is_file(): errors.append('strict_map: CODEBASE_MAP.md missing')
        else:
            m=re.search(r'^source_commit=([a-f0-9]{40})$',path.read_text(encoding='utf-8'),re.M)
            if not m: errors.append('strict_map: missing source_commit')
            elif m.group(1)!=(current_sha or sha): errors.append('strict_map: stale map source_commit')
    return errors

def main()->int:
    parser=argparse.ArgumentParser(description='Read-only AnyTV context validation')
    parser.add_argument('--root',default='.')
    parser.add_argument('--expected-branch')
    parser.add_argument('--current-sha')
    parser.add_argument('--strict-map',action='store_true')
    args=parser.parse_args()
    errors=validate_context(Path(args.root),expected_branch=args.expected_branch,
                            current_sha=args.current_sha,strict_map=args.strict_map)
    for e in errors: print('FAIL:',e)
    if errors: return 1
    print('PASS: documentary consistency only; no OTA/device verification')
    return 0

if __name__=='__main__':
    raise SystemExit(main())
