# Lenovo Xiaoxin Pad Plus (TB-J607F) — PixelOS device tree (DRAFT)

**Status: never built or booted; stock values filled in.** This is a starting point for
porting the TB520FU tree's setup to the TB-J607F. The layout follows the
TB520FU tree; the platform parts (lito, msm-4.19) come from the LineageOS
trees for other SM7225/SM7250 devices. Every value that still has to come
from the stock J607F firmware is marked `TODO(J607F)` in the files and listed
below.

| | |
|---|---|
| SoC | Qualcomm SM7225 Snapdragon 750G (lito), Adreno 619 |
| Kernel | msm-4.19 (non-GKI), Lenovo's "arnoz" source release rebased on 4.19.325 + CIP, built from source |
| Display | 11" IPS LCD, 1200×2000 portrait-native BOE panel (nt36523 or Himax), 60 Hz, density 240 |
| Memory | 6 GB RAM, 128 GB UFS, microSD |
| Audio | 4 speakers (JBL) on 4× Cirrus CS35L41 amps, Dolby Atmos, no 3.5 mm jack |
| Cameras | 13 MP rear OV13B10 (AF), 8 MP front OV8856 or S5K4H7 |
| Sensors | BMI26x or ICM4x6xx accel/gyro, LTR578 light/proximity, TMF8801 ToF proximity, Abov SAR, hall |
| Connectivity | Wi-Fi 5 (dual band), Bluetooth 5.1 (WCN39xx), GNSS, no cellular |
| Battery | 7700 mAh |
| Shipped | Android 11, ZUI 12.5 (shipping API 30) |

