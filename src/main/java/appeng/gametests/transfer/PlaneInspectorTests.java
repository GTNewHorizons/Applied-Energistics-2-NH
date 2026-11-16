package appeng.gametests.transfer;

import static appeng.gametests.transfer.TransferTestHelpers.TEMPLATE;
import static appeng.gametests.transfer.TransferTestHelpers.exports;
import static appeng.gametests.transfer.TransferTestHelpers.gold;
import static appeng.gametests.transfer.TransferTestHelpers.imports;
import static appeng.gametests.transfer.TransferTestHelpers.install;
import static appeng.gametests.transfer.TransferTestHelpers.iron;
import static appeng.gametests.transfer.TransferTestHelpers.place;
import static appeng.gametests.transfer.TransferTestHelpers.water;
import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;

import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import appeng.api.AEApi;
import appeng.api.definitions.IMaterials;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.storage.StorageName;
import appeng.core.AppEng;
import appeng.helpers.transfer.AnnihilationPlaneInspector;
import appeng.helpers.transfer.FormationPlaneInspector;
import appeng.parts.automation.PartAnnihilationPlane;
import appeng.parts.automation.PartFormationPlane;

@GameTestHolder(AppEng.MOD_ID)
public class PlaneInspectorTests {

    private static final FormationPlaneInspector FORMATION = new FormationPlaneInspector();

    private static IMaterials cards() {
        return AEApi.instance().definitions().materials();
    }

    private static PartFormationPlane formationPlane(GameTestHelper helper) {
        return place(helper, AEApi.instance().definitions().parts().formationPlane(), ForgeDirection.UP);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void emptyFormationPlaneExportsEverything(GameTestHelper helper) {
        TransferDescription description = FORMATION.inspect(formationPlane(helper));

        helper.assertTrue(exports(description, iron()), "An unfiltered plane accepts anything");
        helper.assertFalse(imports(description, iron()), "A plane never imports");
        helper.assertFalse(exports(description, water()), "An item plane ignores fluids");
        helper.assertTrue(description.storageHandler, "Formation planes follow AE's routing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void formationPlaneFiltersAndInverts(GameTestHelper helper) {
        PartFormationPlane plane = formationPlane(helper);
        plane.getAEInventoryByName(StorageName.CONFIG).putAEStackInSlot(0, iron());

        TransferDescription whitelist = FORMATION.inspect(plane);
        helper.assertTrue(exports(whitelist, iron()), "Listed stack passes");
        helper.assertFalse(exports(whitelist, gold()), "Other stacks are refused");
        helper.assertTrue(whitelist.matcher.isExplicitlyListed(iron()), "Whitelisted stack is listed");

        install(plane.getInventoryByName("upgrades"), cards().cardInverter());
        TransferDescription blacklist = FORMATION.inspect(plane);
        helper.assertFalse(exports(blacklist, iron()), "Inverted list refuses the stack");
        helper.assertTrue(exports(blacklist, gold()), "Inverted list passes other stacks");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void formationPlaneHonoursCapacityAndPriority(GameTestHelper helper) {
        PartFormationPlane plane = formationPlane(helper);
        plane.getAEInventoryByName(StorageName.CONFIG).putAEStackInSlot(20, iron());
        plane.setPriority(9);

        TransferDescription withoutCard = FORMATION.inspect(plane);
        helper.assertTrue(exports(withoutCard, gold()), "Slot 20 is unusable, so the plane is unfiltered");
        helper.assertEquals(9, withoutCard.priority, "Priority should be reported");

        install(plane.getInventoryByName("upgrades"), cards().cardCapacity());
        TransferDescription withCard = FORMATION.inspect(plane);
        helper.assertTrue(exports(withCard, iron()), "A capacity card opens slot 20");
        helper.assertFalse(exports(withCard, gold()), "The filter now applies");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void annihilationPlaneImportsItsStackType(GameTestHelper helper) {
        PartAnnihilationPlane plane = place(
                helper,
                AEApi.instance().definitions().parts().annihilationPlane(),
                ForgeDirection.UP);

        TransferDescription description = new AnnihilationPlaneInspector(() -> ITEM_STACK_TYPE).inspect(plane);

        helper.assertTrue(description.canImport, "Annihilation planes import");
        helper.assertTrue(imports(description, iron()), "Any item can be broken");
        helper.assertFalse(exports(description, iron()), "A plane never exports");
        helper.assertFalse(imports(description, water()), "An item plane does not produce fluids");
        helper.succeed();
    }
}
