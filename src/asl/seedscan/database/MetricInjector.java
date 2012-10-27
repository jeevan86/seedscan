/**
 * 
 */
package asl.seedscan.database;

import java.util.logging.Logger;

import asl.concurrent.Task;
import asl.concurrent.TaskThread;
import asl.seedscan.metrics.Metric;

/**
 * @author Joel D. Edwards <jdedwards@usgs.gov>
 *
 */
public class MetricInjector
extends TaskThread<Metric>
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.database.MetricInjector");
    
	MetricDatabase metricDB;
	
	/**
	 * 
	 */
	public MetricInjector(MetricDatabase metricDB) {
		super();
		this.metricDB = metricDB;
	}

	/**
	 * @param capacity
	 */
	public MetricInjector(MetricDatabase metricDB, int capacity) {
		super(capacity);
		this.metricDB = metricDB;
	}

	/* (non-Javadoc)
	 * @see asl.concurrent.TaskThread#setup()
	 */
	@Override
	protected void setup() {
		// Pre-run logic goes here
	}

	/* (non-Javadoc)
	 * @see asl.concurrent.TaskThread#performTask(asl.concurrent.Task)
	 */
	@Override
	protected void performTask(Task<Metric> task) {
		String command = task.getCommand();
		Metric metric = task.getData();
		
		if (command.equals("INJECT")) {
			metricDB.insertMetricData(metric);
		}
	}

	/* (non-Javadoc)
	 * @see asl.concurrent.TaskThread#cleanup()
	 */
	@Override
	protected void cleanup() {
		// Post-run logic goes here
	}

	public void inject(Metric metric)
	throws InterruptedException
	{
		addTask("INJECT", metric);
	}
}
