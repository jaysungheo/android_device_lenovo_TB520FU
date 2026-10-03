# Lenovo Xiaoxin Pad Plus (TB-J607F) — PixelOS device tree (DRAFT)

**Status: scaffold, never built or booted.** This is a starting point for
porting the TB520FU tree's setup to the TB-J607F. The layout follows the
TB520FU tree; the platform parts (lito, msm-4.19) come from the LineageOS
trees for other SM7225/SM7250 devices. Every value that still has to come
from the stock J607F firmware is marked `TODO(J607F)` in the files and listed
below.

| | |
|---|---|
| SoC | Qualcomm SM7225 Snapdragon 750G (lito), Adreno 619 |
| Kernel | msm-4.19 (non-GKI), Lenovo source release, built from source |
| Display | 11" IPS LCD, 2000×1200, 60 Hz, ~212 ppi |
| Memory | 6 GB RAM, 128 GB UFS, microSD |
| Audio | 4 speakers (JBL), no 3.5 mm jack |
| Cameras | 13 MP rear (AF), 8 MP front, ToF depth |
| Connectivity | Wi-Fi 5 (dual band), Bluetooth 5.1, no cellular |
| Battery | 7700 mAh |
| Shipped | Android 11, ZUI 12.5 (shipping API 30) |

Hardware facts from [GSMArena](https://m.gsmarena.com/lenovo_pad_plus-10927.php);
not yet checked against the stock firmware.

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

## Placeholders to fill from the stock firmware

| What | Where | How to get it |
|---|---|---|
| A/B, Virtual A/B | `BoardConfig.mk`, `device.mk` | partition table (`boot_a`/`boot_b`), `ro.virtual_ab.enabled` in vendor build.prop |
| Recovery in boot or separate | `BoardConfig.mk` `BOARD_USES_RECOVERY_AS_BOOT` | partition table (`recovery_a`?) |
| Boot header version, cmdline, base, page size, vendor_boot | `BoardConfig.mk` | `unpack_bootimg --boot_img boot.img` |
| Partition sizes (boot, dtbo, super) | `BoardConfig.mk` | `rawprogram*.xml` or `/dev/block/by-name` sizes, `lpdump super.img` |
| System / vendor filesystem (ext4 or erofs) | `BoardConfig.mk`, `fstab.qcom` | stock fstab, `file` on the images |
| Kernel source and defconfig name | `BoardConfig.mk`, `lineage.dependencies` | Lenovo open source portal (TB-J607F release); `/proc/config.gz` on stock |
| dtb / dtbo built or prebuilt | `BoardConfig.mk` | does the source release carry the J607F board dts? |
| fstab (encryption flags, Lenovo partitions, microSD path) | `init/fstab.qcom` | stock `/vendor/etc/fstab.qcom` |
| zram size | `init/fstab.zram` | stock fstab.zram / init |
| LCD density | `BoardConfig.mk`, `vendor.prop` | `ro.sf.lcd_density` |
| Recovery rotation | `BoardConfig.mk` | panel orientation on the device |
| Vendor security patch, fingerprint, `ro.product.*` | `BoardConfig.mk`, `custom_J607F.mk` | stock build.prop |
| Vendor / odm / product properties | `*.prop` | `tools/bringup/mkprops.py` from the TB520FU tree, pointed at the J607F dump |
| Blob list | `proprietary-files.txt` | `tools/bringup/blobdiff.py` (this tree) on the dump, then add the Lenovo audio configs + ACDB, camera, sensor configs, thermal configs, touch firmware |
| Extra blob fixups | `extract-files.py` | first `m nothing` / boot |
| VINTF manifest | `vintf/manifest.xml` | stock `/vendor/etc/vintf/manifest.xml` and fragments |
| GNSS present on the Wi-Fi model | `device.mk`, blob list | loc HAL / xtra-daemon in the dump |
| Vibrator present | `device.mk` | vibrator HAL / leds node in the dump |
| Sensor set (proximity, compass, ...) | `device.mk` | stock `/vendor/etc/permissions` |
| IRQ numbers for msm_drm / kgsl | `init/init.target.rc` | `/proc/interrupts` on stock |
| WLAN firmware directory (`qca_cld/` or a chip subdirectory) | `Android.bp` | `/vendor/firmware/wlan` on stock |
| Auto-brightness curve, backlight range, power profile | `overlay/FrameworksResJ607F` | stock framework-res overlay APK |
| Camera public libraries | `configs/public.libraries.txt` | stock `/vendor/etc/public.libraries.txt` |
| Device nodes and sysfs labels (touch, pen, keyboard dock, amp, Type-C) | `sepolicy/vendor` | stock vendor `file_contexts`, booted device |

`configs/power/powerhint.json` also carries gauguin's `/sys/touchpanel/double_tap`
node, which the Lenovo touch driver probably does not have.

## Known risks

- PixelOS `seventeen` (Android 17) on a 4.19 kernel: LineageOS still ships
  lito 4.19 devices on `lineage-24.0` (gauguin), so it is possible, but the
  Lenovo kernel source will likely need the same backports those kernels
  carry (BPF, cgroup v2 / uclamp, newer binder and incremental-fs fixes).
- The kernel repository does not exist yet. Without Lenovo's source release
  the fallback is a prebuilt stock kernel, which keeps the 4.19 limits above.

## Next steps

1. Dump the stock J607F firmware (boot, dtbo, vendor, odm, system_ext,
   product, partition table).
2. Fill the partition, boot image and fstab placeholders.
3. Run `tools/bringup/blobdiff.py`, rebuild `proprietary-files.txt`, run
   `./extract-files.py <dump>` to create `vendor/lenovo/J607F`.
4. Set up `kernel/lenovo/J607F` from the Lenovo source release.
5. `breakfast J607F` and work through `m nothing` errors, then boot.
