package com.mercadopago.android.px.tracking.internal.model;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;

@SuppressWarnings("unused")
@Keep
public class AvailableOfflineMethod extends TrackingMapModel {

    @NonNull
    /* default */ final String paymentMethodId;
    @NonNull
    /* default */ final String paymentTypeId;

    public AvailableOfflineMethod(@NonNull final String paymentTypeId, @NonNull final String paymentMethodId) {
        this.paymentTypeId = paymentTypeId;
        this.paymentMethodId = paymentMethodId;
    }
}
