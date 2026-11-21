package appeng.crafting.v2;

import appeng.core.localization.GuiText;

/**
 * Thrown when ingredients of a plan are still missing after it has been re-resolved as often as allowed. Like
 * {@link CraftingStepLimitExceeded}, {@link #toString()} is a translation key, because the job reports it as its error
 * message for the crafting confirm screen. The detail, including which items are missing, is in {@link #getMessage()}.
 */
public class CraftingIngredientsKeptChanging extends RuntimeException {

    public CraftingIngredientsKeptChanging(String message) {
        super(message);
    }

    @Override
    public String toString() {
        return GuiText.CraftingIngredientsKeptChanging.getUnlocalized();
    }
}
