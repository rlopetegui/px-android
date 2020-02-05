package com.mercadopago.android.px.internal.features.express;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.mercadolibre.android.cardform.internal.CardFormWithFragment;
import com.mercadolibre.android.ui.widgets.MeliButton;
import com.mercadolibre.android.ui.widgets.MeliSnackbar;
import com.mercadopago.android.px.R;
import com.mercadopago.android.px.addons.BehaviourProvider;
import com.mercadopago.android.px.addons.model.SecurityValidationData;
import com.mercadopago.android.px.core.DynamicDialogCreator;
import com.mercadopago.android.px.internal.di.Session;
import com.mercadopago.android.px.internal.features.Constants;
import com.mercadopago.android.px.internal.features.SecurityCodeActivity;
import com.mercadopago.android.px.internal.features.cardvault.CardVaultActivity;
import com.mercadopago.android.px.internal.features.checkout.CheckoutActivity;
import com.mercadopago.android.px.internal.features.disable_payment_method.DisabledPaymentMethodDetailDialog;
import com.mercadopago.android.px.internal.features.explode.ExplodeDecorator;
import com.mercadopago.android.px.internal.features.explode.ExplodeParams;
import com.mercadopago.android.px.internal.features.explode.ExplodingFragment;
import com.mercadopago.android.px.internal.features.express.add_new_card.OfflineMethodsFragment;
import com.mercadopago.android.px.internal.features.express.add_new_card.OtherPaymentMethodFragment;
import com.mercadopago.android.px.internal.features.express.animations.ExpandAndCollapseAnimation;
import com.mercadopago.android.px.internal.features.express.animations.FadeAnimationListener;
import com.mercadopago.android.px.internal.features.express.animations.FadeAnimator;
import com.mercadopago.android.px.internal.features.express.installments.InstallmentRowHolder;
import com.mercadopago.android.px.internal.features.express.installments.InstallmentsAdapter;
import com.mercadopago.android.px.internal.features.express.slider.ConfirmButtonAdapter;
import com.mercadopago.android.px.internal.features.express.slider.HubAdapter;
import com.mercadopago.android.px.internal.features.express.slider.PaymentMethodFragment;
import com.mercadopago.android.px.internal.features.express.slider.PaymentMethodFragmentAdapter;
import com.mercadopago.android.px.internal.features.express.slider.PaymentMethodHeaderAdapter;
import com.mercadopago.android.px.internal.features.express.slider.SplitPaymentHeaderAdapter;
import com.mercadopago.android.px.internal.features.express.slider.SummaryViewAdapter;
import com.mercadopago.android.px.internal.features.express.slider.TitlePagerAdapter;
import com.mercadopago.android.px.internal.features.plugins.PaymentProcessorActivity;
import com.mercadopago.android.px.internal.util.ApiUtil;
import com.mercadopago.android.px.internal.util.FragmentUtil;
import com.mercadopago.android.px.internal.util.VibrationUtils;
import com.mercadopago.android.px.internal.util.ViewUtils;
import com.mercadopago.android.px.internal.view.DiscountDetailDialog;
import com.mercadopago.android.px.internal.view.DynamicHeightViewPager;
import com.mercadopago.android.px.internal.view.ElementDescriptorView;
import com.mercadopago.android.px.internal.view.LabeledSwitch;
import com.mercadopago.android.px.internal.view.OnSingleClickListener;
import com.mercadopago.android.px.internal.view.PaymentMethodHeaderView;
import com.mercadopago.android.px.internal.view.ScrollingPagerIndicator;
import com.mercadopago.android.px.internal.view.SummaryView;
import com.mercadopago.android.px.internal.view.TitlePager;
import com.mercadopago.android.px.internal.viewmodel.PayButtonViewModel;
import com.mercadopago.android.px.internal.viewmodel.PostPaymentAction;
import com.mercadopago.android.px.internal.viewmodel.RenderMode;
import com.mercadopago.android.px.internal.viewmodel.SplitSelectionState;
import com.mercadopago.android.px.internal.viewmodel.drawables.DrawableFragmentItem;
import com.mercadopago.android.px.internal.viewmodel.drawables.PaymentMethodDrawableItemMapper;
import com.mercadopago.android.px.model.Card;
import com.mercadopago.android.px.model.Currency;
import com.mercadopago.android.px.model.DiscountConfigurationModel;
import com.mercadopago.android.px.model.IPaymentDescriptor;
import com.mercadopago.android.px.model.OfflinePaymentTypesMetadata;
import com.mercadopago.android.px.model.PayerCost;
import com.mercadopago.android.px.model.PaymentRecovery;
import com.mercadopago.android.px.model.Site;
import com.mercadopago.android.px.model.StatusMetadata;
import com.mercadopago.android.px.model.exceptions.MercadoPagoError;
import com.mercadopago.android.px.model.internal.DisabledPaymentMethod;
import java.util.Arrays;
import java.util.List;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class ExpressPaymentFragment extends Fragment implements ExpressPayment.View, ViewPager.OnPageChangeListener,
    InstallmentsAdapter.ItemListener,
    ExplodingFragment.ExplodingAnimationListener,
    SplitPaymentHeaderAdapter.SplitListener,
    PaymentMethodFragment.DisabledDetailDialogLauncher,
    OtherPaymentMethodFragment.OnOtherPaymentMethodClickListener,
    OfflineMethodsFragment.SheetHidability {

    private static final String TAG_EXPLODING_FRAGMENT = "TAG_EXPLODING_FRAGMENT";
    public static final String TAG_OFFLINE_METHODS_FRAGMENT = "TAG_OFFLINE_METHODS_FRAGMENT";
    private static final String TAG_HEADER_DYNAMIC_DIALOG = "TAG_HEADER_DYNAMIC_DIALOG";
    private static final String EXTRA_RENDER_MODE = "render_mode";
    private static final int REQ_CODE_PAYMENT_PROCESSOR = 101;
    private static final int REQ_CODE_CARD_VAULT = 102;
    private static final int REQ_CODE_SECURITY_CODE = 103;
    private static final int REQ_CODE_BIOMETRICS = 104;
    private static final int REQ_CODE_DISABLE_DIALOG = 105;
    public static final int REQ_CODE_CARD_FORM = 106;
    private static final float PAGER_NEGATIVE_MARGIN_MULTIPLIER = -1.5f;

    @Nullable private CallBack callback;

    /* default */ ExpressPaymentPresenter presenter;

    private SummaryView summaryView;
    private MeliButton confirmButton;
    private RecyclerView installmentsRecyclerView;
    /* default */ DynamicHeightViewPager paymentMethodPager;
    /* default */ View pagerAndConfirmButtonContainer;
    private ScrollingPagerIndicator indicator;
    private ExpandAndCollapseAnimation expandAndCollapseAnimation;
    private FadeAnimator fadeAnimation;
    @Nullable private Animation slideUpAndFadeAnimation;
    @Nullable private Animation slideDownAndFadeAnimation;
    private InstallmentsAdapter installmentsAdapter;
    private TitlePager titlePager;
    private PaymentMethodHeaderView paymentMethodHeaderView;
    private LabeledSwitch splitPaymentView;
    private PaymentMethodFragmentAdapter paymentMethodFragmentAdapter;
    @RenderMode private String renderMode;

    private HubAdapter hubAdapter;
    /* default */ View bottomSheet;
    private BottomSheetBehavior<View> bottomSheetBehavior;

    public static Fragment getInstance() {
        return new ExpressPaymentFragment();
    }

    @Override
    public void onSplitChanged(final boolean isChecked) {
        presenter.onSplitChanged(isChecked);
    }

    @Override
    public void setSheetHidability(final boolean b) {
        bottomSheetBehavior.setHideable(b);
    }

    public interface CallBack {
        void onOneTapCanceled();
    }

    @Nullable
    @Override
    public Animation onCreateAnimation(final int transit, final boolean enter, final int nextAnim) {
        final FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null && fragmentManager.findFragmentByTag(CardFormWithFragment.TAG) != null) {
            final int duration = getResources().getInteger(R.integer.cf_anim_duration);
            final int offset = getResources().getInteger(R.integer.px_card_form_animation_offset);
            if (enter) {
                final Animation slideUp = AnimationUtils.loadAnimation(getContext(), R.anim.px_summary_slide_up_in);
                slideUp.setStartOffset(offset);
                paymentMethodPager
                    .startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.px_summary_slide_up_in));

                final Animation fadeIn = AnimationUtils.loadAnimation(getContext(), R.anim.px_fade_in);
                fadeIn.setDuration(duration);
                fadeIn.setStartOffset(offset);

                paymentMethodHeaderView.startAnimation(fadeIn);
                splitPaymentView.startAnimation(fadeIn);
                indicator.startAnimation(fadeIn);
                confirmButton.startAnimation(slideUp);

                summaryView.animateEnter();
            } else {
                final Animation slideDown =
                    AnimationUtils.loadAnimation(getContext(), R.anim.px_summary_slide_down_out);
                paymentMethodPager.startAnimation(slideDown);

                final Animation fadeOut = AnimationUtils.loadAnimation(getContext(), R.anim.px_fade_out);
                fadeOut.setDuration(duration);

                paymentMethodHeaderView.startAnimation(fadeOut);
                splitPaymentView.startAnimation(fadeOut);
                indicator.startAnimation(fadeOut);
                confirmButton.startAnimation(slideDown);

                summaryView.animateExit();
            }
        }
        return super.onCreateAnimation(transit, enter, nextAnim);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
        @Nullable final ViewGroup container,
        @Nullable final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.px_fragment_express_payment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        configureViews(view);

        presenter = createPresenter();
        if (savedInstanceState != null) {
            renderMode = savedInstanceState.getString(EXTRA_RENDER_MODE);
            presenter.recoverFromBundle(savedInstanceState);
        }
        presenter.attachView(this);
        if (savedInstanceState == null) {
            //TODO if we need the view attached for tracking, then it should be done in another place
            presenter.trackExpressView();
        }

        summaryView.setOnLogoClickListener(v -> presenter.onHeaderClicked());

        confirmButton.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(final View view) {
                if (ApiUtil.checkConnection(getContext())) {
                    presenter.startSecuredPayment();
                } else {
                    presenter.manageNoConnection();
                }
            }
        });

        paymentMethodPager.addOnPageChangeListener(this);
    }

    private void configureViews(@NonNull final View view) {
        splitPaymentView = view.findViewById(R.id.labeledSwitch);
        titlePager = view.findViewById(R.id.title_pager);
        summaryView = view.findViewById(R.id.summary_view);

        pagerAndConfirmButtonContainer = view.findViewById(R.id.container);
        paymentMethodPager = view.findViewById(R.id.payment_method_pager);
        indicator = view.findViewById(R.id.indicator);
        installmentsRecyclerView = view.findViewById(R.id.installments_recycler_view);
        confirmButton = view.findViewById(R.id.confirm_button);
        expandAndCollapseAnimation = new ExpandAndCollapseAnimation(installmentsRecyclerView);
        fadeAnimation = new FadeAnimator(view.getContext());

        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        installmentsRecyclerView.setLayoutManager(linearLayoutManager);
        installmentsRecyclerView
            .addItemDecoration(new DividerItemDecoration(view.getContext(), linearLayoutManager.getOrientation()));

        paymentMethodPager.setPageMargin(
            ((int) (getResources().getDimensionPixelSize(R.dimen.px_m_margin) * PAGER_NEGATIVE_MARGIN_MULTIPLIER)));
        paymentMethodPager.setOffscreenPageLimit(2);
        slideDownAndFadeAnimation.setAnimationListener(new FadeAnimationListener(paymentMethodPager, INVISIBLE));
        slideUpAndFadeAnimation.setAnimationListener(new FadeAnimationListener(paymentMethodPager, VISIBLE));

        paymentMethodHeaderView = view.findViewById(R.id.installments_header);
        paymentMethodHeaderView.setListener(new PaymentMethodHeaderView.Listener() {
            @Override
            public void onDescriptorViewClicked() {
                presenter.onInstallmentsRowPressed();
            }

            @Override
            public void onInstallmentsSelectorCancelClicked() {
                presenter.onInstallmentSelectionCanceled();
            }

            @Override
            public void onDisabledDescriptorViewClick() {
                presenter.onDisabledDescriptorViewClick();
            }
        });

        if (getActivity() instanceof AppCompatActivity) {
            summaryView.configureToolbar((AppCompatActivity) getActivity(), v -> presenter.cancel());
        }

        final TitlePagerAdapter titlePagerAdapter = new TitlePagerAdapter(titlePager);
        titlePager.setAdapter(titlePagerAdapter);

        hubAdapter = new HubAdapter(Arrays.asList(titlePagerAdapter,
            new SummaryViewAdapter(summaryView),
            new SplitPaymentHeaderAdapter(splitPaymentView, this),
            new PaymentMethodHeaderAdapter(paymentMethodHeaderView),
            new ConfirmButtonAdapter(confirmButton)
        ));

        configureBottomSheet();
    }

    private void configureBottomSheet() {
        bottomSheet = getActivity().findViewById(R.id.off_methods_fragment);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull final View view, final int state) {
                final OfflineMethodsFragment fragment = FragmentUtil
                    .getFragmentByTag(getFragmentManager(), TAG_OFFLINE_METHODS_FRAGMENT,
                        OfflineMethodsFragment.class);
                switch (state) {
                case BottomSheetBehavior.STATE_HIDDEN:
                    presenter.trackAbort();
                    getFragmentManager().popBackStackImmediate();
                    break;
                case BottomSheetBehavior.STATE_DRAGGING:
                    fragment.onDragSheet();
                    break;
                case BottomSheetBehavior.STATE_EXPANDED:
                    fragment.onExpandedSheet();
                    break;
                default:
                }

                if (fragment != null) {
                    fragment.onSheetStateChanged(state);
                }
            }

            @Override
            public void onSlide(@NonNull final View view, final float v) {
                // Do not nothing
            }
        });

        getFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentAttached(@NonNull final FragmentManager fm, @NonNull final Fragment fragment,
                @NonNull final Context context) {
                if (fragment instanceof OfflineMethodsFragment) {
                    presenter.onOtherPaymentMethodClickableStateChanged(false);
                }
                super.onFragmentAttached(fm, fragment, context);
            }

            @Override
            public void onFragmentDetached(@NonNull final FragmentManager fm, @NonNull final Fragment fragment) {
                if (fragment instanceof ExpressPaymentFragment) {
                    getFragmentManager().unregisterFragmentLifecycleCallbacks(this);
                } else if (fragment instanceof OfflineMethodsFragment) {
                    bottomSheet.setVisibility(GONE);
                    presenter.onOtherPaymentMethodClickableStateChanged(true);
                }
                super.onFragmentDetached(fm, fragment);
            }
        }, false);
    }

    private ExpressPaymentPresenter createPresenter() {
        final Session session = Session.getInstance();
        return new ExpressPaymentPresenter(session.getPaymentRepository(),
            session.getConfigurationModule().getPaymentSettings(),
            session.getConfigurationModule().getDisabledPaymentMethodRepository(),
            session.getConfigurationModule().getPayerCostSelectionRepository(),
            session.getDiscountRepository(),
            session.getAmountRepository(),
            session.getInitRepository(),
            session.getAmountConfigurationRepository(),
            session.getConfigurationModule().getChargeSolver(),
            session.getMercadoPagoESC(),
            session.getProductIdProvider(),
            new PaymentMethodDrawableItemMapper(getContext(),
                session.getConfigurationModule().getDisabledPaymentMethodRepository(),
                session.getConfigurationModule().getChargeSolver()));
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        outState.putString(EXTRA_RENDER_MODE, renderMode);
        if (presenter != null) {
            super.onSaveInstanceState(presenter.storeInBundle(outState));
        } else {
            super.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //TODO remove null check after session is persisted
        if (presenter != null) {
            presenter.onViewResumed();
        }
    }

    @Override
    public void onPause() {
        if (presenter != null) {
            presenter.onViewPaused();
        }
        super.onPause();
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        slideDownAndFadeAnimation = AnimationUtils.loadAnimation(context, R.anim.px_slide_down_and_fade);
        slideUpAndFadeAnimation = AnimationUtils.loadAnimation(context, R.anim.px_slide_up_and_fade);
        if (context instanceof CallBack) {
            callback = (CallBack) context;
        }
    }

    @Override
    public void onDestroy() {
        FragmentUtil.removeFragment(getChildFragmentManager(), TAG_EXPLODING_FRAGMENT);
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        callback = null;
        slideDownAndFadeAnimation = null;
        slideUpAndFadeAnimation = null;
        //TODO remove null check after session is persisted
        if (presenter != null) {
            presenter.detachView();
        }
        super.onDetach();
    }

    @Override
    public void clearAdapters() {
        paymentMethodPager.setAdapter(null);
    }

    @Override
    public void configureAdapters(@NonNull final Site site, @NonNull final Currency currency) {
        installmentsAdapter = new InstallmentsAdapter(this);
        installmentsRecyclerView.setAdapter(installmentsAdapter);
        installmentsRecyclerView.setVisibility(GONE);

        // Order is important, should update all others adapters before update paymentMethodAdapter

        if (paymentMethodPager.getAdapter() == null) {
            paymentMethodFragmentAdapter = new PaymentMethodFragmentAdapter(getChildFragmentManager());
            if (renderMode == null) {
                summaryView.setMeasureListener((itemsClipped) -> {
                    summaryView.setMeasureListener(null);
                    renderMode = itemsClipped ? RenderMode.LOW_RES : RenderMode.HIGH_RES;
                    onRenderModeDecided();
                });
            } else {
                onRenderModeDecided();
            }
            paymentMethodPager.setAdapter(paymentMethodFragmentAdapter);
            indicator.attachToPager(paymentMethodPager);
        }
    }

    @Override
    public void updateAdapters(@NonNull final HubAdapter.Model model) {
        hubAdapter.update(model);
    }

    @Override
    public void updatePaymentMethods(@NonNull final List<DrawableFragmentItem> items) {
        paymentMethodFragmentAdapter.setItems(items);
    }

    @Override
    public void setPayButtonText(@NonNull final PayButtonViewModel payButtonViewModel) {
        confirmButton.setText(payButtonViewModel.getButtonText(getContext()));
    }

    @Override
    public void cancel() {
        if (callback != null) {
            callback.onOneTapCanceled();
        }
    }

    @Override
    public void showInstallmentsList(final int selectedIndex, @NonNull final List<InstallmentRowHolder.Model> models) {
        animateViewPagerDown();
        installmentsRecyclerView.scrollToPosition(selectedIndex);
        installmentsAdapter.setModels(models);
        installmentsAdapter.setPayerCostSelected(selectedIndex);
        installmentsAdapter.notifyDataSetChanged();
        hubAdapter.showInstallmentsList();
        expandAndCollapseAnimation.expand();
    }

    private void animateViewPagerDown() {
        paymentMethodPager.startAnimation(slideDownAndFadeAnimation);
        fadeAnimation.fadeOutFast(confirmButton);
        fadeAnimation.fadeOutFast(indicator);
    }

    @Override
    public void showToolbarElementDescriptor(@NonNull final ElementDescriptorView.Model elementDescriptorModel) {
        summaryView.showToolbarElementDescriptor(elementDescriptorModel);
    }

    @Override
    public void showDisabledPaymentMethodDetailDialog(@NonNull final DisabledPaymentMethod disabledPaymentMethod,
        @NonNull final StatusMetadata currentStatus) {
        DisabledPaymentMethodDetailDialog
            .showDialog(this, REQ_CODE_DISABLE_DIALOG, disabledPaymentMethod.getPaymentStatusDetail(), currentStatus);
    }

    @Override
    public void collapseInstallmentsSelection() {
        paymentMethodPager.startAnimation(slideUpAndFadeAnimation);
        fadeAnimation.fadeIn(confirmButton);
        fadeAnimation.fadeIn(indicator);
        expandAndCollapseAnimation.collapse();
        paymentMethodFragmentAdapter.notifyDataSetChanged();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == REQ_CODE_CARD_VAULT) {
            handleCardVaultResult(resultCode);
        } else if (requestCode == REQ_CODE_SECURITY_CODE) {
            handleSecurityCodeResult(resultCode);
        } else if (requestCode == REQ_CODE_BIOMETRICS) {
            handleBiometricsResult(resultCode);
        } else if (requestCode == REQ_CODE_DISABLE_DIALOG) {
            resetPagerIndex();
        } else if (requestCode == REQ_CODE_CARD_FORM) {
            handleCardFormResult(resultCode);
        } else if (resultCode == Constants.RESULT_ACTION) {
            handleAction(data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void handleCardFormResult(final int resultCode) {
        if (resultCode == RESULT_OK) {
            presenter.onChangePaymentMethod();
        }
    }

    public void handleSecurityCodeResult(final int resultCode) {
        if (resultCode == RESULT_OK) {
            presenter.onTokenResolved();
        } else {
            cancelLoading();
        }
    }

    private void handleBiometricsResult(final int resultCode) {
        if (resultCode == RESULT_OK) {
            presenter.confirmPayment();
        } else {
            presenter.trackSecurityFriction();
        }
        confirmButton.setState(MeliButton.State.NORMAL);
    }

    private void handleCardVaultResult(final int resultCode) {
        if (resultCode == RESULT_OK) {
            presenter.onTokenResolved();
        } else if (resultCode == RESULT_CANCELED) {
            presenter.trackExpressView();
        }
    }

    private void handleAction(final Intent data) {
        if (data != null && data.getExtras() != null) {
            PostPaymentAction.fromBundle(data.getExtras()).execute(presenter);
        }
    }

    @Override
    public void showPaymentProcessor() {
        PaymentProcessorActivity.start(this, REQ_CODE_PAYMENT_PROCESSOR);
    }

    @Override
    public void showErrorScreen(@NonNull final MercadoPagoError error) {
        if (getActivity() != null) {
            ((CheckoutActivity) getActivity()).presenter.onPaymentError(error);
        }
    }

    @SuppressLint("Range")
    @Override
    public void showErrorSnackBar(@NonNull final MercadoPagoError error) {
        if (getView() != null && getActivity() != null) {
            MeliSnackbar.make(getView(), error.getMessage(), Snackbar.LENGTH_LONG, MeliSnackbar.SnackbarType.ERROR)
                .show();
        }
    }

    @Override
    public void updateViewForPosition(final int paymentMethodIndex,
        final int payerCostSelected,
        @NonNull final SplitSelectionState splitSelectionState) {
        hubAdapter.updateData(paymentMethodIndex, payerCostSelected, splitSelectionState);
    }

    @Override
    public void showPaymentResult(@NonNull final IPaymentDescriptor paymentResult) {
        //TODO refactor, this should be handled here and not in CheckoutActivity
        if (getActivity() != null) {
            ((CheckoutActivity) getActivity()).presenter.onPaymentFinished(paymentResult);
        }
    }

    @Override
    public void startSecurityValidation(@NonNull final SecurityValidationData data) {
        confirmButton.setState(MeliButton.State.DISABLED);
        BehaviourProvider.getSecurityBehaviour().startValidation(this, data, REQ_CODE_BIOMETRICS);
    }

    //FIXME Used to start payment from activity
    @Override
    public void startPayment() {
        presenter.confirmPayment();
    }

    @Override
    public void finishLoading(@NonNull final ExplodeDecorator params) {
        final ExplodingFragment fragment =
            FragmentUtil.getFragmentByTag(getChildFragmentManager(), TAG_EXPLODING_FRAGMENT, ExplodingFragment.class);
        if (fragment != null) {
            fragment.finishLoading(params);
        } else {
            presenter.hasFinishPaymentAnimation();
        }
    }

    @Override
    public void cancelLoading() {
        showConfirmButton();
        final FragmentManager childFragmentManager = getChildFragmentManager();
        final ExplodingFragment fragment =
            (ExplodingFragment) childFragmentManager.findFragmentByTag(TAG_EXPLODING_FRAGMENT);
        if (fragment != null && fragment.isAdded() && fragment.hasFinished()) {
            childFragmentManager
                .beginTransaction()
                .remove(fragment)
                .commitNowAllowingStateLoss();
            restoreStatusBar();
        }
    }

    private void restoreStatusBar() {
        final Activity activity = getActivity();

        if (activity != null) {
            ViewUtils.setStatusBarColor(ContextCompat.getColor(activity, R.color.px_colorPrimaryDark),
                activity.getWindow());
        }
    }

    private void showConfirmButton() {
        confirmButton.clearAnimation();
        confirmButton.setVisibility(VISIBLE);
    }

    private void hideConfirmButton() {
        confirmButton.clearAnimation();
        confirmButton.setVisibility(INVISIBLE);
    }

    @Override
    public void enableToolbarBack() {
        if (getActivity() instanceof AppCompatActivity) {
            summaryView.enableToolbarBack((AppCompatActivity) getActivity());
        }
    }

    @Override
    public void disableToolbarBack() {
        if (getActivity() instanceof AppCompatActivity) {
            summaryView.disableToolbarBack((AppCompatActivity) getActivity());
        }
    }

    @Override
    public void startLoadingButton(final int paymentTimeout, @NonNull final PayButtonViewModel payButtonViewModel) {
        hideConfirmButton();
        ViewUtils.runWhenViewIsFullyMeasured(getView(), () -> {
            final ExplodeParams explodeParams = ExplodingFragment.getParams(confirmButton,
                payButtonViewModel.getButtonProgressText(confirmButton.getContext()), paymentTimeout);
            final FragmentManager childFragmentManager = getChildFragmentManager();
            final ExplodingFragment explodingFragment = ExplodingFragment.newInstance(explodeParams);
            childFragmentManager.beginTransaction()
                .replace(R.id.exploding_frame, explodingFragment, TAG_EXPLODING_FRAGMENT)
                .commitNowAllowingStateLoss();
        });
    }

    @Override
    public void showSecurityCodeScreen(@NonNull final Card card) {
        SecurityCodeActivity.startForSavedCard(card, this, REQ_CODE_SECURITY_CODE);
    }

    @Override
    public void showCardFlow(@NonNull final PaymentRecovery paymentRecovery) {
        CardVaultActivity.startActivityForRecovery(this, REQ_CODE_CARD_VAULT, paymentRecovery);
    }

    @Override
    public void onClick(final PayerCost payerCostSelected) {
        presenter.onPayerCostSelected(payerCostSelected);
    }

    @Override
    public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
        hubAdapter.updatePosition(positionOffset, position);
    }

    @Override
    public void onPageSelected(final int position) {
        presenter.onSliderOptionSelected(position);
        VibrationUtils.smallVibration(getContext());
    }

    @Override
    public void onPageScrollStateChanged(final int state) {
        // do nothing.
    }

    @Override
    public void showDiscountDetailDialog(@NonNull final Currency currency,
        @NonNull final DiscountConfigurationModel discountModel) {
        DiscountDetailDialog.showDialog(getFragmentManager(), currency, discountModel);
    }

    @Override
    public void onAnimationFinished() {
        presenter.hasFinishPaymentAnimation();
    }

    @Override
    public boolean isExploding() {
        return FragmentUtil.isFragmentVisible(getChildFragmentManager(), TAG_EXPLODING_FRAGMENT);
    }

    @Override
    public void resetPagerIndex() {
        paymentMethodPager.setCurrentItem(0);
    }

    @Override
    public void showDynamicDialog(@NonNull final DynamicDialogCreator creator,
        @NonNull final DynamicDialogCreator.CheckoutData checkoutData) {
        if (creator.shouldShowDialog(getContext(), checkoutData)) {
            creator.create(getContext(), checkoutData).show(getChildFragmentManager(),
                TAG_HEADER_DYNAMIC_DIALOG);
        }
    }

    private void onRenderModeDecided() {
        //Workaround to pager not updating the fragments
        paymentMethodPager.post(() -> paymentMethodFragmentAdapter.setRenderMode(renderMode));
    }

    @Override
    public int getRequestCode() {
        return REQ_CODE_DISABLE_DIALOG;
    }

    @Override
    public void onOtherPaymentMethodClicked(@NonNull final OfflinePaymentTypesMetadata offlineMethods) {
        presenter.onOtherPaymentMethodClicked(offlineMethods);
    }

    @Override
    public void showOfflineMethods(@NonNull final OfflinePaymentTypesMetadata offlineMethods) {
        bottomSheet.setVisibility(VISIBLE);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        if (FragmentUtil.getFragmentByTag(getFragmentManager(), TAG_OFFLINE_METHODS_FRAGMENT) == null) {
            OfflineMethodsFragment instance = OfflineMethodsFragment.getInstance(offlineMethods);
            getFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.px_slide_up, 0, 0, R.animator.px_slide_down)
                .replace(R.id.off_methods_fragment, instance,
                    TAG_OFFLINE_METHODS_FRAGMENT)
                .addToBackStack(TAG_OFFLINE_METHODS_FRAGMENT)
                .commit();
            instance.setTargetFragment(this, 1);
        } else {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    @Override
    public void updateBottomSheetStatus(final boolean hasToExpand) {
        if (hasToExpand) {
            bottomSheet.setVisibility(VISIBLE);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }
}