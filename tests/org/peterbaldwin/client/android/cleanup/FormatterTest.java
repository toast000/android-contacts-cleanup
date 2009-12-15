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
