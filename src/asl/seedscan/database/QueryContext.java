package asl.seedscan.database;

import java.util.concurrent.LinkedBlockingQueue;

public class QueryContext<T>
{
	private LinkedBlockingQueue<T> replyQueue;

	public QueryContext() {
		this.replyQueue = new LinkedBlockingQueue<T>();
	}
	
	public LinkedBlockingQueue<T> getReplyQueue()
	{
		return replyQueue;
	}
}
