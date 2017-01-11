package asl.seedscan.scanner;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.MetaServer;
import asl.seedscan.database.MetricDatabase;

public class ScanManager {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.seedscan.scanner.ScanManager.class);

	final MetricDatabase database;
	final MetaServer metaServer;
	final ThreadPoolExecutor threadPool;

	public ScanManager(MetricDatabase database, MetaServer metaServer){
		this.database = database;
		this.metaServer = metaServer;
		
		int threadCount = Runtime.getRuntime().availableProcessors();
		logger.info("Number of Threads to Use = [{}]", threadCount);

		
		BlockingQueue<Runnable> workQueue = new PriorityBlockingQueue<Runnable>();
		
		this.threadPool = new ThreadPoolExecutor(threadCount, threadCount, 10, TimeUnit.MINUTES, workQueue);
	}
	
	/**
	 * Begins the scan process.
	 * This blocks indefinitely while scans are being performed.
	 */
	public void scan(){
		while (true){
			
		}
		
	}
}
