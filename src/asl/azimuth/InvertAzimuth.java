package asl.azimuth;

import java.util.Arrays;

import org.apache.commons.math.linear.BlockRealMatrix;

import ZS.Solve.LM;
import asl.worker.Worker;
import freq.Cmplx;

/**
 * Locates the best estimate azimuth of the sensor supplying the north+east
 * channels with respect to the channel from the reference sensor.
 *
 * @author  Adam Ringler
 */
public class InvertAzimuth 
{

    public final static int OVERLAP=500;
    public final static int WINDOWLENGTH=2000;
    public final static int SPS = 1;

    /* Time Series */
    private double[] m_north = null;
    private double[] m_east = null;
    private double[] m_reference = null;

    /* Results */
    //Here are all of the thetas that are estimated
    private double[] m_thetas = null;

    //Here are all of the correlations from the above thetas
    private double[] m_correlations = null;
    private double[] m_offsets = {0.0, 0.0, 0.0};
    //Here is the standard deviation from the thetas
    private double   m_standardDeviation = 0.0;
    private boolean  m_successful = false;
    //Here is the mean of the 15% highest correlations
    private double m_bestCorr = 0.0;
    //Here is the mean of the thetas with the 15% highest correlations
    private double m_bestTheta = 0.0;

    /**
     * Constructor.
     *
     * @param   north       North channel time series.
     * @param   east        East channel time series.
     * @param   reference   Reference channel time series.
     */
    public InvertAzimuth(double[] north, double[] east, double[] reference)
    {
        m_north = north;
        m_east = east;
        m_reference = reference;
    }

