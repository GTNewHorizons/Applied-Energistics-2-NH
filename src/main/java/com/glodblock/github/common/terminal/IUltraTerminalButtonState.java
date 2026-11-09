package com.glodblock.github.common.terminal;

/**
 * Server safe view of the ultra terminal button state, so that packets do not have to reference client classes.
 */
public interface IUltraTerminalButtonState {

    void toggleRestock();

    void toggleMagnetMode();
}
