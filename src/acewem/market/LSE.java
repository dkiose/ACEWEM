package acewem.market;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Random;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.SimplePortrayal2D;
import acewem.initials.Settings;

public class LSE extends SimplePortrayal2D implements Steppable {
	private static final long serialVersionUID = 1L;
	private final double[][] load;
	private final double[] lseData;
	private Random random;

	// C-O-N-S-T-R-U-C-T-O-R--------------------------------------------------------------------------------
	/**
	 * does the LSE needs to know the market that it belongs to?
	 * 
	 * @param id
	 * @param node
	 * @param electricitydemand
	 */
	// public LSE(String id, int node, Hashtable<String, Double>
	// electricitydemand)
	public LSE(final double[] initData, final double[][] load) {

		lseData = initData;
		this.load = load;

		 if (Settings.DEMAND_SHOCK) {
			 random = new Random();
		 }
	}

	public double[] getElectricityDemand(final int marketDay) {

		int fixedDay = -1;
		if (Settings.DEMAND_FIXED) {
			fixedDay = 0;
		} else {
			fixedDay = marketDay;
		}
		
		if (Settings.DEMAND_SHOCK && getId() == 4) {
			final double[] loadDay = new double[Settings.HOURS];
			for (int i = 0; i < Settings.HOURS; i++) {
				loadDay[i] = load[fixedDay][i]
						*(1.0 + 0.2*random.nextDouble());
			}
			return loadDay;
		} else {
			return load[fixedDay];
		}
	}

	// -GET-&-SET-METHODS------------------------------------------------------------------------------

	public double getId() {
		return lseData[Settings.LSE];
	}

	public double getNode() {
		return lseData[Settings.LSE_AT_NODE];
	}

	public void step(final SimState state) {

	}
	
	public void draw(final Object object, final Graphics2D graphics,
			final DrawInfo2D info) {
		double width = info.draw.width * Settings.AGENT_WIDTH;
		double height = info.draw.height * Settings.AGENT_HEIGHT;
		final int x = (int) (info.draw.x - width / 2.0);
		final int y = (int) (info.draw.y - height / 2.0);
		final int w = (int) (width);
		final int h = (int) (height);
		
		graphics.setColor(Color.cyan);
		graphics.fillRect(x, y, w, h);
		
		if (Settings.MODELTYPE != Settings.UK) {
			final String label = "L" + Integer.toString((int) getId()); 
			graphics.setColor(Color.black);
			graphics.drawString(label, x, (int) (y + 4 * Settings.AGENT_WIDTH));
		}
	}
	
	 public boolean hitObject(Object object, DrawInfo2D range) {
     final double SLOP = 1.0;  // need a little extra diameter to hit circles
     final double width = range.draw.width * Settings.AGENT_WIDTH;
     final double height = range.draw.height * Settings.AGENT_HEIGHT;
     
     Rectangle2D.Double rectangle = new  Rectangle2D.Double(range.draw.x-width/2-SLOP, 
             range.draw.y-height/2-SLOP, 
             width+SLOP*2,
             height+SLOP*2 );
     return ( rectangle.intersects( range.clip.x, range.clip.y, range.clip.width, range.clip.height ) );
     }

}
