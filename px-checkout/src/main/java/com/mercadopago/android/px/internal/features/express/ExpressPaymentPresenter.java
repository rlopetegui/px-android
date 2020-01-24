package com.mercadopago.android.px.internal.features.express;

import android.os.Bundle;
import android.support.annotation.NonNull;
import com.mercadopago.android.px.addons.ESCManagerBehaviour;
import com.mercadopago.android.px.addons.model.SecurityValidationData;
import com.mercadopago.android.px.configuration.DynamicDialogConfiguration;
import com.mercadopago.android.px.core.DynamicDialogCreator;
import com.mercadopago.android.px.internal.base.BasePresenter;
import com.mercadopago.android.px.internal.core.ProductIdProvider;
import com.mercadopago.android.px.internal.features.explode.ExplodeDecoratorMapper;
import com.mercadopago.android.px.internal.features.express.installments.InstallmentRowHolder;
import com.mercadopago.android.px.internal.features.express.slider.HubAdapter;
import com.mercadopago.android.px.internal.features.express.slider.SplitPaymentHeaderAdapter;
import com.mercadopago.android.px.internal.repository.AmountConfigurationRepository;
import com.mercadopago.android.px.internal.repository.AmountRepository;
import com.mercadopago.android.px.internal.repository.ChargeRepository;
import com.mercadopago.android.px.internal.repository.DisabledPaymentMethodRepository;
import com.mercadopago.android.px.internal.repository.DiscountRepository;
import com.mercadopago.android.px.internal.repository.InitRepository;
import com.mercadopago.android.px.internal.repository.PayerCostSelectionRepository;
import com.mercadopago.android.px.internal.repository.PaymentRepository;
import com.mercadopago.android.px.internal.repository.PaymentSettingRepository;
import com.mercadopago.android.px.internal.util.ApiUtil;
import com.mercadopago.android.px.internal.util.SecurityValidationDataFactory;
import com.mercadopago.android.px.internal.view.AmountDescriptorView;
import com.mercadopago.android.px.internal.view.ElementDescriptorView;
import com.mercadopago.android.px.internal.view.PaymentMethodDescriptorView;
import com.mercadopago.android.px.internal.view.SummaryView;
import com.mercadopago.android.px.internal.viewmodel.ConfirmButtonViewModel;
import com.mercadopago.android.px.internal.viewmodel.PayButtonViewModel;
import com.mercadopago.android.px.internal.viewmodel.PostPaymentAction;
import com.mercadopago.android.px.internal.viewmodel.SplitSelectionState;
import com.mercadopago.android.px.internal.viewmodel.drawables.PaymentMethodDrawableItemMapper;
import com.mercadopago.android.px.internal.viewmodel.mappers.ConfirmButtonViewModelMapper;
import com.mercadopago.android.px.internal.viewmodel.mappers.ElementDescriptorMapper;
import com.mercadopago.android.px.internal.viewmodel.mappers.InstallmentViewModelMapper;
import com.mercadopago.android.px.internal.viewmodel.mappers.PayButtonViewModelMapper;
import com.mercadopago.android.px.internal.viewmodel.mappers.PaymentMethodDescriptorMapper;
import com.mercadopago.android.px.internal.viewmodel.mappers.SplitHeaderMapper;
import com.mercadopago.android.px.internal.viewmodel.mappers.SummaryInfoMapper;
import com.mercadopago.android.px.internal.viewmodel.mappers.SummaryViewModelMapper;
import com.mercadopago.android.px.model.AmountConfiguration;
import com.mercadopago.android.px.model.Card;
import com.mercadopago.android.px.model.DiscountConfigurationModel;
import com.mercadopago.android.px.model.ExpressMetadata;
import com.mercadopago.android.px.model.IPaymentDescriptor;
import com.mercadopago.android.px.model.OfflinePaymentTypesMetadata;
import com.mercadopago.android.px.model.PayerCost;
import com.mercadopago.android.px.model.PaymentData;
import com.mercadopago.android.px.model.PaymentRecovery;
import com.mercadopago.android.px.model.exceptions.ApiException;
import com.mercadopago.android.px.model.exceptions.MercadoPagoError;
import com.mercadopago.android.px.model.exceptions.NoConnectivityException;
import com.mercadopago.android.px.model.internal.InitResponse;
import com.mercadopago.android.px.model.internal.SummaryInfo;
import com.mercadopago.android.px.preferences.CheckoutPreference;
import com.mercadopago.android.px.services.Callback;
import com.mercadopago.android.px.tracking.internal.events.ConfirmEvent;
import com.mercadopago.android.px.tracking.internal.events.FrictionEventTracker;
import com.mercadopago.android.px.tracking.internal.events.InstallmentsEventTrack;
import com.mercadopago.android.px.tracking.internal.events.SwipeOneTapEventTracker;
import com.mercadopago.android.px.tracking.internal.views.OneTapViewTracker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/* default */ class ExpressPaymentPresenter extends BasePresenter<ExpressPayment.View>
    implements PostPaymentAction.ActionController, ExpressPayment.Actions,
    AmountDescriptorView.OnClickListener {

    private static final String BUNDLE_STATE_SPLIT_PREF = "state_split_pref";
    private static final String BUNDLE_STATE_CURRENT_PM_INDEX = "state_current_pm_index";
    private static final String BUNDLE_STATE_OTHER_PM_CLICKABLE = "state_other_pm_clickable";

    @NonNull private final PaymentRepository paymentRepository;
    @NonNull private final AmountRepository amountRepository;
    @NonNull private final DiscountRepository discountRepository;
    @NonNull private final PaymentSettingRepository paymentSettingRepository;
    @NonNull private final AmountConfigurationRepository amountConfigurationRepository;
    @NonNull private final DisabledPaymentMethodRepository disabledPaymentMethodRepository;
    @NonNull private final ChargeRepository chargeRepository;
    @NonNull private final ProductIdProvider productIdProvider;
    @NonNull private final ExplodeDecoratorMapper explodeDecoratorMapper;
    @NonNull private final ESCManagerBehaviour escManagerBehaviour;
    @NonNull /* default */ final InitRepository initRepository;
    private final PayerCostSelectionRepository payerCostSelectionRepository;
    private final PaymentMethodDrawableItemMapper paymentMethodDrawableItemMapper;
    private final PayButtonViewModel payButtonViewModel;
    /* default */ List<ExpressMetadata> expressMetadataList; //FIXME remove.
    /* default */ int paymentMethodIndex;
    private SplitSelectionState splitSelectionState;
    private Set<String> cardsWithSplit;
    private boolean otherPaymentMethodClickable = true;
    @Nullable private Runnable unattendedEvent;

    /* default */ ExpressPaymentPresenter(@NonNull final PaymentRepository paymentRepository,
        @NonNull final PaymentSettingRepository paymentSettingRepository,
        @NonNull final DisabledPaymentMethodRepository disabledPaymentMethodRepository,
        @NonNull final PayerCostSelectionRepository payerCostSelectionRepository,
        @NonNull final DiscountRepository discountRepository,
        @NonNull final AmountRepository amountRepository,
        @NonNull final InitRepository initRepository,
        @NonNull final AmountConfigurationRepository amountConfigurationRepository,
        @NonNull final ChargeRepository chargeRepository,
        @NonNull final ESCManagerBehaviour escManagerBehaviour,
        @NonNull final ProductIdProvider productIdProvider,
        @NonNull final PaymentMethodDrawableItemMapper paymentMethodDrawableItemMapper) {

        this.paymentRepository = paymentRepository;
        this.paymentSettingRepository = paymentSettingRepository;
        this.disabledPaymentMethodRepository = disabledPaymentMethodRepository;
        this.payerCostSelectionRepository = payerCostSelectionRepository;
        this.discountRepository = discountRepository;
        this.amountRepository = amountRepository;
        this.initRepository = initRepository;
        this.amountConfigurationRepository = amountConfigurationRepository;
        this.chargeRepository = chargeRepository;
        this.escManagerBehaviour = escManagerBehaviour;
        this.productIdProvider = productIdProvider;
        this.paymentMethodDrawableItemMapper = paymentMethodDrawableItemMapper;

        explodeDecoratorMapper = new ExplodeDecoratorMapper();
        splitSelectionState = new SplitSelectionState();
        payButtonViewModel = new PayButtonViewModelMapper().map(
            paymentSettingRepository.getAdvancedConfiguration().getCustomStringConfiguration());
    }

    /* default */ void onFailToRetrieveInitResponse() {
        throw new IllegalStateException("groups missing rendering one tap");
    }

    @Override
    public void loadViewModel() {
        final SummaryInfo summaryInfo = new SummaryInfoMapper().map(paymentSettingRepository.getCheckoutPreference());

        final ElementDescriptorView.Model elementDescriptorModel =
            new ElementDescriptorMapper().map(summaryInfo);

        final List<SummaryView.Model> summaryModels =
            new SummaryViewModelMapper(paymentSettingRepository.getCurrency(),
                discountRepository, amountRepository, elementDescriptorModel, this, summaryInfo,
                chargeRepository).map(new ArrayList<>(expressMetadataList));

        final List<PaymentMethodDescriptorView.Model> paymentModels =
            new PaymentMethodDescriptorMapper(paymentSettingRepository.getCurrency(),
                amountConfigurationRepository, disabledPaymentMethodRepository).map(expressMetadataList);

        final List<SplitPaymentHeaderAdapter.Model> splitHeaderModels =
            new SplitHeaderMapper(paymentSettingRepository.getCurrency(), amountConfigurationRepository)
                .map(expressMetadataList);

        final List<ConfirmButtonViewModel> confirmButtonViewModels =
            new ConfirmButtonViewModelMapper(disabledPaymentMethodRepository).map(expressMetadataList);

        final HubAdapter.Model model =
            new HubAdapter.Model(paymentModels, summaryModels, splitHeaderModels, confirmButtonViewModels);

        getView().showToolbarElementDescriptor(elementDescriptorModel);

        getView().updateAdapters(model);
        updateElements();
        getView().configureAdapters(paymentSettingRepository.getSite(), paymentSettingRepository.getCurrency());
        getView().updatePaymentMethods(paymentMethodDrawableItemMapper.map(expressMetadataList));
        getView().updateBottomSheetStatus(!otherPaymentMethodClickable);
        getView().setPayButtonText(payButtonViewModel);
    }

    @Override
    public void onViewResumed() {
        paymentRepository.attach(this);
    }

    @Override
    public void attachView(final ExpressPayment.View view) {
        super.attachView(view);
        initPresenter();
    }

    private void initPresenter() {
        initRepository.init().execute(new Callback<InitResponse>() {
            @Override
            public void success(final InitResponse initResponse) {
                expressMetadataList = initResponse.getExpress();
                cardsWithSplit = initResponse.getIdsWithSplitAllowed();
                loadViewModel();
            }

            @Override
            public void failure(final ApiException apiException) {
                onFailToRetrieveInitResponse();
            }
        });
    }

    @Override
    public void detachView() {
        onViewPaused();
        super.detachView();
    }

    @Override
    public void recoverFromBundle(@NonNull final Bundle bundle) {
        splitSelectionState = bundle.getParcelable(BUNDLE_STATE_SPLIT_PREF);
        paymentMethodIndex = bundle.getInt(BUNDLE_STATE_CURRENT_PM_INDEX);
        otherPaymentMethodClickable = bundle.getBoolean(BUNDLE_STATE_OTHER_PM_CLICKABLE);
    }

    @NonNull
    @Override
    public Bundle storeInBundle(@NonNull final Bundle bundle) {
        bundle.putParcelable(BUNDLE_STATE_SPLIT_PREF, splitSelectionState);
        bundle.putInt(BUNDLE_STATE_CURRENT_PM_INDEX, paymentMethodIndex);
        bundle.putBoolean(BUNDLE_STATE_OTHER_PM_CLICKABLE, otherPaymentMethodClickable);
        return bundle;
    }

    @Override
    public void trackExpressView() {
        final OneTapViewTracker oneTapViewTracker =
            new OneTapViewTracker(expressMetadataList, paymentSettingRepository.getCheckoutPreference(),
                discountRepository.getCurrentConfiguration(), escManagerBehaviour.getESCCardIds(), cardsWithSplit,
                disabledPaymentMethodRepository.getDisabledPaymentMethods().size());
        setCurrentViewTracker(oneTapViewTracker);
    }

    @Override
    public void startSecuredPayment() {
        final SecurityValidationData data = SecurityValidationDataFactory
            .create(productIdProvider, paymentSettingRepository, getCurrentExpressMetadata());
        getView().startSecurityValidation(data);
    }

    @Override
    public void confirmPayment() {
        refreshExplodingState();

        // TODO improve: This was added because onetap can detach this listener on its onDestroy
        paymentRepository.attach(this);

        final ExpressMetadata expressMetadata = getCurrentExpressMetadata();

        PayerCost payerCost = null;

        final String customOptionId = expressMetadata.getCustomOptionId();
        final AmountConfiguration amountConfiguration =
            amountConfigurationRepository.getConfigurationFor(customOptionId);

        if (expressMetadata.isCard() || expressMetadata.isConsumerCredits()) {
            payerCost = amountConfiguration.getCurrentPayerCost(splitSelectionState.userWantsToSplit(),
                payerCostSelectionRepository.get(customOptionId));
        }

        final boolean splitPayment = splitSelectionState.userWantsToSplit() && amountConfiguration.allowSplit();
        ConfirmEvent
            .from(escManagerBehaviour.getESCCardIds(), expressMetadata, payerCost, splitPayment, paymentMethodIndex)
            .track();

        paymentRepository.startExpressPayment(expressMetadata, payerCost, splitPayment);
    }

    private ExpressMetadata getCurrentExpressMetadata() {
        return expressMetadataList.get(paymentMethodIndex);
    }

    @Override
    public void trackSecurityFriction() {
        // TODO Review ID
        FrictionEventTracker
            .with(OneTapViewTracker.PATH_REVIEW_ONE_TAP_VIEW, FrictionEventTracker.Id.GENERIC,
                FrictionEventTracker.Style.CUSTOM_COMPONENT).track();
    }

    private void refreshExplodingState() {
        if (paymentRepository.isExplodingAnimationCompatible()) {
            getView().startLoadingButton(paymentRepository.getPaymentTimeout(), payButtonViewModel);
            getView().disableToolbarBack();
        }
    }

    @Override
    public void cancel() {
        trackAbort();
        getView().cancel();
    }

    public void trackAbort(){
        tracker.trackAbort();
    }

    @Override
    public void onTokenResolved() {
        cancelLoading();
        confirmPayment();
    }

    /**
     * When there is no visual interaction needed this callback is called.
     *
     * @param payment plugin payment.
     */
    @Override
    public void onPaymentFinished(@NonNull final IPaymentDescriptor payment) {
        getView().finishLoading(explodeDecoratorMapper.map(payment));
    }

    @Override
    public void onPaymentError(@NonNull final MercadoPagoError error) {
        cancelLoading();
        if (error.isInternalServerError() || error.isNoConnectivityError()) {
            FrictionEventTracker.with(OneTapViewTracker.PATH_REVIEW_ONE_TAP_VIEW,
                FrictionEventTracker.Id.GENERIC, FrictionEventTracker.Style.CUSTOM_COMPONENT,
                error)
                .track();
            getView().showErrorSnackBar(error);
        } else {
            getView().showErrorScreen(error);
        }
    }

    @Override
    public void onVisualPayment() {
        getView().showPaymentProcessor();
    }

    @Override
    public void onCvvRequired(@NonNull final Card card) {
        getView().showSecurityCodeScreen(card);
    }

    @Override
    public void onRecoverPaymentEscInvalid(final PaymentRecovery recovery) {
        cancelLoading();
        getView().showCardFlow(recovery);
    }

    private void updateElementPosition(final int selectedPayerCost) {
        payerCostSelectionRepository.save(getCurrentExpressMetadata().getCustomOptionId(), selectedPayerCost);
        updateElements();
    }

    @Override
    public void onViewPaused() {
        paymentRepository.detach(this);
    }

    @Override
    public void onInstallmentsRowPressed() {
        final ExpressMetadata expressMetadata = getCurrentExpressMetadata();
        final String customOptionId = expressMetadata.getCustomOptionId();
        final AmountConfiguration amountConfiguration =
            amountConfigurationRepository.getConfigurationFor(customOptionId);
        final List<PayerCost> payerCostList =
            amountConfiguration.getAppliedPayerCost(splitSelectionState.userWantsToSplit());
        final int selectedIndex = amountConfiguration.getCurrentPayerCostIndex(splitSelectionState.userWantsToSplit(),
            payerCostSelectionRepository.get(customOptionId));
        final List<InstallmentRowHolder.Model> models =
            new InstallmentViewModelMapper(paymentSettingRepository.getCurrency(), expressMetadata.getBenefits())
                .map(payerCostList);
        getView().showInstallmentsList(selectedIndex, models);
        new InstallmentsEventTrack(expressMetadata, amountConfiguration).track();
    }

    /**
     * When user cancel the payer cost selection this method will be called with the current payment method position
     */
    @Override
    public void onInstallmentSelectionCanceled() {
        updateElements();
        getView().collapseInstallmentsSelection();
    }

    /**
     * When user selects a new payment method this method will be called with the new current paymentMethodIndex.
     *
     * @param paymentMethodIndex current payment method paymentMethodIndex.
     */
    @Override
    public void onSliderOptionSelected(final int paymentMethodIndex) {
        this.paymentMethodIndex = paymentMethodIndex;
        new SwipeOneTapEventTracker().track();
        updateElementPosition(payerCostSelectionRepository.get(getCurrentExpressMetadata().getCustomOptionId()));
    }

    private void updateElements() {
        getView().updateViewForPosition(paymentMethodIndex,
            payerCostSelectionRepository.get(getCurrentExpressMetadata().getCustomOptionId()), splitSelectionState);
    }

    /**
     * When user selects a new payer cost for certain payment method this method will be called.
     *
     * @param payerCostSelected user selected payerCost.
     */
    @Override
    public void onPayerCostSelected(final PayerCost payerCostSelected) {
        final String customOptionId = getCurrentExpressMetadata().getCustomOptionId();
        final int selected = amountConfigurationRepository.getConfigurationFor(customOptionId)
            .getAppliedPayerCost(splitSelectionState.userWantsToSplit())
            .indexOf(payerCostSelected);
        updateElementPosition(selected);
        getView().collapseInstallmentsSelection();
    }

    public void onDisabledDescriptorViewClick() {
        getView().showDisabledPaymentMethodDetailDialog(
            disabledPaymentMethodRepository.getDisabledPaymentMethod(getCurrentExpressMetadata().getCustomOptionId()),
            getCurrentExpressMetadata().getStatus());
    }

    @Override
    public void hasFinishPaymentAnimation() {
        final IPaymentDescriptor payment = paymentRepository.getPayment();
        if (payment != null) {
            getView().showPaymentResult(payment);
        }
    }

    private void cancelLoading() {
        getView().enableToolbarBack();
        getView().cancelLoading();
    }

    @Override
    public void manageNoConnection() {
        final NoConnectivityException exception = new NoConnectivityException();
        final ApiException apiException = ApiUtil.getApiException(exception);
        final MercadoPagoError mercadoPagoError = new MercadoPagoError(apiException, null);
        FrictionEventTracker.with(OneTapViewTracker.PATH_REVIEW_ONE_TAP_VIEW,
            FrictionEventTracker.Id.GENERIC, FrictionEventTracker.Style.CUSTOM_COMPONENT,
            mercadoPagoError)
            .track();
        getView().showErrorSnackBar(mercadoPagoError);
    }

    @Override
    public void onDiscountAmountDescriptorClicked(@NonNull final DiscountConfigurationModel discountModel) {
        getView().showDiscountDetailDialog(paymentSettingRepository.getCurrency(), discountModel);
    }

    @Override
    public void onChargesAmountDescriptorClicked(@NonNull final DynamicDialogCreator dynamicDialogCreator) {
        final DynamicDialogCreator.CheckoutData checkoutData = new DynamicDialogCreator.CheckoutData(
            paymentSettingRepository.getCheckoutPreference(), Collections.singletonList(new PaymentData()));
        getView().showDynamicDialog(dynamicDialogCreator, checkoutData);
    }

    @Override
    public void onSplitChanged(final boolean isChecked) {
        if (splitSelectionState.userWantsToSplit() != isChecked) {
            resetPayerCostSelection();
        }
        splitSelectionState.setUserWantsToSplit(isChecked);
        // cancel also update the position.
        // it is used because the installment selection can be expanded by the user.
        onInstallmentSelectionCanceled();
    }

    @Override
    public void onHeaderClicked() {
        final CheckoutPreference checkoutPreference = paymentSettingRepository.getCheckoutPreference();
        final DynamicDialogConfiguration dynamicDialogConfiguration =
            paymentSettingRepository.getAdvancedConfiguration().getDynamicDialogConfiguration();

        final DynamicDialogCreator.CheckoutData checkoutData =
            new DynamicDialogCreator.CheckoutData(checkoutPreference, Collections.singletonList(new PaymentData()));

        if (dynamicDialogConfiguration.hasCreatorFor(DynamicDialogConfiguration.DialogLocation.TAP_ONE_TAP_HEADER)) {
            getView().showDynamicDialog(
                dynamicDialogConfiguration.getCreatorFor(DynamicDialogConfiguration.DialogLocation.TAP_ONE_TAP_HEADER),
                checkoutData);
        }
    }

    /* default */ void resetPayerCostSelection() {
        payerCostSelectionRepository.reset();
    }

    @Override
    public void onChangePaymentMethod() {
        cancelLoading();
        postDisableModelUpdate();
    }

    private void postDisableModelUpdate() {
        initRepository.refresh().enqueue(new Callback<InitResponse>() {
            @Override
            public void success(final InitResponse initResponse) {
                expressMetadataList = initResponse.getExpress();
                resetPayerCostSelection();
                paymentMethodIndex = 0;
                getView().clearAdapters();
                loadViewModel();
            }

            @Override
            public void failure(final ApiException apiException) {
                onFailToRetrieveInitResponse();
            }
        });
    }

    @Override
    public void recoverPayment(@NonNull final PostPaymentAction postPaymentAction) {
        cancelLoading();
        getView().showCardFlow(paymentRepository.createPaymentRecovery());
    }

    @Override
    public void onOtherPaymentMethodClicked(@NonNull final OfflinePaymentTypesMetadata offlineMethods) {
        final Runnable event = () -> getView().showOfflineMethods(offlineMethods);
        if (otherPaymentMethodClickable) {
            event.run();
        } else {
            unattendedEvent = event;
        }
    }

    @Override
    public void onOtherPaymentMethodClickableStateChanged(final boolean state) {
        otherPaymentMethodClickable = state;
        if (otherPaymentMethodClickable) {
            executeUnattendedEvent();
        }
    }

    private void executeUnattendedEvent() {
        if (unattendedEvent != null) {
            unattendedEvent.run();
            unattendedEvent = null;
        }
    }
}