package asl.azimuth;

import asl.seedsplitter.ContiguousBlock;
import freq.Cmplx;

/**
 * Extends ContiguousBlock data class to include both the raw and filtered data for the block.  It also implements the filter operation.
 * 
 * @author fshelly
 */
public class FilterBlock extends ContiguousBlock
{
    public  int length =0;
    private int [][]  m_intData = {null, null, null};
    private int [][]  m_filterData = {null, null, null};

    public FilterBlock(long startTime, long endTime, long interval, int [][] data)
    {
        super(startTime, endTime, interval);
        m_intData = data;
        length = m_intData[0].length;

        // Convert int data to double
        for (int i=0; i < m_intData.length; i++)
        {
            double filterIn[] = new double [length];
            for (int j=0; j < length; j++)
            {
                filterIn[j] = m_intData[i][j];
            }

            // Apply low pass filter
            double filterOut[] = lowpassfilter(filterIn, (int)(1000000 / getInterval()));

            m_filterData[i] = new int [length];
            for (int j=0; j < length; j++)
            {
                m_filterData[i][j] = (int)filterOut[j];
            }
        } // loop through each instrument channel
    } // constructor

    /**
     * Constructor which makes a new FilterBlock out of a subset of another one
     * @param superset  The FilterBlock that we want to create a subset of
     * @param iStart    Start index of where we want to grab data
     * @param size      The number of data points that we want to grab
     */
    public FilterBlock(FilterBlock superset, int iStart, int size)
    {
        super(superset.getStartTime()+iStart*superset.getInterval(),
                superset.getStartTime()+(iStart+size)*superset.getInterval(), 
                superset.getInterval());
        length = size;

        // Copy subset of integer data
        for (int i=0; i < 3; i++)
        {
            m_intData[i] = new int[size];
            m_filterData[i] = new int [size];
            System.arraycopy(superset.getIntData(i), iStart, m_intData[i], 0, size);
            System.arraycopy(superset.getFilterData(i), iStart, m_filterData[i], 0, size);
        }
    } // constructor for subset of another FilterBlock

    /**
     * Get the unfiltered values for the specified instrument
     * @param instrument  channel 0-2
     * @return array of unfiltered time series values
     */
    public int [] getIntData(int instrument)
    {
        return m_intData[instrument];
    }

    /**
     * Get the filtered values for the specified instrument
     * @param instrument  channel 0-2
     * @return array of filtered time series values
     */
    public int [] getFilterData(int instrument)
    {
        return m_filterData[instrument];
    }

    /**
     * Implements low pass band filter
     * @param timeseries  The data to be filtered
     * @param sps         Samples per second  
     * @return            The filtered data
     */
    private double[] lowpassfilter(double[] timeseries, int sps)
    {
        float[] timeseriesFilter = new float[timeseries.length];
        double[] timeseriesdouble = new double[timeseries.length];
        double fl = 1 / 500.0;
        double fh = 1 / 100.0;

        for (int ind = 0; ind < timeseries.length; ind++)
        {
            timeseriesFilter[ind] = (float) timeseries[ind];
        }

        Cmplx[] timeseriesC = Cmplx.fft(timeseriesFilter);

        timeseriesC = apply((double) sps, timeseriesC, fl, fh);

        timeseriesFilter = Cmplx.fftInverse(timeseriesC, timeseries.length);

        for (int ind = 0; ind < timeseries.length; ind++)
        {
            timeseriesdouble[ind] = (double) timeseriesFilter[ind];
        }

        return timeseriesdouble;
    }

    /**
     * Implements bandpass filter for lowpassfilter()
     * @param dt  Time step
     * @param cx  Complex number form of time series
     * @param fl  Low corner frequency
     * @param fh  High corner frequency
     * @return  Complex form of filtered time series
     */
    private Cmplx[] apply(double dt, Cmplx[] cx, double fl, double fh)
    {

        int npts = cx.length;
        // double fl = 0.01;
        // double fh = 2.0;
        int npole = 2;
        int numPoles = npole;
        int twopass = 2;
        double TWOPI = Math.PI * 2;
        double PI = Math.PI;

        Cmplx c0 = new Cmplx(0., 0.);
        Cmplx c1 = new Cmplx(1., 0.);

        Cmplx[] sph = new Cmplx[numPoles];
        Cmplx[] spl = new Cmplx[numPoles];

        Cmplx cjw, cph, cpl;
        int nop, nepp, np;
        double wch, wcl, ak, ai, ar, w, dw;
        int i, j;

        if (npole % 2 != 0)
        {
            System.out.println("WARNING - Number of poles not a multiple of 2!");
        }

        nop = npole - 2 * (npole / 2);
        nepp = npole / 2;
        wch = TWOPI * fh;
        wcl = TWOPI * fl;

        np = -1;
        if (nop > 0)
        {
            np = np + 1;
            sph[np] = new Cmplx(1., 0.);
        }
        if (nepp > 0)
        {
            for (i = 0; i < nepp; i++)
            {
                ak = 2. * Math
                    .sin((2. * (double) i + 1.0) * PI / (2. * (double) npole));
                ar = ak * wch / 2.;
                ai = wch * Math.sqrt(4. - ak * ak) / 2.;
                np = np + 1;
                sph[np] = new Cmplx(-ar, -ai);
                np = np + 1;
                sph[np] = new Cmplx(-ar, ai);
            }
        }
        np = -1;
        if (nop > 0)
        {
            np = np + 1;
            spl[np] = new Cmplx(1., 0.);
        }
        if (nepp > 0)
        {
            for (i = 0; i < nepp; i++)
            {
                ak = 2. * Math
                    .sin((2. * (double) i + 1.0) * PI / (2. * (double) npole));
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
        for (i = 1; i < npts / 2 + 1; i++)
        {
            w = w + dw;
            cjw = new Cmplx(0., -w);
            cph = c1;
            cpl = c1;
            for (j = 0; j < npole; j++)
            {
                cph = Cmplx.div(Cmplx.mul(cph, sph[j]), Cmplx.add(sph[j], cjw));
                cpl = Cmplx.div(Cmplx.mul(cpl, cjw), Cmplx.add(spl[j], cjw));
            }
            cx[i] = Cmplx.mul(cx[i], (Cmplx.mul(cph, cpl)).conjg());
            if (twopass == 2)
            {
                cx[i] = Cmplx.mul(cx[i], Cmplx.mul(cph, cpl));
            }
            cx[npts - i] = (cx[i]).conjg();
        }

        return (cx);

    }
} // class FilterBlock
