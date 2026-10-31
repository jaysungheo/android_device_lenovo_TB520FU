#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#
"""Check the undefined symbols of the J607F blobs against Android 17 libraries.

usage: symcheck.py <vendor repo> <android 17 vendor dir> <android 17 system dir> [32|64]

<vendor repo> is vendor/lenovo/J607F (its proprietary/ directory is scanned).
The Android 17 directories come from any Android 17 build, e.g. the vendor and
system images of a PixelOS lapis OTA (payload_dumper, fsck.erofs --extract,
debugfs rdump), with system/lib64/{libc,libm,libdl}.so copied from bootstrap/.

For each blob the DT_NEEDED closure is resolved in this order: the blobs, the
Android 17 vendor libs, the Android 17 system libs. Every undefined, non-weak
symbol that no library of the closure defines is reported, with the NEEDED
libraries that could not be found (built by the J607F tree itself, e.g. the
sm8250 display HAL, protobuf vendorcompat or the shims): a missing symbol is
only a real problem when none of those could provide it.

For 32-bit blobs the Android 17 libraries can be 64-bit only (lapis is
core_64_bit_only); names are then compared demangled, with integer widths
folded, which finds removed functions but not changed signatures.

Needs pyelftools and c++filt.
"""

import os
import re
import subprocess
import sys

from elftools.elf.dynamic import DynamicSection
from elftools.elf.elffile import ELFFile

BIONIC = {'libc.so', 'libm.so', 'libdl.so', 'libdl_android.so', 'liblog.so', 'libc++.so'}


def index(root, subdirs):
    libs = {}
    for subdir in subdirs:
        for dirpath, _, files in os.walk(os.path.join(root, subdir)):
            for f in files:
                if f.endswith('.so'):
                    libs.setdefault(f, os.path.join(dirpath, f))
    return libs


_elf_cache = {}


def load(path, bits=None):
    if path in _elf_cache:
        return _elf_cache[path]
    result = None
    try:
        with open(path, 'rb') as f:
            elf = ELFFile(f)
            if bits is None or elf.elfclass == bits:
                needed, undefined, defined = [], set(), set()
                for section in elf.iter_sections():
                    if isinstance(section, DynamicSection):
                        needed = [
                            t.needed
                            for t in section.iter_tags()
                            if t.entry.d_tag == 'DT_NEEDED'
                        ]
                    if section.name == '.dynsym':
                        for sym in section.iter_symbols():
                            if not sym.name:
                                continue
                            if sym['st_shndx'] != 'SHN_UNDEF':
                                defined.add(sym.name)
                            elif sym['st_info']['bind'] != 'STB_WEAK':
                                undefined.add(sym.name)
                result = (needed, undefined, defined)
    except Exception:
        pass
    _elf_cache[path] = result
    return result


_folded = {}


def fold(names):
    todo = [n for n in names if n not in _folded]
    if todo:
        out = subprocess.run(
            ['c++filt'], input='\n'.join(todo), capture_output=True, text=True
        ).stdout.split('\n')
        for name, demangled in zip(todo, out):
            demangled = re.sub(r'\b(\d+)(ull|ul|ll|u|l)\b', r'\1', demangled)
            _folded[name] = re.sub(
                r'\b(unsigned )?(long long|long|int)\b', 'INT', demangled
            )
    return {_folded[n] for n in names}


def main():
    blobs, a17_vendor, a17_system = sys.argv[1:4]
    bits = int(sys.argv[4]) if len(sys.argv) > 4 else 64
    lib = 'lib64' if bits == 64 else 'lib'
    blobs = os.path.join(blobs, 'proprietary')
    blob_libs = index(blobs, [f'vendor/{lib}', f'system_ext/{lib}'])
    a17_libs = [index(a17_vendor, ['lib64', lib]), index(a17_system, ['lib64', lib])]

    def find(name):
        if name in blob_libs:
            return load(blob_libs[name], bits)
        for libs in a17_libs:
            if name in libs:
                return load(libs[name])
        return None

    for dirpath, _, files in sorted(os.walk(blobs)):
        for f in sorted(files):
            path = os.path.join(dirpath, f)
            if os.path.islink(path) or '/rfsa/' in path:  # rfsa: DSP code
                continue
            elf = load(path, bits)
            if not elf:
                continue
            queue, seen, defined, unknown = list(elf[0]) + list(BIONIC), set(), set(), []
            while queue:
                name = queue.pop(0)
                if name in seen:
                    continue
                seen.add(name)
                dep = find(name)
                if not dep:
                    if name not in BIONIC:
                        unknown.append(name)
                    continue
                defined |= dep[2]
                queue.extend(dep[0])
            missing = sorted(s for s in elf[1] if s not in defined)
            if missing and bits == 32:
                folded = fold(defined)
                missing = [
                    s
                    for s in missing
                    if not s.startswith('__aeabi_') and _folded_one(s) not in folded
                ]
            if missing:
                print(f'{os.path.relpath(path, blobs)} (not found: {", ".join(unknown) or "-"})')
                for s in missing:
                    print(f'    {s}')


def _folded_one(name):
    return next(iter(fold([name])))


if __name__ == '__main__':
    main()
