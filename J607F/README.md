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
| Kernel | msm-4.19 (non-GKI), Lenovo source release, built from source |
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
| Init | stock SAR permissions, Lenovo ship mode and battery protection triggers | `init/init.target.rc` |

The stock dumps, configs and comparison notes live in the project files
(`j607f-stock/`, `j607z-stock/`).

## Still open

| What | Where | How to get it |
|---|---|---|
| Kernel source and defconfig name | `BoardConfig.mk`, `lineage.dependencies` | Lenovo open source portal (TB-J607F release); the stock IKCONFIG is in the dump |
| dtb / dtbo built or prebuilt | `BoardConfig.mk` | does the source release carry the Lenovo Lagoon QRD (board-id 0x1000b) dts? |
| Extra blob fixups | `extract-files.py` | first `m nothing` / boot |
| VINTF manifest | `vintf/manifest.xml` | reconcile the rest with the stock manifest and fragments |
| IRQ numbers for msm_drm / kgsl | `init/init.target.rc` | `/proc/interrupts` on a running device |
| Auto-brightness curve, backlight range, power profile | `overlay/FrameworksResJ607F` | stock framework-res overlay APK |
| Device nodes and sysfs labels (touch, pen, keyboard dock, amp, Type-C) | `sepolicy/vendor` | stock vendor `file_contexts`, booted device |
| Which IMU fits (BMI26x or ICM4x6xx) | none, both configs ship | runtime |

## Known risks

- PixelOS `seventeen` (Android 17) on a 4.19 kernel: LineageOS still ships
  lito 4.19 devices on `lineage-24.0` (gauguin), so it is possible, but the
  Lenovo kernel source will likely need the same backports those kernels
  carry (BPF, cgroup v2 / uclamp, newer binder and incremental-fs fixes).
- The kernel repository does not exist yet. Without Lenovo's source release
  the fallback is a prebuilt stock kernel, which keeps the 4.19 limits above.

## Next steps

1. ~~Dump the stock J607F firmware~~ and ~~fill the partition, boot image
   and fstab placeholders~~ (done, see above).
2. Run `./extract-files.py <J607Z dump>` to create `vendor/lenovo/J607F`
   from the J607Z vendor, odm, system_ext and product trees.
3. Set up `kernel/lenovo/J607F` from the Lenovo source release.
4. `breakfast J607F` and work through `m nothing` errors, then boot.
