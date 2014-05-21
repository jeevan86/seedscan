package asl.seedscan.database;

import java.util.concurrent.LinkedBlockingQueue;

public class QueryContext<T> {
	private LinkedBlockingQueue<QueryResult<T>> replyQueue;

	public QueryContext() {
		this.replyQueue = new LinkedBlockingQueue<QueryResult<T>>();
	}

	public LinkedBlockingQueue<QueryResult<T>> getReplyQueue() {
		return replyQueue;
	}
}
