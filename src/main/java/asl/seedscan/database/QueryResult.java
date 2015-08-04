package asl.seedscan.database;

public class QueryResult<T> {
	private T result;

	QueryResult(T result) {
		this.result = result;
	}

	public T getResult() {
		return result;
	}
}
