package asl.concurrent;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Joel D. Edwards  - USGS
 * 
 *         The FallOffQueue is a queue who's elements "fall off the end" when
 *         its maximum capacity is exceeded. This is not thread safe. We never
 *         run the risk of out of order data, but we can fail to add an element
 *         after pushing off the last element. This is a result of extending
 *         LinkedBlockingQueue.
 */
public class FallOffQueue<E> extends LinkedBlockingQueue<E> {
	public static final long serialVersionUID = 1L;

	private int m_capacity;

	/**
	 * Constructor.
	 * 
	 * @param capacity
	 *            The total queue capacity.
	 */
	public FallOffQueue(int capacity) {
		super(capacity);
		m_capacity = capacity;
	}

	/**
	 * Adds an element to the queue.
	 * 
	 * @param e
	 *            Element to be added to the queue.
	 */
	public boolean add(E e) {
		boolean result = true;
		if (m_capacity == 0) {
			result = false;
		} else {
			if (remainingCapacity() == 0) {
				poll();
			}
			result = super.add(e);
		}
		return result;
	}

	/**
	 * Adds all elements from a collection to the Queue.
	 * 
	 * @param c
	 *            Collection whose elements should be added to the queue.
	 */
	public boolean addAll(Collection<? extends E> c) {
		Iterator<? extends E> i = c.iterator();
		while (i.hasNext()) {
			add(i.next());
		}
		return true;
	}

	/**
	 * Attempt to add an element to the queue.
	 * 
	 * @param e
	 *            Element to be added to the queue.
	 * @return True if the element was added; otherwise false.
	 */
	public boolean offer(E e) {
		boolean result = true;
		if (m_capacity == 0) {
			result = false;
		} else {
			if (remainingCapacity() == 0) {
				poll();
			}
			result = super.offer(e);
		}
		return result;
	}

	/**
	 * Add an element to the queue.
	 * 
	 * @param e
	 *            The element being added.
	 */
	public void put(E e) {
		add(e);
	}
}
