/*-
 *  Copyright (C) 2009 Peter Baldwin   
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

/**
 * An {@link Activity} to generate, preview, and apply changes.
 * 
 * TODO: Move all strings to strings.xml
 * 
 * TODO: Smarter non-US phone number detection
 */
public class PreviewActivity extends Activity implements OnClickListener,
		DialogInterface.OnClickListener, DialogInterface.OnCancelListener,
		Runnable {

	private static final int SDK = Integer.parseInt(Build.VERSION.SDK);
	private static final boolean ECLAIR = SDK >= 5;

	private static final Uri CONTENT_URI = Uri.parse(
			ECLAIR ? "content://com.android.contacts/data/phones"
					: "content://contacts/phones");
	
	private static final String COLUMN_ID = "_id";
	private static final String COLUMN_NUMBER = ECLAIR ? "data1" : "number";
	private static final String COLUMN_DISPLAY_NAME = "display_name";
	
	private static final String SORT_ORDER = "display_name ASC";
	
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
			if (!isFinishing()) {
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
			Uri uri = CONTENT_URI;
			String[] projection = { COLUMN_ID, COLUMN_NUMBER,
					COLUMN_DISPLAY_NAME };
			String selection = null;
			String[] selectionArgs = null;
			String sortOrder = SORT_ORDER;
			Cursor cursor = managedQuery(uri, projection, selection,
					selectionArgs, sortOrder);

			int phoneIdColumn = cursor.getColumnIndexOrThrow(COLUMN_ID);
			int phoneNumberColumn = cursor.getColumnIndexOrThrow(COLUMN_NUMBER);
			int displayNameColumn = cursor.getColumnIndexOrThrow(COLUMN_DISPLAY_NAME);

			Context context = this;
			EditListAdapter adapter = new EditListAdapter(context);
			if (cursor.moveToFirst()) {
				do {
					Edit edit = new Edit();
					edit.mPhoneId = cursor.getLong(phoneIdColumn);
					edit.mOriginalValue = cursor.getString(phoneNumberColumn);
					edit.mNewValue = mFormatter.cleanup(edit.mOriginalValue);
					edit.mDisplayName = cursor.getString(displayNameColumn);
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
				long id = edit.mPhoneId;
				Uri uri = ContentUris.withAppendedId(CONTENT_URI, id);
				ContentValues values = new ContentValues();
				values.put(COLUMN_NUMBER, edit.mNewValue);
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
		
		// TODO: Re-attach thread to new Activity instance
		// if configuration changes.
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
