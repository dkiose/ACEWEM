package acewem.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import sim.field.network.Edge;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.network.EdgeDrawInfo2D;
import sim.portrayal.network.SimpleEdgePortrayal2D;
import acewem.initials.Settings;
import acewem.market.ACEWEMmodel;

public class BranchPortrayal extends SimpleEdgePortrayal2D {

	private final ACEWEMmodel market;

	public BranchPortrayal(final ACEWEMmodel market) {
		this.market = market;
		labelFont = new Font("Verdana", Font.BOLD, 12);
	}

	public void draw(final Object object, final Graphics2D graphics,
			final DrawInfo2D info) {

		final EdgeDrawInfo2D ei = (EdgeDrawInfo2D) info;
		final Edge e = (Edge) object;

		final NodePortrayal nodeFrom = (NodePortrayal) e.getFrom();
		final NodePortrayal nodeTo = (NodePortrayal) e.getTo();
		
		

		// our start (x,y), ending (x,y), and midpoint (for drawing the label)
		final int startX = (int) ei.draw.x;
		final int startY = (int) ei.draw.y;
		final int endX = (int) ei.secondPoint.x;
		final int endY = (int) ei.secondPoint.y;
		final int midX = (int) ((ei.draw.x + ei.secondPoint.x) / 2);
		final int midY = (int) ((ei.draw.y + ei.secondPoint.y) / 2);
		
		BranchSerializable b = (BranchSerializable) (e.info);

		final double[] branch = market.getBranchList()[b.getIdNum() - 1];
		final double reactance = branch[Settings.REACTANCE];
		double voltageDiff = 0;
		for (int h = 0; h < Settings.HOURS; h++) {
			voltageDiff = voltageDiff
					+ Math.abs(market.getAngleList(Settings.DA)[nodeTo.getIdNum() - 1][h]
							- market.getAngleList(Settings.DA)[nodeFrom.getIdNum() - 1][h]);
		}
		voltageDiff = voltageDiff / Settings.HOURS;
		final double branchFlow = (1 / reactance) * voltageDiff
				* Settings.BASES;
		float congestionCoeff = (float) (branchFlow / branch[Settings.CAPACITY]);

		if (congestionCoeff < 0) {
			congestionCoeff = 0;
		}

	//	if (congestionCoeff > 1) {
	//		congestionCoeff = 1;
	//	}
		
		//graphics.setStroke(new BasicStroke(1));
		if(Settings.MODELTYPE == Settings.UK) {
			graphics.setStroke(new BasicStroke(1));
		} else {
			graphics.setStroke(new BasicStroke(10));			
		}
			
		if (congestionCoeff > 1) {
			graphics.setColor(Color.black);
		} else {
			graphics.setColor(new Color((float) 1.0, (float) 0.0, 1 - congestionCoeff));
		}
		graphics.drawLine(startX, startY, endX, endY);
		
		
		// draw label in blue
		if(Settings.MODELTYPE != Settings.UK) {
			graphics.setColor(Color.gray);
			graphics.setFont(labelFont);
			// default font for Edge labels
			final String information = Double.toString((double) Math
					.round(congestionCoeff * 100) / 100);// strengthFormat.format(((BranchSerializable)(e.info)).weight);
			 int width = graphics.getFontMetrics().stringWidth("5");
			graphics.drawString(information, midX + 10, midY - 10);
		}
	}
}