    /**
     * Locates the best estimate azimuth of the sensor supplying the north+east
     * channels with respect to the channel from the reference sensor.
     *
     * @param   worker  The worker object that acts as parent to this InvertAzimuth instance.
     */
    public void process(Worker worker) 
    throws AzimuthInversionHalted, AzimuthConvergenceException
    {
        LMAzimuth function = null;
        int dataLength = m_reference.length;
        int windowLength = WINDOWLENGTH;
        int overlap = OVERLAP;
        int sps = SPS;
        double preval;
        double eps = 1*10^-6;
        /* Compute the number of windows we should go through from the size and overlap */
        int numberOfWindows = (int)Math.floor((double)dataLength / (double)overlap) - (int)(Math.ceil((double)(windowLength / overlap)));
        /* These are dummy parameters that are taken care of in our function definitions */
        double[][] x = new double[1][1];
        x[0][0] = 1;
        double[] y = new double[1];
        y[0] = 0;
        /* Windowed time series */
        double[] northTest = new double[windowLength];
        double[] eastTest = new double[windowLength];
        double[] refTest = new double[windowLength];
        m_thetas = new double[numberOfWindows];
        m_correlations = new double[numberOfWindows];
        double[] a = {0.0};
        double[] scaleFactor = {1.0};
        boolean[] stepsVaried = {true};
        boolean debug = false;
        double meantheta =0.0;
        double peakval = 0.0;

        if (debug) {
            System.out.println("Number of Windows: " + numberOfWindows);
        }

        peakval = findpeak(m_reference);
        double fl=peakval/2.0;
        double fh =peakval*2.0;
        if(fh > 0.33){
            fh = 0.33;
        }
        if(fl < 1.0/18.0){
            fl=1.0/18.0;
        }
        System.out.println("The low frequency microseism corner is " + fl + " Hz");
        System.out.println("The high frequency microseism corner is " + fh + " Hz");

        int lastProgress = -1;
        int progress = 0;
        /* Lets go through each window and estimate the azimuth */
        for(int index = 0; index < numberOfWindows; index++){
            // Bail if we were cancelled
            if (worker.cancelled()) {
                throw new AzimuthInversionHalted(); 
            }
            /*Display the progress of the process */
            progress = (int)((index * 100.0) / numberOfWindows);
            progress = (progress > 99) ? 99 : progress;
            if (progress > lastProgress) {
                worker.setProgressPercent(progress);
            }

            if(debug) {
                System.out.println("index = " + index);
            }

            // Populate the windows
            System.arraycopy(m_north, overlap * index, northTest, 0, windowLength);
            System.arraycopy(m_east, overlap * index, eastTest, 0, windowLength);
            System.arraycopy(m_reference, overlap * index, refTest, 0, windowLength);

            //Time to remove the mean
            double northMean = mean(northTest);
            double eastMean = mean(eastTest);
            double refMean = mean(refTest);
            for(int popind = 0; popind < windowLength; popind++){
                northTest[popind] -= northMean;
                eastTest[popind] -= eastMean;
                refTest[popind] -= refMean;
            }

            // Apply filter(s) to each time series
            northTest = filterdata(northTest, peakval, sps);
            eastTest = filterdata(eastTest, peakval, sps);
            refTest = filterdata(refTest, peakval, sps);

            if(debug) {
                System.out.println("m_thetas[" + index + "]=" + m_thetas[index]);
            }

            /* Lets define our function */
            double aold = a[0];
            if(index == 0){
                preval = 0.0;
                a[0] = preval;
            }
            else{

                preval = m_correlations[indexOfMax(m_correlations)];
                a[0] = m_thetas[indexOfMax(m_correlations)];

                if(debug){
                    System.out.println("Here is the indexOfMax: " + indexOfMax(m_correlations));
                    System.out.println("Here is the length of the corrs: " + m_correlations.length);

                }

            }

            // Initialize our solver
            function = new LMAzimuth(northTest, eastTest, refTest, worker, index, preval, a[0], eps);

            try {
                if(debug) {
                    System.out.println("Before update a=" + a[0]);
                    System.out.println("Here is the value of our function: " + function.val(x[0],a));
                }
                LM.solve(x, a, y, scaleFactor, stepsVaried, 
                        function, 0.01, eps, 100, 0);
                m_thetas[index]= a[0] % 360;

                if(m_thetas[index] < 0) {
                    m_thetas[index] = m_thetas[index] + 360.0;
                }
                if(debug){
                    System.out.println("m_thetas[" + index + "]=" + m_thetas[index]);
                }		
                if(a[0] < 0 ){
                    a[0] = a[0] + 360;
                }

                function = new LMAzimuth(northTest, eastTest, refTest, worker, 0, preval, a[0], eps);
                m_correlations[index] = function.val(x[0],a) + 1;
                if(debug){
                    System.out.println("m_correlation[" + index + "]=" + m_correlations[index]);
                }
                if(debug) {

                    System.out.println("x=" + x[0][0]);
                    System.out.println("a=" + m_thetas[index]);
                    System.out.println("y=" + y[0]);
                }
            } catch (Exception e) {
                throw new AzimuthConvergenceException();
            }

        }

        meantheta = mean(m_thetas);

        //Lets find the best theta and the best correlations and get the mean of them
        int numOfBest = (int)Math.ceil((double)m_thetas.length*0.15);
        double[] corrs_temp = new double[m_thetas.length];
        double[] good_corrs = new double[numOfBest];
        double[] good_thetas = new double[numOfBest];
        double[] thetas_temp = new double[m_thetas.length];
        int tempind = 0;
        for(int ind = 0; ind < m_thetas.length; ind++){
            corrs_temp[ind] = m_correlations[ind];
            thetas_temp[ind] = m_thetas[ind];

        }
        double shiftby = m_thetas[0] + 45.0;
        for(int ind = 0; ind < numOfBest; ind++){
            tempind = indexOfMax(corrs_temp);
            good_corrs[ind]=corrs_temp[tempind];
            corrs_temp[tempind] = 0.0;
            good_thetas[ind]=m_thetas[tempind] + shiftby;
            good_thetas[ind]=good_thetas[ind] % 360;
        }

        m_bestTheta= mean(good_thetas) - shiftby;
        for(int ind=0; ind < numOfBest; ind++){
            good_thetas[ind]=good_thetas[ind] - shiftby;
        }	

        m_bestCorr= mean(good_corrs);
        for(int index = 0; index < numOfBest; index++){
            m_standardDeviation += Math.pow(good_thetas[index] - m_bestTheta,2);
        }
        m_standardDeviation = (1.0/((double)good_thetas.length))*m_standardDeviation;
        m_standardDeviation = Math.sqrt(m_standardDeviation);

        System.out.println("Here is your best theta " + m_bestTheta);
        System.out.println("Here is the mean of your 15% best correlations " + m_bestCorr);

        // Bail if we were cancelled before completing
        if (worker.cancelled()) {
            throw new AzimuthInversionHalted(); 
        }

        m_successful = true;
    }


