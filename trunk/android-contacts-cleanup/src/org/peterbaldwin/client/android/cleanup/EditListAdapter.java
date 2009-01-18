package org.peterbaldwin.client.android.cleanup;

import android.content.Context;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;

/**
 * A {@link ListAdapter} for an array of {@link Edit} objects.
 */
public class EditListAdapter extends ArrayAdapter<Edit> {

	public EditListAdapter(Context context) {
		super(context, R.layout.edit, R.id.OriginalValue);
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		View view = super.getView(position, convertView, parent);

		EditText editText = (EditText) view.findViewById(R.id.NewValue);
		Edit edit = getItem(position);

		// Set value
		editText.setText(edit.mNewValue);

		// Watch for changes
		TextWatcher watcher = new EditTextWatcher(edit);
		editText.addTextChangedListener(watcher);

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
