package engineering.swat.rascal.lsp.util;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;

public interface LocationToRangeMap {
	Location lookup(Location from);
}
