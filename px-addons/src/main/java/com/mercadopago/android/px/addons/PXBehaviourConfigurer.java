package com.mercadopago.android.px.addons;

import android.support.annotation.NonNull;

public final class PXBehaviourConfigurer {

    private SecurityBehaviour securityBehaviour;
    private ESCManagerBehaviour escManagerBehaviour;
    private TrackingBehaviour trackingBehaviour;
    private LocaleBehaviour localeBehaviour;

    public PXBehaviourConfigurer with(@NonNull final ESCManagerBehaviour escManagerBehaviour) {
        this.escManagerBehaviour = escManagerBehaviour;
        return this;
    }

    public PXBehaviourConfigurer with(@NonNull final SecurityBehaviour securityBehaviour) {
        this.securityBehaviour = securityBehaviour;
        return this;
    }

    public PXBehaviourConfigurer with(@NonNull final TrackingBehaviour trackingBehaviour) {
        this.trackingBehaviour = trackingBehaviour;
        return this;
    }

    public PXBehaviourConfigurer with(@NonNull final LocaleBehaviour localeBehaviour) {
        this.localeBehaviour = localeBehaviour;
        return this;
    }

    public void configure() {
        BehaviourProvider.set(securityBehaviour);
        BehaviourProvider.set(escManagerBehaviour);
        BehaviourProvider.set(trackingBehaviour);
        BehaviourProvider.set(localeBehaviour);
    }
}