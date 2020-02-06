package com.mercadopago.android.px.internal.features.express.add_new_card;

import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import com.mercadopago.android.px.R;
import com.mercadopago.android.px.internal.util.ViewUtils;
import com.mercadopago.android.px.internal.view.MPTextView;

/* default */ final class OfflineMethodsRowHolder extends RecyclerView.ViewHolder {

    private final MPTextView paymentTypeName;
    private final MPTextView paymentMethodName;
    private final MPTextView description;
    private final ImageView methodImageView;
    private final RadioButton radioButton;

    /* default */ OfflineMethodsRowHolder(@NonNull final View itemView) {
        super(itemView);
        paymentTypeName = itemView.findViewById(R.id.payment_type_name);
        paymentMethodName = itemView.findViewById(R.id.payment_method_name);
        description = itemView.findViewById(R.id.description);
        methodImageView = itemView.findViewById(R.id.method_image);
        radioButton = itemView.findViewById(R.id.radio_button);
    }

    /* default */ void populate(final OfflineMethodItem offlineItem,
        final OfflineMethodsAdapter.OnItemClicked onItemClicked) {
        if (offlineItem.isOfflinePaymentTypeItem()) {
            ViewUtils.loadOrHide(View.GONE, offlineItem.getName(), paymentTypeName);
            paymentMethodName.setVisibility(View.GONE);
            description.setVisibility(View.GONE);
            methodImageView.setVisibility(View.GONE);
            radioButton.setVisibility(View.GONE);
            itemView.setOnClickListener(null);
        } else {
            paymentTypeName.setVisibility(View.GONE);
            ViewUtils.loadOrHide(View.GONE, offlineItem.getName(), paymentMethodName);
            ViewUtils.loadOrHide(View.GONE, offlineItem.getDescription(), description);
            ViewUtils.loadOrGone(offlineItem.getIconId(), methodImageView);
            radioButton.setVisibility(View.VISIBLE);
            itemView.setOnClickListener(v -> onItemClicked.onClick());
        }
    }

    /* default */ void setChecked(final boolean checked) {
        radioButton.setChecked(checked);
    }
}