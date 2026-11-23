package appeng.container.stackedit;

import appeng.core.sync.GuiBridge;

public enum StackEditMode {

    AMOUNT(GuiBridge.GUI_STACK_AMOUNT),
    NAME(GuiBridge.GUI_STACK_RENAME);

    private final GuiBridge gui;

    StackEditMode(final GuiBridge gui) {
        this.gui = gui;
    }

    public GuiBridge getGui() {
        return this.gui;
    }
}