Hardware facts from [GSMArena](https://m.gsmarena.com/lenovo_pad_plus-10927.php),
checked against the stock J607F firmware (ZUI 12.6.142_210801) and the stock
firmware of the 5G sibling TB-J607Z (same board, S540663_241109).

## Where this directory goes

The tree belongs in its own repository, `android_device_lenovo_J607F`,
checked out at `device/lenovo/J607F` (see `tools/local_manifest.xml` and
`lineage.dependencies`). It sits in this repository only for review.
`.find-ignore` keeps the build system from scanning it while it lives under
`device/lenovo/TB520FU`; delete that file when it moves to its own repository.

## What came from where

| Part | Source |
|---|---|
| Layout, `custom_J607F.mk`, `AndroidProducts.mk`, overlays, `.prop` split, `extract-files.py` / `setup-makefiles.py` style | TB520FU (this repository) |
| `BoardConfig.mk` lito / 4.19 kernel / partitions / Wi-Fi | LineageOS [gauguin](https://github.com/LineageOS/android_device_xiaomi_gauguin) (SM7225) and [motorola/sm7250-common](https://github.com/LineageOS/android_device_motorola_sm7250-common) (A/B lito), `lineage-24.0` |
| `device.mk` HAL selection | gauguin and motorola/sm7250-common |
| `init/` rc files, `ueventd`, `init.qti.dcvs.sh`, `configs/wifi`, `configs/power/powerhint.json`, `config.fs` | gauguin, minus radio / NFC / ATFWD / Xiaomi camera parts |
| `init/fstab.qcom` | gauguin, changed to A/B (slotselect) |
| `vintf/manifest.xml` | gauguin, minus radio / IMS / data / NFC / fingerprint HALs |
| `proprietary-files.txt` | gauguin platform sections only (see its header) |
| `sepolicy/` | generic lito rules from gauguin |
| Kernel choice, module and dtc notes | [bigsaltyfishes/device_lenovo_J607F](https://github.com/bigsaltyfishes/device_lenovo_J607F), an earlier community J607F ROM tree (Android 12/13) on the same kernel lineage |

Not taken from TB520FU: the pen, keyboard and folio code (`input/`,
`parts/`, `lenovo/`), Dolby, the GKI kernel setup and the pineapple init
scripts. Those are specific to TB520FU hardware (SM8650, Pen Plus, the
Bluetooth keyboard). The J607F's pogo-pin keyboard and Precision Pen 2 need
their own look once the device boots.

## Stock firmware values

Filled from the stock J607F firmware (`TB-J607F_CN_WIFI_USER_Q00010.0_R_ZUI_12.6.142_ST_210801`)
and, for the blobs, the TB-J607Z firmware (`TB-J607Z_S540663_241109_ROW`).
The J607Z is the 5G model of the same board: same 4.19.157 kernel (22 config
lines apart), DTBO, vendor kernel modules, cameras and audio configs, but its
vendor image is two years newer (security patch 2023-01-05).

| What | Value | Where |
|---|---|---|
| A/B, Virtual A/B | yes (`ro.virtual_ab.enabled=true`) | `BoardConfig.mk`, `device.mk` |
| Recovery | separate `recovery_a/_b` (96 MiB), not recovery-as-boot | `BoardConfig.mk` |
| Boot image | header v2, page 4096, base 0x0, dtb in boot, no vendor_boot | `BoardConfig.mk` |
| Partitions | boot 96 MiB, recovery 96 MiB, dtbo 24 MiB, super 10 GiB, group super − 8 MiB | `BoardConfig.mk` |
| Filesystems | ext4 for every logical partition | `BoardConfig.mk`, `init/fstab.qcom` |
| fstab | stock `/vendor/etc/fstab.qcom` | `init/fstab.qcom` |
| Density, rotation | 240; portrait panel, no recovery rotation | `BoardConfig.mk`, `vendor.prop` |
| Vendor security patch | 2023-01-05 (matches the J607Z blobs) | `BoardConfig.mk` |
| Vendor / odm props | stock J607Z build.prop, radio props dropped | `vendor.prop`, `odm.prop` |
| Blob list | gauguin platform entries trimmed to stock, plus Lenovo audio, camera, Dolby, sensors, thermal, touch firmware and their NEEDED libs | `proprietary-files.txt` |
| GNSS, vibrator | both present | `device.mk` |
| Sensors | accel, gyro, light, proximity, step counter/detector; no compass | `device.mk` |
| Keymaster | 4.1 | `vintf/manifest.xml`, `proprietary-files.txt` |
| WLAN firmware | `qca_cld/`, stock symlinks | `Android.bp` |
| Init | stock SAR permissions, Lenovo ship mode and battery protection triggers, audio module loading | `init/init.target.rc` |
| Kernel | [jaysungheo/kernel_lenovo_J607Z](https://github.com/jaysungheo/kernel_lenovo_J607Z) `CipA17` (fork of nzlnice/kernel_lenovo_J607Z), `vendor/arnoz_defconfig` | `BoardConfig.mk`, `tools/local_manifest.xml` |
| dtb / dtbo | built from the kernel source; its Lenovo board overlay (Lagoon QRD, board-id 0x1000b) matches stock | `BoardConfig.mk` |

The stock dumps, configs and comparison notes live in the project files
(`j607f-stock/`, `j607z-stock/`).

## Still open

| What | Where | How to get it |
|---|---|---|
| Extra blob fixups | `extract-files.py` | first `m nothing` / boot |
| Display blobs pinned or not | `proprietary-files.txt` (Display, Display Postprocessing) | gauguin pins older `libsdm-*`, `libdisplayqos`, `qdcmss` to match the source display HAL; the J607Z ones are taken unpinned, so pin them if the display HAL rejects them |
| Rear camera EEPROM libs | `proprietary-files.txt` (Camera) | the J607Z camera set has no `com.qti.eeprom.lenovo_ov13b10_*` or `com.qti.node.lenovo` (J607F 2021 firmware does); add them from the J607F dump only if the rear camera fails to read calibration |
| VINTF manifest | `vintf/manifest.xml` | reconcile the rest with the stock manifest and fragments |
| IRQ numbers for msm_drm / kgsl | `init/init.target.rc` | `/proc/interrupts` on a running device |
| Auto-brightness curve, backlight range, power profile | `overlay/FrameworksResJ607F` | stock framework-res overlay APK |
| Device nodes and sysfs labels (touch, pen, keyboard dock, amp, Type-C) | `sepolicy/vendor` | stock vendor `file_contexts`, booted device |
| Which IMU fits (BMI26x or ICM4x6xx) | none, both configs ship | runtime |

## Known risks

- PixelOS `seventeen` (Android 17) on a 4.19 kernel: LineageOS still ships
  lito 4.19 devices on `lineage-24.0` (gauguin), so it is possible. The
  kernel branch already carries the BPF ring buffer, EROFS and dm-user
  backports; uclamp is still missing, as on every 4.19 kernel.
- The kernel is a community branch (4.19.325 + CIP + KernelSU-free
  Android 17 backports), not Lenovo's own 4.19.157 tree. It builds cleanly
  with clang 18 and produces the stock module set, but it has not been
  booted with this tree yet. Lenovo's untouched 4.19.157 release is the
  `Source Code from Lenovo OpenSource Portal` commit (893a4957927d) in
  [bigsaltyfishes/kernel_lenovo_J607Z](https://github.com/bigsaltyfishes/kernel_lenovo_J607Z)
  if a bisect is ever needed.
- The panel init sequence in the source's board overlay is the J607Z ROW one,
  which differs from stock J607F in a few nt36523 register values.

## Next steps

1. ~~Dump the stock J607F firmware~~ and ~~fill the partition, boot image
   and fstab placeholders~~ (done, see above).
2. ~~Run `./extract-files.py <J607Z dump>` to create `vendor/lenovo/J607F`~~
   (done: 643 blobs from `TB-J607Z_S540663_241109_ROW`, repository
   `android_vendor_lenovo_J607F`, see `tools/local_manifest.xml`).
3. ~~Set up `kernel/lenovo/J607F`~~ (done: see `tools/local_manifest.xml`).
4. `breakfast J607F` and work through `m nothing` errors, then boot.
