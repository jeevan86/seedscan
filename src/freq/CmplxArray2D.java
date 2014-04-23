package freq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author crotwell
 * Created on Aug 3, 2005
 */
public class CmplxArray2D {
	private static final Logger logger = LoggerFactory.getLogger(freq.CmplxArray2D.class);
	
    public CmplxArray2D(int x, int y) 
    throws IllegalArgumentException
    {
        this(new float[x][y], new float[x][y]);   
    }

    public CmplxArray2D(float[][] real, float[][] imag) 
    throws IllegalArgumentException
    {
        if (real.length != imag.length) {
            throw new IllegalArgumentException("real and imag arrays must have same length: "+real.length+" "+imag.length);
        }
        for(int i = 0; i < real.length; i++) {
            if (real[0].length != real[i].length) {
                throw new IllegalArgumentException("real array must be square: "+i+"  "+real[0].length+" "+real[i].length);
            }
            if (real[i].length != imag[i].length) {
                throw new IllegalArgumentException("real[i] and imag[i] arrays must have same length: "+i+"  "+real[i].length+" "+imag[i].length);
            }
        }
        this.real = real;
        this.imag = imag;
    }
    
    float[][] real, imag;
    
    public int getXLength() {
        return real.length;
    }
    
    public int getYLength() {
        return real[0].length;
    }
    
    public float getReal(int x, int y) {
        return real[x][y];
    }
    
    public float getImag(int x, int y) {
        return imag[x][y];
    }
    
    public void setReal(int x, int y, float val) {
        real[x][y] = val;
    }
    
    public void setImag(int x, int y, float val) {
        imag[x][y] = val;
    }

    public Cmplx get(int x, int y) {
        return new Cmplx(getReal(x, y), getImag(x, y));
    }
    
    public void set(int x, int y, Cmplx val) {
        setReal(x, y, (float)val.real());
        setImag(x, y, (float)val.imag());
    }
    
    public final float mag(int x, int y)
    {
        return (float)Math.sqrt(getReal(x,y) * getReal(x,y) + getImag(x,y) * getImag(x,y));
    }

}
