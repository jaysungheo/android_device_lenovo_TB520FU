# Lenovo Yoga Tab Plus (TB520FU) — PixelOS device tree

Unofficial device tree for building PixelOS (Android 17, `seventeen`) for the
Lenovo Yoga Tab Plus / YOGA Pad Pro (TB520FU, Qualcomm Snapdragon 8 Gen 3).

| | |
|---|---|
| SoC | Qualcomm SM8650 (pineapple) |
| Kernel | GKI `6.1.138-android14-11`, built from source |
| Display | 2944×1840 dual-DSI, natively landscape, density 340 |
| Stock firmware | `ZUI_17.5.10.362_260719_ROW` |
| Shipping API | 34 |

Status: used daily. SELinux enforcing, dm-verity on, and the bootloader can be
relocked (see "Verified boot"). Widevine L1 (Netflix HD), Play Integrity
BASIC.

## Downloads

Latest build: [20260926-1818](https://github.com/wnduddld0513/android_device_lenovo_TB520FU/releases/tag/20260926-1818),
installation steps in the release notes. Files are on
[SourceForge](https://sourceforge.net/projects/pixelos-unofficial-tb520fu/files/seventeen/).
Pick the region of your device: ROW and PRC only differ in the device tree
(dtb) and the signed images that carry it.

| Region | File | SHA256 |
|---|---|---|
| ROW | [`PixelOS_TB520FU-17.0-20260926-1818-ROW.zip`](https://sourceforge.net/projects/pixelos-unofficial-tb520fu/files/seventeen/ROW/PixelOS_TB520FU-17.0-20260926-1818-ROW.zip/download) | `0ea2f3d1f9273c8d9b15acbe8ce967d0cdd57e907001db159cb5981d29973ffa` |
| ROW | [`ltbox_ROW_PixelOS_TB520FU-17.0-20260926-1818.7z`](https://sourceforge.net/projects/pixelos-unofficial-tb520fu/files/seventeen/ROW/ltbox_ROW_PixelOS_TB520FU-17.0-20260926-1818.7z/download) | `b6bf8364530604f45f2e0d4fd266b42907b12bf885514bbb60688b33770220bc` |
| PRC | [`PixelOS_TB520FU-17.0-20260926-1818-PRC.zip`](https://sourceforge.net/projects/pixelos-unofficial-tb520fu/files/seventeen/PRC/PixelOS_TB520FU-17.0-20260926-1818-PRC.zip/download) | `75afeecaf0ca1ba05e76a83148619c99e5cc16f2cf3d3f618ed4ef996ba5dc33` |
| PRC | [`ltbox_PRC_PixelOS_TB520FU-17.0-20260926-1818.7z`](https://sourceforge.net/projects/pixelos-unofficial-tb520fu/files/seventeen/PRC/ltbox_PRC_PixelOS_TB520FU-17.0-20260926-1818.7z/download) | `ebcbd8b06f3733de96483049925590cb7dc8a059b0898c9966d7ea6b703a6435` |

The `.zip` installs from TWRP or the PixelOS recovery; the `ltbox_*.7z` is a
full firmware package for LTBox (EDL) and wipes the device.

## Repositories

| Path | Repository | Contents |
|---|---|---|
| `device/lenovo/TB520FU` | `android_device_lenovo_TB520FU` | this tree |
| `kernel/lenovo/TB520FU` | `android_kernel_lenovo_TB520FU` | Android common kernel `android14-6.1` at `2ecae636cf9b` (the source of the stock GKI kernel) plus the Qualcomm UAPI headers |
| `vendor/lenovo/TB520FU` | `android_vendor_lenovo_TB520FU` | proprietary blobs, plus the stock vendor kernel modules, dtb and dtbo in `kernel/` (Git LFS for files over 50 MB) |

`tools/local_manifest.xml` lists them; `lineage.dependencies` does the same
for roomservice.

## Kernel

The stock firmware runs Google's GKI build of `android14-6.1`
(`6.1.138-android14-11-g2ecae636cf9b-ab14676408`). This tree builds the same
source with `gki_defconfig` (clang r547379; GKI uses r487747c, newer clang
fails on this kernel). The 60 GKI modules go to system_dlkm, signed with a
key generated for each build. Lenovo/Qualcomm only ship the vendor modules
(vendor_boot, vendor_dlkm, all unsigned) and the device trees; their source
is not published at this version, so they come from the stock firmware
(`vendor/lenovo/TB520FU/kernel/`). Only `android14-6.1` updates keep working
with them (stable KMI).

Source: [kernel/common](https://android.googlesource.com/kernel/common/+/2ecae636cf9be43fdfe04adb25b2c2987838955a),
Lenovo's release: https://support.lenovo.com/us/en/solutions/ht511330-lenovo-open-source-portal

## Getting the source

Git LFS is needed for the wallpaper APK in this tree and for two blobs in the
vendor repository.

```bash
sudo apt install git-lfs && git lfs install
mkdir pixelos && cd pixelos
repo init -u https://github.com/PixelOS-AOSP/android_manifest -b seventeen --git-lfs
mkdir -p .repo/local_manifests
# copy tools/local_manifest.xml to .repo/local_manifests/TB520FU.xml and
# set fetch= to the GitHub account hosting the four repositories
repo sync -c -j$(nproc)
repo forall device/lenovo/TB520FU vendor/lenovo/TB520FU -c git lfs pull
```

## Building

The PixelOS source needs a few patches (see `patches/apply.sh`). They are
applied with `git apply` only, nothing is committed, so `repo sync` keeps
working; run the script again after every sync.

```bash
bash device/lenovo/TB520FU/patches/apply.sh
source build/envsetup.sh
breakfast TB520FU
m pixelos
```

or unattended, with the log in `build.log` and the result in `build.status`:

```bash
setsid nohup device/lenovo/TB520FU/tools/build.sh > /dev/null 2>&1 < /dev/null &
```

## Installing

Sideload the OTA package (`out/target/product/TB520FU/PixelOS_TB520FU-*.zip`)
from the PixelOS recovery: Apply update > Apply from ADB, then
`adb sideload <zip>`. It installs to the other slot, like any A/B update.

If data has to be wiped (first install, or a change of signing keys), format
data **before** sideloading. Formatting after the sideload also wipes the
update snapshot in `/metadata`, and the new slot does not boot.

The pvmfw image is part of the package: with dm-verity on, the bootloader
checks it through vbmeta (stock `pvmfw.img` in the vendor repository, added
with `--include_descriptors_from_image`).

## Layout

Based on the LineageOS OnePlus Pad 2 (`caihong`) and `oneplus/sm8650-common`
trees, merged into one tree with the OnePlus-specific parts removed.

- `init/`, `vintf/`, `sepolicy/`, `overlay/` — from the stock firmware
  (`LapisRowFrameworksOverlay`, `LapisRowWifiResOverlay`,
  `manifest_pineapple.xml` without IMS/DPM, the device is Wi-Fi only).
- `health/` — QTI health HAL copy that ignores the pen charger (`wls_tx`).
- `parts/` — TB520FUParts, "Lenovo features" in Settings > System: charging
  modes, white balance strength, game performance levels per app, memory
  extension (zram writeback), pen settings, and the physical keyboard page of
  the stock settings (stock strings copied by
  `tools/lenovo_keyboard_strings.py`).
- `input/` — `tb520fu-input.jar`, loaded into system_server as a
  DeviceKeyHandler: Lenovo pen (attach, pairing, battery, writing haptics,
  buttons), keyboard keys, charging modes and double tap to wake, ported from
  the stock ZUI services (see `input/NOTICE`). Also the converted Lenovo
  keylayouts.
- `lenovo/PenService/` — the stock PenService with a compat dex for APIs that
  changed in Android 17.
- `patches/` — PixelOS source patches, applied by `patches/apply.sh`.
- `extras/` — maintainer additions (default wallpaper), see `extras/README.md`.
- `tools/bringup/` — scripts used to generate `proprietary-files.txt` and the
  props from a stock dump (`TB520FU_STOCK`, default `~/tb520fu`).

## Verified boot

The stock firmware is signed with the public AOSP `testkey_rsa4096` (the flaw
LTBox uses), and this tree signs the same way: recovery is chained at
location 1, vbmeta_system at 2 and boot at 3. The device has no
`vbmeta_vendor`. dm-verity is on, so after checking that a build boots
unlocked, `fastboot flashing lock` (wipes data) gives a locked, green boot.

Apps are signed with the AOSP test keys, so anyone can build compatible
updates. Switching to other keys later needs a data wipe.

## Extracting blobs

```bash
./extract-files.py <dump>
```

`<dump>` is an extracted stock firmware containing `vendor/`, `odm/`,
`system_ext/` and `product/`. Not needed when the vendor repository is
synced.
