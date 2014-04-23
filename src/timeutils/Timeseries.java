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
package timeutils;

import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sac.SacTimeSeries;
import sac.SacHeader;

public class Timeseries
{
	private static final Logger logger = LoggerFactory.getLogger(timeutils.Timeseries.class);
    public static void detrend(double[] timeseries)
    {
       int ndata = timeseries.length;
       double sumx=0.0;
       double sumxx=0.0;
       double sumy=0.0;
       double sumyy=0.0;
       double sumxy=0.0;

       for(int i=0;i<ndata;i++)
       {
          sumx  +=(double)i;
          sumxx +=(double)i*(double)i;
          sumy  +=timeseries[i];
          sumyy +=timeseries[i]*timeseries[i];
          sumxy +=(double)i*timeseries[i];
       }

       double del   = sumxx - sumx*sumx/(double)ndata;
       double slope = sumxy - sumx*sumy/(double)ndata;
       slope /= del;
       double yoff = (sumxx*sumy - sumx*sumxy);
       yoff  /= (double)ndata*del;

       for (int i=0; i<ndata; i++){
         timeseries[i] -= (slope * (double)i + yoff);
       }

       //return timeseries;
    }

    public static void debias(double[] timeseries)
    throws RuntimeException
    {
        double mean=0;
        for (int i=0; i<timeseries.length; i++){
            mean += timeseries[i];
        }
        if (timeseries.length == 0) {
            throw new RuntimeException("Error: debias: timeseries.length=0 --> No data!");
        }
        else {
            mean /= (double)timeseries.length;
            for (int i=0; i<timeseries.length; i++){
                timeseries[i] -= mean;
            }
        }
    }

    public static double costaper(double[] timeseries, double width)
    {
        int n=timeseries.length;
        double ramp = width*(double)n;
        double taper;
        double Wss=0;

        for (int i = 0; i < ramp; i++) {
            taper = 0.5 * ( 1.0 - Math.cos( (double)i * Math.PI / ramp ) );
            timeseries[i] *= taper;
            timeseries[n - i - 1] *= taper;
            Wss += 2.0*taper*taper;
        }   
        Wss += (n - 2.*ramp);   
        return Wss;   
    }


    double taper(double[] timeseries, int type)
    {
       double taper=0;
       double Wss=0;
       int n=timeseries.length;

       switch (type) {
           case 0: // Default = Rectangular window
             for(int i = 0; i < n; i++){
               taper = 1.0;
               timeseries[i] *= taper;
               Wss += taper*taper;
             }
             break;
           case 1: // Hann window
             for(int i = 0; i < n; i++){
               taper = (0.5 - 0.5*Math.cos(2*Math.PI*(double)i/(double)n));
               timeseries[i] *= taper;
               Wss += taper*taper;
             }
             break;
           case 2: // Hamming window 
             for(int i = 0; i < n; i++){
               taper = (0.54 - 0.46*Math.cos(2*Math.PI*(double)i/(double)n));
               timeseries[i] *= taper;
               Wss += taper*taper;
             }
             break;
           case 3: // Blackman window 
           case 4: // Welch window 
             for(int i = 0; i < n; i++){
               taper = ( (double)i - (double)(n-1)/2. ) / ( (double)(n-1)/2. );
               timeseries[i] *= (1.0 - taper*taper);
               Wss += taper*taper;
             }
             break;
           case 5: // Bartlett window (=Triangle window)
             for(int i = 0; i < n; i++){
               taper = ( (double)i - (double)(n-1)/2. ) / ( (double)(n-1)/2. );
               timeseries[i] *= (1.0 - taper);
               Wss += taper*taper;
             }
             break;
       } //end switch
       return Wss;
    }

    public static void timeoutXY(Double[] x, Double[] y, String filename)
    {
        double[] xdouble = new double[x.length];
        double[] ydouble = new double[y.length];
        for (int i=0; i<x.length; i++) xdouble[i] = x[i];
        for (int i=0; i<y.length; i++) ydouble[i] = y[i];
        timeoutXY(xdouble, ydouble, filename);
    }

