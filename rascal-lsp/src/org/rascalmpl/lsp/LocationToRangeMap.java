package org.rascalmpl.lsp;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;

public interface LocationToRangeMap {
	Location lookup(Location from);
}
