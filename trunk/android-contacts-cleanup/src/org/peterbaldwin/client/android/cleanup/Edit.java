package org.peterbaldwin.client.android.cleanup;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A class describing a field edit.
 */
public class Edit implements Parcelable {
	public long mPhoneId;
	public String mOriginalValue;
	public String mNewValue;
	public String mDisplayName;

	public static final Parcelable.Creator<Edit> CREATOR = new Parcelable.Creator<Edit>() {
		public Edit createFromParcel(Parcel in) {
			return new Edit(in);
		}

		public Edit[] newArray(int size) {
			return new Edit[size];
		}
	};

	public Edit() {
	}

	private Edit(Parcel in) {
		mPhoneId = in.readLong();
		mOriginalValue = in.readString();
		mNewValue = in.readString();
		mDisplayName = in.readString();
	}

	@Override
	public String toString() {
		return mOriginalValue;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(mPhoneId);
		dest.writeString(mOriginalValue);
		dest.writeString(mNewValue);
		dest.writeString(mDisplayName);
	}
}
