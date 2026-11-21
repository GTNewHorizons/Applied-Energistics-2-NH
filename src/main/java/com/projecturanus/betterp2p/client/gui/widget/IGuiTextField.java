package com.projecturanus.betterp2p.client.gui.widget;

import org.jspecify.annotations.Nullable;

import com.projecturanus.betterp2p.client.gui.InfoWrapper;

import appeng.client.gui.widgets.MEGuiTextField;

public class IGuiTextField extends MEGuiTextField {

    private @Nullable InfoWrapper info;

    public void setVisible(boolean visible) {
        this.field.setVisible(visible);
    }

    public void setFocus(boolean focus, int position) {
        super.setFocused(focus);
        this.field.setCursorPosition(position);
    }

    public IGuiTextField(final int width, final int height, final String tooltip) {
        super(width, height, tooltip);
    }

    public IGuiTextField(final int width, final int height) {
        super(width, height, "");
        this.setVisible(false);
    }

    public void setInfo(@Nullable InfoWrapper _info) {
        info = _info;
    }

    public @Nullable InfoWrapper getInfo() {
        return info;
    }

    public void setFocus(boolean focus) {
        super.setFocused(focus);
    }
}
