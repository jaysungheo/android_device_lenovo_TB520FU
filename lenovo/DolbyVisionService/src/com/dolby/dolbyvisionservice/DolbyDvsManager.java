/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dolby.dolbyvisionservice;

import android.app.ActivityThread;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;

import vendor.dolby.dvs.IDvs;
import vendor.dolby.dvs.IDvsCallback;

/**
 * The DVS client of the stock DolbyVisionService (AIDL path only: the device
 * has the AIDL dvs-aidl-service, no HIDL one), with the same behavior, that
 * also announces the Dolby Vision playback the way the stock firmware expects:
 * the com.dolby.vision_play broadcast with "start" / "stop" in the "action"
 * extra, which the stock ZuiDisplayService (and CabcController of the device)
 * uses to turn the panel CABC off. The Android 17 DVS daemon reports the
 * decoder through onDolbyNotification ("Callback_DolbyRenderStart" and
 * "Callback_DolbyRenderStop") instead of onDolbyVisionPlaybackStart and Stop;
 * nothing in the stock firmware sent the broadcast.
 */
public class DolbyDvsManager {
    private static final String TAG = "DolbyDvsManager";

    /** Stock ZuiDisplayService DOLBY_VISION_ACTION. */
    private static final String ACTION_DOLBY_VISION = "com.dolby.vision_play";

    private static final String EVENT_RENDER_START = "Callback_DolbyRenderStart";
    private static final String EVENT_RENDER_STOP = "Callback_DolbyRenderStop";

    private static IDvs mDvs;
    private static IDvsCallbackImpl mDvsCallbackImpl;

    public interface DvsCallback {
        void onDolbyNotification(String event);

        boolean onDolbyVisionPlaybackStart();

        boolean onDolbyVisionPlaybackStop();

        void onServiceDied();
    }

    public static boolean registerCallback(DvsCallback dvsCallback) {
        if (dvsCallback == null) {
            throw new IllegalArgumentException("Illegal null argument");
        }
        IDvs service = getAidlService();
        if (service == null) {
            return false;
        }
        mDvsCallbackImpl = new IDvsCallbackImpl(dvsCallback);
        try {
            service.registerCallback(mDvsCallbackImpl);
            service.asBinder().linkToDeath(mDvsCallbackImpl, 0);
            return true;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static void setBrightness(int brightness) {
        setParameter("brightness", brightness);
    }

    public static void setAdjustedBrightness(int brightness) {
        setParameter("adjustedBrightness", brightness);
    }

    public static void setCurrentPQmode(int mode) {
        setParameter("pqMode", mode);
    }

    public static void setActiveDisplayID(int id) {
        setParameter("displayID", id);
    }

    private static void setParameter(String key, int value) {
        IDvs service = getAidlService();
        if (service == null) {
            return;
        }
        DolbyDvsParameter parameter = new DolbyDvsParameter();
        parameter.add(key, Integer.valueOf(value));
        try {
            service.setParameters(parameter.toString());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static int getCurrentPQmode() {
        return getParameter("pqMode");
    }

    public static int getActiveDisplayID() {
        return getParameter("displayID");
    }

    private static int getParameter(String key) {
        IDvs service = getAidlService();
        if (service == null) {
            return 0;
        }
        try {
            DolbyDvsParameter parameter = new DolbyDvsParameter(service.getParameters(key));
            Integer[] value = new Integer[1];
            if (parameter.get(key, value)) {
                return value[0].intValue();
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
        return 0;
    }

    public static int[] getDisplayIDs() {
        IDvs service = getAidlService();
        if (service == null) {
            return null;
        }
        try {
            DolbyDvsParameter parameter =
                    new DolbyDvsParameter(service.getParameters("displayIDs"));
            Integer[] values = new Integer[256];
            int[] count = new int[1];
            if (!parameter.get("displayIDs", values, count)) {
                return null;
            }
            int[] ids = new int[count[0]];
            for (int i = 0; i < count[0]; i++) {
                ids[i] = values[i].intValue();
            }
            return ids;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** The stock broadcast for the system server. */
    static void announcePlayback(boolean playing) {
        try {
            Context context = ActivityThread.currentApplication();
            if (context == null) {
                return;
            }
            Intent intent = new Intent(ACTION_DOLBY_VISION)
                    .putExtra("action", playing ? "start" : "stop")
                    .setPackage("android")
                    .addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            context.sendBroadcastAsUser(intent, UserHandle.ALL);
        } catch (Exception e) {
            Log.w(TAG, "dolby vision broadcast", e);
        }
    }

    private static final class IDvsCallbackImpl extends IDvsCallback.Stub
            implements IBinder.DeathRecipient {
        private final DvsCallback mListener;

        IDvsCallbackImpl(DvsCallback listener) {
            mListener = listener;
        }

        @Override
        public int getInterfaceVersion() {
            return 1;
        }

        @Override
        public String getInterfaceHash() {
            return "8531c2009e64a4bcabdd3fd3210e44ea073500b6";
        }

        @Override
        public void onDolbyNotification(String event) {
            if (EVENT_RENDER_START.equals(event)) {
                announcePlayback(true);
            } else if (EVENT_RENDER_STOP.equals(event)) {
                announcePlayback(false);
            }
            if (mListener != null) {
                mListener.onDolbyNotification(event);
            }
        }

        @Override
        public boolean onDolbyVisionPlaybackStart() {
            announcePlayback(true);
            if (mListener != null) {
                mListener.onDolbyVisionPlaybackStart();
            }
            return true;
        }

        @Override
        public boolean onDolbyVisionPlaybackStop() {
            announcePlayback(false);
            if (mListener != null) {
                mListener.onDolbyVisionPlaybackStop();
            }
            return true;
        }

        @Override
        public void binderDied() {
            Log.e(TAG, "dvs-hal-service died");
            mDvs = null;
            // the decoder sessions are gone with it
            announcePlayback(false);
            if (mListener != null) {
                mListener.onServiceDied();
            }
        }
    }

    public static IDvs getAidlService() {
        if (mDvs != null) {
            return mDvs;
        }
        IBinder service = ServiceManager.getService("vendor.dolby.dvs.IDvs/default");
        if (service == null) {
            Log.e(TAG, "Getting vendor.dolby.dvs.IDvs/default service daemon binder failed!");
        } else {
            mDvs = IDvs.Stub.asInterface(service);
        }
        if (mDvs != null) {
            Log.d(TAG, "Succeeded to get dvs-hal-service");
        } else {
            Log.e(TAG, "Failed to get dvs-hal-service");
        }
        return mDvs;
    }
}
