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

import android.text.Editable;
import android.text.TextWatcher;

/**
 * A {@link TextWatcher} implementation that updates an {@link Edit} object.
 */
class EditTextWatcher implements TextWatcher {

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