    public static void timeoutXY(double[] x, double[] y, String filename)
    {
       PrintWriter out=null;
       try {
         out = new PrintWriter(new FileWriter(filename));
         for (int i = 0; i < y.length; i++)
           out.format("%12.6f %12.6f \n", x[i], y[i]);
       } 
       catch (IOException e) {
         //System.err.println("Caught IOException: " +  e.getMessage());
    	   logger.error("Timeseries IOException:", e);
       } 
       finally {
         if (out != null) out.close();
       }
    }

    public static void timeout(double[] timeseries, String filename)
    {
       PrintWriter out=null;
       try {
         out = new PrintWriter(new FileWriter(filename));
         for (int i = 0; i < timeseries.length; i++)
           out.format("%f\n", timeseries[i]);
       } 
       catch (IOException e) {
    	   logger.error("TimeSeries IOException:", e);
       } 
       finally {
         if (out != null) out.close();
       }
    }

    void timeout(float[] timeseries, String filename)
    {
       PrintWriter out=null;
       try {
         out = new PrintWriter(new FileWriter(filename));
         for (int i = 0; i < timeseries.length; i++)
           out.format("%f\n", timeseries[i]);
       } 
       catch (IOException e) {
         //System.err.println("Caught IOException: " +  e.getMessage());
    	   logger.error("Timesries IOException:", e);
       } 
       finally {
         if (out != null) out.close();
       }
    }

//  Numerical Recipes cubic spline interpolation (spline.c and splint.c)
//    Expects arrays with +1 offset: x[1,...,n], etc. - we will pass it arrays
//                   with  0 offset: x[0,1,...,n] and ignore the first points. 

    public static void spline(double[] x, double[] y, int n, double yp1, double ypn, double[] y2) {

        double p,qn,sig,un; 
        p=qn=sig=un=0;

        //u=vector(1,n-1);
        double[] u = new double[n];

        if (yp1 > 0.99e30) y2[1]=u[1]=0.0;
        else {
                y2[1] = -0.5;
                u[1]=(3.0/(x[2]-x[1]))*((y[2]-y[1])/(x[2]-x[1])-yp1);
        }
        for (int i=2;i<=n-1;i++) {
                sig=(x[i]-x[i-1])/(x[i+1]-x[i-1]);
                p=sig*y2[i-1]+2.0;
                y2[i]=(sig-1.0)/p;
                u[i]=(y[i+1]-y[i])/(x[i+1]-x[i]) - (y[i]-y[i-1])/(x[i]-x[i-1]);
                u[i]=(6.0*u[i]/(x[i+1]-x[i-1])-sig*u[i-1])/p;
        }
        if (ypn > 0.99e30)
                qn=un=0.0;
        else {
                qn=0.5;
                un=(3.0/(x[n]-x[n-1]))*(ypn-(y[n]-y[n-1])/(x[n]-x[n-1]));
        }
        y2[n]=(un-qn*u[n-1])/(qn*y2[n-1]+1.0);
        for (int k=n-1;k>=1;k--)
                y2[k]=y2[k]*y2[k+1]+u[k];

    } 

// Same as above (+1 offset arrays) & y=double[1] is used to pass out the interpolated value (y=f(x)).

    public static void splint(double[] xa, double[] ya, double[] y2a, int n, double x, double[] y) {

        int klo,khi,k;
        double h,b,a;

        klo=1;
        khi=n;
        while (khi-klo > 1) {
                k=(khi+klo) >> 1;
                if (xa[k] > x) khi=k;
                else klo=k;
        }
        h=xa[khi]-xa[klo];
        //if (h == 0.0) nrerror("Bad XA input to routine SPLINT");
        if (h == 0.0) System.out.format("Bad XA input to routine SPLINT\n");

        a=(xa[khi]-x)/h;
        b=(x-xa[klo])/h;
        //*y=a*ya[klo]+b*ya[khi]+((a*a*a-a)*y2a[klo]+(b*b*b-b)*y2a[khi])*(h*h)/6.0;
        y[0]=a*ya[klo]+b*ya[khi]+((a*a*a-a)*y2a[klo]+(b*b*b-b)*y2a[khi])*(h*h)/6.0;
    } 


// Interpolate measured Y[X] to the Y[Z]
// We know Y[X] = Y at values of X
// We want Y[Z] = Y interpolated to values of Z

