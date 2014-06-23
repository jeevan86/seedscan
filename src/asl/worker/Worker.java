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
package asl.worker;

/**
 * @author Joel D. Edwards <jdedwards@usgs.gov>
 * 
 *         Interface intended to be associated with a SwingWorker, providing
 *         access to its internal progress methods via implementations of the
 *         specified methods.
 */
public interface Worker {
	/**
	 * Sets the progress percent of this Worker.
	 * 
	 * @param progress
	 *            An integer value representing the progress percent of this
	 *            Worker.
	 */
	public void setProgressPercent(int progress);

	/**
	 * Returns the progress percent of this Worker.
	 * 
	 * @return An integer value representing the progress percent of this
	 *         worker.
	 */
	public int getProgressPercent();

	/**
	 * Tells this Worker to halt its processing operation.
	 */
	public void cancel();

	/**
	 * Reports whether this Worker has been cancelled.
	 * 
	 * @return A boolean value: true if this Worker has been cancelled,
	 *         otherwise false.
	 */
	public boolean cancelled();

	/**
	 * Query to see if the class process was successfully completed
	 * 
	 * @return A boolean value: true if the process completed succesfully,
	 *         otherwise false.
	 */
	public boolean getSuccess();
}
