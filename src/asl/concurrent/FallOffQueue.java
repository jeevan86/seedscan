/*
 * Copyright 2012, United States Geological Survey or
 * third-party contributors as indicated by the @author tags.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/  >.
 *
 */
package asl.concurrent;

import java.lang.InterruptedException;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author  Joel D. Edwards <jdedwards@usgs.gov>
 * 
 * The FallOffQueue is a queue who's elements "fall off the end" when
 * its maximum capacity is exceeded.
 * This is not thread safe. We never run the risk of out of order data, but
 * we can fail to add an element after pushing off the last element. This
 * is a result of extending LinkedBlockingQueue.
 */
public class FallOffQueue<E>
extends LinkedBlockingQueue<E>
{
    public static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(asl.concurrent.FallOffQueue.class);

    private int m_capacity;

    /**
     * Constructor.
     * 
     * @param capacity 	The total queue capacity.
     */
    public FallOffQueue(int capacity)
    {
        super(capacity);
        m_capacity = capacity;
    }

    /**
     * Adds an element to the queue.
     * 
     * @param e	Element to be added to the queue.
     */
    public boolean add(E e)
    {
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
     * @param c Collection whose elements should be added to the queue.
     */
    public boolean addAll(Collection<? extends E> c)
    {
        Iterator<? extends E> i = c.iterator();
        while (i.hasNext()) {
            add(i.next());
        }
        return true;
    }

    /**
     * Attempt to add an element to the queue.
     * 
     * @param e Element to be added to the queue.
     * @return True if the element was added; otherwise false.
     */
    public boolean offer(E e)
    {
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

    // Just because the parent class has it...
    /**
     * Attempt to add an element to the queue, waiting until there is space 
     * available. This is only here because the parent class has it. Sine we
     * simply push off any elements in our way, there should never be any 
     * need to wait.
     * 
     * @param e 		The element being offered.
     * @param timeout 	The maximum amount of time to wait for the element to be added.
     * @param unit 		The units for the timeout argument.
     * @return True if the element was added, otherwise false.
     * @throws InterruptedException If this thread was interrupted while waiting to add the element.
     */
    public boolean offer(E e, long timeout, TimeUnit unit)
    throws InterruptedException
    {
        boolean result = true;
        if (m_capacity == 0) {
            result = false;
        } else {
            if (remainingCapacity() == 0) {
                poll();
            }
            result = super.offer(e, timeout, unit);
        }
        return result;
    }

    /**
     * Add an element to the queue.
     * 
     * @param e The element being added.
     */
    public void put(E e)
    {
        add(e);
    }
}

