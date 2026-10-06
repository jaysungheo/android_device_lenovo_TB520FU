/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// The stock Dolby Vision Codec2 client (c2.dolby.client.so) was built when
// utils::BufferPoolSender was a plain struct. It is now
// android::BufferPoolSender<utils::BufferPoolTypes> with the same layout, so
// only the mangled name of objcpy(WorkBundle*, list<C2Work>, sender*) changed.

extern "C" bool
_ZN7android8hardware5media2c24V1_05utils6objcpyEPNS3_10WorkBundleERKNSt3__14listINS7_10unique_ptrI6C2WorkNS7_14default_deleteISA_EEEENS7_9allocatorISD_EEEEPNS_16BufferPoolSenderINS4_15BufferPoolTypesEEE(
        void* d, const void* s, void* bufferPoolSender);

extern "C" bool
_ZN7android8hardware5media2c24V1_05utils6objcpyEPNS3_10WorkBundleERKNSt3__14listINS7_10unique_ptrI6C2WorkNS7_14default_deleteISA_EEEENS7_9allocatorISD_EEEEPNS4_16BufferPoolSenderE(
        void* d, const void* s, void* bufferPoolSender) {
    return _ZN7android8hardware5media2c24V1_05utils6objcpyEPNS3_10WorkBundleERKNSt3__14listINS7_10unique_ptrI6C2WorkNS7_14default_deleteISA_EEEENS7_9allocatorISD_EEEEPNS_16BufferPoolSenderINS4_15BufferPoolTypesEEE(
            d, s, bufferPoolSender);
}
