#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#
"""Write the update description of a build for the Updater.

For every ROM zip (PixelOS_TB520FU-<version>-<date>[-ROW].zip) this writes
<zip name without .zip>.json (the Updater's update list with one entry), and
with --key also <...>.json.sig (Ed25519 signature of the JSON, raw 64 bytes;
only needed when the build has updater_signing_public_key set). Upload them
with the zip into the OTA folder of the SourceForge project; the updater of a
running build picks the newest description of its own variant (PRC / ROW).

The JSON has no ota_property_files on purpose: the updater then downloads the
whole package and checks its SHA-256 before installing it.

usage: ota_json.py [--key ota_signing_key.pem] [--folder-url URL] ZIP...
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

FOLDER_URL = 'https://sourceforge.net/projects/pixelos-unofficial-tb520fu/files/seventeen/OTA'
NAME_RE = re.compile(r'PixelOS_TB520FU-([0-9.]+)-(\d{8}-\d{4})(-ROW)?\.zip')


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


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('--key', help='Ed25519 private key (PEM), to also write <...>.json.sig')
    ap.add_argument('--folder-url', default=FOLDER_URL,
                    help='SourceForge folder the files are uploaded to')
    ap.add_argument('zips', nargs='+')
    args = ap.parse_args()
    folder = args.folder_url.rstrip('/')
    if not folder.startswith('https://sourceforge.net/projects/'):
        sys.exit('folder URL must be a https://sourceforge.net/projects/ folder')

    for path in args.zips:
        name = os.path.basename(path)
        m = NAME_RE.fullmatch(name)
        if not m:
            sys.exit('unexpected package name: ' + name)
        meta = metadata(path)
        if meta.get('ota-type') != 'AB':
            sys.exit(name + ': not an A/B package')
        update = [{
            'datetime': int(meta['post-timestamp']),
            'version': m.group(1),
            'files': [{
                'filename': name,
                'os_patch_level': meta['post-security-patch-level'],
                'os_sdk_level': int(meta['post-sdk-level']),
                'sha256': sha256(path),
                'size': os.path.getsize(path),
                'url': '%s/%s/download' % (folder, name),
            }],
        }]
        data = (json.dumps(update, indent=2) + '\n').encode()
        base = os.path.splitext(path)[0]
        with open(base + '.json', 'wb') as f:
            f.write(data)
        if args.key:
            with open(base + '.json.sig', 'wb') as f:
                f.write(sign(args.key, data))
        print('wrote %s.json%s' % (os.path.basename(base), '(.sig)' if args.key else ''))


if __name__ == '__main__':
    main()