    /**
     * Reverses the order of an Array
     * @param list  The array to be reversed
     */
    private void reverseOrder(double[] list)
    {
        int length = list.length;
        int index = 0;
        double temp = 0;
        for (int i=0; i < (length / 2); i++) {
            index = length - i;
            temp = list[i];
            list[i] = list[index];
            list[index] = temp;
        }
    }

    /**
     * Scales everything to 0-1
     * @param series the series to be scaled
     */
    private void scaleSeries(BlockRealMatrix series)
    {
        double[] origSeries = series.getColumn(1);
        int seriesLength = origSeries.length;
        double[] tempSeries = new double[seriesLength];
        for (int i=0; i < seriesLength; i++) {
            tempSeries[i] = Math.abs(origSeries[i]);
        }
        Arrays.sort(tempSeries);
        reverseOrder(tempSeries);
        double maxValue = tempSeries[indexOfMax(tempSeries)];
        series.scalarMultiply(1/maxValue);
    }

    /**
     * Finds the index of the max value of inputarray
     * @param inputarray  The array to find the max value of
     * @return The index of the max value of inputarray
     */
    private int indexOfMax(double[] inputarray)
    {
        //Here we find the index of the maximum element in an array
        int maxindex = -1;
        double maxvalue = 0;
        maxvalue = inputarray[0];
        maxindex = 0;
        for(int i=0; i < inputarray.length; i++){
            if(inputarray[i] > maxvalue){
                maxvalue = inputarray[i];
                maxindex = i;
            }
        }
        return maxindex;
    }

    /**
     * Find the mean value of inputarray
     * @param inputarray
     * @return The mean value
     */
    private double mean(double[] inputarray)
    {
        int len = inputarray.length;
        double val = 0;
        for(int i = 0; i < len; i++){
            val = val + inputarray[i];
        }
        val = val / Double.parseDouble(Integer.toString(len));
        return val;
    }

