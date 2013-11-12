/**
 * 
 */
package asl.seedscan.database;

import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.concurrent.Task;
import asl.concurrent.TaskThread;
import asl.seedscan.metrics.MetricResult;

/**
 * @author Joel D. Edwards <jdedwards@usgs.gov>
 *
 */
@SuppressWarnings("cast")
public class MetricReader
extends TaskThread<QueryContext<? extends Object>>
{
    private static final Logger logger = LoggerFactory.getLogger(asl.seedscan.database.MetricInjector.class);
    
	MetricDatabase metricDB;

	/**
	 * 
	 */
	public MetricReader(MetricDatabase metricDB) {
		super();
		this.metricDB = metricDB;
	}

    public boolean isConnected() {
        //System.out.println("== MetricReader.isConnected() = " + metricDB.isConnected() );
        return metricDB.isConnected();
    }

	/**
	 * @param capacity
	 */
	public MetricReader(MetricDatabase metricDB, int capacity) {
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
	protected void performTask(Task<QueryContext<? extends Object>> task) {
		String command = task.getCommand();
		
		logger.info("performTask: command=" + command + " task=" + task);
		
		try {
			if (command.equals("GET-METRIC-DIGEST")) {
				MetricContext<ByteBuffer> context = (MetricContext<ByteBuffer>)task.getData(); 
				MetricValueIdentifier id = context.getId();
				ByteBuffer digest = metricDB.getMetricDigest(id.getDate(), id.getMetricName(), id.getStation());
				QueryResult<ByteBuffer> result = new QueryResult<ByteBuffer>(digest);
				context.getReplyQueue().put(result);
			}
			else if (command.equals("GET-METRIC-VALUE-DIGEST")) {
				MetricContext<ByteBuffer> context = (MetricContext<ByteBuffer>)task.getData(); 
				MetricValueIdentifier id = context.getId();
				ByteBuffer digest = metricDB.getMetricValueDigest(id.getDate(), id.getMetricName(), id.getStation(), id.getChannel());
				QueryResult<ByteBuffer> result = new QueryResult<ByteBuffer>(digest);
				context.getReplyQueue().put(result);
			}
			else if (command.equals("GET-METRIC-VALUE")) {
				MetricContext<Double> context = (MetricContext<Double>)task.getData(); 
				MetricValueIdentifier id = context.getId();
				Double value = metricDB.getMetricValue(id.getDate(), id.getMetricName(), id.getStation(), id.getChannel());
				QueryResult<Double> result = new QueryResult<Double>(value);
				context.getReplyQueue().put(result);
			}
		} catch (InterruptedException ex) {
			logger.warn("Interrupted while attempting to send reply. This may have caused a station thread to hang!");
		}
	}

	/* (non-Javadoc)
	 * @see asl.concurrent.TaskThread#cleanup()
	 */
	@Override
	protected void cleanup() {
		// Post-run logic goes here
	}

	public Double getMetricValue(MetricValueIdentifier id)
	{
		Double value = null;
		try {
			MetricContext<Double> context = new MetricContext<Double>(id);
			addTask("GET-METRIC-VALUE", context);
			value = context.getReplyQueue().take().getResult();
		} catch (InterruptedException ex) {
			logger.warn("Interrupted while awaiting reply from database reader thread.");
		}
		return value;
	}
	
	public ByteBuffer getMetricDigest(MetricValueIdentifier id)
	{
		ByteBuffer digest = null;
		try {
			MetricContext<ByteBuffer> context = new MetricContext<ByteBuffer>(id);
			addTask("GET-METRIC-DIGEST", context);
			digest = context.getReplyQueue().take().getResult();
		} catch (InterruptedException ex) {
			logger.warn("Interrupted while awaiting reply from database reader thread.");
		}
		return digest;
	}
	
    /**
     * Currently getMetricValueDigest() is the only method called (from the MetricData class)
    */
	public ByteBuffer getMetricValueDigest(MetricValueIdentifier id)
	{
		ByteBuffer digest = null;
		try {
			MetricContext<ByteBuffer> context = new MetricContext<ByteBuffer>(id);
			addTask("GET-METRIC-VALUE-DIGEST", context);
			digest = context.getReplyQueue().take().getResult();
		} catch (InterruptedException ex) {
			logger.warn("Interrupted while awaiting reply from database reader thread.");
		}
		return digest;
	}
}
