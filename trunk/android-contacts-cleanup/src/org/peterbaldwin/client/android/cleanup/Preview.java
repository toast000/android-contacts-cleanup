package org.peterbaldwin.client.android.cleanup;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.BaseColumns;
import android.provider.Contacts;
import android.provider.Contacts.PhonesColumns;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

/**
 * An {@link Activity} to generate, preview, and apply changes.
 * 
 * TODO: generalize to non-phone fields
 * 
 * TODO: move all strings to strings.xml
 * 
 * TODO: show contact name to help guess area code
 * 
 * TODO: debug missing button issue
 */
public class Preview extends Activity implements OnClickListener,
		DialogInterface.OnClickListener, DialogInterface.OnCancelListener,
		Runnable {
	public static final String EXTRA_COUNTRY_CODE = "country_code";
	public static final String EXTRA_AREA_CODE = "area_code";
	public static final String EXTRA_SEPARATOR = "separator";

	private static final String[] PROJECTION = { Contacts.Phones._ID,
			Contacts.PhonesColumns.NUMBER };

	private static final String CHARACTERS = "+-. ()";

	private static final String EDITS = "edits";

	static final int HANDLE_ADAPTER_READY = 1;
	static final int HANDLE_UPDATE_COMPLETE = 2;
	static final int HANDLE_ALL_UPDATES_COMPLETE = 3;

	private String mCountryCode;
	private String mAreaCode;
	private String mSeparator;

	private ProgressDialog mProgressDialog;

	private ListView mListView;
	private EditListAdapter mAdapter;

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HANDLE_ADAPTER_READY:
				mProgressDialog.dismiss();
				setListAdapter((EditListAdapter) msg.obj);
				break;
			case HANDLE_UPDATE_COMPLETE:
				mProgressDialog.incrementProgressBy(1);
				break;
			case HANDLE_ALL_UPDATES_COMPLETE:
				mProgressDialog.dismiss();
				finish();
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		mCountryCode = intent.getStringExtra(EXTRA_COUNTRY_CODE);
		mAreaCode = intent.getStringExtra(EXTRA_AREA_CODE);
		mSeparator = intent.getStringExtra(EXTRA_SEPARATOR);
		if (savedInstanceState != null && savedInstanceState.containsKey(EDITS)) {
			Context context = this;
			Edit[] array = (Edit[]) savedInstanceState
					.getParcelableArray(EDITS);
			setListAdapter(EditListAdapter.fromArray(context, array));
		} else {
			loadContacts();
		}
	}

	void setListAdapter(EditListAdapter adapter) {
		if (adapter.isEmpty()) {
			Context context = this;
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setMessage("All contacts are tidy!");
			builder.setPositiveButton(android.R.string.ok, this);
			builder.setOnCancelListener(this);
			AlertDialog dialog = builder.create();
			dialog.show();
		} else {
			mAdapter = adapter;

			setContentView(R.layout.preview);

			Button applyButton = (Button) findViewById(R.id.ApplyButton);
			applyButton.setOnClickListener(this);

			Button cancelButton = (Button) findViewById(R.id.CancelButton);
			cancelButton.setOnClickListener(this);

			mListView = (ListView) findViewById(R.id.PreviewList);
			mListView.setAdapter(adapter);
		}
	}

	@Override
	public void run() {
		// TODO: report error on exception
		if (mAdapter == null) {
			Uri uri = Contacts.Phones.CONTENT_URI;
			String selection = null;
			String[] selectionArgs = null;

			// Show shorter items first because they will be modified most.
			String sortOrder = "LENGTH(" + PhonesColumns.NUMBER + ") ASC";

			Cursor cursor = managedQuery(uri, PROJECTION, selection,
					selectionArgs, sortOrder);
			int idColumn = cursor.getColumnIndex(BaseColumns._ID);
			int numberColumn = cursor.getColumnIndex(Contacts.Phones.NUMBER);
			Context context = this;
			EditListAdapter adapter = new EditListAdapter(context);
			cursor.moveToFirst();
			if (cursor.isFirst()) {
				do {
					Edit edit = new Edit();
					edit.mId = cursor.getLong(idColumn);
					edit.mOriginalValue = cursor.getString(numberColumn);
					edit.mNewValue = cleanup(edit.mOriginalValue);
					if (!edit.mNewValue.equals(edit.mOriginalValue)) {
						adapter.add(edit);
					}
				} while (cursor.moveToNext());
			}
			Message message = new Message();
			message.what = HANDLE_ADAPTER_READY;
			message.obj = adapter;
			mHandler.sendMessage(message);
		} else {
			for (int i = 0; i < mAdapter.getCount(); i++) {
				Edit edit = mAdapter.getItem(i);
				ContentResolver resolver = getContentResolver();
				Uri uri = ContentUris.withAppendedId(
						Contacts.Phones.CONTENT_URI, edit.mId);
				ContentValues values = new ContentValues();
				values.put(Contacts.PhonesColumns.NUMBER, edit.mNewValue);
				String where = null;
				String[] selectionArgs = null;
				resolver.update(uri, values, where, selectionArgs);
				mHandler.sendEmptyMessage(HANDLE_UPDATE_COMPLETE);
			}
			mHandler.sendEmptyMessage(HANDLE_ALL_UPDATES_COMPLETE);
		}
	}

	/**
	 * Formats the given phone number as <code>+XX YYY ZZZ ZZZZ</code>, where
	 * <code>XX</code> is the country code, <code>YYY</code> is the area code,
	 * <code>ZZZ ZZZZ</code> is the local phone number, and the punctuation can
	 * be changed with {@link #mSeparator}.
	 * 
	 * Reference:
	 * <ul>
	 * <li><a href=
	 * "http://en.wikipedia.org/wiki/Telephone_numbering_plan#Country_code">
	 * Telephone numbering plan</a></li>
	 * <li><a href="http://www.freelabs.com/~whitis/date_phone_formats.html">
	 * Proper Formats for Dates and Phone Numbers</a></li>
	 * </ul>
	 * 
	 * @param value
	 *            the phone number to clean-up.
	 * @return the formatted phone number.
	 */
	private String cleanup(final String value) {
		StringBuilder buffer = new StringBuilder(16);

		for (int i = 0, n = value.length(); i < n; i++) {
			char c = value.charAt(i);
			if (Character.isDigit(c)) {
				buffer.append(c);
			} else if (CHARACTERS.indexOf(c) == -1) {
				// Don't format the number if it contains a character that is
				// neither a number nor punctuation.
				return value;
			}
		}

		if (buffer.length() == 7) {
			buffer.insert(0, mAreaCode);
		}

		if (buffer.length() == 10) {
			buffer.insert(0, mCountryCode);
		}

		if (buffer.length() > 10) {
			buffer.insert(0, '+');

			// TODO: Support non-US grouping
			int len = mSeparator.length();
			if (len != 0) {
				// Insert a separator before the last 4 digits.
				buffer.insert(buffer.length() - 4, mSeparator);

				// Insert a separator before the last 7 digits
				buffer.insert(buffer.length() - (7 + len), mSeparator);

				// Insert a separator before the last 10 digits
				buffer.insert(buffer.length() - (10 + len * 2), mSeparator);
			}
		} else {
			// Unexpected number of digits; abort all formatting.
			return value;
		}

		return buffer.toString();
	}

	private void loadContacts() {
		Context context = this;
		mProgressDialog = new ProgressDialog(context,
				ProgressDialog.STYLE_SPINNER);
		mProgressDialog.setTitle("Loading");
		mProgressDialog.setMessage("Loading phone numbers...");
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.setCancelable(false);
		mProgressDialog.show();
		new Thread(this).start();
	}

	private void apply() {
		Context context = this;
		mProgressDialog = new ProgressDialog(context,
				ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setIndeterminate(false);
		mProgressDialog.setCancelable(false);
		mProgressDialog.setTitle("Updating");
		mProgressDialog.setMessage("Updating phone numbers...");
		mProgressDialog.setMax(mAdapter.getCount());
		mProgressDialog.show();
		new Thread(this).start();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// mCountryCode and mAreaCode are saved with the Intent
		if (mAdapter != null) {
			outState.putParcelableArray(EDITS, mAdapter.toArray());
		}
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		finish();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		finish();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.ApplyButton:
			apply();
			break;
		case R.id.CancelButton:
			finish();
			break;
		}
	}
}
