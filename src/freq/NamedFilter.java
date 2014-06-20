package freq;

/**
 * ColoredFilter.java
 * 
 * 
 * Created: Mon Jul 15 10:12:01 2002
 * 
 * @author <a href="mailto:">Charlie Groves</a>
 * @version
 */

public class NamedFilter extends ButterworthFilter {
	public NamedFilter(SeisGramText localeText, double lowFreqCorner,
			double highFreqCorner, int numPoles, int filterType) {
		this(localeText, lowFreqCorner, highFreqCorner, numPoles, filterType,
				"Unnamed Filter");
	}

	public NamedFilter(SeisGramText localeText, double lowFreqCorner,
			double highFreqCorner, int numPoles, int filterType, String name) {
		super(localeText, lowFreqCorner, highFreqCorner, numPoles, filterType);
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setVisibility(boolean b) {
		visible = b;
	}

	public boolean toggleVisibility() {
		visible = !visible;
		return visible;
	}

	public boolean getVisibility() {
		return visible;
	}

	private boolean visible = false;

	private final String name;
}// ColoredFilter
