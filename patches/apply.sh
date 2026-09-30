#!/usr/bin/env bash
# Source patches for PixelOS (seventeen) that cannot live in the device tree.
# Safe to re-run; re-run after every `repo sync`.
#
# usage: bash device/lenovo/TB520FU/patches/apply.sh [pixelos-source-root]
#
# Patch files are named <project path with / -> _>-NNNN-<description>.patch
# and are applied with `git apply` to the matching project. Nothing is
# committed in the PixelOS projects, so `repo sync` keeps working; a patch
# that no longer applies stops the script with an error.
set -euo pipefail
PATCHES=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
TOP=${1:-$(cd "$PATCHES/../../../.." && pwd)}
cd "$TOP"
[ -f build/envsetup.sh ] || { echo "ERROR: $TOP is not a source root" >&2; exit 1; }

apply_patch() { # repo-dir patch-file
    local dir=$1 patch=$2
    # Projects some ROMs do not have (e.g. packages/apps/ParanoidSense)
    if [ ! -d "$dir" ]; then
        echo "skipped (no $dir): $(basename "$patch")"
        return 0
    fi
    if git -C "$dir" apply --check "$patch" 2>/dev/null; then
        git -C "$dir" apply "$patch" && echo "applied $(basename "$patch")"
    elif git -C "$dir" apply --reverse --check "$patch" 2>/dev/null; then
        echo "already applied: $(basename "$patch")"
    else
        echo "ERROR: $(basename "$patch") does not apply to $dir" >&2
        exit 1
    fi
}

# frameworks/base
# 0001: Lenovo PenService support. Adds android.app.haptic.ZuiPenHapticManager
#       (+ IZuiPenHapticManager) registered as "zui_pen_haptic", which the
#       stock PenService uses for its haptic settings/test, and the
#       config_stylus_pen_haptic_packages array it reads the selectable
#       apps from (the service itself lives in the device jar, input/).
#       Also restores the pre-caller RotationPolicy.setRotationLockAtAngle
#       overload that PenService's EasyJot still calls.
apply_patch frameworks/base \
    "$PATCHES/frameworks_base-0001-lenovo-pen-haptic-manager.patch"
# 0002: Settings.Secure display_white_balance_strength (0-100, default 100),
#       how far white balance follows the ambient light (TB520FUParts slider).
apply_patch frameworks/base \
    "$PATCHES/frameworks_base-0002-display-white-balance-strength.patch"
# 0003: android.app.keyboard.LenovoKeyboardManager (+ ILenovoKeyboardService)
#       registered as "lenovokeyboard". The stock keyboard firmware updater
#       (ZuiKeyboardUpdate) reaches the keyboard only through it; the service
#       itself lives in the device jar (input/KeyboardServiceBinder).
apply_patch frameworks/base \
    "$PATCHES/frameworks_base-0003-lenovo-keyboard-manager.patch"
# 0004: WM Shell turns the display into desktop windowing whenever a keyboard
#       and a touchpad are attached, with no way out. Let the device turn it
#       off (stock setting enter_work_mode_from_keyboard = 0, Lenovo keyboard
#       settings) or leave it until detach (tb520fu_keyboard_desktop_mode_exited,
#       notification button from input/KeyboardDesktopMode). Also enter it
#       on request without a keyboard (tb520fu_pc_mode, PC mode tile in Parts).
apply_patch frameworks/base \
    "$PATCHES/frameworks_base-0004-keyboard-desktop-first-opt-out.patch"
# 0005: Optional Galaxy Tab S11 Ultra (SM-X930) identity for the Play Store,
#       set from TB520FUParts (persist.sys.tb520fu.spoof_galaxy). The framework
#       reads the boot-time snapshot sys.tb520fu.spoof_galaxy, so the switch
#       only takes effect after a restart. Applies to the Play Store and the
#       Play services device check-in; the GMS droidguard process keeps its
#       Play Integrity behaviour.
apply_patch frameworks/base \
    "$PATCHES/frameworks_base-0005-tb520fu-galaxy-device-spoof.patch"

