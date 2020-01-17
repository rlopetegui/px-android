package com.mercadopago.android.px.model;

import android.os.Parcel;
import android.os.Parcelable;
import com.mercadopago.android.px.model.internal.Text;
import java.io.Serializable;

public final class NewCardMetadata implements Parcelable, Serializable {

    private final Text label;
    private final Text description;
    private final String version;

    public static final Creator<NewCardMetadata> CREATOR = new Creator<NewCardMetadata>() {
        @Override
        public NewCardMetadata createFromParcel(final Parcel in) {
            return new NewCardMetadata(in);
        }

        @Override
        public NewCardMetadata[] newArray(final int size) {
            return new NewCardMetadata[size];
        }
    };

    public Text getLabel() {
        return label;
    }

    public Text getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }

    protected NewCardMetadata(final Parcel in) {
        label = in.readParcelable(Text.class.getClassLoader());
        description = in.readParcelable(Text.class.getClassLoader());
        version = in.readString();
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(label, flags);
        dest.writeParcelable(description, flags);
        dest.writeString(version);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}