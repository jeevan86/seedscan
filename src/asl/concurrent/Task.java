package asl.concurrent;

/**
 * The Class Task. This stores task information execution occurs in in the classes that extend {@link asl.concurrent.TaskThread<T>}
 *
 * @param <T> the generic type
 * 
 * @author Joel D. Edwards  - USGS
 */
public class Task<T> {
	
	/** The command. */
	private String command;
	
	/** The data. */
	private T data;

	/**
	 * Instantiates a new task.
	 *
	 * @param command the command
	 * @param data the data
	 */
	public Task(String command, T data) {
		this.command = command;
		this.data = data;
	}

	/**
	 * Gets the command.
	 *
	 * @return the command
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * Gets the data.
	 *
	 * @return the data
	 */
	public T getData() {
		return data;
	}
}
