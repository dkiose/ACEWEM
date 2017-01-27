package acewem.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import acewem.initials.Settings;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.SimplePortrayal2D;

public class NodePortrayal extends SimplePortrayal2D {

	private final int idNum;
	private  double nodeWidth;
	private  double nodeHeight;

	public NodePortrayal(final int idNum) {
		this.idNum = idNum;
		if(Settings.MODELTYPE == Settings.UK){
			nodeWidth = 1.5;
			nodeHeight = 0.5;
		} else {
			nodeWidth = 13;
			nodeHeight = 1.5;
		}
	}

	public void draw(final Object object,
			final Graphics2D graphics,
			final DrawInfo2D info) {

		final double width = info.draw.width * nodeWidth;
		final double height = info.draw.height * nodeHeight;
		final int xNode = (int) (info.draw.x - width / 2.0);
		final int yNode = (int) (info.draw.y - height / 2.0);
		final int wNode = (int) (width);
		final int hNode = (int) (height);
		graphics.setColor(Color.black);
		graphics.fillRect(xNode, yNode, wNode, hNode);
		
		if(Settings.MODELTYPE != Settings.UK) {
			final String information = "Node" + Integer.toString(idNum);
			graphics.drawString(information, xNode, yNode + 25);
		}
	}

	public boolean hitObject(final Object object, final DrawInfo2D range) {
		final double SLOP = 1.0;  // need a little extra diameter to hit circles
		final double width = range.draw.width * nodeWidth;
		final double height = range.draw.height * nodeHeight;

		final Rectangle2D.Double rectangle = new  Rectangle2D.Double(range.draw.x-width/2-SLOP,
				range.draw.y-height/2-SLOP,
				width+SLOP*2,
				height+SLOP*2 );

		return (rectangle.intersects(range.clip.x, range.clip.y, range.clip.width, range.clip.height ));
	}

	public int getIdNum() {
		return idNum;
	}
}