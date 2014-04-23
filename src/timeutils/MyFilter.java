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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freq.*;
import sac.SacTimeSeries;
import sac.SacHeader;

public class MyFilter
{
	private static final Logger logger = LoggerFactory.getLogger(timeutils.MyFilter.class);
    public static void bandpass(SacTimeSeries sacSeries, double f1, double f2, double f3, double f4) {
        SacHeader hdr = sacSeries.getHeader();
        double delta  = (double)hdr.getDelta();
        float[] fdata = sacSeries.getY();
        double[] data = convertFloatsToDoubles(fdata);
        bandpass(data, delta, f1, f2, f3, f4);
        fdata = convertDoublesToFloats(data);
        sacSeries.setY( fdata );
    }

    public static void bandpass(double[] timeseries, double delta, double f1, double f2, double f3, double f4) {

        if (!(f1 < f2 && f2 < f3 && f3 < f4)) {
            String msg = String.format("bandpass: Error: invalid freq: range: [%f-%f ----- %f-%f]", f1, f2, f3, f4);
            //System.out.println(msg);
            logger.error(msg);
            return;
        }
        if (delta <= 0) {
            String msg = String.format("MyFilter bandpass: Error: invalid delta dt: [%f]", delta);
            //System.out.println(msg);
            logger.error(msg);
            return;
        }

        int ndata    = timeseries.length; 

     // Find smallest power of 2 >= ndata:
        int nfft=1;
        while (nfft < ndata) nfft = (nfft << 1);

        double df = 1./(nfft*delta);

     // We are going to do an nfft point FFT which will return 
     //   nfft/2+1 +ve frequencies (including  DC + Nyq)
        int nf=nfft/2 + 1;

        double[] data = new double[ndata];
        for (int i=0; i<ndata; i++){
            data[i] = timeseries[i];
        }
        Timeseries.detrend(data);
        Timeseries.debias(data);
        Timeseries.costaper(data,.01);

        // fft2 returns just the (nf = nfft/2 + 1) positive frequencies
        Cmplx[] xfft = Cmplx.fft2(data);

        double fNyq = (double)(nf-1)*df;

        if (f4 > fNyq) {
            f4 = fNyq;
        }
        int k1=(int)(f1/df); int k2=(int)(f2/df);
        int k3=(int)(f3/df); int k4=(int)(f4/df);

        for(int k = 0; k < nf; k++){
            double taper = bpass(k,k1,k2,k3,k4);
            xfft[k] = Cmplx.mul(xfft[k],taper);   // Bandpass
        }

        Cmplx[] cfft = new Cmplx[nfft];
        cfft[0]    = new Cmplx(0.0, 0.0);   // DC
        cfft[nf-1] = xfft[nf-1];            // Nyq
        for(int k = 1; k < nf-1; k++){      // Reflect spec about the Nyquist to get -ve freqs
            cfft[k]        = xfft[k];
            cfft[2*nf-2-k] = xfft[k].conjg();
        }
        float[] foo = Cmplx.fftInverse(cfft, ndata);
        for (int i=0; i<ndata; i++){
            timeseries[i] = (double)foo[i];
        }

    }

/**
 *  Return val of cos taper at point n
 *  where taper is flat between n2 --- n3
 *  and applies cos between n1-n2 and n3-n4  (i.e., it is zero for n<=n1 and n>= n4)
 */
    private static double bpass(int n,int n1,int n2,int n3,int n4) {
             if (n<=n1 || n>=n4) return(0.);
        else if (n>=n2 && n<=n3) return(1.);
        else if (n>n1  && n<n2 ) return( .5*(1-Math.cos(Math.PI*(n-n1)/(n2-n1))) );
        else if (n>n3  && n<n4 ) return( .5*(1-Math.cos(Math.PI*(n4-n)/(n4-n3))) );
        else return(-9999999.);
    }

