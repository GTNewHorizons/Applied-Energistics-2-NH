package thaumicenergistics.api;

import java.lang.reflect.Method;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Thaumic Energistics API
 *
 * @author Nividica
 *
 */
public abstract class ThEApi {

    protected static ThEApi api = null;

    /**
     * Gets the Thaumic Energistics API. Note: Only available after the PREINIT event.
     */
    @Nullable
    public static ThEApi instance() {
        // Have we already retrieved the api?
        if (ThEApi.api == null) {
            try {
                // Attempt to locate the API implementation
                Class<?> clazz = Class.forName("thaumicenergistics.implementaion.ThEAPIImplementation");

                // Get the instance method
                Method instanceAccessor = clazz.getMethod("instance");

                // Attempt to get the API instance
                ThEApi.api = (ThEApi) instanceAccessor.invoke(null);
            } catch (Exception e) {
                // Unable to locate the API, return null
                return null;
            }
        }

        return ThEApi.api;
    }

    /**
     * Blocks
     */
    @NonNull
    public abstract IThEBlocks blocks();

    /**
     * Configuration
     */
    @NonNull
    public abstract IThEConfig config();

    /**
     * Gets the ThE interaction manager
     *
     * @return
     */
    @NonNull
    public abstract IThEInteractionHelper interact();

    /**
     * Items
     */
    @NonNull
    public abstract IThEItems items();

    /**
     * Cable Parts
     */
    @NonNull
    public abstract IThEParts parts();

    /**
     * Transport Permissions.
     */
    @NonNull
    public abstract IThETransportPermissions transportPermissions();
}
