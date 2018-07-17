/** 
 * Copyright (c) 2018, Davy Landman, SWAT.engineering BV
 * All rights reserved. 
 *  
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met: 
 *  
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 
 *  
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */ 
package engineering.swat.rascal.lsp.util;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

public class TreeMapLookup implements RangeToLocationMap {
	
	private final NavigableMap<Range, Location> data = new TreeMap<>(TreeMapLookup::compareRanges);

	private static int compareRanges(Range a, Range b) {
		Position aStart = a.getStart();
		Position aEnd = a.getEnd();
		Position bStart = b.getStart();
		Position bEnd = b.getEnd();
		if (aEnd.getLine() < bStart.getLine()) {
			return -1;
		}
		if (aStart.getLine() > bEnd.getLine()) {
			return 1;
		}
		// some kind of containment, or just on the same line
		if (aStart.getLine() == bStart.getLine()) {
			// start at same line
			if (aEnd.getLine() == bEnd.getLine()) {
				// end at same line
				if (aStart.getCharacter() == bStart.getCharacter()) {
					return Integer.compare(aEnd.getCharacter(), bEnd.getCharacter());
				}
				return Integer.compare(aStart.getCharacter(), bStart.getCharacter());
			}
			return Integer.compare(aEnd.getLine(), bEnd.getLine());
		}
		return Integer.compare(aStart.getLine(), aStart.getLine());
	}
	
	private static boolean rangeContains(Range a, Range b) {
		Position aStart = a.getStart();
		Position aEnd = a.getEnd();
		Position bStart = b.getStart();
		Position bEnd = b.getEnd();

		if (aStart.getLine() <= bStart.getLine()
				&& aEnd.getLine() >= bEnd.getLine()) {
			if (aStart.getLine() == bStart.getLine()) {
				if (aStart.getCharacter() > bStart.getCharacter()) {
					return false;
				}
			}
			if (aEnd.getLine() == bEnd.getLine()) {
				if (aEnd.getCharacter() < bEnd.getCharacter()) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public Optional<Location> lookup(Range from) {
		Entry<Range, Location> result = data.floorEntry(from);
		if (result == null) {
			// could be that it's at the start of the entry
			result = data.ceilingEntry(from);
		}
		if (result != null) {
            Range match = result.getKey();
            if (rangeContains(match, from)) {
                return Optional.of(result.getValue());
            }
		}
		return Optional.empty();
	}

	public void add(Range from, Location to) {
		data.put(from, to);
	}
	
	@Override
	public String toString() {
		return "lookup: " + data;
	}
}
