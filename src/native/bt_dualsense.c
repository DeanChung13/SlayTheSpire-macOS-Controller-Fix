#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <CoreFoundation/CoreFoundation.h>
#include <IOKit/IOKitLib.h>
#include <IOKit/IOCFPlugIn.h>
#include <IOKit/hid/IOHIDLib.h>
#include <IOKit/hid/IOHIDKeys.h>

typedef unsigned char jboolean;
typedef int jint;
typedef float jfloat;

static IOHIDDeviceInterface122 **g_hidDevice = NULL;
static IOHIDQueueInterface **g_queue = NULL;
static int g_cookie = 79;

static float g_axes[7] = {0.0f};
static int g_buttons[16] = {0};

__attribute__((visibility("default")))
jboolean Java_com_badlogic_gdx_controllers_desktop_ois_BluetoothDualSense_nativeInit(void *env, void *cls) {
    if (g_queue) {
        return 1;
    }

    CFMutableDictionaryRef matching = IOServiceMatching(kIOHIDDeviceKey);
    io_iterator_t iter;
    if (IOServiceGetMatchingServices(kIOMainPortDefault, matching, &iter) != kIOReturnSuccess) {
        return 0;
    }

    io_service_t service;
    io_service_t targetService = 0;
    while ((service = IOIteratorNext(iter)) != 0) {
        CFStringRef prod = (CFStringRef)IORegistryEntryCreateCFProperty(service, CFSTR(kIOHIDProductKey), kCFAllocatorDefault, 0);
        CFStringRef trans = (CFStringRef)IORegistryEntryCreateCFProperty(service, CFSTR(kIOHIDTransportKey), kCFAllocatorDefault, 0);
        if (prod && trans) {
            char pBuf[128] = "";
            char tBuf[128] = "";
            CFStringGetCString(prod, pBuf, sizeof(pBuf), kCFStringEncodingUTF8);
            CFStringGetCString(trans, tBuf, sizeof(tBuf), kCFStringEncodingUTF8);
            if ((strstr(pBuf, "DualSense") || strstr(pBuf, "Wireless Controller")) &&
                (strstr(tBuf, "Bluetooth") != NULL)) {
                targetService = service;
                CFRelease(prod);
                CFRelease(trans);
                break;
            }
        }
        if (prod) CFRelease(prod);
        if (trans) CFRelease(trans);
        IOObjectRelease(service);
    }
    IOObjectRelease(iter);

    if (!targetService) {
        return 0;
    }

    IOCFPlugInInterface **plugInInterface = NULL;
    SInt32 score = 0;
    IOReturn kr = IOCreatePlugInInterfaceForService(targetService, kIOHIDDeviceUserClientTypeID, kIOCFPlugInInterfaceID, &plugInInterface, &score);
    IOObjectRelease(targetService);
    if (kr != kIOReturnSuccess || !plugInInterface) {
        return 0;
    }

    HRESULT res = (*plugInInterface)->QueryInterface(plugInInterface, CFUUIDGetUUIDBytes(kIOHIDDeviceInterfaceID122), (LPVOID*)&g_hidDevice);
    (*plugInInterface)->Release(plugInInterface);
    if (res != S_OK || !g_hidDevice) {
        return 0;
    }

    kr = (*g_hidDevice)->open(g_hidDevice, 0);
    if (kr != kIOReturnSuccess) {
        (*g_hidDevice)->Release(g_hidDevice);
        g_hidDevice = NULL;
        return 0;
    }

    CFArrayRef elements = NULL;
    int foundCookie = 79;
    if ((*g_hidDevice)->copyMatchingElements(g_hidDevice, NULL, &elements) == kIOReturnSuccess && elements) {
        CFIndex count = CFArrayGetCount(elements);
        for (CFIndex i = 0; i < count; i++) {
            CFDictionaryRef dict = (CFDictionaryRef)CFArrayGetValueAtIndex(elements, i);
            CFNumberRef pNum = (CFNumberRef)CFDictionaryGetValue(dict, CFSTR(kIOHIDElementUsagePageKey));
            CFNumberRef uNum = (CFNumberRef)CFDictionaryGetValue(dict, CFSTR(kIOHIDElementUsageKey));
            CFNumberRef cNum = (CFNumberRef)CFDictionaryGetValue(dict, CFSTR(kIOHIDElementCookieKey));
            int page = 0, usage = 0, cookie = 0;
            if (pNum) CFNumberGetValue(pNum, kCFNumberIntType, &page);
            if (uNum) CFNumberGetValue(uNum, kCFNumberIntType, &usage);
            if (cNum) CFNumberGetValue(cNum, kCFNumberIntType, &cookie);
            if (page == 0xff00 && usage == 0x3b) {
                foundCookie = cookie;
                break;
            }
        }
        CFRelease(elements);
    }
    g_cookie = foundCookie;

    g_queue = (*g_hidDevice)->allocQueue(g_hidDevice);
    if (!g_queue) {
        (*g_hidDevice)->close(g_hidDevice);
        (*g_hidDevice)->Release(g_hidDevice);
        g_hidDevice = NULL;
        return 0;
    }

    (*g_queue)->create(g_queue, 0, 64);
    (*g_queue)->addElement(g_queue, (IOHIDElementCookie)g_cookie, 0);
    kr = (*g_queue)->start(g_queue);
    if (kr != kIOReturnSuccess) {
        (*g_queue)->dispose(g_queue);
        (*g_queue)->Release(g_queue);
        g_queue = NULL;
        (*g_hidDevice)->close(g_hidDevice);
        (*g_hidDevice)->Release(g_hidDevice);
        g_hidDevice = NULL;
        return 0;
    }

    return 1;
}

