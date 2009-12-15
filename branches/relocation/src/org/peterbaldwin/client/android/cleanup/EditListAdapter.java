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

import android.content.Context;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;

/**
 * A {@link ListAdapter} for an array of {@link Edit} objects.
 */
class EditListAdapter extends ArrayAdapter<Edit> {

	public EditListAdapter(Context context) {
		super(context, R.layout.edit, R.id.original_phone);
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		// Set convertView to null to avoid reuse
		View view = super.getView(position, null, parent);
		TextView textDisplayName = (TextView) view
				.findViewById(R.id.display_name);
		TextView textOriginalPhone = (TextView) view
				.findViewById(R.id.original_phone);
		EditText editNewPhone = (EditText) view.findViewById(R.id.new_phone);

		Edit edit = getItem(position);
		textDisplayName.setText(edit.mDisplayName);
		textOriginalPhone.setText(edit.mOriginalValue);
		editNewPhone.setText(edit.mNewValue);

		// Watch for changes
		TextWatcher watcher = new EditTextWatcher(edit);
		editNewPhone.addTextChangedListener(watcher);

		return view;
	}

	public Edit[] toArray() {
		final int n = getCount();
		Edit[] array = new Edit[n];
		for (int i = 0; i < n; i++) {
			array[i] = getItem(i);
		}
		return array;
	}

	public static EditListAdapter fromArray(Context context, Edit[] array) {
		EditListAdapter adapter = new EditListAdapter(context);
		for (int i = 0; i < array.length; i++) {
			adapter.add(array[i]);
		}
		return adapter;
	}
}
