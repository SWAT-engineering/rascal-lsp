package engineering.swat.rascal.lsp.util;

import static org.junit.jupiter.api.Assertions.assertSame;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;
import engineering.swat.rascal.lsp.util.TreeMapLookup;

public class LookupTest {
	
	
	@Test
	public void testSimpleLookup() {
		TreeMapLookup target = new TreeMapLookup();
		Location def = new Location("", new Range(new Position(1,1), new Position(1,3)));
		Location use = new Location("", new Range(new Position(1,5), new Position(1,7)));
		target.add(use, def);
		assertSame(def, target.lookup(use));
	}

	@Test
	public void testSimpleLookupInside() {
		TreeMapLookup target = new TreeMapLookup();
		Location def = new Location("", new Range(new Position(1,1), new Position(1,3)));
		Location use = new Location("", new Range(new Position(1,5), new Position(1,8)));
		target.add(use, def);
		Location cursor = new Location("", new Range(new Position(1,6), new Position(1,7)));
		assertSame(def, target.lookup(cursor));
	}

	@Test
	public void testSimpleLookupFront() {
		TreeMapLookup target = new TreeMapLookup();
		Location def = new Location("", new Range(new Position(1,1), new Position(1,3)));
		Location use = new Location("", new Range(new Position(1,5), new Position(1,8)));
		target.add(use, def);
		Location cursor = new Location("", new Range(new Position(1,5), new Position(1,5)));
		assertSame(def, target.lookup(cursor));
	}

	@Test
	public void testSimpleLookupEnd() {
		TreeMapLookup target = new TreeMapLookup();
		Location def = new Location("", new Range(new Position(1,1), new Position(1,3)));
		Location use = new Location("", new Range(new Position(1,5), new Position(1,8)));
		target.add(use, def);
		Location cursor = new Location("", new Range(new Position(1,8), new Position(1,8)));
		assertSame(def, target.lookup(cursor));
	}

	@Test
	public void testSimpleLookupInside1() {
		TreeMapLookup target = new TreeMapLookup();
		Location def = new Location("", new Range(new Position(1,1), new Position(1,3)));
		Location use = new Location("", new Range(new Position(1,5), new Position(1,8)));
		target.add(use, def);
		Location use2 = new Location("", new Range(new Position(1,9), new Position(1,12)));
		target.add(use2, def);
		Location cursor = new Location("", new Range(new Position(1,6), new Position(1,7)));
		assertSame(def, target.lookup(cursor));
	}

	@Test
	public void testSimpleLookupFront1() {
		TreeMapLookup target = new TreeMapLookup();
		Location def = new Location("", new Range(new Position(1,1), new Position(1,3)));
		Location use = new Location("", new Range(new Position(1,5), new Position(1,8)));
		target.add(use, def);
		Location use2 = new Location("", new Range(new Position(1,9), new Position(1,12)));
		target.add(use2, def);
		Location cursor = new Location("", new Range(new Position(1,5), new Position(1,5)));
		assertSame(def, target.lookup(cursor));
	}

	@Test
	public void testSimpleLookupEnd1() {
		TreeMapLookup target = new TreeMapLookup();
		Location def = new Location("", new Range(new Position(1,1), new Position(1,3)));
		Location use = new Location("", new Range(new Position(1,5), new Position(1,8)));
		target.add(use, def);
		Location use2 = new Location("", new Range(new Position(1,9), new Position(1,12)));
		target.add(use2, def);
		Location cursor = new Location("", new Range(new Position(1,8), new Position(1,8)));
		assertSame(def, target.lookup(cursor));
	}

}
