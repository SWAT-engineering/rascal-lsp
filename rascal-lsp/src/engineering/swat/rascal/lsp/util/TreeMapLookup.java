package engineering.swat.rascal.lsp.util;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

public class TreeMapLookup implements LocationToRangeMap {
	
	private final NavigableMap<Location, Location> data = new TreeMap<>(TreeMapLookup::compareLocations);
	
	private static int compareLocations(Location a, Location b) {
		int result = a.getUri().compareTo(b.getUri());
		if (result != 0) {
			return result;
		}
		return compareRanges(a.getRange(), b.getRange());
	}
	
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
	public Location lookup(Location from) {
		Entry<Location, Location> result = data.floorEntry(from);
		if (result == null) {
			// could be that it's at the start of the entry
			result = data.ceilingEntry(from);
		}
		if (result != null) {
            Location match = result.getKey();
            if (match.getUri().equals(from.getUri()) && rangeContains(match.getRange(), from.getRange())) {
                return result.getValue();
            }
		}
		return null;
	}

	public void add(Location from, Location to) {
		data.put(from, to);
	}
	
	@Override
	public String toString() {
		return "lookup: " + data;
	}
}
