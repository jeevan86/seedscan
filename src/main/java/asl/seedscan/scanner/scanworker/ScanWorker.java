package asl.seedscan.scanner.scanworker;

import asl.seedscan.scanner.ScanManager;

public abstract class ScanWorker implements Comparable<ScanWorker>, Runnable {

	protected final ScanManager manager;

	protected ScanWorker(ScanManager manager) {
		this.manager = manager;
	}

	/**
	 * Returns the base priority. This is used for setting levels of running. EG
	 * Metrics run before Station scans. Lower values are higher priority.
	 * Standard base priorities are below. <br>
	 * 55 - Generic Scan <br>
	 * 45 - Station Scan <br>
	 * 35 - Metric <br>
	 * 
	 * If a specialized ScanWorker needs to always run before or after other
	 * workers of the same type, then its priority can vary within the ones
	 * place. Other fine ordering can occur in getFinePriority()
	 * 
	 * @return the rough priority of this worker.
	 */
	protected abstract Integer getBasePriority();

	/**
	 * This is used after base priority comparison. This is used to compare
	 * workers of the same type. Its methodology may differ based on what child
	 * ScanWorker type is being used.
	 * 
	 * @return the fine detailed priority
	 */
	protected abstract Long getFinePriority();

	/**
	 * This compares first the base priority, then the fine priority.
	 * 
	 * Children classes need only to convert themselves into a Long that is
	 * suitable for comparison.
	 */
	public int compareTo(ScanWorker other) {
		// Compare base priority first
		if (!(this.getBasePriority().equals(other.getBasePriority()))) {
			return this.getBasePriority().compareTo(other.getBasePriority());
		}
		return this.getFinePriority().compareTo(other.getFinePriority());
	}

}
