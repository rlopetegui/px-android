package com.mercadopago.android.px.internal.features.express.slider;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.mercadopago.android.px.R;
import com.mercadopago.android.px.internal.view.LinkableTextView;
import com.mercadopago.android.px.internal.viewmodel.drawables.ConsumerCreditsDrawableFragmentItem;
import com.mercadopago.android.px.model.ConsumerCreditsDisplayInfo;

public class ConsumerCreditsLowResFragment extends ConsumerCreditsFragment {

    @NonNull
    public static Fragment getInstance(@NonNull final ConsumerCreditsDrawableFragmentItem model) {
        final ConsumerCreditsLowResFragment instance = new ConsumerCreditsLowResFragment();
        instance.storeModel(model);
        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container,
        @Nullable final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.px_fragment_consumer_credits_low_res, container, false);
    }

    @Override
    protected void showDisplayInfo(final View view, @NonNull final ConsumerCreditsDisplayInfo displayInfo) {
        ((LinkableTextView) view.findViewById(R.id.bottom_text)).updateModel(displayInfo.bottomText);
    }
}