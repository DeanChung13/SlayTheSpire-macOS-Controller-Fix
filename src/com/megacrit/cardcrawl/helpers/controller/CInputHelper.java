/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.badlogic.gdx.Gdx
 *  com.badlogic.gdx.controllers.Controller
 *  com.badlogic.gdx.controllers.ControllerListener
 *  com.badlogic.gdx.controllers.Controllers
 *  com.badlogic.gdx.utils.Array
 *  com.megacrit.cardcrawl.core.Settings
 *  com.megacrit.cardcrawl.dungeons.AbstractDungeon
 *  com.megacrit.cardcrawl.helpers.Hitbox
 *  com.megacrit.cardcrawl.helpers.ImageMaster
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 *  org.lwjgl.opengl.Display
 */
package com.megacrit.cardcrawl.helpers.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.utils.Array;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputAction;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.controller.CInputListener;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.Display;

public class CInputHelper {
    private static final Logger logger = LogManager.getLogger((String)CInputHelper.class.getName());
    public static Array<Controller> controllers = null;
    public static Controller controller = null;
    public static ArrayList<CInputAction> actions = new ArrayList();
    public static CInputListener listener = null;
    private static boolean initializedController = false;
    public static ControllerModel model = null;

    public static void loadSettings() {
        if (!(Gdx.input instanceof MacInputWrapper)) {
            Gdx.input = new MacInputWrapper(Gdx.input);
            logger.info("[CONTROLLER] MacInputWrapper installed in loadSettings!");
        }
        CInputActionSet.load();
    }

    public static void initializeIfAble() {
        if (!initializedController && Display.isActive()) {
            if (!(Gdx.input instanceof MacInputWrapper)) {
                Gdx.input = new MacInputWrapper(Gdx.input);
                logger.info("[CONTROLLER] MacInputWrapper installed in initializeIfAble!");
            }
            initializedController = true;
            logger.info("[CONTROLLER] Utilizing DirectInput");
            try {
                controllers = Controllers.getControllers();
            }
            catch (Exception e) {
                logger.info(e.getMessage());
            }
            if (controllers == null) {
                logger.info("[ERROR] getControllers() returned null");
                return;
            }
            for (int i = 0; i < CInputHelper.controllers.size; ++i) {
                logger.info("CONTROLLER[" + i + "] " + ((Controller)controllers.get(i)).getName());
            }
            if (CInputHelper.controllers.size != 0) {
                Settings.isControllerMode = true;
                Settings.isTouchScreen = false;
                listener = new CInputListener();
                controller = (Controller)controllers.first();
                controller.addListener((ControllerListener)listener);
                if (controller.getName().contains("360")) {
                    model = ControllerModel.XBOX_360;
                    ImageMaster.loadControllerImages((ControllerModel)ControllerModel.XBOX_360);
                } else if (controller.getName().contains("Xbox One")) {
                    model = ControllerModel.XBOX_ONE;
                    ImageMaster.loadControllerImages((ControllerModel)ControllerModel.XBOX_ONE);
                } else {
                    String name = controller.getName().toLowerCase();
                    if (name.contains("dualsense") || name.contains("wireless controller") || name.contains("sony") || name.contains("ps5") || name.contains("ps4") || name.contains("playstation")) {
                        model = ControllerModel.PS4;
                        ImageMaster.loadControllerImages(ControllerModel.PS4);
                        logger.info("[CONTROLLER] Detected PlayStation / DualSense controller: " + controller.getName() + " -> Loaded PS4 icons!");
                    } else {
                        model = ControllerModel.XBOX_360;
                        ImageMaster.loadControllerImages((ControllerModel)ControllerModel.XBOX_360);
                    }
                }
            } else {
                logger.info("[CONTROLLER] No controllers detected");
            }
        }
    }

    public static void setController(Controller controllerToUse) {
        Settings.isControllerMode = true;
        Settings.isTouchScreen = false;
        controller = controllerToUse;
        controller.addListener((ControllerListener)listener);
        Controllers.removeListener((ControllerListener)listener);
        String name = controller.getName().toLowerCase();
        if (name.contains("dualsense") || name.contains("wireless controller") || name.contains("sony") || name.contains("ps5") || name.contains("ps4") || name.contains("playstation")) {
            model = ControllerModel.PS4;
            ImageMaster.loadControllerImages(ControllerModel.PS4);
        } else if (controller.getName().contains("Xbox One")) {
            model = ControllerModel.XBOX_ONE;
            ImageMaster.loadControllerImages(ControllerModel.XBOX_ONE);
        } else {
            model = ControllerModel.XBOX_360;
            ImageMaster.loadControllerImages(ControllerModel.XBOX_360);
        }
    }

    public static void setCursor(Hitbox hb) {
        if (hb != null) {
            InputHelper.mX = (int)hb.cX;
            InputHelper.mY = (int)hb.cY;
            hb.hovered = true;
            hb.justHovered = true;
            int y = Settings.HEIGHT - (int)hb.cY;
            if (y < 0) {
                y = 0;
            } else if (y > Settings.HEIGHT) {
                y = Settings.HEIGHT;
            }
            try {
                Gdx.input.setCursorPosition((int)hb.cX, y);
            } catch (Exception ignored) {
            }
        }
    }

    public static void updateLast() {
        for (CInputAction a : actions) {
            a.justPressed = false;
            a.justReleased = false;
        }
    }

    public static boolean listenerPress(int keycode) {
        for (CInputAction a : actions) {
            if (a.keycode != keycode) continue;
            a.justPressed = true;
            a.pressed = true;
            break;
        }
        return false;
    }

    public static boolean listenerRelease(int keycode) {
        for (CInputAction a : actions) {
            if (a.keycode != keycode) continue;
            a.justReleased = true;
            a.pressed = false;
            break;
        }
        return false;
    }

    public static boolean isJustPressed(int keycode) {
        for (CInputAction a : actions) {
            if (a.keycode != keycode) continue;
            return a.justPressed;
        }
        return false;
    }

    public static boolean isJustReleased(int keycode) {
        for (CInputAction a : actions) {
            if (a.keycode != keycode) continue;
            return a.justReleased;
        }
        return false;
    }

    public static void regainInputFocus() {
        CInputListener.remapping = false;
    }

    public static boolean isTopPanelActive() {
        return AbstractDungeon.topPanel.selectPotionMode || AbstractDungeon.player.viewingRelics || !AbstractDungeon.topPanel.potionUi.isHidden;
    }

    public static enum ControllerModel {
        XBOX_360,
        XBOX_ONE,
        PS3,
        PS4,
        STEAM,
        OTHER;

    }
}
