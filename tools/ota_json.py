#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#
"""Write the update descriptions of a build for the Updater.

For every full ROM zip (PixelOS_TB520FU-<version>-<date>[-ROW].zip) this writes
<zip name without .zip>.json, the Updater's update list with one entry:

  files        the full package
  incremental  the incremental package of the same variant, if given with
               --incremental (PixelOS_TB520FU-<version>-<date>-incremental-
               <base date>[-ROW].zip)

Upload the .json files with the full and incremental zips into the OTA folder
(builds up to 20260927-1632 accept packages only from the folder of the
.json, so keep everything of a release together there). The
updater of a running build picks the newest description of its own variant
(PRC / ROW) that is newer than itself, uses the incremental package when
update_engine can apply it to the running build and the full one otherwise.

With --key it also writes <...>.json.sig (Ed25519 signature of the JSON, raw
64 bytes), only needed for builds with updater_signing_public_key set.

usage: ota_json.py [--incremental INC.zip --base-full BASE.zip]...
                   [--key KEY.pem] FULL.zip...
"""
import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
import tempfile
import zipfile

OTA_URL = 'https://sourceforge.net/projects/pixelos-unofficial-tb520fu/files/seventeen/OTA'
FULL_RE = re.compile(r'PixelOS_TB520FU-([0-9.]+)-(\d{8}-\d{4})(-ROW)?\.zip')
INC_RE = re.compile(r'PixelOS_TB520FU-([0-9.]+)-(\d{8}-\d{4})-incremental-(\d{8}-\d{4})(-ROW)?\.zip')


def metadata(path):
    with zipfile.ZipFile(path) as z:
        text = z.read('META-INF/com/android/metadata').decode()
    return dict(line.split('=', 1) for line in text.splitlines() if '=' in line)


def sha256(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        for chunk in iter(lambda: f.read(1 << 20), b''):
            h.update(chunk)
    return h.hexdigest()


def sign(key, data):
    with tempfile.NamedTemporaryFile() as tmp:
        tmp.write(data)
        tmp.flush()
        sig = subprocess.run(['openssl', 'pkeyutl', '-sign', '-rawin', '-inkey', key,
                              '-in', tmp.name], check=True, capture_output=True).stdout
    if len(sig) != 64:
        sys.exit('unexpected signature length %d (is the key Ed25519?)' % len(sig))
    return sig


def file_entry(path, url, meta, streaming):
    entry = {
        'filename': os.path.basename(path),
        'os_patch_level': meta['post-security-patch-level'],
        'os_sdk_level': int(meta['post-sdk-level']),
        'sha256': sha256(path),
        'size': os.path.getsize(path),
        'url': url,
    }
    if streaming:
        # The updater checks with the payload metadata whether update_engine can
        # apply the incremental to the running build, and streams it.
        property_files = meta['ota-property-files'].strip()
        ranges = {}
        for token in property_files.split(','):
            fields = token.strip().split(':')
            if len(fields) != 3:
                sys.exit('%s: malformed ota-property-files entry: %s' % (path, token))
            name, offset, size = fields
            try:
                offset, size = int(offset), int(size)
            except ValueError:
                sys.exit('%s: invalid ota-property-files range: %s' % (path, token))
            if name in ranges or offset < 0 or size <= 0 or offset + size > entry['size']:
                sys.exit('%s: invalid ota-property-files range: %s' % (path, token))
            ranges[name] = (offset, size)
        if not {'payload_metadata.bin', 'payload.bin', 'payload_properties.txt'} <= ranges.keys():
            sys.exit('%s: missing payload streaming ranges' % path)
        entry['ota_property_files'] = property_files
    return entry


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('--incremental', action='append', default=[],
                    help='incremental package of one of the builds (repeatable)')
    ap.add_argument('--base-full', action='append', default=[],
                    help='full package of an incremental source build (repeatable)')
    ap.add_argument('--ota-url', default=OTA_URL,
                    help='SourceForge folder the .json and packages are uploaded to')
    ap.add_argument('--key', help='Ed25519 private key (PEM), to also write <...>.json.sig')
    ap.add_argument('zips', nargs='+', help='full packages')
    args = ap.parse_args()
    folder = args.ota_url.rstrip('/')
    if not folder.startswith('https://sourceforge.net/projects/'):
        sys.exit('OTA URL must be a https://sourceforge.net/projects/ folder')

    bases = {}
    for path in args.base_full:
        match = FULL_RE.fullmatch(os.path.basename(path))
        if not match:
            sys.exit('unexpected base package name: ' + os.path.basename(path))
        key = (match.group(1), match.group(2), bool(match.group(3)))
        if key in bases:
            sys.exit('two base packages for the same build and variant: ' + path)
        bases[key] = metadata(path)

    incrementals = {}
    for path in args.incremental:
        m = INC_RE.fullmatch(os.path.basename(path))
        if not m:
            sys.exit('unexpected incremental package name: ' + os.path.basename(path))
        key = (m.group(1), m.group(2), bool(m.group(4)))
        if key in incrementals:
            sys.exit('two incrementals for the same build and variant: ' + path)
        incrementals[key] = path

    for path in args.zips:
        name = os.path.basename(path)
        m = FULL_RE.fullmatch(name)
        if not m:
            sys.exit('unexpected package name: ' + name)
        meta = metadata(path)
        if meta.get('ota-type') != 'AB':
            sys.exit(name + ': not an A/B package')
        update = {
            'datetime': int(meta['post-timestamp']),
            'version': m.group(1),
            'files': [file_entry(path, '%s/%s/download' % (folder, name), meta,
                                 streaming=False)],
        }
        inc = incrementals.pop((m.group(1), m.group(2), bool(m.group(3))), None)
        if inc:
            inc_meta = metadata(inc)
            inc_name = INC_RE.fullmatch(os.path.basename(inc))
            base = bases.get((inc_name.group(1), inc_name.group(3), bool(inc_name.group(4))))
            if base is None:
                sys.exit('%s: --base-full for its source build is required' % inc)
            if (inc_meta.get('ota-type') != 'AB' or
                    inc_meta.get('post-timestamp') != meta['post-timestamp'] or
                    inc_meta.get('post-build') != meta.get('post-build') or
                    inc_meta.get('post-build-incremental') != meta.get('post-build-incremental') or
                    inc_meta.get('pre-device') != meta.get('pre-device') or
                    inc_meta.get('pre-build') != base.get('post-build') or
                    inc_meta.get('pre-build-incremental') != base.get('post-build-incremental') or
                    base.get('pre-device') != meta.get('pre-device')):
                sys.exit('%s is not an incremental to %s' % (os.path.basename(inc), name))
            update['incremental'] = [file_entry(
                inc, '%s/%s/download' % (folder, os.path.basename(inc)), inc_meta,
                streaming=True)]
        data = (json.dumps([update], indent=2) + '\n').encode()
        base = os.path.splitext(path)[0]
        with open(base + '.json', 'wb') as f:
            f.write(data)
        if args.key:
            with open(base + '.json.sig', 'wb') as f:
                f.write(sign(args.key, data))
        print('wrote %s.json%s%s' % (os.path.basename(base), '(.sig)' if args.key else '',
                                    ' with incremental' if inc else ''))
    if incrementals:
        sys.exit('incrementals without their full package: %s' % list(incrementals.values()))


if __name__ == '__main__':
    main()
