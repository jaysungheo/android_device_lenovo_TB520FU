# Lenovo Yoga Tab Plus (TB520FU, lapis): HDR, HDR10+, Dolby Vision and display brightness

This document records how HDR10, HDR10+ and Dolby Vision are enabled on the
PixelOS 17 build for the TB520FU, how the display reaches its HDR brightness,
and how the panel's high brightness mode (HBM) is used, with the brightness
curve recalculated from the Pixel configuration. It explains what was done and
why, so the work can be checked or continued.

## 0. Panel and starting point

| Item | Value |
| --- | --- |
| Panel | Tianma NT36532 3K LCD (1840x2944, up to 144 Hz), DCI-P3 |
| Backlight | KTZ8866, `/sys/class/backlight/panel0-backlight/brightness` (0-2047) |
| Brightness (specification) | 650 nits typical, 900 nits peak with HBM |
| HBM node | `.../panel0-backlight/hbm` (0 / 1 / 2). Written only through the Lenovo display HAL (`vendor.lenovo.hardware.display.IDisplay`, `setHbmState`, transaction 4) |
| HDR luminance reported by the HWC | maxLuminance 420 (a value of the HWC, not the real capability of the panel) |

Measured: HBM step 1 is about 1.25x, step 2 about 1.395x the normal backlight
(the specification, 650 to 900 nits, is 1.385x). Step 2 is therefore used as
"HBM on".

The stock (ZUI, global ROW) firmware lists only HDR10 and HLG. HDR10+ and Dolby
Vision are not advertised even though the hardware and most of the stock
software support them, so they are connected here.

## 1. Enabling each format

### 1.1 HDR10 / HLG
Supported by the stock HWC. Nothing to do: when an app decodes HDR, SurfaceFlinger
sees an HDR layer.

### 1.2 HDR10+
- Cause: the stock `libclstc_algorithm_adapter` `dlopen`s `libhdrdynamic.so`
  (HDR10+) and `libhdrvivid.so`, and the stock image has neither.
- Fix: `libhdrdynamic.so` is taken from the OnePlus Pad 2 (caihong), which has
  the same structure (its adapter and `libhdr_tm` are byte-identical to the
  stock ones) and shipped as a vendor blob of the common tree.
- Result: the HWC itself lists HDR10+, `supportedHdrTypes=[1,2,3,4]`.
- AV1 HDR10+: the Qualcomm AV1 decoders (secure ones too) already advertise
  `Main10HDRPlus` and the stock `msm_video.ko` has the `META_HDR10PLUS DEC`
  table, so nothing is needed (the frameworks/av AV1 patch was dropped).

### 1.3 Dolby Vision
- Architecture: **decoder-side Dolby Vision**. `c2.dolby.decoder.hevc` wraps the
  Qualcomm HEVC decoder (DolbyC2Client). The stock `/vendor/etc/dolby_vision.cfg`
  (made for the TB520FU, Tmax 600) is read by `dvs-aidl-service` and
  `libdolbydecoderprocessor`, which do the tone mapping. The composer-side CLSTC
  plugin is not needed (it was tried and reverted).
- Components:
  - the stock Dolby Vision Codec2 store (`vendor.dolby.media.c2@1.0-service-vision`),
    the `c2.dolby.*` decoders, the DVS daemon and the DolbyVisionService app that
    gives DVS the panel brightness;
  - `libcodec2_shim` / `libcodec2_hidl_shim` for the Android 17 ABI differences;
  - SELinux with the stock vendor types (vendorcodec, dvs_aidl_*,
    vendor_dolbyv_prop), nothing permissive;
  - a SurfaceFlinger patch (frameworks/native) that lists DOLBY_VISION as an HDR
    type of the built-in display.
- **Playback crash fix (libui-v34)**: the stock `-v34` Codec2 libraries expect the
  `libui` of Android 14 and the `GraphicBuffer` layout differs, so playback
  crashed. A `libui-v34` (from `hardware/lineage/compat`) is built and the
  Dolby Vision libraries are linked to it with blob fixups in `extract-files.py`
  (`replace_needed`, `fix_soname`). Profile 5 and profile 8.1 then play.
