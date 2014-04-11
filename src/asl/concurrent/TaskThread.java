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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TaskThread<T>
implements Runnable
{
    private static final Logger logger = LoggerFactory.getLogger(asl.concurrent.TaskThread.class);

    private boolean running = false;
    private LinkedBlockingQueue<Task<T>> queue;
    private long timeout = -1;
    private TimeUnit unit;

 // constructor(s)
    public TaskThread()
    {
        queue = new LinkedBlockingQueue<Task<T>>();
    }

    public TaskThread(int capacity)
    {
        queue = new LinkedBlockingQueue<Task<T>>(capacity);
    }

 // timeout
    public void setTimeout(long timeout, TimeUnit unit)
    {
        this.timeout = timeout;
        this.unit = unit;
    }

    public long getTimeout()
    {
        return timeout;
    }

    public TimeUnit getUnit()
    {
        return unit;
    }
    
    public void addTask(String command, T data)
    throws InterruptedException
    {
    	queue.put(new Task<T>(command, data));
    }

 // implements Runnable's run() method 
    public void run()
    {
        setup();
        Task<T> task;
        running = true;
        while (running) {
            try {
                if (timeout < 0) {
                	// Wait indefinitely if timeout is not specified
                    task = queue.take();
                } else {
                	// Otherwise wait for the duration specified
                    task = queue.poll(timeout, unit);
                }

                // If we received a halt command, wrap-up the thread
                if ((task != null) && (task.getCommand() == "HALT")) {
                    logger.debug("Halt requested.");
                    running = false;
                }
                // Otherwise hand off the task
                else {
                    logger.debug(String.format("Performing task %s : %s", task.getCommand(), (task.getData() == null) ? "null" : task.getData()));
                    performTask(task);
                }
            } catch (InterruptedException exception) {
                logger.warn("Caught InterruptedException:", exception);
            }
        }
        cleanup();
    }

 // abstract methods
    protected abstract void setup();

    protected abstract void performTask(Task<T> data);

    protected abstract void cleanup();

 // halt
    public void halt()
    throws InterruptedException
    {
        halt(false);
    }

    public void halt(boolean now)
    throws InterruptedException
    {
        if (now) {
            running = false;
        }
        queue.put(new Task<T>("HALT", null));
    }
}