    public static double[] interpolate(double[] X, double[] Y, double[] Z) {

        double[] interpolatedValues = new double[Z.length];

        int n = X.length;

        double[] tmpY = new double[n+1];
        double[] tmpX = new double[n+1];

   // Create offset (+1) arrays to use with Num Recipes interpolation (spline.c)
        for (int i=0; i<n; i++) {
            tmpY[i+1] = Y[i];
            tmpX[i+1] = X[i];
        }
        double[] y2 = new double[n+1];
        spline(tmpX, tmpY, n, 0., 0., y2);

        double[] y = new double[1];

        for (int i=0; i<Z.length; i++){
            splint(tmpX, tmpY, y2, n, Z[i], y);
            interpolatedValues[i] = y[0];
        }
        return interpolatedValues;
    }

    public static void writeSacFile(double[] data, double dt, String filename, String kstnm, String kcmpnm) {
        writeSacFile(data, dt, filename, kstnm, kcmpnm, null);
    }
    public static void writeSacFile(double[] data, double dt, String filename, String kstnm, String kcmpnm, String kevnm) {

        SacHeader hdr = null;
        try {
            hdr = new SacHeader("/Users/mth/mth/Projects/asl/seedscan/resources/sac.file");
        }
        catch (Exception e) {
            //System.out.format("== Timeseries.writeSacFile: Error when attempting to read in default sac hdr [%s]\n", e);
            String message = "== Timeseries.writeSacFile: Error when attempting to read in default sac hdr\n";
            logger.error(message, e);
        }

        hdr.setDelta((float)dt);
        if (kstnm != null) {
            hdr.setKstnm(kstnm);
        }
        if (kcmpnm != null) {
            hdr.setKcmpnm(kcmpnm);
        }
        if (kevnm != null) {
            hdr.setKevnm(kevnm);
        }

        SacTimeSeries sac = new SacTimeSeries(hdr, data);
        try {
            sac.write(filename);
        }
        catch (Exception e) {
        	logger.error("TimeSeries Exception:", e);
        }
    }

    public static void rotate_xy_to_ne(double az1, double az2, double[] x, double[] y, double[] n, double[] e) {

    // INITIALLY: Lets assume the horizontal channels are PERPENDICULAR and use a single azimuth to rotate
    //  We'll check the azimuths and flip signs to put channel1 to +N half and channel 2 to +E

        int quadrant=0;
        double  azimuth=-999;
        int sign1 = 1;
        int sign2 = 1;
        if (az1 >= 0 && az1 < 90) {
            quadrant = 1;
            azimuth  = az1;
        }
        else if (az1 >= 90 && az1 < 180) {
            quadrant = 2;
            azimuth  = az1 - 180;
            sign1    =-1;
        }
        else if (az1 >= 180 && az1 < 270) {
            quadrant = 3;
            azimuth  = az1 - 180;
            sign1    =-1;
        }
        else if (az1 >= 270 && az1 < 360) {
            quadrant = 4;
            azimuth  = az1 - 360;
        }
        else { // ?? 
            System.out.format("== OOPS: MetricData.createRotatedChannels(): Don't know how to rotate az1=%f\n", az1);
        }

        sign2 = 1;
        if (az2 >= 0 && az2 < 180) {
            sign2    = 1;
        }
        else if (az2 >= 180 && az2 < 360) {
            sign2    =-1;
        }
        else { // ?? 
            System.out.format("== OOPS: MetricData.createRotatedChannels(): Don't know how to rotate az2=%f\n", az2);
        }

        double cosAz   = Math.cos( azimuth * Math.PI/180 );
        double sinAz   = Math.sin( azimuth * Math.PI/180 );

        for (int i=0; i<x.length; i++){
            n[i] = sign1 * x[i] * cosAz - sign2 * y[i] * sinAz;
            e[i] = sign1 * x[i] * sinAz + sign2 * y[i] * cosAz;
        }

    } // end rotate_xy_to_ne

}

