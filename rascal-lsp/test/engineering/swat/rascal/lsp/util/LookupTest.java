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
		Range use = new Range(new Position(1,5), new Position(1,7));
		target.add(use, def);
		assertSame(def, target.lookup(use).get());
	}

	@Test
	public void testSimpleLookupInside() {
		TreeMapLookup target = new TreeMapLookup();
		Location def = new Location("", new Range(new Position(1,1), new Position(1,3)));
		Range use = new Range(new Position(1,5), new Position(1,8));
		target.add(use, def);
		Range cursor = new Range(new Position(1,6), new Position(1,7));
		assertSame(def, target.lookup(cursor).get());
	}

	@Test
	public void testSimpleLookupFront() {
		TreeMapLookup target = new TreeMapLookup();
		Location def = new Location("", new Range(new Position(1,1), new Position(1,3)));
		Range use = new Range(new Position(1,5), new Position(1,8));
		target.add(use, def);
		Range cursor = new Range(new Position(1,5), new Position(1,5));
		assertSame(def, target.lookup(cursor).get());
	}

	@Test
	public void testSimpleLookupEnd() {
		TreeMapLookup target = new TreeMapLookup();
		Location def = new Location("", new Range(new Position(1,1), new Position(1,3)));
		Range use = new Range(new Position(1,5), new Position(1,8));
		target.add(use, def);
		Range cursor = new Range(new Position(1,8), new Position(1,8));
		assertSame(def, target.lookup(cursor).get());
	}

	@Test
	public void testSimpleLookupInside1() {
		TreeMapLookup target = new TreeMapLookup();
		Location def = new Location("", new Range(new Position(1,1), new Position(1,3)));
		Range use = new Range(new Position(1,5), new Position(1,8));
		target.add(use, def);
		Range use2 = new Range(new Position(1,9), new Position(1,12));
		target.add(use2, def);
		Range cursor = new Range(new Position(1,6), new Position(1,7));
		assertSame(def, target.lookup(cursor).get());
	}

	@Test
	public void testSimpleLookupFront1() {
		TreeMapLookup target = new TreeMapLookup();
		Location def = new Location("", new Range(new Position(1,1), new Position(1,3)));
		Range use = new Range(new Position(1,5), new Position(1,8));
		target.add(use, def);
		Range use2 = new Range(new Position(1,9), new Position(1,12));
		target.add(use2, def);
		Range cursor = new Range(new Position(1,5), new Position(1,5));
		assertSame(def, target.lookup(cursor).get());
	}

	@Test
	public void testSimpleLookupEnd1() {
		TreeMapLookup target = new TreeMapLookup();
		Location def = new Location("", new Range(new Position(1,1), new Position(1,3)));
		Range use = new Range(new Position(1,5), new Position(1,8));
		target.add(use, def);
		Range use2 = new Range(new Position(1,9), new Position(1,12));
		target.add(use2, def);
		Range cursor = new Range(new Position(1,8), new Position(1,8));
		assertSame(def, target.lookup(cursor).get());
	}

}
