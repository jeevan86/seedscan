package asl.seedscan;

import java.util.Hashtable;
import java.util.Set;

class ScanFilter {
	private Hashtable<String, Boolean> filters;
	public Set<String> getKeys() {
		return filters.keySet();
	}
}
