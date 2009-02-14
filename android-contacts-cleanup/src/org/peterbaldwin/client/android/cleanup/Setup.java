package org.peterbaldwin.client.android.cleanup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

/**
 * An activity to configure and initiate a clean-up operation.
 * 
 * TODO: store values in preferences so that activity can be repeated more
 * easily.
 */
public class Setup extends Activity implements View.OnClickListener,
		DialogInterface.OnClickListener {

	private static final String DEFAULT_COUNTRY_CODE = "1";
	private static final String DEFAULT_AREA_CODE = "";

	private static final int PREFERENCES_MODE = MODE_PRIVATE;

	private RadioGroup mGroupFormat;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setup);

		mGroupFormat = (RadioGroup) findViewById(R.id.group_format);

		Button button = (Button) findViewById(R.id.PreviewButton);
		button.setOnClickListener(this);

		RadioButton radioButton = (RadioButton) findViewById(R.id.format_space);
		radioButton.toggle();

		loadPreferences();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		savePreferences();
	}

	@Override
	protected void onPause() {
		super.onPause();
		savePreferences();
	}

	private void loadPreferences() {
		SharedPreferences preferences = getPreferences(PREFERENCES_MODE);
		setView(preferences, R.string.pref_country_code, R.id.CountryCodeText,
				DEFAULT_COUNTRY_CODE);
		setView(preferences, R.string.pref_area_code, R.id.AreaCodeText,
				DEFAULT_AREA_CODE);

		String format = getPreference(preferences, R.string.pref_format, null);
		if (format != null) {
			for (int i = 0; i < mGroupFormat.getChildCount(); i++) {
				RadioButton button = (RadioButton) mGroupFormat.getChildAt(i);
				if (format.equals(getRadioButtonText(button))) {
					button.toggle();
					break;
				}
			}
		}
	}

	private void setView(SharedPreferences preferences, int prefId, int viewId,
			String defValue) {
		String value = getPreference(preferences, prefId, defValue);
		EditText editText = (EditText) findViewById(viewId);
		editText.setText(value);
	}

	private String getEditText(int editTextId) {
		EditText editText = (EditText) findViewById(editTextId);
		return editText.getText().toString();
	}

	private String getPreference(SharedPreferences preferences, int resId,
			String defValue) {
		String key = getString(resId);
		return preferences.getString(key, defValue);
	}

	private String getRadioButtonText(RadioButton button) {
		return button.getText().toString();
	}

	private String getRadioButtonText(int viewId) {
		RadioButton button = (RadioButton) findViewById(viewId);
		return getRadioButtonText(button);
	}

	private String getPreferenceValue(int viewId) {
		if (viewId == R.id.group_format) {
			int buttonId = mGroupFormat.getCheckedRadioButtonId();
			return getRadioButtonText(buttonId);
		} else {
			return getEditText(viewId);
		}
	}

	private void putPreference(SharedPreferences.Editor editor, int viewId,
			int resId) {
		String key = getString(resId);
		String value = getPreferenceValue(viewId);
		editor.putString(key, value);
	}

	private void savePreferences() {
		SharedPreferences preferences = getPreferences(PREFERENCES_MODE);
		SharedPreferences.Editor editor = preferences.edit();
		putPreference(editor, R.id.CountryCodeText, R.string.pref_country_code);
		putPreference(editor, R.id.AreaCodeText, R.string.pref_area_code);
		putPreference(editor, R.id.group_format, R.string.pref_format);
		editor.commit();
	}

	private String getSeparator() {
		RadioGroup group = (RadioGroup) findViewById(R.id.group_format);
		int id = group.getCheckedRadioButtonId();
		switch (id) {
		case R.id.format_dash:
			return "-";
		case R.id.format_dot:
			return ".";
		case R.id.format_nopunc:
			return "";
		case R.id.format_space:
			return " ";
		default:
			RadioButton button = (RadioButton) findViewById(id);
			throw new RuntimeException("unexpected format: " + button.getText());
		}
	}
	
	private static boolean isNumberOfLength(int n, String value) {
		if (value.length() == n) {
			for (int i = 0; i < n; i++) {
				char c = value.charAt(i);
				if (!Character.isDigit(c)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	private void showError(int resId) {
		String message = getString(resId);
		Context context = this;
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(message);
		builder.setPositiveButton(android.R.string.ok, this);
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	@Override
	public void onClick(View v) {
		String countryCode = getEditText(R.id.CountryCodeText);
		String areaCode = getEditText(R.id.AreaCodeText);
		String separator = getSeparator();

		if (!isNumberOfLength(1, countryCode)) {
			showError(R.string.error_invalid_country_code);
		} else if (!isNumberOfLength(3, areaCode)) {
			showError(R.string.error_invalid_area_code);
		} else {
			Context context = getApplicationContext();
			Intent intent = new Intent(context, Preview.class);
			intent.putExtra(Preview.EXTRA_COUNTRY_CODE, countryCode);
			intent.putExtra(Preview.EXTRA_AREA_CODE, areaCode);
			intent.putExtra(Preview.EXTRA_SEPARATOR, separator);
			startActivity(intent);
			finish();
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		// Pass
	}
}