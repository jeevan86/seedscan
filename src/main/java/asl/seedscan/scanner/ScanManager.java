package asl.seedscan.scanner;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.MetaGenerator;
import asl.seedscan.database.MetricDatabase;
import asl.seedscan.scanner.scanworker.RetrieveScan;
import asl.seedscan.scanner.scanworker.ScanWorker;
import asl.util.Logging;

public class ScanManager {
	private static final Logger logger = LoggerFactory.getLogger(asl.seedscan.scanner.ScanManager.class);

	public final MetricDatabase database;
	public final MetaGenerator metaGenerator;

	/**
	 * Pool of threads for scanning. Must remain private to restrict how it is
	 * used. Because it is implemented with a PriorityBlockingQueue its tasks
	 * must also implement Comparable.
	 */
	private final ThreadPoolExecutor threadPool;
	
	private final BlockingQueue<Runnable> workQueue;

	public ScanManager(MetricDatabase database, MetaGenerator metaGenerator) {
		this.database = database;
		this.metaGenerator = metaGenerator;

		int threadCount = Runtime.getRuntime().availableProcessors();
		logger.info("Number of Threads to Use = [{}]", threadCount);

		 workQueue = new PriorityBlockingQueue<>();

		this.threadPool = new ThreadPoolExecutor(threadCount, threadCount, 10, TimeUnit.MINUTES, workQueue);
	}

	/**
	 * Begins the scan process. This blocks indefinitely while scans are being
	 * performed.
	 */
	public void scan() {
		//Start full set of threads going.
		for(int i = 0; i <=this.threadPool.getCorePoolSize(); i++){
			threadPool.execute(new RetrieveScan(this));
		}
		
		while (true) {
			if(workQueue.isEmpty()){
				//Since queue is empty add a retrieving scan.
				threadPool.execute(new RetrieveScan(this));
			}
			try {
				/*We want to wait a little bit so as to not overload the db with getScan requests.*/
				Thread.sleep(300000);
				/*Update incase available processors changes.
				 * This is not a constant, but can vary with the OS according to Oracle Javadoc.
				 */
				if(Runtime.getRuntime().availableProcessors() > this.threadPool.getCorePoolSize()){
					this.threadPool.setCorePoolSize(Runtime.getRuntime().availableProcessors());
				}
			} catch (InterruptedException e) {
				//Shouldn't happen, as nothing on our end signals an Interrupt
				String msg = Logging.exceptionToString(e);
				logger.error(msg);
				database.insertError(msg);
			}
		}

	}

	public void addTask(ScanWorker task) {
		/* We cannot use .submit() because of issues when wrapping the Runnable
		 * into a FutureTask. Our PriorityQueue requires our task to be
		 * Comparable while FutureTasks are not.
		 */
		threadPool.execute(task);
	}
}