    public static double[] convertFloatsToDoubles(float[] input)
    {
        if (input == null)
        {
            return null; // Or throw an exception - your choice
        }
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++)
        {
            output[i] = (double)input[i];
        }
        return output;
    }

    public static float[] convertDoublesToFloats(double[] input)
    {
        if (input == null)
        {
            return null; // Or throw an exception - your choice
        }
        float[] output = new float[input.length];
        for (int i = 0; i < input.length; i++)
        {
            output[i] = (float)input[i];
        }
        return output;
    }


    /**
     * Band pass filter around the microseismic peak
     * @param timeseries  Input data
     * @param peakval     The peak value frequency
     * @param sps         Samples per second
     * @return filtered time series
     */
    public static double[] filterdata(double[] timeseries, double delta, double fl, double fh)
    {
        float[] timeseriesFilter = new float[timeseries.length];
        double[] timeseriesdouble = new double[timeseries.length];

        for(int ind = 0; ind < timeseries.length; ind++){
            timeseriesFilter[ind] = (float)timeseries[ind];
        }

        Cmplx[] timeseriesC = Cmplx.fft(timeseriesFilter);
        timeseriesC = apply(delta, timeseriesC, fl,fh);
        timeseriesFilter = Cmplx.fftInverse(timeseriesC, timeseries.length);

        for(int ind = 0; ind < timeseries.length; ind++){
            timeseriesdouble[ind] = (double)timeseriesFilter[ind];
        }
        return timeseriesdouble; 
    }

    /**
     * Implementation for filterdata function
     * @param dt  Time step
     * @param cx  Complex number form of time series
     * @param fl  Low corner frequency
     * @param fh  High corner frequency
     * @return  Complex form of filtered time series
     */
    private static Cmplx[] apply(double dt, Cmplx[] cx, double fl, double fh)
    {
        int npts = cx.length;
        int npole = 6;
        int numPoles = npole;
        int twopass = 2; 
        double TWOPI = Math.PI*2;
        double PI = Math.PI;

        Cmplx c0 = new Cmplx(0., 0.);
        Cmplx c1 = new Cmplx(1., 0.);		

        Cmplx[] sph = new Cmplx[numPoles];
        Cmplx[] spl = new Cmplx[numPoles];

        Cmplx cjw, cph, cpl;
        int nop, nepp, np;
        double wch, wcl, ak, ai, ar, w, dw;
        int i, j;

        if (npole % 2 != 0) {
            //System.out.println("WARNING - Number of poles not a multiple of 2!");
        	logger.warn("WARNING - Number of poles not a multiple of 2!");
        }

        nop = npole - 2 * (npole / 2);
        nepp = npole / 2;
        wch = TWOPI * fh;
        wcl = TWOPI * fl;

        np = -1;
        if (nop > 0) {
            np = np + 1;
            sph[np] = new Cmplx(1., 0.);
        }
        if (nepp > 0) {
            for (i = 0; i < nepp; i++) {
                ak = 2. * Math.sin((2. * (double) i + 1.0) * PI / (2. * (double) npole));
                ar =ak * wch / 2.;
                ai = wch * Math.sqrt(4. - ak * ak) / 2.;
                np = np + 1;
                sph[np] = new Cmplx(-ar, -ai);
                np = np + 1;
                sph[np] = new Cmplx(-ar, ai);
            }
        }
        np = -1;
        if (nop > 0) {
            np = np + 1;
            spl[np] = new Cmplx(1., 0.);
        }
        if (nepp > 0) {
            for (i = 0; i < nepp; i++) {
                ak = 2. * Math.sin((2. * (double) i + 1.0) * PI / (2. * (double) npole));
                ar = ak * wcl / 2.;
                ai = wcl * Math.sqrt(4. - ak * ak) / 2.;
                np = np + 1;
                spl[np] = new Cmplx(-ar, -ai);
                np = np + 1;
                spl[np] = new Cmplx(-ar, ai);
            }
        }

        cx[0] = c0;
        dw = TWOPI / ((double) npts * dt);
        w = 0.;
        for (i = 1; i < npts/2 + 1; i++) {
            w = w + dw;
            cjw = new Cmplx(0., -w);
            cph = c1;
            cpl = c1;
            for (j = 0; j < npole; j++) {
                cph = Cmplx.div(Cmplx.mul(cph, sph[j]), Cmplx.add(sph[j], cjw));
                cpl = Cmplx.div(Cmplx.mul(cpl, cjw), Cmplx.add(spl[j], cjw));
            }
            cx[i] = Cmplx.mul(cx[i], (Cmplx.mul(cph, cpl)).conjg());
            if (twopass == 2) {
                cx[i]= Cmplx.mul(cx[i], Cmplx.mul(cph, cpl));
            }
            cx[npts - i] = (cx[i]).conjg();
        }

        return(cx);
    }


}

