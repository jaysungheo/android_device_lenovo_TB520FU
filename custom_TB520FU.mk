#
# Copyright (C) 2026 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit_only.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base.mk)

# Inherit from TB520FU device
$(call inherit-product, device/lenovo/TB520FU/device.mk)

# Optional customizations repository (Lenovo Notes, game performance,
# Play Store identity, OTA updater, default wallpaper). It is not required:
# without it this builds a plain PixelOS for the device.
$(call inherit-product-if-exists, vendor/lenovo/TB520FU-custom/custom.mk)

# Inherit some common PixelOS stuff.
$(call inherit-product, vendor/custom/config/common_full_tablet_wifionly.mk)

PRODUCT_NAME := custom_TB520FU
PRODUCT_DEVICE := TB520FU
PRODUCT_MANUFACTURER := Lenovo
PRODUCT_BRAND := Lenovo
PRODUCT_MODEL := TB520FU
PRODUCT_CHARACTERISTICS := nosdcard,tablet

PRODUCT_GMS_CLIENTID_BASE := android-lenovo

PRODUCT_BUILD_PROP_OVERRIDES += \
    DeviceName=TB520FU \
    DeviceProduct=TB520FU \
    SystemDevice=TB520FU \
    SystemName=TB520FU
