package asl.metadata;

/**
 * This class exists only to force toString, equals, and hashCode on children
 * Key classes.
 * 
 * @author James Holland - USGS
 * 
 */
abstract class Key {
	@Override
	public abstract String toString();

	@Override
	public abstract boolean equals(Object obj);

	@Override
	public abstract int hashCode();
}
