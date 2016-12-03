package asl.seedscan;

import java.util.Hashtable;
import java.util.Set;

class ScanFilter {
	private Hashtable<String, Boolean> filters;
	private boolean exclusive = false;

	ScanFilter(boolean exclusive) {
		this.exclusive = exclusive;
		filters = new Hashtable<String, Boolean>();
	}

	void addFilter(String key) {
		filters.put(key, exclusive);
	}

	boolean filter(String key) {
		return filters.containsKey(key) ^ exclusive;
	}

	public Set<String> getKeys() {
		return filters.keySet();
	}
}
