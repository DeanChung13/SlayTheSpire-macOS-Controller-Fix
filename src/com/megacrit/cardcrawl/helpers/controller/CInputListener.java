package com.megacrit.cardcrawl.helpers.controller;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.math.Vector3;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.controller.CInputHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.options.RemapInputElement;
import java.io.FileWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CInputListener implements ControllerListener {
    private static final Logger logger = LogManager.getLogger(CInputListener.class.getName());
    private static final float DEADZONE = 0.5f;
    private float[] axisValues = new float[32];
    private int lastDpadState = 8; // 8 = centered
    public static boolean remapping = false;
    public static RemapInputElement element = null;

    private static void logInput(String msg) {
        System.out.println("[CONTROLLER] " + msg);
        logger.info("[CONTROLLER] " + msg);
        try {
            FileWriter fw = new FileWriter("controller_debug.log", true);
            fw.write(System.currentTimeMillis() + " " + msg + "\n");
            fw.close();
        } catch (Exception ignored) {
        }
    }

    public static void listenForRemap(RemapInputElement remapInputElement) {
        element = remapInputElement;
        remapping = true;
    }

    private static boolean isPlaystation(Controller controller) {
        if (controller == null) {
            return false;
        }
        String string = controller.getName().toLowerCase();
        return string.contains("dualsense") || string.contains("wireless controller") || string.contains("sony") || string.contains("ps5") || string.contains("ps4");
    }

    private int translateButton(Controller controller, int n) {
        if (!CInputListener.isPlaystation(controller)) {
            return n;
        }
        switch (n) {
            case 0: return 2;     // Square -> STS Top Panel (2)
            case 1: return 0;     // Cross -> STS Select (0)
            case 2: return 1;     // Circle -> STS Cancel (1)
            case 3: return 3;     // Triangle -> STS Proceed (3)
            case 4: return 4;     // L1 -> STS Page Left (4)
            case 5: return 5;     // R1 -> STS Page Right (5)
            case 6: return 1004;  // L2 (button) -> STS Draw Pile
            case 7: return -1004; // R2 (button) -> STS Discard Pile
            case 8: return 6;     // Share/Create -> STS Map (6)
            case 9: return 7;     // Options -> STS Settings (7)
            case 10: return 8;    // L3 -> STS Peek (8)
            case 11: return 9;    // R3
            case 15: return -2000; // D-Pad Up
            case 16: return 2001;  // D-Pad Right
            case 17: return 2000;  // D-Pad Down
            case 18: return -2001; // D-Pad Left
        }
        return n;
    }

    public void connected(Controller controller) {
        logInput("CONNECTED: " + controller.getName());
    }

    public void disconnected(Controller controller) {
        logInput("DISCONNECTED: " + controller.getName());
    }

    public boolean buttonDown(Controller controller, int n) {
        if (!Settings.CONTROLLER_ENABLED) {
            return false;
        }
        int n2 = this.translateButton(controller, n);
        logInput("buttonDown: raw=" + n + " translated=" + n2);
        if (remapping && element != null && CInputListener.element.cAction != null) {
            if (CInputListener.element.listener.willRemapController(element, CInputListener.element.cAction.keycode, n2)) {
                CInputListener.element.cAction.remap(n2);
            }
            remapping = false;
            CInputListener.element.hasInputFocus = false;
            InputHelper.regainInputFocus();
            CInputHelper.regainInputFocus();
            element = null;
            return false;
        }
        if (!Settings.isControllerMode) {
            Settings.isControllerMode = true;
            Settings.isTouchScreen = false;
            logger.info("Entering controller mode: BUTTON pressed");
        }
        CInputHelper.listenerPress(n2);
        return false;
    }

    public boolean buttonUp(Controller controller, int n) {
        int n2 = this.translateButton(controller, n);
        logInput("buttonUp: raw=" + n + " translated=" + n2);
        CInputHelper.listenerRelease(n2);
        return false;
    }

    private void handleDpadAxis(float val) {
        int newState = 8;
        if (val > 1.1f) {
            newState = 8; // Centered
        } else if (val < -0.85f) {
            newState = 0; // Up
        } else if (val >= -0.85f && val < -0.6f) {
            newState = 1; // Up-Right
        } else if (val >= -0.6f && val < -0.25f) {
            newState = 2; // Right
        } else if (val >= -0.25f && val < -0.05f) {
            newState = 3; // Down-Right
        } else if (val >= -0.05f && val < 0.3f) {
            newState = 4; // Down
        } else if (val >= 0.3f && val < 0.55f) {
            newState = 5; // Down-Left
        } else if (val >= 0.55f && val < 0.85f) {
            newState = 6; // Left
        } else if (val >= 0.85f && val <= 1.1f) {
            newState = 7; // Up-Left
        }

        if (newState == this.lastDpadState) {
            return;
        }

        // Release keys from previous state
        if (this.lastDpadState == 0 || this.lastDpadState == 1 || this.lastDpadState == 7) {
            CInputHelper.listenerRelease(-2000); // Release Up
        }
        if (this.lastDpadState == 3 || this.lastDpadState == 4 || this.lastDpadState == 5) {
            CInputHelper.listenerRelease(2000); // Release Down
        }
        if (this.lastDpadState == 1 || this.lastDpadState == 2 || this.lastDpadState == 3) {
            CInputHelper.listenerRelease(2001); // Release Right
        }
        if (this.lastDpadState == 5 || this.lastDpadState == 6 || this.lastDpadState == 7) {
            CInputHelper.listenerRelease(-2001); // Release Left
        }

        if (!Settings.isControllerMode && newState != 8) {
            Settings.isControllerMode = true;
            Settings.isTouchScreen = false;
        }

        // Press keys for new state
        if (newState == 0 || newState == 1 || newState == 7) {
            CInputHelper.listenerPress(-2000); // Press Up
            logInput("D-Pad UP pressed");
        }
        if (newState == 3 || newState == 4 || newState == 5) {
            CInputHelper.listenerPress(2000); // Press Down
            logInput("D-Pad DOWN pressed");
        }
        if (newState == 1 || newState == 2 || newState == 3) {
            CInputHelper.listenerPress(2001); // Press Right
            logInput("D-Pad RIGHT pressed");
        }
        if (newState == 5 || newState == 6 || newState == 7) {
            CInputHelper.listenerPress(-2001); // Press Left
            logInput("D-Pad LEFT pressed");
        }

        this.lastDpadState = newState;
    }

    public boolean axisMoved(Controller controller, int n, float f) {
        if (!Settings.CONTROLLER_ENABLED) {
            return false;
        }
        if (n >= this.axisValues.length) {
            return false;
        }

        boolean isPS = CInputListener.isPlaystation(controller);
        if (isPS && n == 6) {
            // DualSense D-Pad on macOS
            this.handleDpadAxis(f);
            this.axisValues[n] = f;
            return false;
        }

        int n2 = n;
        if (isPS) {
            if (n == 0) {
                // Left stick X <-> Y
                n2 = 1;
            } else if (n == 1) {
                n2 = 0;
            } else if (n == 2) {
                // Right stick X <-> Y
                n2 = 3;
            } else if (n == 3) {
                n2 = 2;
            } else if (n == 4) {
                // L2 analog trigger
                if (f > 0.3f && this.axisValues[n] <= 0.3f) {
                    CInputHelper.listenerPress(1004);
                } else if (f <= 0.3f && this.axisValues[n] > 0.3f) {
                    CInputHelper.listenerRelease(1004);
                }
                this.axisValues[n] = f;
                return false;
            } else if (n == 5) {
                // R2 analog trigger
                if (f > 0.3f && this.axisValues[n] <= 0.3f) {
                    CInputHelper.listenerPress(-1004);
                } else if (f <= 0.3f && this.axisValues[n] > 0.3f) {
                    CInputHelper.listenerRelease(-1004);
                }
                this.axisValues[n] = f;
                return false;
            }
        }

        if (remapping && element != null && CInputListener.element.cAction != null) {
            if (f > 0.5f && this.axisValues[n] <= 0.5f) {
                if (CInputListener.element.listener.willRemapController(element, CInputListener.element.cAction.keycode, 1000 + n2)) {
                    CInputListener.element.cAction.remap(1000 + n2);
                }
                remapping = false;
                CInputListener.element.hasInputFocus = false;
                InputHelper.regainInputFocus();
                CInputHelper.regainInputFocus();
                element = null;
                return false;
            }
            if (f < -0.5f && this.axisValues[n] >= -0.5f) {
                if (CInputListener.element.listener.willRemapController(element, CInputListener.element.cAction.keycode, -1000 - n2)) {
                    CInputListener.element.cAction.remap(-1000 - n2);
                }
                remapping = false;
                CInputListener.element.hasInputFocus = false;
                InputHelper.regainInputFocus();
                CInputHelper.regainInputFocus();
                element = null;
                return false;
            }
        }

        if (f > 0.5f && this.axisValues[n] <= 0.5f) {
            CInputHelper.listenerPress(1000 + n2);
            if (!Settings.isControllerMode) {
                Settings.isControllerMode = true;
                Settings.isTouchScreen = false;
                logger.info("Entering controller mode: AXIS moved");
            }
        } else if (f < -0.5f && this.axisValues[n] >= -0.5f) {
            CInputHelper.listenerPress(-1000 - n2);
            if (!Settings.isControllerMode) {
                Settings.isControllerMode = true;
                Settings.isTouchScreen = false;
                logger.info("Entering controller mode: AXIS moved");
            }
        }
        if (this.axisValues[n] > 0.5f && f <= 0.5f) {
            CInputHelper.listenerRelease(1000 + n2);
        } else if (this.axisValues[n] < -0.5f && f >= -0.5f) {
            CInputHelper.listenerRelease(-1000 - n2);
        }
        this.axisValues[n] = f;
        return false;
    }

    public boolean povMoved(Controller controller, int n, PovDirection povDirection) {
        if (!Settings.CONTROLLER_ENABLED) {
            return false;
        }
        if (remapping && element != null && CInputListener.element.cAction != null) {
            int n2 = -2000;
            switch (povDirection) {
                case north: {
                    n2 = -2000;
                    break;
                }
                case south: {
                    n2 = 2000;
                    break;
                }
                case west: 
                case northWest: 
                case southWest: {
                    n2 = -2001;
                    break;
                }
                case east: 
                case northEast: 
                case southEast: {
                    n2 = 2001;
                    break;
                }
                default: {
                    n2 = -2000;
                }
            }
            if (CInputListener.element.listener.willRemapController(element, CInputListener.element.cAction.keycode, n2)) {
                CInputListener.element.cAction.remap(n2);
            }
            remapping = false;
            CInputListener.element.hasInputFocus = false;
            InputHelper.regainInputFocus();
            CInputHelper.regainInputFocus();
            element = null;
            return false;
        }
        if (povDirection == PovDirection.center) {
            CInputHelper.listenerRelease(-2000);
            CInputHelper.listenerRelease(2000);
            CInputHelper.listenerRelease(-2001);
            CInputHelper.listenerRelease(2001);
            return false;
        }
        if (!Settings.isControllerMode) {
            Settings.isControllerMode = true;
            Settings.isTouchScreen = false;
            logger.info("Entering controller mode: D-Pad pressed");
        }
        switch (povDirection) {
            case north: {
                CInputHelper.listenerPress(-2000);
                break;
            }
            case south: {
                CInputHelper.listenerPress(2000);
                break;
            }
            case west: {
                CInputHelper.listenerPress(-2001);
                break;
            }
            case east: {
                CInputHelper.listenerPress(2001);
                break;
            }
            case northWest: {
                CInputHelper.listenerPress(-2000);
                CInputHelper.listenerPress(-2001);
                break;
            }
            case northEast: {
                CInputHelper.listenerPress(-2000);
                CInputHelper.listenerPress(2001);
                break;
            }
            case southWest: {
                CInputHelper.listenerPress(2000);
                CInputHelper.listenerPress(-2001);
                break;
            }
            case southEast: {
                CInputHelper.listenerPress(2000);
                CInputHelper.listenerPress(2001);
                break;
            }
            default:
                break;
        }
        return false;
    }

    public boolean xSliderMoved(Controller controller, int n, boolean bl) {
        return false;
    }

    public boolean ySliderMoved(Controller controller, int n, boolean bl) {
        return false;
    }

    public boolean accelerometerMoved(Controller controller, int n, Vector3 vector3) {
        return false;
    }
}
