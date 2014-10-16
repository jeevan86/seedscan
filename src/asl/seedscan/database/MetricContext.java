package asl.seedscan.database;

public class MetricContext<T> extends QueryContext<T> {
	private MetricValueIdentifier id;

	MetricContext(MetricValueIdentifier id) {
		super();
		this.id = id;
	}

	public MetricValueIdentifier getId() {
		return id;
	}
}