    /**
     * Finds the frequency at which the microseismic peak occurs
     * @param timeseries  The data to find the peak of
     * @return The frequency
     */
    private double findpeak(double[] timeseries)
    {
        double   peakval         = 1.0/4.0;
        double   curseriesMean   = 0.0;		
        int      numOfWins       = (int)Math.floor((double)timeseries.length/(double)256) - 1;
        double[] curseries       = new double[512];
        float[]  curseriesPrefft = new float[512];
        Cmplx[]  curseriesfft    = new Cmplx[512];
        double[] psd             = new double[512];
        double[] freq            = new double[512];
        double[] psdMic          = new double[119];
        double[] freqMic         = new double[119];
        int      psdMicInd       = 0;
        boolean  debug           = false;

        for(int ind = 0; ind < 512; ind++){
            psd[ind]=0.0;
        }

        for(int ind = 0; ind < numOfWins; ind++){

            System.arraycopy(timeseries, 256 * ind, curseries, 0, 512);

            curseriesMean = mean(curseries);
            for(int curind = 0; curind < 512; curind++){
                curseries[curind] -= mean(curseries);
                if(debug){
                    System.out.println("Here is curseries: " + curseries[curind]);
                }
            }

            curseries = taper(curseries);
            for(int curind = 0; curind < 512; curind++){
                curseriesPrefft[curind] = (float)curseries[curind];
                if(debug){
                    System.out.println("Prefft time series: " + curseriesPrefft[curind]);
                }
            }

            curseriesfft = Cmplx.fft(curseriesPrefft);
            for(int curind = 0; curind < 512; curind++){
                psd[curind]= psd[curind] +  Math.pow(curseriesfft[curind].mag(),2);
            }
        }

        for(int curind = 0; curind < 512; curind++){
            psd[curind] = psd[curind]/(double)numOfWins;
            freq[curind] = ((double)SPS)*(double)curind/((double)512);

        }

        //For 1 SPS we will just use 52 to 170 which is from 3 to 10 seconds
        for(int curind = 52; curind < 170; curind++){
            psdMic[psdMicInd] = 10.0*Math.log(psd[curind]*Math.pow(2.0*freq[curind]*Math.PI,2))/Math.log(10);
            freqMic[psdMicInd] = freq[curind]; 

            if(debug){
                System.out.println("psdMic = " + psd[psdMicInd] + " freq " + freqMic[psdMicInd]);
            } 

            psdMicInd = psdMicInd + 1;
        }

        peakval = freqMic[indexOfMax(psdMic)];
        System.out.println("Here is the peak value: " + 1.0/peakval);
        return peakval;
    }

    /**
     * Implements a cosine taper
     * @param timeseries  Time series data
     * @return The tapered series
     */
    private double[] taper(double[] timeseries)
    {
        double[] timeseriestaper = new double[timeseries.length];

        for(int ind = 0; ind < timeseries.length; ind++){
            timeseriestaper[ind]= timeseries[ind]*(0.54 - 0.46*Math.cos(2*Math.PI*(double)ind/(double)timeseries.length));
        }

        return timeseriestaper;
    }

    /**
     * Band pass filter around the microseismic peak
     * @param timeseries  Input data
     * @param peakval     The peak value frequency
     * @param sps         Samples per second
     * @return filtered time series
     */
    private double[] filterdata(double[] timeseries, double peakval, int sps)
    {
        float[] timeseriesFilter = new float[timeseries.length];
        double[] timeseriesdouble = new double[timeseries.length];
        double fh=2.0*peakval;
        double fl=peakval/2.0;
        if(fh > 0.33){
            fh = 0.33;
        }
        if(fl < 1.0/18.0){
            fl = 1.0/18.0;
        }

        for(int ind = 0; ind < timeseries.length; ind++){
            timeseriesFilter[ind] = (float)timeseries[ind];
        }

        Cmplx[] timeseriesC = Cmplx.fft(timeseriesFilter);
        timeseriesC = apply((double)sps, timeseriesC, fl,fh);
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
    private Cmplx[] apply(double dt, Cmplx[] cx, double fl, double fh)
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
            System.out.println("WARNING - Number of poles not a multiple of 2!");
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


    /**
     * gets the array of correlation values
     * @return correlation array
     */
    public double[] getCorrelations()
    {
        return m_correlations;
    }

    /**
     * Gets the theta/offset array
     * @return theta array
     */
    public double[] getThetas()
    {
        return m_thetas;
    }

    /**
     * Obsolete
     * @return m_offsets
     */
    public double[] getOffsets()
    {
        return m_offsets;
    }

    /**
     * Get theta with highest correlation
     * @return theta with highest correlation
     */
    public double getBestTheta()
    {
        return m_bestTheta;
    }

    /**
     * Get Mean of the Best Correlation values
     * @return Mean of the Best Correlation values
     */
    public double getMeanOfBestCorrelations()
    {
        return m_bestCorr;
    }

    /**
     * Get the Standard Deviation in the theta array
     * @return Standard Deviation in the theta array
     */
    public double getStandardDeviation()
    {
        return m_standardDeviation;
    }

    /**
     * If solution succeeded
     * @return true on successful solution
     */
    public boolean getSuccess()
    {
        return m_successful;
    }
}

