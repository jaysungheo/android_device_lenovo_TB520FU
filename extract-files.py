#!/usr/bin/env -S PYTHONPATH=../../../tools/extract-utils python3
#
# SPDX-FileCopyrightText: 2026 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

from extract_utils.fixups_blob import (
    blob_fixup,
    blob_fixups_user_type,
)
from extract_utils.fixups_lib import (
    lib_fixups,
    lib_fixups_user_type,
)
from extract_utils.main import (
    ExtractUtils,
    ExtractUtilsModule,
)

namespace_imports = [
    'device/lenovo/lapis',
    'vendor/lenovo/sm8650-common',
    'hardware/qcom-caf/sm8650',
    'hardware/qcom-caf/wlan',
    'vendor/qcom/opensource/commonsys/display',
    'vendor/qcom/opensource/commonsys-intf/display',
    'vendor/qcom/opensource/dataservices',
]


def lib_fixup_vendor_suffix(lib: str, partition: str, *args, **kwargs):
    return f'{lib}_{partition}' if partition == 'vendor' else None


lib_fixups: lib_fixups_user_type = {
    **lib_fixups,
    (
        'com.qualcomm.qti.dpm.api@1.0',
        'vendor.qti.diaghal@1.0',
        'vendor.qti.hardware.dpmaidlservice-V1-ndk',
        'vendor.qti.hardware.dpmservice@1.0',
        'vendor.qti.hardware.qccsyshal@1.0',
        'vendor.qti.hardware.qccsyshal@1.1',
        'vendor.qti.hardware.qccsyshal@1.2',
        'vendor.qti.hardware.wifidisplaysession@1.0',
        'vendor.qti.qccvndhal_aidl-V1-ndk',
    ): lib_fixup_vendor_suffix,
}


def clear_opencl_versions(fixup):
    # Lenovo camera algorithm libs reference OpenCL symbols with OPENCL_x.y
    # versions, but the Adreno libOpenCL exports them unversioned.
    for symbol in OPENCL_SYMBOLS:
        fixup = fixup.clear_symbol_version(symbol)
    return fixup


def clear_nativewindow_versions(fixup):
    # AHardwareBuffer symbols are referenced as @LIBNATIVEWINDOW, but the
    # vendor libnativewindow stub exports them unversioned.
    for symbol in NATIVEWINDOW_SYMBOLS:
        fixup = fixup.clear_symbol_version(symbol)
    return fixup


NATIVEWINDOW_SYMBOLS = (
    'AHardwareBuffer_allocate',
    'AHardwareBuffer_describe',
    'AHardwareBuffer_lock',
    'AHardwareBuffer_lockPlanes',
    'AHardwareBuffer_release',
    'AHardwareBuffer_unlock',
)


OPENCL_SYMBOLS = (
    'clBuildProgram',
    'clCreateBuffer',
    'clCreateCommandQueue',
    'clCreateCommandQueueWithProperties',
    'clCreateContext',
    'clCreateKernel',
    'clCreateProgramWithBinary',
    'clCreateProgramWithSource',
    'clEnqueueCopyBuffer',
    'clEnqueueFillBuffer',
    'clEnqueueMapBuffer',
    'clEnqueueNDRangeKernel',
    'clEnqueueReadBuffer',
    'clEnqueueUnmapMemObject',
    'clFinish',
    'clGetDeviceIDs',
    'clGetDeviceInfo',
    'clGetExtensionFunctionAddressForPlatform',
    'clGetPlatformIDs',
    'clGetPlatformInfo',
    'clGetProgramBuildInfo',
    'clGetProgramInfo',
    'clReleaseCommandQueue',
    'clReleaseContext',
    'clReleaseKernel',
    'clReleaseMemObject',
    'clReleaseProgram',
    'clSetKernelArg',
)

blob_fixups: blob_fixups_user_type = {
    'system_ext/lib64/libwfdnative.so': blob_fixup()
        .add_needed('libinput_shim.so'),
    'system_ext/lib64/libwfdservice.so': blob_fixup()
        .replace_needed('android.media.audio.common.types-V4-cpp.so', 'android.media.audio.common.types-V5-cpp.so'),
    (
        'vendor/lib64/c2.dolby.hevc.dec.so',
        'vendor/lib64/c2.dolby.hevc.sec.dec.so',
    ): blob_fixup()
        .add_needed('libcodec2_shim.so'),
    'vendor/lib64/c2.dolby.client.so': blob_fixup()
        .add_needed('libcodec2_hidl_shim.so'),
    (
        'vendor/lib64/libpandora.algorithm.arcsoft.superportraitvideo.1.so',
        'vendor/lib64/libpandora.algorithm.arcsoft.tablethdrchecker.1.so',
        'vendor/lib64/libpandora.algorithm.arcsoft.tabletnormalhdr.1.so',
        'vendor/lib64/libpandora.algorithm.arcsoft.tabletportraithdr.1.so',
        'vendor/lib64/libpandora.algorithm.bst.filter.1.so',
        'vendor/lib64/libpandora.algorithm.bst.tabletaicapture.1.so',
        'vendor/lib64/libpandora.algorithm.bst.tabletsinglebokehcapture.1.so',
        'vendor/lib64/libpandora.algorithm.camera.AlgorithmsDev.1.so',
        'vendor/lib64/libpandora.algorithm.fotonation.facebeauty.1.so',
        'vendor/lib64/libpandora.algorithm.lenovo.Watermark.1.so',
        'vendor/lib64/libpandora.algorithm.morpho.tabletdccapture.1.so',
        'vendor/lib64/libpandora.algorithm.morpho.tabletdcvideo.1.so',
        'vendor/lib64/libpandora.algorithm.morpho.tabletdenoise.1.so',
        'vendor/lib64/libpandora.algorithm.morpho.tabletsr.1.so',
        'vendor/lib64/libpandora.algorithm.niklas.BmpWatermark.0.so',
        'vendor/lib64/libpandora.algorithm.niklas.YUVRotator.0.so',
        'vendor/lib64/libpandora.algorithm.niklas.YUVScaler.0.so',
        'vendor/lib64/libpandora.so',
    ): clear_opencl_versions(blob_fixup()),
    (
        'vendor/lib64/libFNVfbEngineHAL.so',
        'vendor/lib64/libarcsoft_video_superportrait.so',
    ): clear_nativewindow_versions(blob_fixup()),
    'vendor/lib64/hw/gf_fingerprint.default.so': blob_fixup()
        .fix_soname(),
}  # fmt: skip

module = ExtractUtilsModule(
    'lapis',
    'lenovo',
    blob_fixups=blob_fixups,
    lib_fixups=lib_fixups,
    namespace_imports=namespace_imports,
    add_firmware_proprietary_file=True,
)

if __name__ == '__main__':
    utils = ExtractUtils.device_with_common(
        module, 'sm8650-common', module.vendor
    )
    utils.run()
