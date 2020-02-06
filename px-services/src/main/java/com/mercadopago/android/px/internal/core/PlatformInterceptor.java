package com.mercadopago.android.px.internal.core;

import android.content.Context;
import android.support.annotation.NonNull;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public final class PlatformInterceptor implements Interceptor {

    private static final String HEADER_PLATFORM = "x-platform";
    private static final String PLATFORM_MP = "MP";
    private static final String PLATFORM_ML = "ML";
    private Context context;

    public PlatformInterceptor(@NonNull final Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public Response intercept(final Chain chain) throws IOException {
        final Request originalRequest = chain.request();
        final Request request = originalRequest.newBuilder()
            .header(HEADER_PLATFORM, getPlatform(context))
            .build();
        return chain.proceed(request);
    }


    private String getPlatform(final Context context) {
        final String packageName = context.getApplicationInfo().packageName;
        return packageName.contains("com.mercadolibre") ? PLATFORM_ML : PLATFORM_MP;
    }
}
