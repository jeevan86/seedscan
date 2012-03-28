package asl.azimuth;

import org.apache.commons.math.linear.BlockRealMatrix;
import org.apache.commons.math.stat.correlation.PearsonsCorrelation;

import ZS.Solve.LMfunc;
import asl.worker.Worker;

/**
 * Implements the solver function for the AzimuthLocator class.
 * 
 * @author Adam Ringler
 */
public class LMAzimuth implements LMfunc
{
    private Worker m_worker = null;
    double[] m_north;
    double[] m_east;
    double[] m_reference;
    int index;
    double priorval;
    double priortheta;
    double errorstep;

    /**
     * Constructor
     * 
     * @param north Data from North channel
     * @param east Data from East channel
     * @param reference Data from Reference channel
     * @param worker The parent worker running the solver
     * @param ind indicate function to perform
     * @param prevval Value of previous iteration
     * @param prevtheta Theta value of previous iteration
     * @param eps Used to determine the error step
     */
    public LMAzimuth(double[] north, double[] east, double[] reference,
            Worker worker, int ind, double prevval, double prevtheta, double eps)
    {
        m_worker = worker;
        m_north = north;
        m_east = east;
        m_reference = reference;
        index = ind;
        priorval = prevval;
        priortheta = prevtheta;
        errorstep = eps * 0.25;
    }

    /**
     * Program using this class calls this routine to have it abort the operation
     * and gracefully halt.
     * 
     * @return false if the seedreader worker class is still running
     */
    public boolean cancelled()
    {
        return m_worker.cancelled();
    }

    /**
     * Gives value of the function at the current point in time
     * 
     * @param x single point domain
     * @param a The parameter that we are estimating
     * @return The value of the function at current point in time
     */
    public double val(double[] x, double[] a)
    {
        /**
         * Since our domain is one point we don't depend on the x value
         */

        /**
         * Setup our theta parameter
         */
        assert a.length == 1;
        double theta = a[0] % 360;
        boolean debug = false;
        double valToReturn = 0.0;

        BlockRealMatrix seisTest1 = new BlockRealMatrix(m_north.length, 1);
        BlockRealMatrix seisTest2 = new BlockRealMatrix(m_east.length, 1);
        seisTest1.setColumn(0, m_north);
        seisTest2.setColumn(0, m_east);

        BlockRealMatrix seisRef1 = new BlockRealMatrix(m_reference.length, 1);
        seisRef1.setColumn(0, m_reference);

        BlockRealMatrix tempMatrix = new BlockRealMatrix(m_reference.length, 1);

        PearsonsCorrelation corrEngine = new PearsonsCorrelation();

        if (theta < 0)
        {
            theta = theta + 360;
        }
        tempMatrix.setColumnMatrix(0, seisTest1);
        tempMatrix.setColumnMatrix(0,
                tempMatrix.scalarMultiply(Math.cos(theta * (Math.PI / 180))));
        tempMatrix.setColumnMatrix(
                0,
                tempMatrix.add(seisTest2.scalarMultiply(-Math.sin(theta
                            * (Math.PI / 180)))));

        if (debug)
        {
            System.out.println("theta = " + theta);
            System.out.println("corr at a[0] = "
                    + corrEngine.correlation(tempMatrix.getColumn(0),
                        seisRef1.getColumn(0)));
        }
        if (index == 0)
        {
            valToReturn = corrEngine.correlation(tempMatrix.getColumn(0),
                    seisRef1.getColumn(0)) - 1.0;
        } else
        {
            valToReturn = corrEngine.correlation(tempMatrix.getColumn(0),
                    seisRef1.getColumn(0))
                - 1.0 + Math.pow(((priorval + 1.0) * (a[0] - priortheta)), 2);
            if (debug)
            {
                System.out.println("Here is our prior value" + priorval);
                System.out.println("Here is our prior theta" + priortheta);
                System.out.println("Here is our damping: "
                        + Math.pow(((priorval + 1.0) * (a[0] - priortheta)), 2));

            }

        }
        if (debug)
        {
            System.out.println("Here is the index: " + index);
            System.out.println("Here is the function value: " + valToReturn);
        }
        return valToReturn;
    }

