package asl.seedscan;

import java.util.Hashtable;
import java.util.Set;

public class ScanFilter {
	private Hashtable<String, Boolean> filters;
	private boolean exclusive = false;

	public ScanFilter(boolean exclusive) {
		this.exclusive = exclusive;
		filters = new Hashtable<String, Boolean>();
	}

	public void addFilter(String key) {
		filters.put(key, exclusive);
	}

	public void removeFilter(String key) {
		filters.remove(key);
	}

	public boolean filter(String key) {
		return filters.containsKey(key) ^ exclusive;
	}

	public Set<String> getKeys() {
		return filters.keySet();
	}
}
