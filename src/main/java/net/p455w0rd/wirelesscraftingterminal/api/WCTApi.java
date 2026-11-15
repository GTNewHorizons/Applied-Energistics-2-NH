package net.p455w0rd.wirelesscraftingterminal.api;

import java.lang.reflect.Method;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public abstract class WCTApi {

    protected static WCTApi api = null;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static @Nullable WCTApi instance() {
        // Have we already retrieved the api?
        if (WCTApi.api == null) {
            try {
                // Attempt to locate the API implementation
                Class clazz = Class
                        .forName("net.p455w0rd.wirelesscraftingterminal.implementation.WCTAPIImplementation");

                // Get the instance method
                Method instanceAccessor = clazz.getMethod("instance");

                // Attempt to get the API instance
                WCTApi.api = (WCTApi) instanceAccessor.invoke(null);
            } catch (Throwable e) {
                // Unable to locate the API, return null
                return null;
            }
        }

        return WCTApi.api;
    }

    public abstract @NonNull IWCTInteractionHelper interact();

    public abstract @NonNull IWCTItems items();

}
