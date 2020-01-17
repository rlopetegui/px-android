package com.mercadopago.android.px.internal.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import com.mercadopago.android.px.R;
import java.util.ArrayList;
import java.util.List;

public class SummaryView extends FrameLayout {

    @NonNull private final ElementDescriptorView bigHeaderDescriptor;
    @NonNull private final AmountDescriptorView totalAmountDescriptor;
    @NonNull private final ElementDescriptorView toolbarElementDescriptor;
    @NonNull private final FrameLayout itemsContainer;
    private final DetailAdapter detailAdapter;

    private final RecyclerView detailRecyclerView;
    @Nullable private OnMeasureListener measureListener;

    private final Animation toolbarAppearAnimation;
    private final Animation toolbarDisappearAnimation;
    private final Animation listAppearAnimation;
    private final Animation logoAppearAnimation;
    private final Animation logoDisappearAnimation;
    private final Animation slideDownIn;

    private boolean showingBigLogo = false;
    private boolean animating = false;
    private int maxElementsToShow;
    private boolean shouldAnimateReturnFromCardForm = false;

    public SummaryView(final Context context) {
        this(context, null);
    }

    public SummaryView(final Context context, @Nullable final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SummaryView(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(getContext(), R.layout.px_view_express_summary, this);
        itemsContainer = findViewById(R.id.items_container);
        bigHeaderDescriptor = findViewById(R.id.bigElementDescriptor);
        bigHeaderDescriptor.setVisibility(INVISIBLE);
        totalAmountDescriptor = findViewById(R.id.total);
        detailRecyclerView = findViewById(R.id.recycler);
        detailRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        detailAdapter = new DetailAdapter();
        detailRecyclerView.setAdapter(detailAdapter);
        toolbarElementDescriptor = findViewById(R.id.element_descriptor_toolbar);

        toolbarAppearAnimation = AnimationUtils.loadAnimation(context, R.anim.px_toolbar_appear);
        toolbarDisappearAnimation = AnimationUtils.loadAnimation(context, R.anim.px_toolbar_disappear);
        listAppearAnimation = AnimationUtils.loadAnimation(context, R.anim.px_summary_list_appear);
        listAppearAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(final Animation animation) {
                detailRecyclerView.setAlpha(1.0f);
                animating = true;
            }

            @Override
            public void onAnimationEnd(final Animation animation) {
                animating = false;
            }

            @Override
            public void onAnimationRepeat(final Animation animation) {

            }
        });
        logoAppearAnimation = AnimationUtils.loadAnimation(context, R.anim.px_summary_logo_appear);
        logoDisappearAnimation = AnimationUtils.loadAnimation(context, R.anim.px_summary_logo_disappear);
        slideDownIn = AnimationUtils.loadAnimation(getContext(), R.anim.px_summary_slide_down_in);
    }

    public void setMaxElementsToShow(final int maxElementsToShow) {
        this.maxElementsToShow = maxElementsToShow;
    }

    public void setOnLogoClickListener(@NonNull final OnClickListener listener) {
        bigHeaderDescriptor.setOnClickListener(listener);
        toolbarElementDescriptor.setOnClickListener(listener);
    }

    public void setMeasureListener(@Nullable final OnMeasureListener measureListener) {
        this.measureListener = measureListener;
    }

    private boolean isViewOverlapping(final View firstView, final View secondView) {
        final int yFirstViewEnd = firstView.getTop() + firstView.getHeight();
        final int ySecondViewInit = secondView.getTop();

        return yFirstViewEnd >= ySecondViewInit;
    }

    public void showToolbarElementDescriptor(@NonNull final ElementDescriptorView.Model elementDescriptorModel) {
        toolbarElementDescriptor.update(elementDescriptorModel);
        toolbarElementDescriptor.setVisibility(VISIBLE);
    }

    public void animateEnter() {
        shouldAnimateReturnFromCardForm = true;
        final int duration = getResources().getInteger(R.integer.cf_anim_duration);

        final View container = findViewById(R.id.container);
        final Animation translateAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.px_summary_translate_in);
        container.startAnimation(translateAnimation);

        final Animation fadeIn = AnimationUtils.loadAnimation(getContext(), R.anim.px_fade_in);
        fadeIn.setStartOffset(duration);
        fadeIn.setDuration(duration);
        findViewById(R.id.separator).startAnimation(fadeIn);

        final View totalLabel = totalAmountDescriptor.findViewById(R.id.title);
        final View amountLabel = totalAmountDescriptor.findViewById(R.id.amount);
        final Animation slideLeft = AnimationUtils.loadAnimation(getContext(), R.anim.px_summary_slide_left_in);
        final Animation slideRight = AnimationUtils.loadAnimation(getContext(), R.anim.px_summary_slide_right_in);
        totalLabel.startAnimation(slideRight);
        amountLabel.startAnimation(slideLeft);
    }

    public void animateExit() {
        final int duration = getResources().getInteger(R.integer.cf_anim_duration);

        final Animation slideUp = AnimationUtils.loadAnimation(getContext(), R.anim.px_summary_slide_up_out);
        final Animation fadeOut = AnimationUtils.loadAnimation(getContext(), R.anim.px_fade_out);
        fadeOut.setDuration(duration);

        if (showingBigLogo) {
            bigHeaderDescriptor.startAnimation(fadeOut);
        } else {
            toolbarElementDescriptor.startAnimation(slideUp);
        }
        detailRecyclerView.startAnimation(fadeOut);
        findViewById(R.id.separator).startAnimation(fadeOut);

        final View totalLabel = totalAmountDescriptor.findViewById(R.id.title);
        final View amountLabel = totalAmountDescriptor.findViewById(R.id.amount);
        totalLabel.startAnimation(fadeOut);
        amountLabel.startAnimation(fadeOut);

        final View container = findViewById(R.id.container);
        final Animation translateAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.px_summary_translate_out);
        container.startAnimation(translateAnimation);
    }

    public void animateElementList(final float positionOffset) {
        if (!animating) {
            detailRecyclerView.setAlpha(1.0f - positionOffset);
        }
    }

    public void configureToolbar(@NonNull final AppCompatActivity activity, @NonNull final View.OnClickListener listener) {
        final Toolbar toolbar = findViewById(R.id.toolbar);
        activity.setSupportActionBar(toolbar);

        final ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        toolbar.setNavigationOnClickListener(listener);
    }

    public void enableToolbarBack(@NonNull final AppCompatActivity activity) {
        final ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
        }
    }

    public void disableToolbarBack(@NonNull final AppCompatActivity activity) {
        final ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(false);
        }
    }

    public void update(@NonNull final Model model) {
        if (model.headerDescriptor != null) {
            bigHeaderDescriptor.update(model.headerDescriptor);
        } else {
            bigHeaderDescriptor.setVisibility(GONE);
        }

        totalAmountDescriptor.setTextSize(R.dimen.px_l_text);
        totalAmountDescriptor.setBold(AmountDescriptorView.Position.RIGHT);
        totalAmountDescriptor.update(model.total);

        detailAdapter.updateItems(model.elements);
        detailRecyclerView.startAnimation(listAppearAnimation);
    }

    @Override
    protected void onLayout(final boolean changed, final int l, final int t, final int r, final int b) {
        super.onLayout(changed, l, t, r, b);
        if (isViewOverlapping(bigHeaderDescriptor, detailRecyclerView)) {
            if (showingBigLogo) {
                showingBigLogo = false;

                if (shouldAnimateReturnFromCardForm) {
                    shouldAnimateReturnFromCardForm = false;
                    toolbarElementDescriptor.startAnimation(slideDownIn);
                } else {
                    toolbarElementDescriptor.startAnimation(toolbarAppearAnimation);
                }
                bigHeaderDescriptor.startAnimation(logoDisappearAnimation);
            }
        } else if (!showingBigLogo) {
            bigHeaderDescriptor.setVisibility(VISIBLE);
            showingBigLogo = true;

            if (shouldAnimateReturnFromCardForm) {
                shouldAnimateReturnFromCardForm = false;
                bigHeaderDescriptor.startAnimation(slideDownIn);
            } else {
                bigHeaderDescriptor.startAnimation(logoAppearAnimation);
            }
            toolbarElementDescriptor.startAnimation(toolbarDisappearAnimation);
        }
        if (measureListener != null) {
            final int availableSummaryHeight = itemsContainer.getMeasuredHeight();
            final float singleItemHeight = AmountDescriptorView.getDesiredHeight(getContext());
            final int expectedItemsHeight = Math.round(singleItemHeight * maxElementsToShow);
            measureListener.onSummaryMeasured(expectedItemsHeight > availableSummaryHeight);
        }
    }

    public interface OnMeasureListener {
        void onSummaryMeasured(boolean itemsClipped);
    }

    public static final class Model {

        /* default */ @NonNull final List<AmountDescriptorView.Model> elements;

        /* default */ @Nullable final ElementDescriptorView.Model headerDescriptor;

        /* default */ @NonNull final AmountDescriptorView.Model total;

        public Model(@Nullable final ElementDescriptorView.Model headerDescriptor,
            @NonNull final List<AmountDescriptorView.Model> elements,
            @NonNull final AmountDescriptorView.Model total) {
            this.elements = elements;
            this.headerDescriptor = headerDescriptor;
            this.total = total;
        }

        public int getElementsSize() {
            return elements.size();
        }
    }

    /* default */ static final class DetailAdapter extends RecyclerView.Adapter<AmountViewHolder> {
        @NonNull private List<AmountDescriptorView.Model> items;

        /* default */ DetailAdapter() {
            items = new ArrayList<>();
        }

        @Override
        public AmountViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
            final View view =
                LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.px_viewholder_amountdescription, parent, false);
            return new AmountViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final AmountViewHolder holder, final int position) {
            holder.populate(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public void updateItems(@NonNull final List<AmountDescriptorView.Model> items) {
            this.items = items;
            notifyDataSetChanged();
        }
    }

    /* default */ static class AmountViewHolder extends RecyclerView.ViewHolder {

        private final AmountDescriptorView amountDescView;

        public AmountViewHolder(final View itemView) {
            super(itemView);
            amountDescView = (AmountDescriptorView) itemView;
        }

        public void populate(@NonNull final AmountDescriptorView.Model model) {
            amountDescView.update(model);
        }
    }
}