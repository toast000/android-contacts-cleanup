package org.peterbaldwin.client.android.cleanup;

import junit.framework.TestCase;

public class FormatterTest extends TestCase {
	public void testCleanup() {
		Formatter formatter = new Formatter("1", "555", " ");
		
		// Add missing area code
		assertEquals("+1 555 123 4567", formatter.cleanup("123-4567"));
		
		// Add missing country code
		assertEquals("+1 555 123 4567", formatter.cleanup("(555) 123-4567"));
		
		// Add calling prefix
		assertEquals("+1 555 123 4567", formatter.cleanup("1-555-123-4567"));
		
		// Reformat
		assertEquals("+1 555 123 4567", formatter.cleanup("+1-555-123-4567"));
		
		// Don't modify international numbers
		assertEquals("+65 1234 4321", formatter.cleanup("+65 1234 4321"));
	}
}
