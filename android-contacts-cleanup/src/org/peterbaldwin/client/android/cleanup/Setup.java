package org.peterbaldwin.client.android.cleanup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * An activity to configure and initiate a clean-up operation.
 * 
 * TODO: store values in preferences so that activity can be repeated more
 * easily.
 */
public class Setup extends Activity implements OnClickListener {

	private static final String DEFAULT_COUNTRY_CODE = "1";
	private static final String DEFAULT_AREA_CODE = "";

	private static final int PREFERENCES_MODE = MODE_PRIVATE;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setup);

		loadPreferences();

		Button button = (Button) findViewById(R.id.PreviewButton);
		button.setOnClickListener(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		savePreferences();
	}

	private void loadPreferences() {
		SharedPreferences preferences = getPreferences(PREFERENCES_MODE);
		setView(preferences, R.string.pref_country_code, R.id.CountryCodeText,
				DEFAULT_COUNTRY_CODE);
		setView(preferences, R.string.pref_area_code, R.id.AreaCodeText,
				DEFAULT_AREA_CODE);
	}

	private void setView(SharedPreferences preferences, int prefId, int viewId,
			String defValue) {
		String value = getPreference(preferences, prefId, defValue);
		EditText editText = (EditText) findViewById(viewId);
		editText.setText(value);
	}

	private void putExtra(Intent intent, String key, int editTextId) {
		EditText editText = (EditText) findViewById(editTextId);
		String value = editText.getText().toString();
		intent.putExtra(key, value);
	}

	private String getPreference(SharedPreferences preferences, int resId,
			String defValue) {
		String key = getString(resId);
		return preferences.getString(key, defValue);
	}

	private void putPreference(SharedPreferences.Editor editor, int viewId,
			int resId) {
		String key = getString(resId);
		EditText editText = (EditText) findViewById(viewId);
		String value = editText.getText().toString();
		editor.putString(key, value);
	}

	private void savePreferences() {
		SharedPreferences preferences = getPreferences(PREFERENCES_MODE);
		SharedPreferences.Editor editor = preferences.edit();
		putPreference(editor, R.id.CountryCodeText, R.string.pref_country_code);
		putPreference(editor, R.id.AreaCodeText, R.string.pref_area_code);
		editor.commit();
	}

	@Override
	public void onClick(View v) {
		Context context = getApplicationContext();

		savePreferences();

		Intent intent = new Intent(context, Preview.class);
		putExtra(intent, Extras.COUNTRY_CODE, R.id.CountryCodeText);
		putExtra(intent, Extras.AREA_CODE, R.id.AreaCodeText);
		startActivity(intent);
		finish();
	}
}