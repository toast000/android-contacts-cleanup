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
import android.database.CursorJoiner;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.BaseColumns;
import android.provider.Contacts;
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

	private static final String EDITS = "edits";

	static final int HANDLE_ADAPTER_READY = 1;
	static final int HANDLE_UPDATE_COMPLETE = 2;
	static final int HANDLE_ALL_UPDATES_COMPLETE = 3;

	private Formatter mFormatter;

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
		String countryCode = intent.getStringExtra(EXTRA_COUNTRY_CODE);
		String areaCode = intent.getStringExtra(EXTRA_AREA_CODE);
		String separator = intent.getStringExtra(EXTRA_SEPARATOR);

		mFormatter = new Formatter(countryCode, areaCode, separator);

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
			mListView.setItemsCanFocus(true);
			mListView.setSelector(android.R.drawable.list_selector_background);
			mListView.setAdapter(adapter);
		}
	}

	@Override
	public void run() {
		// TODO: report error on exception
		if (mAdapter == null) {
			Cursor phoneCursor = managedQuery(Contacts.Phones.CONTENT_URI,
					new String[] { Contacts.Phones._ID, Contacts.Phones.NUMBER,
							Contacts.Phones.PERSON_ID }, null, null,
					Contacts.Phones.PERSON_ID);
			Cursor peopleCursor = managedQuery(Contacts.People.CONTENT_URI,
					new String[] { Contacts.People._ID,
							Contacts.People.DISPLAY_NAME }, null, null,
					Contacts.People._ID);

			int phoneIdColumn = phoneCursor
					.getColumnIndexOrThrow(Contacts.Phones._ID);
			int phoneNumberColumn = phoneCursor
					.getColumnIndexOrThrow(Contacts.Phones.NUMBER);
			int displayNameColumn = peopleCursor
					.getColumnIndexOrThrow(Contacts.People.DISPLAY_NAME);

			Context context = this;
			EditListAdapter adapter = new EditListAdapter(context);
			CursorJoiner joiner = new CursorJoiner(phoneCursor,
					new String[] { Contacts.Phones.PERSON_ID }, peopleCursor,
					new String[] { Contacts.People._ID });
			for (CursorJoiner.Result result : joiner) {
				switch (result) {
				case LEFT:
					// The phone number is not associated with a person
					break;
				case RIGHT:
					// The person does not have a phone number
					break;
				case BOTH:
					Edit edit = new Edit();
					edit.mPhoneId = phoneCursor.getLong(phoneIdColumn);
					edit.mOriginalValue = phoneCursor
							.getString(phoneNumberColumn);
					edit.mNewValue = mFormatter.cleanup(edit.mOriginalValue);
					edit.mDisplayName = peopleCursor
							.getString(displayNameColumn);
					if (!edit.mNewValue.equals(edit.mOriginalValue)) {
						adapter.add(edit);
					}
					break;
				}
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
						Contacts.Phones.CONTENT_URI, edit.mPhoneId);
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
