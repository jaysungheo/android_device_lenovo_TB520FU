# Lenovo Yoga Tab Plus (lapis) — device tree

Unofficial device tree for the Lenovo Yoga Tab Plus / YOGA Pad Pro (model
TB520FU, codename lapis, Qualcomm Snapdragon 8 Gen 3).

| Branch | Builds |
|---|---|
| `lineage-24.0` | plain LineageOS 24 (`lineage_lapis`), no source patches |
| `seventeen` | PixelOS 17 (`custom_lapis`): `lineage-24.0` plus the source patches, LunarisDolby, video motion smoothing and the maintainer customizations (`custom/`) |

| | |
|---|---|
| SoC | Qualcomm SM8650 (pineapple) |
| Kernel | GKI `6.1.138-android14-11`, built from source |
| Display | 2944×1840 dual-DSI, natively landscape, density 340 |
| Codename | lapis (`ro.vendor.config.lgsi.project`) |
| Stock firmware | `ZUI_17.5.10.362_260719_ROW` |
| Shipping API | 34 |

Status: used daily. SELinux enforcing, dm-verity on, and the bootloader can be
relocked (see "Verified boot"). Widevine L1 (Netflix HD), Play Integrity
STRONG(RKP Sign), Thanks to [sungwon1002](https://github.com/sungwon1002).

## Downloads

Latest build: see [Releases](https://github.com/wnduddld0513/android_device_lenovo_lapis/releases/),
installation steps in the release notes. Files are on
[SourceForge](https://sourceforge.net/projects/pixelos-unofficial-tb520fu/files/seventeen/).
Pick the region of your device: ROW and PRC only differ in the device tree
(dtb) and the signed images that carry it.

The `.zip` installs from TWRP or the PixelOS recovery; the `ltbox_*.7z` is a
firmware package for LTBox (EDL). Both keep the user data when updating an
installed build; coming from the stock firmware or another ROM, format data.
Installed builds that include the optional customizations also update
themselves (Settings > System > System update).

## Repositories

Laid out like the LineageOS trees (device, SoC common tree, blobs, kernel):

| Path | Repository | Contents |
|---|---|---|
| `device/lenovo/lapis` | `android_device_lenovo_lapis` | this tree |
| `device/lenovo/sm8650-common` | `android_device_lenovo_sm8650-common` | SM8650 (pineapple) platform configuration |
| `device/lenovo/lapis-kernel` | `android_device_lenovo_lapis-kernel` | stock vendor kernel modules, dtb and dtbo |
| `kernel/lenovo/sm8650` | `android_kernel_lenovo_sm8650` | Android common kernel `android14-6.1` at `2ecae636cf9b` (the source of the stock GKI kernel) plus the Qualcomm UAPI headers |
| `vendor/lenovo/lapis` | `proprietary_vendor_lenovo_lapis` | device blobs (Git LFS for files over 50 MB) |
| `vendor/lenovo/sm8650-common` | `proprietary_vendor_lenovo_sm8650-common` | platform blobs |

Every repository has a `lineage-24.0` and a `seventeen` branch.
`tools/local_manifest.xml` lists them; `lineage.dependencies` does the same
for roomservice.

On `seventeen`, `custom/` holds the maintainer additions on top of PixelOS -
Lenovo Notes, the per-app game performance profiles, the per-app Play Store
installer switch, the OTA updater with its publishing tools and the default
live wallpaper - with their app ("Custom Tweaks"), overlays, patches and a
small system_server extension. TB520FUParts (Lenovo features) is part of
both branches.

## Kernel

The stock firmware runs Google's GKI build of `android14-6.1`
(`6.1.138-android14-11-g2ecae636cf9b-ab14676408`). This tree builds the same
source with `gki_defconfig` (clang r547379; GKI uses r487747c, newer clang
fails on this kernel). The 60 GKI modules go to system_dlkm, signed with a
key generated for each build. Lenovo/Qualcomm only ship the vendor modules
(vendor_boot, vendor_dlkm, all unsigned) and the device trees; their source
is not published at this version, so they come from the stock firmware
(`device/lenovo/lapis-kernel/`). Only `android14-6.1` updates keep working
with them (stable KMI).

Source: [kernel/common](https://android.googlesource.com/kernel/common/+/2ecae636cf9be43fdfe04adb25b2c2987838955a),
Lenovo's release: https://support.lenovo.com/us/en/solutions/ht511330-lenovo-open-source-portal

## Getting the source

Git LFS is needed for two blobs in the vendor repository (and, on
`seventeen`, for the wallpaper APK and Lenovo Notes in `custom/`).

```bash
sudo apt install git-lfs && git lfs install
mkdir pixelos && cd pixelos
repo init -u https://github.com/PixelOS-AOSP/android_manifest -b seventeen --git-lfs
mkdir -p .repo/local_manifests
# copy tools/local_manifest.xml to .repo/local_manifests/lapis.xml
repo sync -c -j$(nproc)
repo forall device/lenovo/lapis vendor/lenovo/lapis -c git lfs pull
```

## Building

The PixelOS source needs a few patches (see `patches/apply.sh`). They are
applied with `git apply` only, nothing is committed, so `repo sync` keeps
working; run the script again after every sync.

```bash
bash device/lenovo/lapis/patches/apply.sh
source build/envsetup.sh
breakfast lapis user
m pixelos
```

The script also runs `device/lenovo/lapis/custom/patches/apply.sh` when
`custom/` is there, and reverts its patches when it is gone.

The build helper selects the `user` variant and keeps ADB authentication
configured on by default. `WITH_ADB_INSECURE=true` explicitly requests the
insecure ADB setting; unset, empty and `false` values keep authentication on.
The AVB and app signing keys stay as configured below. `user` builds exclude
debug tools, ADB root and the OTA `addon.d` preservation path.

Or unattended, with the log in `build.log` and the result in `build.status`:

```bash
setsid nohup device/lenovo/lapis/tools/build.sh > /dev/null 2>&1 < /dev/null &
```

## OTA publishing

Belongs to the customizations
(`custom/`, `seventeen` only); see its README for the SourceForge folder
layout, its `tools/ota_json.py` and the incremental OTA steps.

## Installing

Sideload the OTA package (`out/target/product/lapis/PixelOS_lapis-*.zip`)
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
  modes, white balance strength, memory extension (zram writeback), pen
  settings, folio case mode, and the physical keyboard page of the stock
  settings with the keyboard firmware update (stock strings copied by
  `tools/lenovo_keyboard_strings.py`). Sub pages open as their own activity,
  like the Settings sub pages. The game performance page lives in "Custom
  Tweaks" (`device/lenovo/lapis/custom`).
- `input/` — `tb520fu-input.jar`, loaded into system_server as a
  DeviceKeyHandler: Lenovo pen (attach, pairing, battery, writing haptics,
  buttons), keyboard keys, charging modes, double tap to wake and the folio
  case mode (the cover is the sensor HAL hall effect sensor, confirmed by the
  light sensor, as on stock), ported from the stock ZUI services (see
  `input/NOTICE`). Also the converted Lenovo keylayouts.
- `lenovo/PenService/` — the stock PenService with a compat dex for APIs that
  changed in Android 17 and the PixelOS look of the pen settings (card groups).
- `lenovo/KeyboardUpdate/` — the stock keyboard firmware updaters with a
  compat dex that gives their page the PixelOS look (no resource overlays).
- `system_ext.prop` — besides the stock values: `ro.config.lgsi.device.type=pad`
  (the stock Lenovo apps use the tablet dialog layout with it) and a linear
  brightness slider like stock ZUI.
- `patches/` — PixelOS source patches, applied by `patches/apply.sh`.
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
