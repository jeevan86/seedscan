/*
 * Copyright 2011, United States Geological Survey or
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
package asl.azimuth;


import java.awt.Image;
import javax.swing.ImageIcon;                                                                  

/**
 * Helps implement icons in buttons
 * 
 * @author Joel Edwards
 *
 */
public class Resources
{
    /**
     * Finds the Icon name associated with name and scales it to the desired size
     * @param name    Name of icon
     * @param height  Desired height
     * @param width   Desired width
     * @return        new ImageIcon of the desired icon
     */
    public static ImageIcon getAsImageIcon(String name, int height, int width)
    {
        return new ImageIcon((new ImageIcon(ClassLoader.getSystemResource(name))).getImage().getScaledInstance(height, width, Image.SCALE_SMOOTH));
    }
}

