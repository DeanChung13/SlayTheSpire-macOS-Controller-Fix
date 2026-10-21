package com.megacrit.cardcrawl.helpers.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.input.InputHelper;

public class MacInputWrapper implements Input {
    private final Input delegate;
    public static int forcedX = -1;
    public static int forcedY = -1;

    public MacInputWrapper(Input delegate) {
        this.delegate = delegate;
    }

    @Override
    public void setCursorPosition(int x, int y) {
        forcedX = x;
        forcedY = y;
        InputHelper.mX = x;
        InputHelper.mY = Settings.HEIGHT - y;
        this.delegate.setCursorPosition(x, y);
    }

    @Override
    public int getX() {
        if (Settings.isControllerMode && forcedX >= 0) {
            return forcedX;
        }
        return this.delegate.getX();
    }

    @Override
    public int getX(int pointer) {
        if (pointer > 0) return 0;
        return this.getX();
    }

    @Override
    public int getY() {
        if (Settings.isControllerMode && forcedY >= 0) {
            return forcedY;
        }
        return this.delegate.getY();
    }

    @Override
    public int getY(int pointer) {
        if (pointer > 0) return 0;
        return this.getY();
    }

    @Override
    public int getDeltaX() {
        int dx = this.delegate.getDeltaX();
        if (Math.abs(dx) > 10) {
            forcedX = -1;
            forcedY = -1;
        }
        return dx;
    }

    @Override
    public int getDeltaX(int pointer) {
        if (pointer > 0) return 0;
        return this.getDeltaX();
    }

    @Override
    public int getDeltaY() {
        int dy = this.delegate.getDeltaY();
        if (Math.abs(dy) > 10) {
            forcedX = -1;
            forcedY = -1;
        }
        return dy;
    }

    @Override
    public int getDeltaY(int pointer) {
        if (pointer > 0) return 0;
        return this.getDeltaY();
    }

    @Override
    public float getAccelerometerX() { return this.delegate.getAccelerometerX(); }
    @Override
    public float getAccelerometerY() { return this.delegate.getAccelerometerY(); }
    @Override
    public float getAccelerometerZ() { return this.delegate.getAccelerometerZ(); }
    @Override
    public float getGyroscopeX() { return this.delegate.getGyroscopeX(); }
    @Override
    public float getGyroscopeY() { return this.delegate.getGyroscopeY(); }
    @Override
    public float getGyroscopeZ() { return this.delegate.getGyroscopeZ(); }
    @Override
    public boolean isTouched() { return this.delegate.isTouched(); }
    @Override
    public boolean justTouched() { return this.delegate.justTouched(); }
    @Override
    public boolean isTouched(int pointer) { return this.delegate.isTouched(pointer); }
    @Override
    public boolean isButtonPressed(int button) { return this.delegate.isButtonPressed(button); }
    @Override
    public boolean isKeyPressed(int key) { return this.delegate.isKeyPressed(key); }
    @Override
    public boolean isKeyJustPressed(int key) { return this.delegate.isKeyJustPressed(key); }
    @Override
    public void getTextInput(TextInputListener listener, String title, String text, String hint) {
        this.delegate.getTextInput(listener, title, text, hint);
    }
    @Override
    public void setOnscreenKeyboardVisible(boolean visible) { this.delegate.setOnscreenKeyboardVisible(visible); }
    @Override
    public void vibrate(int milliseconds) { this.delegate.vibrate(milliseconds); }
    @Override
    public void vibrate(long[] pattern, int repeat) { this.delegate.vibrate(pattern, repeat); }
    @Override
    public void cancelVibrate() { this.delegate.cancelVibrate(); }
    @Override
    public float getAzimuth() { return this.delegate.getAzimuth(); }
    @Override
    public float getPitch() { return this.delegate.getPitch(); }
    @Override
    public float getRoll() { return this.delegate.getRoll(); }
    @Override
    public void getRotationMatrix(float[] matrix) { this.delegate.getRotationMatrix(matrix); }
    @Override
    public long getCurrentEventTime() { return this.delegate.getCurrentEventTime(); }
    @Override
    public void setCatchBackKey(boolean catchBack) { this.delegate.setCatchBackKey(catchBack); }
    @Override
    public boolean isCatchBackKey() { return this.delegate.isCatchBackKey(); }
    @Override
    public void setCatchMenuKey(boolean catchMenu) { this.delegate.setCatchMenuKey(catchMenu); }
    @Override
    public boolean isCatchMenuKey() { return this.delegate.isCatchMenuKey(); }
    @Override
    public void setInputProcessor(InputProcessor processor) { this.delegate.setInputProcessor(processor); }
    @Override
    public InputProcessor getInputProcessor() { return this.delegate.getInputProcessor(); }
    @Override
    public boolean isPeripheralAvailable(Peripheral peripheral) { return this.delegate.isPeripheralAvailable(peripheral); }
    @Override
    public int getRotation() { return this.delegate.getRotation(); }
    @Override
    public Orientation getNativeOrientation() { return this.delegate.getNativeOrientation(); }
    @Override
    public void setCursorCatched(boolean catched) { this.delegate.setCursorCatched(catched); }
    @Override
    public boolean isCursorCatched() { return this.delegate.isCursorCatched(); }
}
