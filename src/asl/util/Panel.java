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

package asl.util;

import java.util.ArrayList;

/** 
 * @author Mike Hagerty    <hagertmb@bc.edu>
 */
public class Panel 
{
    private ArrayList<Trace> traces;
    private String panelTitle;

    // constructor(s)
    public Panel(String panelTitle)
    {
        this.panelTitle = panelTitle;
        traces = new ArrayList<Trace>();
    }
    public String getTitle(){
        return panelTitle;
    }

    public void addTrace(Trace trace) {
        traces.add(trace);
    }

    public ArrayList<Trace> getTraces() {
        return traces;
    }

    public int getNumberOfTraces() {
        return traces.size();
    }

// log v. linear -- enum ?
    private class axis {
        private double minRange;
        private double maxRange;
        private String axisLabel;
        public axis(double minRange, double maxRange, String label) {
            this.minRange = minRange;
            this.maxRange = maxRange;
            this.axisLabel= label;
        }
    }

}
