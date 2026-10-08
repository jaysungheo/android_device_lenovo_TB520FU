# Lenovo Yoga Tab Plus (TB520FU, lapis): HDR, HDR10+, Dolby Vision and display brightness

This document records how HDR10, HDR10+ and Dolby Vision are enabled on the
PixelOS 17 build for the TB520FU, why HDR content is not boosted above the SDR
brightness, and how the panel's high brightness mode (HBM) is used (in sunlight,
as on the stock firmware). It explains what was done and why, so the work can be
checked or continued.

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
- Decoder-side Dolby Vision is not seen as an HDR layer by SurfaceFlinger. The
  stock firmware waits for a `com.dolby.vision_play` broadcast that nothing in
  it sends; the stock DolbyVisionService app now sends it (section 2).

## 2. CABC

The panel sends `55 01` (UI mode) at every power on, and the `cabc_mode` node is
write-only. The CABC part of the stock ZuiDisplayService is ported as
`CabcController` (`hardware/lenovo/input`, Lenovo display HAL `setCabcMode`,
transaction 3): UI mode for most apps, moving image or off for the stock app
lists, and **off while an HDR layer is on the screen or Dolby Vision plays**,
since CABC dims the backlight behind the tone mapping. The mode is set again
whenever the screen turns on.

Dolby Vision is tone mapped in the decoder, so SurfaceFlinger sees an SDR layer.
The DVS daemon calls back the stock DolbyVisionService app when the Dolby Vision
decoder starts and stops (`onDolbyVisionPlaybackStart` / `Stop`, logged as
"notify Dolby Vision playback start"). A compat dex prepended to the app
(`lenovo/DolbyVisionService`, like `lenovo/PenService`) replaces its DVS client
`DolbyDvsManager` with one that also sends the broadcast the stock
ZuiDisplayService expects: `com.dolby.vision_play` with `start` / `stop` in the
`action` extra, to the system server only. TB520FUParts declares it as a
protected broadcast (system apps only). The callback is not taken from the app:
the DVS daemon keeps a single one, and the app needs it to give DVS the
backlight.

## 3. Brightness: no HDR boost, sunlight HBM as on stock

### 3.1 Decision
HDR content is tone mapped within the SDR brightness, as on the stock firmware
and other LCD tablets:

| Device | Panel | HDR brightness boost | HBM |
| --- | --- | --- | --- |
| Pixel Tablet (tangorpro) | LCD, 500 nits | none: no `highBrightnessMode` nor `hdrBrightnessConfig` in its display configuration | none |
| OnePlus Pad 2 (LineageOS) | LCD | none: no display configuration | stock blobs |
| Lenovo stock (ZUI 17.5) | LCD, 650 / 900 nits | none | sunlight only |
| Pixel 7 Pro (cheetah) | OLED | yes | HDR and sunlight |

An LCD lights the whole panel with one backlight: raising it for HDR highlights
also raises the black of the video. The SDR layers, dimmed by SurfaceFlinger to
keep their brightness, are also out of step with the backlight on this panel:
the backlight chip ramps every change over 192 ms (BL_CFG2 0xC5 in its device
tree: BIT(7) | (5 + 192 / 64) << 3 | PWM_HYST, the encoding of the mainline
ktz8866 driver) after the frames that the value takes to reach it, so the screen
flashes or darkens when HDR content comes and goes. A compensation of that ramp
in DisplayManager was tried and dropped (see the history of this document).

### 3.2 Display configuration
`configs/displayconfig/display_id_4630947161651687043.xml`, installed to
`/vendor/etc/displayconfig/`:

- `screenBrightnessMap`: the brightness curve of config.xml, 650 nits (the
  typical brightness of the specifications of the panel) at 1.0.
- `thermalThrottling`: the levels of the Pixel configurations (Pixel Tablet,
  Pixel 7 Pro: 465, 297, 213 and 150 nits). The skin thresholds of the thermal
  HAL start at 48 C (light), so normal use is not throttled. The stock firmware
  has no limit of its own (`config_zui_enter/exitHbmTemperature` 0).
- No `highBrightnessMode` and no `hdrBrightnessConfig`: Android does not boost
  the brightness for HDR content, and the "Enhanced HDR brightness" setting is
  not shown (it is not on the stock firmware either).

### 3.3 Sunlight HBM (`hardware/lenovo` input: `SunlightHbmController`)
The high brightness mode of the panel (`hbm` node, steps 1 and 2, about 1.25x
and 1.4x of the backlight, set through the Lenovo display HAL `setHbmState`)
is used the way the stock ZuiDisplayService does:

- automatic brightness on, the brightness at or near its maximum (0.95: the
  automatic curve with the adjustment of the user may end a little below 1.0;
  the stock service wants 1.0), the screen on;
- step 1 from 5000 lux, step 2 from 10000 lux
  (`config_zui_defaultBrightnessThreshold` of the stock
  LapisRowFrameworksOverlay: 5000, 10000), with the 3 s debounce of the stock
  service both ways;
- off as soon as a condition stops, also when the thermal throttling lowers the
  maximum brightness.

The light sensor is only listened to while the other conditions hold.

## 4. Checked on the device

- With the HBM bridge (an earlier version) the slider positions gave the HDR
  peaks of the Pixel shape (650 to 900 nits); dropped with the HDR boost.
- Thermal throttling, injected skin status (`cmd thermalservice
  inject-temperature SKIN <status> skin 50`): backlight 1584 / 1008 / 721 / 509
  of 2047 for light / moderate / severe / critical with the earlier ratios; the
  current values are the absolute Pixel ones above.
- Sunlight HBM: a phone flashlight on the sensor (9000 to 24000 lux) switched
  step 2 on with automatic brightness at the top, and off when the brightness
  went down.

## 5. Where the code is

| Repository | Content |
| --- | --- |
| android_device_lenovo_lapis | display configuration, Dolby decoders and libui-v34 in `extract-files.py`, SELinux, Dolby Vision service compat (`lenovo/DolbyVisionService`) |
| android_vendor_lenovo_lapis | stock Dolby / display HAL blobs (relinked to libui-v34) |
| android_frameworks_native (fork) | SurfaceFlinger Dolby Vision type, RefreshRateSelector steadiness |
| android_hardware_lenovo | `input`: `CabcController`, `SunlightHbmController` |

Refresh rate: during video the YouTube window voted "Max" and the rate flipped
between 60 and 120 Hz. A `RefreshRateSelector` patch ignores Max votes while an
ExplicitExactOrMultiple layer is present and the screen is untouched.

## 6. Checking on a device

- Read `cat /sys/class/backlight/panel0-backlight/brightness` and the `hbm`
  node next to it.
- Raw light sensor values: the "last 50 events" of the ambient light sensor in
  `dumpsys sensorservice`.
- Logs: `logcat -s TB520FUSunlightHbm TB520FUCabc`.
- When bind-mounting binaries for a root test, mind the SELinux labels
  (`chcon`) and that `/data` is `nosuid`, which breaks the SurfaceFlinger domain
  transition (use tmpfs).