__attribute__((visibility("default")))
jboolean Java_com_badlogic_gdx_controllers_desktop_ois_BluetoothDualSense_nativePoll(void *env, void *cls) {
    if (!g_queue) {
        return 0;
    }

    AbsoluteTime zeroTime = {0, 0};
    IOHIDEventStruct event;
    int gotEvent = 0;
    unsigned char latest[16];

    while ((*g_queue)->getNextEvent(g_queue, &event, zeroTime, 0) == kIOReturnSuccess) {
        if (event.longValue && event.longValueSize >= 11) {
            memcpy(latest, (unsigned char*)event.longValue, 11);
            gotEvent = 1;
        }
    }

    if (gotEvent) {
        // Left Stick (b[1], b[2]), Right Stick (b[3], b[4]) -> -1.0 .. +1.0
        g_axes[0] = ((float)latest[1] - 128.0f) / 128.0f;
        g_axes[1] = ((float)latest[2] - 128.0f) / 128.0f;
        g_axes[2] = ((float)latest[3] - 128.0f) / 128.0f;
        g_axes[3] = ((float)latest[4] - 128.0f) / 128.0f;
        // L2, R2 trigger depth
        g_axes[4] = ((float)latest[5]) / 255.0f;
        g_axes[5] = ((float)latest[6]) / 255.0f;

        // D-Pad: latest[8] & 0x0F
        // 0=Up, 1=Up-Right, 2=Right, 3=Down-Right, 4=Down, 5=Down-Left, 6=Left, 7=Up-Left, 8=Center
        int dpad = latest[8] & 0x0F;
        float dpadVal = 1.2857f;
        switch (dpad) {
            case 0: dpadVal = -1.0f; break;     // Up
            case 1: dpadVal = -0.71f; break;    // Up-Right
            case 2: dpadVal = -0.43f; break;    // Right
            case 3: dpadVal = -0.14f; break;    // Down-Right
            case 4: dpadVal = 0.14f; break;     // Down
            case 5: dpadVal = 0.43f; break;     // Down-Left
            case 6: dpadVal = 0.71f; break;     // Left
            case 7: dpadVal = 0.85f; break;     // Up-Left
            default: dpadVal = 1.2857f; break;  // Centered
        }
        g_axes[6] = dpadVal;

        // Buttons:
        // Square (0), Cross (1), Circle (2), Triangle (3)
        g_buttons[0] = (latest[8] >> 4) & 1;
        g_buttons[1] = (latest[8] >> 5) & 1;
        g_buttons[2] = (latest[8] >> 6) & 1;
        g_buttons[3] = (latest[8] >> 7) & 1;

        // L1 (4), R1 (5), L2 button (6), R2 button (7)
        g_buttons[4] = (latest[9] >> 0) & 1;
        g_buttons[5] = (latest[9] >> 1) & 1;
        g_buttons[6] = (latest[9] >> 2) & 1;
        g_buttons[7] = (latest[9] >> 3) & 1;

        // Share/Create (8), Options (9), L3 (10), R3 (11)
        g_buttons[8] = (latest[9] >> 4) & 1;
        g_buttons[9] = (latest[9] >> 5) & 1;
        g_buttons[10] = (latest[9] >> 6) & 1;
        g_buttons[11] = (latest[9] >> 7) & 1;

        // PS Button (12)
        g_buttons[12] = (latest[10] >> 0) & 1;

        return 1;
    }

    return 0;
}

__attribute__((visibility("default")))
jfloat Java_com_badlogic_gdx_controllers_desktop_ois_BluetoothDualSense_nativeGetAxis(void *env, void *cls, jint axis) {
    if (axis >= 0 && axis < 7) {
        return g_axes[axis];
    }
    return 0.0f;
}

__attribute__((visibility("default")))
jboolean Java_com_badlogic_gdx_controllers_desktop_ois_BluetoothDualSense_nativeGetButton(void *env, void *cls, jint btn) {
    if (btn >= 0 && btn < 16) {
        return g_buttons[btn] ? 1 : 0;
    }
    return 0;
}

__attribute__((visibility("default")))
void Java_com_badlogic_gdx_controllers_desktop_ois_BluetoothDualSense_nativeClose(void *env, void *cls) {
    if (g_queue) {
        (*g_queue)->stop(g_queue);
        (*g_queue)->dispose(g_queue);
        (*g_queue)->Release(g_queue);
        g_queue = NULL;
    }
    if (g_hidDevice) {
        (*g_hidDevice)->close(g_hidDevice);
        (*g_hidDevice)->Release(g_hidDevice);
        g_hidDevice = NULL;
    }
}
