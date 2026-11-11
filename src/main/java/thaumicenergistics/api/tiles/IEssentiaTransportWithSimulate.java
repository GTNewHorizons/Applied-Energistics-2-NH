package thaumicenergistics.api.tiles;

import net.minecraftforge.common.util.ForgeDirection;

import org.jspecify.annotations.NonNull;

import appeng.api.config.Actionable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;

/**
 * An essentia transport that supports {@link Actionable#SIMULATE} and {@link Actionable#MODULATE}.
 *
 * @author Nividica
 *
 */
public interface IEssentiaTransportWithSimulate extends IEssentiaTransport {

    /**
     * Adds essentia to the transport.
     *
     * @param aspect
     * @param amount
     * @param side
     * @param mode
     * @return Amount that <strong>was</strong> injected.
     */
    int addEssentia(@NonNull Aspect aspect, int amount, @NonNull ForgeDirection side, @NonNull Actionable mode);
}
