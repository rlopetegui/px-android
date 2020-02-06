package com.mercadopago.android.px.internal.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import com.mercadopago.android.px.R;
import com.mercadopago.android.px.core.DynamicDialogCreator;
import com.mercadopago.android.px.internal.font.PxFont;
import com.mercadopago.android.px.internal.util.ViewUtils;
import com.mercadopago.android.px.internal.viewmodel.EmptyLocalized;
import com.mercadopago.android.px.internal.viewmodel.IDetailColor;
import com.mercadopago.android.px.internal.viewmodel.IDetailDrawable;
import com.mercadopago.android.px.internal.viewmodel.ILocalizedCharSequence;
import com.mercadopago.android.px.model.DiscountConfigurationModel;
import com.mercadopago.android.px.model.internal.Text;

import static com.mercadopago.android.px.internal.util.TextUtil.isEmpty;

public class AmountDescriptorView extends ConstraintLayout {

    private MPTextView leftLabel;
    private View leftContainer;
    private TextView rightLabel;
    private ImageView imageView;
    private boolean rightLabelSemiBold;
    private boolean leftLabelSemiBold;

    public static int getDesiredHeight(@NonNull final Context context) {
        final View view = inflate(context, R.layout.px_viewholder_amountdescription, null);
        view.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        return view.getMeasuredHeight();
    }

    public AmountDescriptorView(final Context context) {
        this(context, null);
    }

    public AmountDescriptorView(final Context context, @Nullable final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AmountDescriptorView(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public interface OnClickListener {
        void onDiscountAmountDescriptorClicked(@NonNull final DiscountConfigurationModel discountModel);
        void onChargesAmountDescriptorClicked(@NonNull final DynamicDialogCreator dynamicDialogCreator);
    }

    private void init() {
        inflate(getContext(), R.layout.px_view_amount_descriptor, this);
        leftContainer = findViewById(R.id.title);
        leftLabel = leftContainer.findViewById(R.id.label);
        rightLabel = findViewById(R.id.amount);
        imageView = findViewById(R.id.icon_descriptor_amount);
    }

    public void animateEnter() {
        final Animation slideLeft = AnimationUtils.loadAnimation(getContext(), R.anim.px_summary_slide_left_in);
        final Animation slideRight = AnimationUtils.loadAnimation(getContext(), R.anim.px_summary_slide_right_in);
        leftContainer.startAnimation(slideRight);
        rightLabel.startAnimation(slideLeft);
    }

    public void update(@NonNull final AmountDescriptorView.Model model) {
        updateTextColor(model.detailColor);
        updateLeftLabel(model);
        updateRightLabel(model);
        updateDrawable(model.detailDrawable, model.detailDrawableColor);
        setOnClickListener(model.listener);
    }

    private void updateRightLabel(@NonNull final AmountDescriptorView.Model model) {
        updateLabel(model.right.get(getContext()), rightLabel, rightLabelSemiBold);
    }

    private void updateLeftLabel(@NonNull final AmountDescriptorView.Model model) {
        if (model.leftText != null) {
            updateLabel(leftLabel, model.leftText);
        } else {
            updateLabel(model.left.get(getContext()), leftLabel, leftLabelSemiBold);
        }
    }

    private void updateLabel(@NonNull final CharSequence charSequence, @NonNull final TextView textView,
        final boolean isSemiBold) {
        final SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        spannableStringBuilder.append(charSequence);

        if (isSemiBold) {
            ViewUtils.setFontInSpannable(getContext(), PxFont.SEMI_BOLD, spannableStringBuilder);
        }

        if (isEmpty(charSequence)) {
            textView.setVisibility(GONE);
        }

        textView.setText(spannableStringBuilder);
    }

    private void updateLabel(@NonNull final MPTextView textView, @NonNull final Text text) {
        ViewUtils.loadOrHide(View.GONE, text, textView);
    }

    public void setBold(@NonNull final Position label) {
        if (Position.LEFT == label) {
            leftLabelSemiBold = true;
        } else if (Position.RIGHT == label) {
            rightLabelSemiBold = true;
        }
    }

    public void setTextSize(final int dimen) {
        final int size = (int) getContext().getResources().getDimension(dimen);
        leftLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        rightLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

    public void setTextColor(final int color) {
        leftLabel.setTextColor(color);
        rightLabel.setTextColor(color);
    }

    private void updateTextColor(@NonNull final IDetailColor detailColor) {
        leftLabel.setTextColor(detailColor.getColor(getContext()));
        rightLabel.setTextColor(detailColor.getColor(getContext()));
    }

    private void updateDrawable(@Nullable final IDetailDrawable detailDrawable,
        @Nullable final IDetailColor detailColor) {
        if (detailDrawable != null) {
            imageView.setVisibility(VISIBLE);
            imageView.setImageDrawable(detailDrawable.getDrawable(getContext()));
        } else {
            imageView.setVisibility(INVISIBLE);
        }

        if (detailColor != null) {
            imageView.setColorFilter(detailColor.getColor(getContext()));
        }
    }

    public static class Model {
        /* default */ @NonNull final ILocalizedCharSequence left;
        /* default */ @NonNull final ILocalizedCharSequence right;
        /* default */ @NonNull final IDetailColor detailColor;
        /* default */ @Nullable final Text leftText;
        /* default */ @Nullable IDetailDrawable detailDrawable;
        /* default */ @Nullable IDetailColor detailDrawableColor;
        /* default */ @Nullable View.OnClickListener listener;

        public Model(@NonNull final ILocalizedCharSequence left, @NonNull final ILocalizedCharSequence right,
            @NonNull final IDetailColor detailColor) {
            this.left = left;
            this.right = right;
            this.detailColor = detailColor;
            leftText = null;
        }

        public Model(@NonNull final ILocalizedCharSequence left, @NonNull final IDetailColor detailColor) {
            this.left = left;
            this.detailColor = detailColor;
            right = new EmptyLocalized();
            leftText = null;
        }

        public Model(@NonNull final Text leftText, @NonNull final IDetailColor detailColor) {
            this.leftText = leftText;
            this.detailColor = detailColor;
            left = new EmptyLocalized();
            right = new EmptyLocalized();
        }

        public AmountDescriptorView.Model setDetailDrawable(@Nullable final IDetailDrawable detailDrawable,
            @Nullable final IDetailColor detailDrawableColor) {
            this.detailDrawable = detailDrawable;
            this.detailDrawableColor = detailDrawableColor;
            return this;
        }

        public AmountDescriptorView.Model setListener(@NonNull final View.OnClickListener listener) {
            this.listener = listener;
            return this;
        }
    }

    public enum Position {LEFT, RIGHT}
}