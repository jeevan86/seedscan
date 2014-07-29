package asl.seedscan.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.concurrent.Task;
import asl.concurrent.TaskThread;
import asl.seedscan.metrics.MetricResult;

/**
 * The Class MetricInjector. This class extends TaskThread<MetricResult> and
 * handles adding injection tasks to the task queue in TaskThread.java.
 * 
 * @author Joel D. Edwards <jdedwards@usgs.gov>
 */
public class MetricInjector extends TaskThread<MetricResult> {

	private static final Logger logger = LoggerFactory
			.getLogger(asl.seedscan.database.MetricInjector.class);

	/** The metric db. */
	MetricDatabase metricDB;

	/**
	 * Instantiates a new metric injector.
	 * 
	 * @param metricDB
	 *            the MetricDatabase to inject into
	 */
	public MetricInjector(MetricDatabase metricDB) {
		super();
		this.metricDB = metricDB;
	}

	/**
	 * Instantiates a new metric injector.
	 * 
	 * @param metricDB
	 *            the MetricDatabase to inject into
	 * @param capacity
	 *            the size of the task queue
	 */
	public MetricInjector(MetricDatabase metricDB, int capacity) {
		super(capacity);
		this.metricDB = metricDB;
	}

	/**
	 * Checks if the database is connected.
	 * 
	 * @return true, if connected
	 */
	public boolean isConnected() {
		// System.out.println("== MetricInjector.isConnected() = " +
		// metricDB.isConnected() );
		return metricDB.isConnected();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see asl.concurrent.TaskThread#setup()
	 */
	@Override
	protected void setup() {
		// Pre-run logic goes here
	}

	/*
	 * (non-Javadoc)
	 * 
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see asl.concurrent.TaskThread#cleanup()
	 */
	@Override
	protected void cleanup() {
		// Post-run logic goes here
	}

	/**
	 * Add a task to inject into the database.
	 * 
	 * @param results
	 *            the metric result to inject
	 * @throws InterruptedException
	 *             the interrupted exception
	 */
	public void inject(MetricResult results) throws InterruptedException {
		try {
			addTask("INJECT", results);
		} catch (InterruptedException e) {
			throw e;
		}
	}
}