    /**
     * return the kth component of the gradient df(x,a)/da_k (derivative)
     * 
     * @param x Single domain point
     * @param a Parameter that we are taking the derivative of
     * @param ak In this implementation, ak is a
     * @return The derivative
     */
    public double grad(double[] x, double[] a, int ak)
    {

        /**
         * Since our domain is one point we don't depend on the x value
         */

        /**
         * Setup our theta parameter
         */
        assert a.length == 1;
        double theta = a[0];
        boolean debug = false;
        double valToReturn = 0.0;
        double valToReturnPlusH = 0.0;
        double errh = errorstep;

        BlockRealMatrix seisTest1 = new BlockRealMatrix(m_north.length, 1);
        BlockRealMatrix seisTest2 = new BlockRealMatrix(m_east.length, 1);
        seisTest1.setColumn(0, m_north);
        seisTest2.setColumn(0, m_east);
        BlockRealMatrix seisRef1 = new BlockRealMatrix(m_reference.length, 1);
        seisRef1.setColumn(0, m_reference);

        BlockRealMatrix tempMatrix = new BlockRealMatrix(m_reference.length, 1);
        BlockRealMatrix tempMatrixPlusH = new BlockRealMatrix(m_reference.length, 1);

        PearsonsCorrelation corrEngine = new PearsonsCorrelation();

        theta = a[0] % 360;
        if (theta < 0)
        {
            theta = theta + 360;
        }
        tempMatrix.setColumnMatrix(0, seisTest1);
        tempMatrix.setColumnMatrix(0,
                tempMatrix.scalarMultiply(Math.cos(theta * (Math.PI / 180))));
        tempMatrix.setColumnMatrix(
                0,
                tempMatrix.add(seisTest2.scalarMultiply(-Math.sin(theta
                            * (Math.PI / 180)))));
        tempMatrixPlusH.setColumnMatrix(0, seisTest1);
        tempMatrixPlusH.setColumnMatrix(
                0,
                tempMatrixPlusH.scalarMultiply(Math.cos((theta + errh)
                        * (Math.PI / 180))));
        tempMatrixPlusH.setColumnMatrix(
                0,
                tempMatrixPlusH.add(seisTest2.scalarMultiply(-Math.sin((theta + errh)
                            * (Math.PI / 180)))));

        valToReturnPlusH = corrEngine.correlation(tempMatrixPlusH.getColumn(0),
                seisRef1.getColumn(0));
        valToReturn = corrEngine.correlation(tempMatrix.getColumn(0),
                seisRef1.getColumn(0));

        valToReturn = valToReturnPlusH - valToReturn;

        if (index == 1)
        {
            valToReturn = valToReturn / errh;
        } else
        {
            valToReturn = (valToReturn
                    + Math.pow((priorval * (a[0] + errh - priortheta)), 2) - Math.pow(
                        (priorval * (a[0] - priortheta)), 2)) / errh;

        }

        if (debug)
        {
            System.out.println("Here is the gradient: " + valToReturn);
        }

        return valToReturn;
    }

    /**
     * return initial guess at a[]
     * @return initial guess
     */
    public double[] initial()
    {
        double[] result = { 0 };
        return result;
    }

    /**
     * Return an array[4] of x,a,y,s for a test case; a is the desired final
     * answer.
     * @return an array[4] of x,a,y,s for a test case; a is the desired final
     * answer.
     */
    public Object[] testdata()
    {
        Object[] result = new Object[4];
        // Sources:
        // m_north
        // m_east
        // m_reference
        int npts;
        npts = m_north.length;
        double[][] x = new double[npts][1];
        double[] y = new double[npts];
        double[] s = new double[npts];
        double[] a = new double[6];

        // Adam, make this work.

        result[0] = x;
        result[1] = a;
        result[2] = y;
        result[3] = s;
        return result;
    }
}
