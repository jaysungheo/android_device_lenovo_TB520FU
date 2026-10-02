#
# Copyright (C) 2026 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
# lito ships 32-bit vendor blobs (audio HAL, some camera libs), so keep the
# 64/32 configuration instead of TB520FU's core_64_bit_only.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base.mk)

# Inherit from J607F device
$(call inherit-product, device/lenovo/J607F/device.mk)

# Inherit some common PixelOS stuff.
$(call inherit-product, vendor/custom/config/common_full_tablet_wifionly.mk)

PRODUCT_NAME := custom_J607F
PRODUCT_DEVICE := J607F
PRODUCT_MANUFACTURER := Lenovo
PRODUCT_BRAND := Lenovo
PRODUCT_MODEL := TB-J607F
PRODUCT_CHARACTERISTICS := tablet

PRODUCT_GMS_CLIENTID_BASE := android-lenovo

# TODO(J607F): ro.product.device / ro.product.name and the build fingerprint
# of the stock firmware (getprop on the device, or the stock build.prop).
PRODUCT_BUILD_PROP_OVERRIDES += \
    DeviceName=J607F \
    DeviceProduct=J607F \
    SystemDevice=J607F \
    SystemName=J607F
