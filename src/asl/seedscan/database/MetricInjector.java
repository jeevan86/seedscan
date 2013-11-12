/**
 * 
 */
package asl.seedscan.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.concurrent.Task;
import asl.concurrent.TaskThread;
import asl.seedscan.metrics.MetricResult;

/**
 * @author Joel D. Edwards <jdedwards@usgs.gov>
 *
 */
public class MetricInjector
extends TaskThread<MetricResult>
{
    private static final Logger logger = LoggerFactory.getLogger(asl.seedscan.database.MetricInjector.class);
    
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

    public boolean isConnected() {
        //System.out.println("== MetricInjector.isConnected() = " + metricDB.isConnected() );
        return metricDB.isConnected();
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
	protected void performTask(Task<MetricResult> task) {
		String command = task.getCommand();
		MetricResult results = task.getData();
		
		logger.info("performTask: command=" + command + " results=" + results);
		
		if (command.equals("INJECT")) {
			int i = metricDB.insertMetricData(results);
            if (i != 0) {
                logger.error("metricDB.insertMetricData FAILED!");
            }
		}
	}

	/* (non-Javadoc)
	 * @see asl.concurrent.TaskThread#cleanup()
	 */
	@Override
	protected void cleanup() {
		// Post-run logic goes here
	}

	public void inject(MetricResult results)
	throws InterruptedException
	{
		addTask("INJECT", results);
	}
}
