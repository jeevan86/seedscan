package asl.metadata;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.List;

import asl.metadata.meta_new.StationMeta;

/**
 * Remote Interface for MetaGenerator
 */
public interface MetaInterface extends Remote {
	/**
	 * Remotely invocable method.
	 * 
	 * @exception RemoteException
	 *                must be declared to be thrown
	 */
	// These are the only methods that can be called remotely on a MetaGenerator
	// object:
	public StationMeta getStationMeta(Station station, LocalDateTime timestamp)
			throws RemoteException, RuntimeException;

	public List<Station> getStationList() throws RemoteException;

	public String getString() throws RemoteException;
}
