package com.mercadopago.android.px.addons;

import android.support.annotation.NonNull;
import com.mercadopago.android.px.addons.internal.ESCManagerDefaultBehaviour;
import com.mercadopago.android.px.addons.internal.LocaleDefaultBehaviour;
import com.mercadopago.android.px.addons.internal.SecurityDefaultBehaviour;
import com.mercadopago.android.px.addons.internal.TrackingDefaultBehaviour;

public final class BehaviourProvider {

    private static SecurityBehaviour securityBehaviour;
    private static ESCManagerBehaviour escManagerBehaviour;
    private static TrackingBehaviour trackingBehaviour;
    private static LocaleBehaviour localeBehaviour;

    private BehaviourProvider() {
    }

    /* default */ static void set(final SecurityBehaviour securityBehaviour) {
        BehaviourProvider.securityBehaviour = securityBehaviour;
    }

    /* default */ static void set(final ESCManagerBehaviour escManagerBehaviour) {
        BehaviourProvider.escManagerBehaviour = escManagerBehaviour;
    }

    /* default */ static void set(final TrackingBehaviour trackingBehaviour) {
        BehaviourProvider.trackingBehaviour = trackingBehaviour;
    }

    /* default */ static void set(final LocaleBehaviour localeBehaviour) {
        BehaviourProvider.localeBehaviour = localeBehaviour;
    }

    @NonNull
    public static SecurityBehaviour getSecurityBehaviour() {
        return securityBehaviour != null ? securityBehaviour : new SecurityDefaultBehaviour();
    }

    /**
     * @param session session id for tracking purpose
     * @param escEnabled indicates if current flow works with esc or not
     * @return EscManagerBehaviour implementation.
     */
    @NonNull
    public static ESCManagerBehaviour getEscManagerBehaviour(@NonNull final String session, final boolean escEnabled) {
        final ESCManagerBehaviour escManagerBehaviour = resolveEscImplementation(escEnabled);
        escManagerBehaviour.setSessionId(session);
        return escManagerBehaviour;
    }

    @NonNull
    public static TrackingBehaviour getTrackingBehaviour(@NonNull final String applicationContext) {
        if (trackingBehaviour != null) {
            trackingBehaviour.setApplicationContext(applicationContext);
            return trackingBehaviour;
        } else {
            return new TrackingDefaultBehaviour();
        }
    }

    @NonNull
    public static LocaleBehaviour getLocaleBehaviour() {
        return localeBehaviour != null ? localeBehaviour : new LocaleDefaultBehaviour();
    }

    @NonNull
    private static ESCManagerBehaviour resolveEscImplementation(final boolean escEnabled) {
        if (escEnabled) {
            return escManagerBehaviour != null ? escManagerBehaviour : new ESCManagerDefaultBehaviour();
        } else {
            return new ESCManagerDefaultBehaviour();
        }
    }
}