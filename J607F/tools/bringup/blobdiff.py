#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#
"""Compare the draft J607F proprietary-files.txt against a stock J607F dump.

Same idea as the TB520FU tools/bringup/blobdiff.py, with the draft list of
this tree (gauguin-derived) as the reference.

usage: J607F_STOCK=~/j607f blobdiff.py <out dir> [list, default ../../proprietary-files.txt]

J607F_STOCK is an extracted stock firmware (vendor/, odm/, system_ext/,
product/, system/ directories, e.g. from the super.img of the stock package).

Writes to <out dir>:
  matched.txt    draft entries (lines kept verbatim) whose file exists in stock
  missing.txt    draft entries absent from stock (drop or find the Lenovo name)
  unlisted.txt   stock vendor/odm files the draft does not name
"""
import os
import sys

STOCK = os.environ.get("J607F_STOCK", os.path.expanduser("~/j607f"))
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = sys.argv[1]
LIST = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, "..", "..", "proprietary-files.txt")
PARTS = ["vendor", "odm", "system_ext", "product"]


def src_path(line):
    return line.lstrip("-").split(";")[0].split(":")[0].split("|")[0].strip()


def exists(p):
    if p.split("/")[0] not in PARTS:
        return os.path.exists(os.path.join(STOCK, "system", p))
    return os.path.exists(os.path.join(STOCK, p))


if not os.path.isdir(os.path.join(STOCK, "vendor")):
    sys.exit(f"no vendor/ in {STOCK}; set J607F_STOCK to the extracted stock dump")

stock = set()
for part in ("vendor", "odm"):
    for d, _, files in os.walk(os.path.join(STOCK, part)):
        for f in files:
            stock.add(os.path.relpath(os.path.join(d, f), STOCK))

matched, missing, listed = [], [], set()
section = ""
for line in open(LIST):
    line = line.rstrip("\n")
    if not line or line.startswith("#"):
        if line.startswith("# "):
            section = line
        continue
    p = src_path(line)
    listed.add(p)
    (matched if exists(p) else missing).append((section, line))

os.makedirs(OUT, exist_ok=True)


def dump(name, rows):
    with open(os.path.join(OUT, name), "w") as f:
        last = None
        for section, line in rows:
            if section != last:
                f.write("\n" + section + "\n")
                last = section
            f.write(line + "\n")


dump("matched.txt", matched)
dump("missing.txt", missing)
unlisted = sorted(p for p in stock if p not in listed)
with open(os.path.join(OUT, "unlisted.txt"), "w") as f:
    f.write("\n".join(unlisted) + "\n")
print(f"matched {len(matched)}  missing {len(missing)}  unlisted {len(unlisted)}")