# frameworks/native
# 0001: RefreshRateSelector: a static screen (all layers vote Min) goes to the
#       lowest mode of at least 60 Hz, not the policy minimum; the idle timer
#       still drops to 30 Hz (30 -> 60 -> 120 Hz, like stock ZUI).
apply_patch frameworks/native \
    "$PATCHES/frameworks_native-0001-static-screen-60hz-floor.patch"

# packages/apps/DolbyAtmos
# 0001: the default profile was hardcoded to Dynamic; move it into an
#       overlayable string (overlay/DolbyAtmosResTB520FU keeps Dynamic).
apply_patch packages/apps/DolbyAtmos \
    "$PATCHES/packages_apps_DolbyAtmos-0001-overlayable-default-profile.patch"
# 0002: the equalizer labelled its sliders 32 Hz-16 kHz and treated the gains
#       as 1/10 dB. The DAX bands are 47 Hz-19.7 kHz (the sliders set every
#       other one) and the gains are 1/16 dB, as in the stock Lenovo equalizer.
apply_patch packages/apps/DolbyAtmos \
    "$PATCHES/packages_apps_DolbyAtmos-0002-geq-dax-bands-and-scale.patch"

# packages/apps/ParanoidSense
# 0001: the face enrollment preview surface is a fixed portrait 240x320dp
#       box. On this landscape tablet (front camera mounted at 270) the
#       preview stays 4:3 landscape and the face was stretched; swap the
#       surface size when the display rotation cancels the sensor rotation.
apply_patch packages/apps/ParanoidSense \
    "$PATCHES/packages_apps_ParanoidSense-0001-fit-enroll-preview-to-landscape.patch"

# packages/apps/Aperture
# 0001: Aperture asks for nosensor (natural = portrait here) and rotates its
#       buttons by the device orientation. Android 17 ignores that request on
#       large screens and shows it landscape, so every icon ended up rotated
#       by 90 degrees. Compensate only what the display rotation does not.
apply_patch packages/apps/Aperture \
    "$PATCHES/packages_apps_Aperture-0001-compensate-ui-for-display-rotation.patch"

# packages/apps/Settings
# 0001: config_show_display_white_balance, so the device can hide the Display
#       white balance switch (it lives in TB520FUParts with a strength slider).
apply_patch packages/apps/Settings \
    "$PATCHES/packages_apps_Settings-0001-optional-display-white-balance-switch.patch"
# 0002: com.android.settings.PLACE_HOLDER, the stock Lenovo settings action the
#       Lenovo PenService uses to open its pen settings (stylus toolbox button).
apply_patch packages/apps/Settings \
    "$PATCHES/packages_apps_Settings-0002-lenovo-place-holder-activity.patch"

# packages/apps/Updater
# 0001: SourceForge folder as update server (RSS feed of the OTA folder):
#       newest signed <package>.json of the running variant (PRC / ROW dtb),
#       Ed25519 signature, HTTPS + sourceforge.net only, package SHA-256 check.
apply_patch packages/apps/Updater \
    "$PATCHES/packages_apps_Updater-0001-sourceforge-folder.patch"
# 0002: the advertised CertifiedProps APK has an AOSPA package name and
#       only spoofs build properties; it is incompatible with this product.
#       Let the TB520FU resource overlay hide the nonfunctional updater item.
apply_patch packages/apps/Updater \
    "$PATCHES/packages_apps_Updater-0002-hide-unsupported-certified-props.patch"

# vendor/lineage
# 0001: kernel.mk installs every kernel module it builds that is not in
#       SYSTEM_KERNEL_MODULES to vendor_dlkm, even an empty set, which
#       overwrote the modules.dep/modules.load of the prebuilt vendor modules
#       and failed on their load list. Skip that step when nothing is left
#       (TB520FU builds only the GKI modules; vendor modules are prebuilt).
apply_patch vendor/lineage \
    "$PATCHES/vendor_lineage-0001-kernel-skip-empty-vendor-module-install.patch"
# 0002: android14-6.1 now exports struct sched_param in linux/sched/types.h,
#       which clashes with bionic's <sched.h> in vendor code using the
#       generated kernel headers. Drop it when cleaning the headers.
apply_patch vendor/lineage \
    "$PATCHES/vendor_lineage-0002-clean-sched-param-from-kernel-headers.patch"

