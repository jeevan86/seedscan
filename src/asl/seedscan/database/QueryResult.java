package asl.seedscan.database;

public class QueryResult<T> {
	private T result;

	public QueryResult(T result) {
		this.result = result;
	}

	public T getResult() {
		return result;
	}
}
