package org.peterbaldwin.client.android.cleanup;

import android.text.Editable;
import android.text.TextWatcher;

/**
 * A {@link TextWatcher} implementation that updates an {@link Edit} object.
 */
public class EditTextWatcher implements TextWatcher {

	private final Edit edit;

	public EditTextWatcher(Edit edit) {
		super();
		this.edit = edit;
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// Pass
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// Pass
	}

	@Override
	public void afterTextChanged(Editable s) {
		edit.mNewValue = s.toString();
	}
}
