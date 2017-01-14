package asl.seedscan.scanner;

abstract class ScanWorker implements Comparable<ScanWorker>, Runnable {
	
	protected final ScanManager manager;
	
	ScanWorker(ScanManager manager){
		this.manager = manager;
	}


	/**
	 * Returns the base priority. This is used for setting levels of running. EG Metrics run before Station scans. 
	 * Lower values are lower priority.
	 * Current priorities are as below.
	 * 1 - Generic Scan
	 * 2 - Station Scan
	 * 3 - Metric
	 * 
	 * @return the priority of this worker.
	 */
	abstract int getPriorityBase();

}
