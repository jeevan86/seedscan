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
package asl.seedsplitter;

import java.util.Comparator;

/**
 * @author Joel D. Edwards <jdedwards@usgs.gov>
 * 
 *         A Comparator for Sequence objects for user with ordered collections.
 */
public class SequenceComparator implements Comparator<Sequence> {
	/**
	 * @param A
	 *            First item to be compared.
	 * @param B
	 *            Second item to be compared.
	 * @return An integer value: 0 if Sequences are considered equal; less than
	 *         1 if Sequence A comes before Sequence B; greater than 1 if
	 *         Sequence B comes before Sequence A.
	 */
	public int compare(Sequence A, Sequence B) {
		int result = 0;
		if (A == null) {
			if (B != null) {
				result = -1;
			}
		} else if (B == null) {
			result = 1;
		} else {
			if (A.startsBefore(B)) {
				result = -1;
			} else if (B.startsBefore(A)) {
				result = 1;
			} else if (B.endsAfter(A)) {
				result = -1;
			} else if (A.endsAfter(B)) {
				result = 1;
			}
		}
		return result;
	}

	/**
	 * @param obj
	 *            Object with which to compare this comparator.
	 * @return A boolean value: true if the two are equal, otherwise false.
	 */
	public boolean equals(Object obj) {
		return super.equals(obj);
	}
}