- Dolby audio: `c2.dolby.eac3.decoder` (AC-3, E-AC-3, Atmos JOC) and
  `c2.dolby.ac4.decoder` are shipped; without them streaming apps do not offer
  5.1 or Atmos.

### 1.4 Open points
- Netflix HDR, Dolby Vision and Atmos can only be checked with a plan that has
  them.
- Decoder-side Dolby Vision is not seen as an HDR layer by SurfaceFlinger, so
  "CABC off while HDR is on the screen" does not trigger for it. The stock
  firmware has no sender of `com.dolby.vision_play` either (only a receiver in
  `services.jar`), so another signal is needed.

## 2. CABC

The panel sends `55 01` (UI mode) at every power on, and the `cabc_mode` node is
write-only. The CABC part of the stock ZuiDisplayService is ported as
`CabcController` (`hardware/lenovo/input`, Lenovo display HAL `setCabcMode`,
transaction 3): UI mode for most apps, moving image or off for the stock app
lists, and **off while an HDR layer is on the screen**, since CABC dims the
backlight behind the tone mapping. The mode is set again whenever the screen
turns on.

## 3. Brightness: making HDR bright

### 3.1 Problem
Without HDR brightness information in the display configuration Android tone
maps HDR video into the range of the SDR white, so HDR looked no brighter than
the UI. The stock firmware uses the HBM for HDR.

### 3.2 Design
1. The HBM is made the top of the Android brightness scale, through the
   framework's own `HighBrightnessMode` and `sdrHdrRatioMap`. The "Enhanced HDR
   brightness" switch and strength slider of Settings then work unchanged.
2. The HDR peak curve is not made up: it has the shape of the Pixel (cheetah)
   configuration, scaled to the 650 / 900 nits of this panel.
3. Automatic and manual brightness use the same path
   (DisplayPowerController to LocalDisplayAdapter), so both work.
4. Brightness changes ramp smoothly in both directions, without steps or jumps.

### 3.3 Display configuration
`device/lenovo/lapis/configs/displayconfig/display_id_4630947161651687043.xml`,
installed to `/vendor/etc/displayconfig/`.

- `screenBrightnessMap`: the previous curve compressed to end at 0.85 (650 nits)
  and a last point `1.0 = 900 nits`. Slider up to 85% is the normal range, above
  it the HBM range.
- `highBrightnessMode`: `transitionPoint` 0.85; `minimumLux` 5000 (direct
  sunlight with auto brightness, like the stock firmware and the Pixel);
  `timing` 1800 s window, at most 300 s, at least 60 s (the usual Android
  behavior, for sunlight only: HDR video does not use this time);
  `allowInLowPowerMode=false`; `minimumHdrPercentOfScreen=0.1`.
- `sdrHdrRatioMap` (74 points): HDR peak = SDR nits x ratio(SDR nits).
  - SDR 2 to 81 nits: ratio 8.0, peak 650 nits
  - up to 491 nits: peak constant at 650 nits (ratio = 650 / SDR)
  - 491 to 650 nits: ratio 1.325 to 1.385, peak up to 900 nits
  - so slider 20-60% gives an HDR peak of 650 nits, 80% about 831, 100% 900.
- No `hdrBrightnessConfig`: an early version with a lux map could lower the
  backlight below the SDR brightness when a video started. With only the HBM
  block, `HdrBrightnessModifier` uses the HBM data as its fallback.

Reference: the Pixel configuration keeps the HDR peak at the maximum of the
normal range for most slider positions and uses the HBM only at high slider
positions. Other devices differ (some hold the peak at every slider position);
the Pixel way fits the AOSP framework best and was converted to this panel.

