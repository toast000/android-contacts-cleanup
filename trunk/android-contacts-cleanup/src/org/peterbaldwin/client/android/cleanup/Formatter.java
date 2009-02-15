package org.peterbaldwin.client.android.cleanup;

public class Formatter {

	private static final String CHARACTERS = "+-. ()";
	private final String mCountryCode;
	private final String mCountryPrefix;
	private final String mAreaCode;
	private final String mSeparator;

	public Formatter(String countryCode, String areaCode, String separator) {
		super();
		mCountryCode = countryCode;
		mAreaCode = areaCode;
		mSeparator = separator;
		
		mCountryPrefix = "+" + mCountryCode;
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
	public String cleanup(final String value) {
		if (value.startsWith("+") && !value.startsWith(mCountryPrefix)) {
			// Ignore numbers from other countries which likely have different
			// formatting conventions.
			return value;
		}
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

			// TODO: Support non-US grouping (http://www.wtng.info/)
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
}
