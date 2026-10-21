package com.badlogic.gdx.controllers.desktop.ois;

import com.badlogic.gdx.utils.SharedLibraryLoader;

public class BluetoothDualSense {
    private static boolean loaded = false;
    private static boolean active = false;
    private static boolean[] lastButtons = new boolean[16];
    private static float[] lastAxes = new float[7];

    static {
        try {
            new SharedLibraryLoader().load("dualsense-bt");
            loaded = true;
            System.out.println("[BluetoothDualSense] Native library loaded via SharedLibraryLoader.");
        } catch (Throwable t1) {
            try {
                System.loadLibrary("dualsense-bt64");
                loaded = true;
                System.out.println("[BluetoothDualSense] Native library loaded via System.loadLibrary.");
            } catch (Throwable t2) {
                System.err.println("[BluetoothDualSense] Failed to load native library: " + t2.getMessage());
            }
        }
    }

    public static native boolean nativeInit();
    public static native boolean nativePoll();
    public static native float nativeGetAxis(int axis);
    public static native boolean nativeGetButton(int btn);
    public static native void nativeClose();

    public static synchronized boolean init() {
        if (!loaded) {
            return false;
        }
        try {
            active = nativeInit();
            if (active) {
                System.out.println("[BluetoothDualSense] Attached to Bluetooth DualSense successfully!");
            }
            return active;
        } catch (Throwable t) {
            active = false;
            return false;
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static void poll(OisJoystick joystick) {
        if (!active) {
            return;
        }
        try {
            if (!nativePoll()) {
                return;
            }

            for (int i = 0; i < 14; i++) {
                boolean cur = nativeGetButton(i);
                if (cur != lastButtons[i]) {
                    if (cur) {
                        joystick.buttonPressed(i);
                    } else {
                        joystick.buttonReleased(i);
                    }
                    lastButtons[i] = cur;
                }
            }

            for (int i = 0; i < 7; i++) {
                float cur = nativeGetAxis(i);
                if (Math.abs(cur - lastAxes[i]) > 0.005f) {
                    joystick.axisMovedDirect(i, cur);
                    lastAxes[i] = cur;
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
