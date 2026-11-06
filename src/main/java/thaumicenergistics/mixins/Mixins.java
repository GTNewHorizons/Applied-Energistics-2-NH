package thaumicenergistics.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    // spotless:off
    ASPECT_HOOKS(new MixinBuilder("Hook Thaumcraft aspect registration")
            .addCommonMixins("thaumcraft.MixinAspect")
            .addRequiredMod(TargetedMod.THAUMCRAFT)
            .setPhase(Phase.LATE)),
    GOLEM_HOOKS(new MixinBuilder("Hook Thaumcraft golems for ThE golem hook handlers")
            .addCommonMixins(
                    "thaumcraft.MixinEntityGolemBase",
                    "thaumcraft.MixinItemGolemBell",
                    "thaumcraft.MixinItemGolemPlacer")
            .addClientMixins("thaumcraft.MixinRenderGolemBase")
            .addRequiredMod(TargetedMod.THAUMCRAFT)
            .setPhase(Phase.LATE));
    // spotless:on

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
