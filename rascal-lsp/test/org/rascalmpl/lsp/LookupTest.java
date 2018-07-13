package org.rascalmpl.lsp;

import static org.junit.jupiter.api.Assertions.assertSame;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;

public class LookupTest {
	
	
	@Test
	public void testSimpleLookup() {
		SimpleLookup target = new SimpleLookup();
		Location def = new Location("", new Range(new Position(1,1), new Position(1,3)));
		Location use = new Location("", new Range(new Position(1,5), new Position(1,7)));
		target.add(use, def);
		assertSame(def, target.lookup(use));
	}

	@Test
	public void testSimpleLookupInside() {
		SimpleLookup target = new SimpleLookup();
		Location def = new Location("", new Range(new Position(1,1), new Position(1,3)));
		Location use = new Location("", new Range(new Position(1,5), new Position(1,8)));
		target.add(use, def);
		Location cursor = new Location("", new Range(new Position(1,6), new Position(1,7)));
		assertSame(def, target.lookup(cursor));
	}

	@Test
	public void testSimpleLookupFront() {
		SimpleLookup target = new SimpleLookup();
		Location def = new Location("", new Range(new Position(1,1), new Position(1,3)));
		Location use = new Location("", new Range(new Position(1,5), new Position(1,8)));
		target.add(use, def);
		Location cursor = new Location("", new Range(new Position(1,5), new Position(1,5)));
		assertSame(def, target.lookup(cursor));
	}

	@Test
	public void testSimpleLookupEnd() {
		SimpleLookup target = new SimpleLookup();
		Location def = new Location("", new Range(new Position(1,1), new Position(1,3)));
		Location use = new Location("", new Range(new Position(1,5), new Position(1,8)));
		target.add(use, def);
		Location cursor = new Location("", new Range(new Position(1,8), new Position(1,8)));
		assertSame(def, target.lookup(cursor));
	}

	@Test
	public void testSimpleLookupInside1() {
		SimpleLookup target = new SimpleLookup();
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
		SimpleLookup target = new SimpleLookup();
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
		SimpleLookup target = new SimpleLookup();
		Location def = new Location("", new Range(new Position(1,1), new Position(1,3)));
		Location use = new Location("", new Range(new Position(1,5), new Position(1,8)));
		target.add(use, def);
		Location use2 = new Location("", new Range(new Position(1,9), new Position(1,12)));
		target.add(use2, def);
		Location cursor = new Location("", new Range(new Position(1,8), new Position(1,8)));
		assertSame(def, target.lookup(cursor));
	}

}