### 3.4 HBM bridge (`frameworks/base`: `LenovoHbmBridge`, `LocalDisplayAdapter`)
The HBM of the panel multiplies the whole backlight by a fixed factor (about
1.385), so the nits Android asks for are sent as two parts: a 0-1 backlight
value for the composer and the `hbm` node.

- Enabled by `ro.vendor.display.hbm_bridge=true` (`vendor.prop`) and an HBM block
  in the display configuration.
- `LocalDisplayAdapter.BacklightAdapter.setBacklight()` maps every brightness
  change (display backlight and SDR layer backlight) with `toComposer()`:
  - up to the transition point: `value = backlight / transition`, `hbm = 0`;
  - above it: `hbm = 2` and `value = nits / 900` (the backlight value is lowered
    by the same factor, so the nits stay the same);
  - a hysteresis of 0.002 around the transition point avoids flipping.
- Order of writes, to avoid a brightness jump: going up, the lowered backlight
  value goes first and the HBM turns on 40 ms later (not if it was turned off in
  the meantime); going down, the HBM turns off first (synchronously) and the
  backlight value rises right after.
- The bridge writes `hbm = 0` when it is created, so an HBM left on by a restart
  of system_server cannot stay on.
- The smooth ramps come from the framework ramps (HDR increase 0.5, decrease
  0.3) together with this mapping.

### 3.5 Settings
The AOSP "Enhanced HDR brightness" switch (`hdr_brightness_enabled`, default on)
and its strength slider (`hdr_brightness_boost_level`, default 1) work as
usual. The entry is shown only when `display.isHdr()` and
`isHdrSdrRatioAvailable` are true, which the `sdrHdrRatioMap` above provides.

## 4. Measured on the device (manual brightness, HDR video playing)

| Slider | Backlight | hbm | Nits |
| --- | --- | --- | --- |
| 20% / 40% / 60% | 2047 (1478 when coming down) | 0 (2 when coming down) | about 650 |
| 69% | 1655 | 2 | about 727 |
| 80% | 1890 | 2 | about 831 |
| 90% / 100% | 2047 | 2 | 900 |

- The two representations of 650 nits ((2047, hbm 0) and (1478, hbm 2)) come from
  the hysteresis, the brightness is the same.
- Without HDR content the hbm node stays 0 and the backlight follows the slider
  (40% is 959, 80% is 1925, 100% is 2047).
- Not done yet: sunlight HBM with auto brightness in real light, a
  `thermalThrottling` map in the display configuration, and the stock
  "2 minutes per session, 30 minutes per day" style limit (only the Android
  HBM time window is used for sunlight).

## 5. Where the code is

| Repository | Content |
| --- | --- |
| android_device_lenovo_lapis | display configuration, `vendor.prop`, Dolby decoders and libui-v34 in `extract-files.py`, SELinux |
| android_vendor_lenovo_lapis | stock Dolby / display HAL blobs (relinked to libui-v34) |
| android_frameworks_base (fork) | `LenovoHbmBridge`, `LocalDisplayAdapter` |
| android_frameworks_native (fork) | SurfaceFlinger Dolby Vision type, RefreshRateSelector steadiness |
| hardware/lenovo/input | `CabcController` |

Refresh rate: during video the YouTube window voted "Max" and the rate flipped
between 60 and 120 Hz. A `RefreshRateSelector` patch ignores Max votes while an
ExplicitExactOrMultiple layer is present and the screen is untouched.

## 6. Checking on a device

- Read `cat /sys/class/backlight/panel0-backlight/brightness` and the `hbm`
  node next to it.
- `settings put system screen_brightness_mode 0` and
  `settings put system screen_brightness <0-255>`, then read the values after
  4 seconds (the ramp).
- Log: `logcat -s LenovoHbmBridge` prints "backlight up to 0.85 is 650.0 nits,
  high brightness mode up to 900.0 nits" at start.
- When bind-mounting binaries for a root test, mind the SELinux labels
  (`chcon`) and that `/data` is `nosuid`, which breaks the SurfaceFlinger domain
  transition (use tmpfs).
