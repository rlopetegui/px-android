package com.mercadopago.android.px.tracking.internal.events;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.mercadopago.android.px.internal.repository.UserSelectionRepository;
import com.mercadopago.android.px.model.ExpressMetadata;
import com.mercadopago.android.px.model.PayerCost;
import com.mercadopago.android.px.tracking.internal.mapper.FromSelectedExpressMetadataToAvailableMethods;
import com.mercadopago.android.px.tracking.internal.mapper.FromUserSelectionToAvailableMethod;
import com.mercadopago.android.px.tracking.internal.model.AvailableMethod;
import com.mercadopago.android.px.tracking.internal.model.ConfirmData;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ConfirmEvent extends EventTracker {

    private static final String EVENT_PATH_REVIEW_CONFIRM = BASE_PATH + "/review/confirm";
    private final ConfirmData data;

    public enum ReviewType {
        ONE_TAP("one_tap"),
        TRADITIONAL("traditional");

        /* default */ public final String value;

        /* default */ ReviewType(final String value) {
            this.value = value;
        }
    }

    @NonNull
    public static ConfirmEvent from(@NonNull final Set<String> cardsWithEsc,
        @NonNull final ExpressMetadata expressMetadata,
        @Nullable final PayerCost selectedPayerCost,
        final boolean isSplit, final int paymentMethodSelectedIndex) {
        return new ConfirmEvent(new ConfirmData(ReviewType.ONE_TAP, paymentMethodSelectedIndex,
            new FromSelectedExpressMetadataToAvailableMethods(cardsWithEsc, selectedPayerCost, isSplit)
                .map(expressMetadata)));
    }

    @NonNull
    public static ConfirmEvent from(@Nullable final String paymentTypeId, @Nullable final String paymentMethodId,
        final boolean isCompliant, final boolean isAdditionalInfoNeeded) {
        final Map<String, Object> extraInfo = new HashMap<>();
        extraInfo.put("has_payer_information", isCompliant);
        extraInfo.put("additional_information_needed", isAdditionalInfoNeeded);

        final AvailableMethod availableMethod = new AvailableMethod(paymentMethodId, paymentTypeId, extraInfo);

        return new ConfirmEvent(new ConfirmData(ReviewType.ONE_TAP, availableMethod));
    }

    @NonNull
    public static ConfirmEvent from(@NonNull final Set<String> cardsWithEsc,
        @NonNull final UserSelectionRepository userSelectionRepository) {
        final AvailableMethod ava = new FromUserSelectionToAvailableMethod(cardsWithEsc).map(userSelectionRepository);
        return new ConfirmEvent(new ConfirmData(ReviewType.TRADITIONAL, ava));
    }

    private ConfirmEvent(@NonNull final ConfirmData data) {
        this.data = data;
    }

    @NonNull
    @Override
    public Map<String, Object> getEventData() {
        return data.toMap();
    }

    @NonNull
    @Override
    public String getEventPath() {
        return EVENT_PATH_REVIEW_CONFIRM;
    }
}
