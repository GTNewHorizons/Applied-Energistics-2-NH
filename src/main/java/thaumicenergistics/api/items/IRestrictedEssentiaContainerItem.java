package thaumicenergistics.api.items;

import org.jspecify.annotations.NonNull;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaContainerItem;

/**
 * Essentia container item that restricts what aspects it can store.
 *
 * @author Nividica
 *
 */
public interface IRestrictedEssentiaContainerItem extends IEssentiaContainerItem {

    /**
     * Returns true if the container accepts the specified aspect.
     *
     * @param aspect
     * @return
     */
    boolean acceptsAspect(@NonNull Aspect aspect);
}
