package acewem.gui;

//import gamlss.utilities.Controls;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Random;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.rosuda.REngine.Rserve.RserveException;

import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.continuous.Continuous2D;
import sim.field.network.Network;
import sim.portrayal.Inspector;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.network.NetworkPortrayal2D;
import sim.portrayal.network.SpatialNetwork2D;
import sim.util.Bag;
import sim.util.Double2D;
import acewem.initials.Settings;
import acewem.market.ACEWEMmodel;
import acewem.market.GenCo;
import acewem.market.LSE;
import acewem.utilities.statistics.graphics.MultiTimeSeriesChartGenerator;

/**
 * 
 * @author
 * 
 */
public class ACEWEMGUI extends GUIState {

	Box box;

	// -------------------------------------------------------------------------------------------------------
	public static Object getInfo() {
		return "<H1> <CENTER>  The ACEWEM Project</CENTER></H1>"
				+ "<CENTER>Agent-based Computational Economics of the Wholesale Electricity Market</CENTER>"
				+ "<CENTER><br>The ACEWEM framework is an agent-based computational laboratory designed for the systematic study of the restructured wholesale electricity markets operating over AC transmission grid subject to congestion. In the ACEWEM framework, electricity traders have learning capabilities permitting them to evolve their trading strategies over time.</CENTER> "
				+ "<CENTER><br><br>Our longer-run goal for the ACEWEM framework is a computational laboratory that rings true to industry participants and policy makers and that can be used as a research and training tool for long-term planning and investment processes in the wholesale electricity markets.</CENTER>"
				+ "<p><b>Authors:</b> Daniil Kiose* & Dr. Vlasios Voudouris**</p>"
		//		+ "<p><b>Authors:</b> Daniil Kiose, Dr. Vlasios Voudouris, Mikis Stasinopoulos</p>"
		//		+ "<br>*daniil.kiose@abm-analytics.com </b>"
		//		+ "<br>**vlasios.voudouris@abm-analytics.com</b>"
		//		+ "<br> </b>"
		//		+ "<br><b><CENTER>ABM Analytics</CENTER></b>"
		//		+ "<b><CENTER>www.abm-analytics.com</CENTER></b>"+
		//		"<b><CENTER>ABM Analytics: Suite 17 125, 145-157 St John Street, EC1V 4PW, London, UK, Tel: +44 (0) 77388 255 40 & 22, rue Pierre Fontaine, 75009, Paris, France, Tel:  +33 (0) 143 877 184</CENTER></b>";
				+ "<br><b><CENTER>ACEWEM Demo Version for Teaching and Research</CENTER></b>"
		
		+ "<br>*dkiose@escpeurope.eu</b>"
		+ "<br>**vvoudouris@escpeurope.eu</b>"
		+ "<br> </b>"
		+ "<br><b><CENTER>RCEM at ESCP Europe Business School</CENTER></b>"
		+ "<b><CENTER>www.escpeurope.eu</CENTER></b>"+
		"<b><CENTER>527 Finchley Rd, London NW3 7BG, Tel: 020 7443 8800</CENTER></b>"
	;
	}

	// -------------------------------------------------------------------------------------------------------
	public static String getName() {
		return "ACEWEM for Wholesale Electricity Markets";
	}

	/** Object of ACEWEMmodel */
	private final ACEWEMmodel market;
	private Console console;


	// C-O-N-S-T-R-U-C-T-O-R-------------------------------------------------------
	/**
	 * 
	 * @param state
	 *            - SimState
	 */
	public ACEWEMGUI(final SimState state) {
		super(state);
		market = (ACEWEMmodel) state;

		final Console c = new Console(this);
		c.setWhenShouldEndTime(Settings.DAYS_MAX);

		// Deactivate play, pause and stop buttons
		box = (Box) c.getContentPane().getComponent(0);
		box.getComponent(0).setEnabled(false);
		box.getComponent(1).setEnabled(false);
		box.getComponent(2).setEnabled(false);
		box.getComponent(3).setEnabled(false);

		final JButton button = new JButton("Set Model");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				market.initialiseMarket();
				updater();
				initPlots();
				box.getComponent(0).setEnabled(true);
				box.getComponent(1).setEnabled(true);
				box.getComponent(2).setEnabled(true);
				box.getComponent(3).setEnabled(true);
			}
		});
		final JPanel frameListPanel = new JPanel();
		frameListPanel.setLayout(new BorderLayout());

		final int n = c.getTabPane().getComponentCount();
		final JScrollPane pane = (JScrollPane) c.getTabPane().getComponent(n - 1);

		frameListPanel.add(pane);
		frameListPanel.add(button, BorderLayout.SOUTH);

		c.getTabPane().addTab("Model", frameListPanel);
		c.getTabPane().revalidate();

		setConsole(c);
		c.setBounds(600, 5, 550, 500);
		c.setVisible(true);
		// this.guiReporter=new GUIReporter(market);
	}

	// -----------------------------------------------------------------------------
	/**
	 * Called either at the proper or a premature end to the simulation. If the
	 * user quits the program, this function may not be called. Ordinarily, you
	 * wouldn't need to override this hook. Does nothing if the GUIState hasn't
	 * been started or loaded yet.
	 */
	public final void finish() {
		super.finish();

		box.getComponent(0).setEnabled(false);
		box.getComponent(1).setEnabled(false);
		box.getComponent(2).setEnabled(false);
		box.getComponent(3).setEnabled(false);

		if (Settings.QP_OPTIMIZER == Settings.QUADPROG_R
				|| Settings.NONLINEAR_OPTIMIZER == Settings.R) {
			try {
				ACEWEMmodel.getrConnection().shutdown();
			} catch (final RserveException e) {
				e.printStackTrace();
			}
		}
		market.resetModelParameters();
		console.hideAllFrames();
		console.unregisterAllFrames();
		init(console);

	}

	// -------------------------------------------------------------------------------------------------------
	public Inspector getInspector() {
		final Inspector i = super.getInspector();
		i.setVolatile(true);
		return i;
	}

	// ----------------------------------------------------------------------------
	/**
	 * this gives the 'model' tab in the console of the GUI.
	 * 
	 * @return Object of GUIState
	 */
	public final Object getSimulationInspectedObject() {
		return state;
	}

	// ------------------------------------------------------------------------------
	/**
	 * Since the GUIState needs to know when the GUI has been launched the init
	 * method is used to register the visualizations using the c.registerFrame
	 * function.
	 * 
	 * @param c
	 *            - the object of Controller
	 */
	public final void init(final Controller c) {
		super.init(c);
	}

	public final void initPlots() {
		prepareGridIllustration(console);


/*		prepareRSupplyGraphicsDA(console);
		prepareDailyProfitsGraphicsDA(console);
		preparePriceGraphicsDA(console);

		if (Settings.LEARNALG == Settings.STOCH) {
			prepareDistributionChoiceDA(console);
		}

		if (Settings.MARKETS == Settings.DA_BM) {
			prepareRSupplyGraphicsBM(console);
			prepareDailyProfitsGraphicsBM(console);
			preparePriceGraphicsBM(console);
			if (Settings.LEARNALG == Settings.STOCH) {
				prepareDistributionChoiceBM(console);
			}
		}
*/
	}

	// -------------------------------------------------------------------------------------------------------
	// To load 'serializable' states - Fix it!
	public void load(final SimState state) {
		super.load(state);
	}

	private void prepareDailyProfitsGraphicsBM(final Controller c) {
		GenCo gen = null;
		MultiTimeSeriesChartGenerator genCoTS = null;
		JFrame genCoFrame = null;
		final Enumeration<String> e = market.getGenCoList().keys();
		while (e.hasMoreElements()) {
			gen = market.getGenCoList().get(e.nextElement());
			genCoTS = new MultiTimeSeriesChartGenerator();
			genCoTS.setTitle("BM Incs daily profit of " + "GenCo "
					+ ((int) gen.getIdNum()));
			genCoTS.setXAxisLabel("Simulated day");
			genCoTS.addSeries(gen.dailyprofitT[Settings.BM_INC], null, 0);
			genCoTS.addSeries(gen.dailyprofitR[Settings.BM_INC], null, 0,
					Color.blue);
			// genCoTS.addSeries(gen.wealthLevel, null, 1, Color.green);
			genCoFrame = genCoTS.createFrame(this);
			genCoFrame.setVisible(false);
			c.registerFrame(genCoFrame);

			genCoTS = new MultiTimeSeriesChartGenerator();
			genCoTS.setTitle("BM Decs daily profit of " + "GenCo "
					+ ((int) gen.getIdNum()));
			genCoTS.setXAxisLabel("Simulated day");
			genCoTS.addSeries(gen.dailyprofitT[Settings.BM_DEC], null, 0);
			genCoTS.addSeries(gen.dailyprofitR[Settings.BM_DEC], null, 0,
					Color.blue);
			// genCoTS.addSeries(gen.wealthLevel, null, 1, Color.green);
			genCoFrame = genCoTS.createFrame(this);
			genCoFrame.setVisible(false);
			c.registerFrame(genCoFrame);

			genCoTS = new MultiTimeSeriesChartGenerator();
			genCoTS.setTitle("BM sum of Incs and Decs daily profit of "
					+ "GenCo " + ((int) gen.getIdNum()));
			genCoTS.setXAxisLabel("Simulated day");
			genCoTS.addSeries(gen.dailyprofitT[Settings.INC_DEC], null, 0);
			genCoTS.addSeries(gen.dailyprofitR[Settings.INC_DEC], null, 0,
					Color.blue);
			// genCoTS.addSeries(gen.wealthLevel, null, 1, Color.green);
			genCoFrame = genCoTS.createFrame(this);
			genCoFrame.setVisible(false);
			c.registerFrame(genCoFrame);
		}
	}

	private void prepareDailyProfitsGraphicsDA(final Controller c) {
		GenCo gen = null;
		MultiTimeSeriesChartGenerator genCoTS = null;
		JFrame genCoFrame = null;
		final Enumeration<String> e = market.getGenCoList().keys();
		while (e.hasMoreElements()) {
			gen = market.getGenCoList().get(e.nextElement());
			genCoTS = new MultiTimeSeriesChartGenerator();
			genCoTS.setTitle("DA daily profit of " + "GenCo "
					+ ((int) gen.getIdNum()));
			genCoTS.setXAxisLabel("Simulated day");
			genCoTS.addSeries(gen.dailyprofitT[Settings.DA], null, 0,
					Color.green);
			genCoTS.addSeries(gen.dailyprofitR[Settings.DA], null, 0,
					Color.blue);
			// genCoTS.addSeries(gen.wealthLevel, null, 1, Color.green);
			genCoFrame = genCoTS.createFrame(this);
			genCoFrame.setVisible(false);
			c.registerFrame(genCoFrame);
		}
	}

	/**
	 * Prepares the graphics of GenCo's reported supply offer, sets the graph
	 * and axis names.
	 * 
	 * @param c
	 *            - the object of Controller
	 */
	private void prepareDistributionChoiceBM(final Controller c) {
		GenCo gen = null;
		MultiTimeSeriesChartGenerator genCoTS = null;
		JFrame genCoFrame = null;
		final Enumeration<String> e = market.getGenCoList().keys();
		while (e.hasMoreElements()) {
			gen = market.getGenCoList().get(e.nextElement());
			genCoTS = new MultiTimeSeriesChartGenerator();
			genCoTS.setTitle("GenCo "
					+ ((int) gen.getIdNum())
					+ "  distribution: 0-NO, 1-TF, 2-GA, 4-ST3, 5-ST4, 6-JSUo, 7-TF2, 8-SST");
			genCoTS.setXAxisLabel("Simulated day");
			genCoTS.addSeries(gen.distribution[Settings.BM_INC], null, 0);
			genCoFrame = genCoTS.createFrame(this);
			genCoFrame.setVisible(false);
			c.registerFrame(genCoFrame);

			genCoTS = new MultiTimeSeriesChartGenerator();
			genCoTS.setTitle("GenCo "
					+ ((int) gen.getIdNum())
					+ "  distribution: 0-NO, 1-TF, 2-GA, 4-ST3, 5-ST4, 6-JSUo, 7-TF2, 8-SST");
			genCoTS.setXAxisLabel("Simulated day");
			genCoTS.addSeries(gen.distribution[Settings.BM_DEC], null, 0);
			genCoFrame = genCoTS.createFrame(this);
			genCoFrame.setVisible(false);
			c.registerFrame(genCoFrame);
		}
	}

	/**
	 * Prepares the graphics of GenCo's reported supply offer, sets the graph
	 * and axis names.
	 * 
	 * @param c
	 *            - the object of Controller
	 */
	private void prepareDistributionChoiceDA(final Controller c) {
		GenCo gen = null;
		MultiTimeSeriesChartGenerator genCoTS = null;
		JFrame genCoFrame = null;
		final Enumeration<String> e = market.getGenCoList().keys();
		while (e.hasMoreElements()) {
			gen = market.getGenCoList().get(e.nextElement());
			genCoTS = new MultiTimeSeriesChartGenerator();
			genCoTS.setTitle("GenCo "
					+ ((int) gen.getIdNum())
					+ "  distribution: 0-NO, 1-TF, 2-GA, 4-ST3, 5-ST4, 6-JSUo, 7-TF2, 8-SST");// +
			// ": trueA="
			// +
			// gen.getaT()
			// + " & trueB=" + gen.getbT());
			genCoTS.setXAxisLabel("Simulated day");
			genCoTS.addSeries(gen.distribution[Settings.DA], null, 0);
			genCoFrame = genCoTS.createFrame(this);
			genCoFrame.setVisible(false);
			c.registerFrame(genCoFrame);
		}
	}

	// -----------------------------------------------------------------------------
	/**
	 * Prepares the graphics of daily aggregate LMP, sets the graph and axis
	 * names.
	 * 
	 * @param c
	 *            - the object of Controller
	 */
	/*
	 * private void prepareCumulativeLMPGraphics(final Controller c) {
	 * 
	 * GenCo gen1 = null; Enumeration<String> e1 = market.getGenCoList().keys();
	 * while (e1.hasMoreElements()) { gen1 =
	 * market.getGenCoList().get(e1.nextElement());
	 * gen1.submitTrueSupplyOffer(); } //opf.solveHourlyPowerFlows(); GenCo gen2
	 * = null; Enumeration<String> e2 = market.getGenCoList().keys(); while
	 * (e2.hasMoreElements()) { gen2 =
	 * market.getGenCoList().get(e2.nextElement());
	 * gen2.updateDailyPerformance(); gen2.lmpCummulativeTrue =
	 * gen2.lmpCummulative; } GenCo gen3 = null; MultiTimeSeriesChartGenerator
	 * genCoTS = null; JFrame genCoFrame = null; Enumeration<String> e3 =
	 * this.market.getGenCoList().keys(); while (e3.hasMoreElements()) { gen3 =
	 * this.market.getGenCoList().get(e3.nextElement()); genCoTS = new
	 * MultiTimeSeriesChartGenerator(); genCoTS.setTitle("Cumulative LMP for " +
	 * gen3.getID() + " at Node" + gen3.getNode() + " (no learning) = " +
	 * gen3.lmpCummulativeTrue); genCoTS.setXAxisLabel("Simulated day");
	 * genCoTS.addSeries(gen3.cumulativelmptrue, null, 0);
	 * genCoTS.addSeries(gen3.cumulativelmp, null, 0); genCoFrame =
	 * genCoTS.createFrame(this); genCoFrame.setVisible(true);
	 * c.registerFrame(genCoFrame); } }
	 */
	// -------------------------------------------------------------------------------------------------------
	/*
	 * private void prepareCumulativeCommitmentsGraphics(Controller c) {
	 * 
	 * GenCo gen1 = null; Enumeration<String> e1 = market.getGenCoList().keys();
	 * while(e1.hasMoreElements()) { gen1 =
	 * market.getGenCoList().get(e1.nextElement());
	 * gen1.submitTrueSupplyOffer(); } //opf.solveHourlyPowerFlows(); GenCo gen2
	 * = null; Enumeration<String> e2 = market.getGenCoList().keys();
	 * while(e2.hasMoreElements()) { gen2 =
	 * market.getGenCoList().get(e2.nextElement());
	 * gen2.updateDailyPerformance(); gen2.commitmentCumulativeTrue =
	 * gen2.commitmentCumulative; } GenCo gen3 = null;
	 * MultiTimeSeriesChartGenerator genCoTS=null; JFrame genCoFrame=null;
	 * Enumeration<String> e3 = this.market.getGenCoList().keys();
	 * while(e3.hasMoreElements()) { gen3 =
	 * this.market.getGenCoList().get(e3.nextElement()); genCoTS= new
	 * MultiTimeSeriesChartGenerator(); genCoTS.setTitle(gen3.getID()+
	 * ": Daily electricity dispatch ( no learning) = " +
	 * gen3.getcommitmentCumulativeTrue());
	 * genCoTS.setXAxisLabel("Simulated day");
	 * genCoTS.addSeries(gen3.cumulativecommitmenttrue,null,0);
	 * genCoTS.addSeries(gen3.cumulativecommitment,null,0); genCoFrame=
	 * genCoTS.createFrame(this); genCoFrame.setVisible(true);
	 * c.registerFrame(genCoFrame); } }
	 */
	// -------------------------------------------------------------------------------------------------------

	public void prepareGridIllustration(final Controller c) {

		final Continuous2D nodes = new Continuous2D(100, 100, 100);
		final Continuous2D genCos = new Continuous2D(100, 100, 100);
		final Continuous2D lses = new Continuous2D(100, 100, 100);
		final Network branches = new Network();

		for (int i = 0; i < market.getNodeList().length; i++) {
			// must be final to be used in the anonymous class below
			final NodePortrayal node = new NodePortrayal(i + 1);
			final Double2D location = new Double2D(
					Double.parseDouble(market.getNodeList()[i][Settings.X_LOCATION]),
					Double.parseDouble(market.getNodeList()[i][Settings.Y_LOCATION]));
			nodes.setObjectLocation(node, location);
			branches.addNode(node);

			int index = 0;
			Enumeration<String> e = market.getGenCoList().keys();
			while (e.hasMoreElements()) {
				final GenCo gen =  market.getGenCoList().get(e.nextElement());
				if (gen.getNode() == (i + 1)) {
					final Double2D locationGenCo = new Double2D(
							Double.parseDouble(market.getNodeList()[i][Settings.X_LOCATION]) - Settings.AGENT_WIDTH / 1.5 - (Settings.AGENT_WIDTH * 1.5 * index),
							Double.parseDouble(market.getNodeList()[i][Settings.Y_LOCATION]) - Settings.AGENT_HEIGHT);
					genCos.setObjectLocation(gen, locationGenCo);
					branches.addNode(gen);
					index++;
				}
			}

			index = 0;
			e = market.getLseList().keys();
			while (e.hasMoreElements()) {
				final LSE lse =  market.getLseList().get(e.nextElement());
				if (lse.getNode() == (i + 1)) {
					final Double2D locationLSE = new Double2D(
							Double.parseDouble(market.getNodeList()[i][Settings.X_LOCATION]) + Settings.AGENT_WIDTH / 1.5 + (Settings.AGENT_WIDTH * 1.5 * index),
							Double.parseDouble(market.getNodeList()[i][Settings.Y_LOCATION]) - Settings.AGENT_HEIGHT);
					lses.setObjectLocation(lse, locationLSE);
					branches.addNode(lse);
					index++;
				}
			}
		}

		Bag nodeObjs = nodes.getAllObjects();
		for (int i = 0; i < market.getBranchList().length; i++) {
			final BranchSerializable branch = new BranchSerializable(i + 1);
			final NodePortrayal from = (NodePortrayal) (nodeObjs.objs[(int) market
			                                                          .getBranchList()[i][Settings.FROM] - 1]);
			final NodePortrayal to = (NodePortrayal) (nodeObjs.objs[(int) market
			                                                        .getBranchList()[i][Settings.TO] - 1]);
			branches.addEdge(from, to, branch);
		}


		final NetworkPortrayal2D branchPortrayal = new NetworkPortrayal2D();
		final ContinuousPortrayal2D nodePortrayal = new ContinuousPortrayal2D();
		final ContinuousPortrayal2D genCoPortrayal = new ContinuousPortrayal2D();
		final ContinuousPortrayal2D lsePortrayal = new ContinuousPortrayal2D();

		final Display2D display = new Display2D(600, 600, this);
		// turn off clipping
		display.setClipping(false);
		final JFrame displayFrame = display.createFrame();
		displayFrame.setTitle("ACEWEM DA Market");
		c.registerFrame(displayFrame); // register the frame so it appears in
		// the "Display" list
		displayFrame.setVisible(true);

		display.attach(branchPortrayal, "Branches");
		display.attach(nodePortrayal, "Nodes");
		display.attach(genCoPortrayal, "GenCo");
		display.attach(lsePortrayal, "LSE");

		branchPortrayal.setField(new SpatialNetwork2D(nodes, branches));

		final BranchPortrayal b = new BranchPortrayal(market);
		branchPortrayal.setPortrayalForAll(b);

		genCoPortrayal.setField(genCos);
		lsePortrayal.setField(lses);
		nodePortrayal.setField(nodes);

		// reschedule the displayer
		display.reset();
		display.setBackdrop(Color.white);
		// redraw the display
		display.repaint();
		
		if (Settings.MARKETS == Settings.DA_BM) {
			final Display2D displayBM = new Display2D(600, 600, this);
			displayBM.setClipping(false);
			final JFrame displayFrameBM = displayBM.createFrame();
			displayFrameBM.setTitle("ACEWEM BM Market");
			c.registerFrame(displayFrameBM); // register the frame so it appears in
			displayFrameBM.setVisible(true);
			final NetworkPortrayal2D branchPortrayalBM = new NetworkPortrayal2D();
			branchPortrayalBM.setField(new SpatialNetwork2D(nodes, branches));
			branchPortrayalBM.setPortrayalForAll(new BranchPortrayalBM(market));
			displayBM.attach(branchPortrayalBM, "Branches");
			displayBM.attach(nodePortrayal, "Nodes");
			displayBM.attach(genCoPortrayal, "GenCo");
			displayBM.attach(lsePortrayal, "LSE");
			displayBM.reset();
			displayBM.setBackdrop(Color.white);
			displayBM.repaint();
		}
		
	}

	private void preparePriceGraphicsBM(final Controller c) {

		final int randomKey = new Random()
		.nextInt(market.getGenCoList().size() - 1) + 1;
		final GenCo gen = market.getGenCoList().get("genco" + randomKey);
		MultiTimeSeriesChartGenerator genCoTS = new MultiTimeSeriesChartGenerator();
		genCoTS.setTitle("MCP INCs");
		genCoTS.setXAxisLabel("Simulated day");
		genCoTS.addSeries(gen.averagePriceT[Settings.BM_INC], null, 0,
				Color.green);
		genCoTS.addSeries(gen.averagePriceR[Settings.BM_INC], null, 0,
				Color.orange);
		JFrame genCoFrame = genCoTS.createFrame(this);
		genCoFrame.setVisible(false);
		c.registerFrame(genCoFrame);

		genCoTS = new MultiTimeSeriesChartGenerator();
		genCoTS.setTitle("MCP DECs");
		genCoTS.setXAxisLabel("Simulated day");
		genCoTS.addSeries(gen.averagePriceT[Settings.BM_DEC], null, 0,
				Color.green);
		genCoTS.addSeries(gen.averagePriceR[Settings.BM_DEC], null, 0,
				Color.orange);
		genCoFrame = genCoTS.createFrame(this);
		genCoFrame.setVisible(false);
		c.registerFrame(genCoFrame);
	}

	/**
	 * Prepares the graphics of GenCo's reported supply offer, sets the graph
	 * and axis names.
	 * 
	 * @param c
	 *            - the object of Controller
	 */
	private void preparePriceGraphicsDA(final Controller c) {

		if (Settings.MARKETS == Settings.SINGLE_DA) {
			GenCo gen = null;
			MultiTimeSeriesChartGenerator genCoTS = null;
			JFrame genCoFrame = null;
			final Enumeration<String> e = market.getGenCoList().keys();
			while (e.hasMoreElements()) {
				gen = market.getGenCoList().get(e.nextElement());
				genCoTS = new MultiTimeSeriesChartGenerator();
				genCoTS.setTitle("Node " + gen.getNode() + "  LMP  (GenCo "
						+ gen.getIdNum() + ")");
				genCoTS.setXAxisLabel("Simulated day");
				genCoTS.addSeries(gen.averagePriceT[Settings.DA], null, 0,
						Color.green);
				genCoTS.addSeries(gen.averagePriceR[Settings.DA], null, 0,
						Color.orange);
				genCoFrame = genCoTS.createFrame(this);
				genCoFrame.setVisible(false);
				c.registerFrame(genCoFrame);
			}
		} else {

			final int randomKey = new Random().nextInt(market.getGenCoList()
					.size() - 1) + 1;
			final GenCo gen = market.getGenCoList().get("genco" + randomKey);
			final MultiTimeSeriesChartGenerator genCoTS = new MultiTimeSeriesChartGenerator();
			genCoTS.setTitle("MCP DA");
			genCoTS.setXAxisLabel("Simulated day");
			genCoTS.addSeries(gen.averagePriceT[Settings.DA], null, 0,
					Color.green);
			genCoTS.addSeries(gen.averagePriceR[Settings.DA], null, 0,
					Color.orange);
			final JFrame genCoFrame = genCoTS.createFrame(this);
			genCoFrame.setVisible(false);
			c.registerFrame(genCoFrame);
		}
	}

	private void prepareRSupplyGraphicsBM(final Controller c) {
		GenCo gen = null;
		MultiTimeSeriesChartGenerator genCoTS = null;
		JFrame genCoFrame = null;
		Enumeration<String> e = null;

		e = market.getGenCoList().keys();
		while (e.hasMoreElements()) {
			gen = market.getGenCoList().get(e.nextElement());
			genCoTS = new MultiTimeSeriesChartGenerator();
			genCoTS.setTitle("GenCo " + ((int) gen.getIdNum())
					+ ":BM Incs trueA=" + gen.getaT() + " & trueB="
					+ gen.getbT());
			genCoTS.setXAxisLabel("Simulated day");
			genCoTS.addSeries(gen.interceptR[Settings.BM_INC], null, 0);
			// genCoTS.addSeries(gen.trueSupplyOfferA,null,0);
			genCoTS.addSeries(gen.slopeR[Settings.BM_INC], null, 1);
			// genCoTS.addSeries(gen.trueSupplyOfferB,null,1);
			genCoFrame = genCoTS.createFrame(this);
			genCoFrame.setVisible(false);
			c.registerFrame(genCoFrame);
		}

		e = market.getGenCoList().keys();
		while (e.hasMoreElements()) {
			gen = market.getGenCoList().get(e.nextElement());
			genCoTS = new MultiTimeSeriesChartGenerator();
			genCoTS.setTitle("GenCo " + ((int) gen.getIdNum())
					+ ":BM Decs trueA=" + gen.getaT() + " & trueB="
					+ gen.getbT());
			genCoTS.setXAxisLabel("Simulated day");
			genCoTS.addSeries(gen.interceptR[Settings.BM_DEC], null, 0);
			// genCoTS.addSeries(gen.trueSupplyOfferA,null,0);
			genCoTS.addSeries(gen.slopeR[Settings.BM_DEC], null, 1);
			// genCoTS.addSeries(gen.trueSupplyOfferB,null,1);
			genCoFrame = genCoTS.createFrame(this);
			genCoFrame.setVisible(false);
			c.registerFrame(genCoFrame);
		}
	}

	private void prepareRSupplyGraphicsDA(final Controller c) {

		GenCo gen = null;
		MultiTimeSeriesChartGenerator genCoTS = null;
		JFrame genCoFrame = null;
		Enumeration<String> e = null;
		e = market.getGenCoList().keys();
		while (e.hasMoreElements()) {
			gen = market.getGenCoList().get(e.nextElement());
			genCoTS = new MultiTimeSeriesChartGenerator();
			genCoTS.setTitle("GenCo " + ((int) gen.getIdNum()) + ": DA trueA="
					+ gen.getaT() + " & trueB=" + gen.getbT());
			genCoTS.setXAxisLabel("Simulated day");
			genCoTS.addSeries(gen.interceptR[Settings.DA], null, 0);
			// genCoTS.addSeries(gen.trueSupplyOfferA,null,0);
			genCoTS.addSeries(gen.slopeR[Settings.DA], null, 1);
			// genCoTS.addSeries(gen.trueSupplyOfferB,null,1);
			genCoFrame = genCoTS.createFrame(this);
			genCoFrame.setVisible(false);
			c.registerFrame(genCoFrame);
		}
	}

	// -------------------------------------------------------------------------------------------------------
	public void quit() {
		super.quit();
		System.exit(0);
	}

	public void setConsole(final Console c) {
		console = c;
	}

	// ------------------------------------------------------------------------------
	/**
	 * this is the first method that is executed once the start button is
	 * pressed.
	 */
	public final void start() {
		super.start();

		//		if (!market.learningSelected) {
		// final Display2D display = new Display2D(600, 600, this);
		// final JFrame jfr = display.createFrame();
		// Utilities
		// .inform("Learning algorithm is not selected",
		// "The learning algorithm was set to Stochastic optimisation with Gamlss",
		// jfr);
		//			Settings.LEARNALG = Settings.STOCH;
		//			market.start();
		//		}
	}

	// ------------------------------------------------------------------------------
	/** This function updates the graphs. */
	private void updater() {

		final Steppable updater = new Steppable() {
			private static final long serialVersionUID = 1L;

			public void step(final SimState simState) {

				GenCo gen = null;
				Enumeration<String> e = null;
				final int step = (int) market.schedule.getSteps();

				e = market.getGenCoList().keys();
				while (e.hasMoreElements()) {
					gen = market.getGenCoList().get(e.nextElement());

					switch (Settings.MARKETS) {
					case Settings.SINGLE_DA:

						if (Settings.LEARNALG == Settings.STOCH) {
							gen.distribution[Settings.DA].add(step,
									gen.getDistrOfChoice(Settings.DA));
						}

						gen.slopeR[Settings.DA].add(step,
								gen.getbR(Settings.DA));
						gen.interceptR[Settings.DA].add(step,
								gen.getaR(Settings.DA));
						gen.slopeT.add(step, gen.getbT());
						gen.interceptT.add(step, gen.getaT());

						gen.averagePriceR[Settings.DA].add(step,
								gen.getTotalDailyPrice(Settings.DA)
								/ Settings.HOURS);
						gen.averagePriceT[Settings.DA].add(step,
								gen.getTruePrice(Settings.DA) / Settings.HOURS);
						gen.dailyprofitR[Settings.DA].add(step,
								gen.getDailyProfit(Settings.DA));
						gen.dailyprofitT[Settings.DA].add(step,
								gen.getDailyTrueProfit(Settings.DA));
						// gen.wealthLevel.add(step, gen.getWealth());
						break;
					case Settings.DA_BM:
						if (Settings.LEARNALG == Settings.STOCH) {
							gen.distribution[Settings.DA].add(step,
									gen.getDistrOfChoice(Settings.DA));
							gen.distribution[Settings.BM_INC].add(step,
									gen.getDistrOfChoice(Settings.BM_INC));
							gen.distribution[Settings.BM_DEC].add(step,
									gen.getDistrOfChoice(Settings.BM_DEC));
						}
						gen.slopeR[Settings.DA].add(step,
								gen.getbR(Settings.DA));
						gen.interceptR[Settings.DA].add(step,
								gen.getaR(Settings.DA));
						gen.slopeT.add(step, gen.getbT());
						gen.interceptT.add(step, gen.getaT());
						gen.slopeR[Settings.BM_INC].add(step,
								gen.getbR(Settings.BM_INC));
						gen.interceptR[Settings.BM_INC].add(step,
								gen.getaR(Settings.BM_INC));
						gen.slopeR[Settings.BM_DEC].add(step,
								gen.getbR(Settings.BM_DEC));
						gen.interceptR[Settings.BM_DEC].add(step,
								gen.getaR(Settings.BM_DEC));

						double totalMCP_DA = 0;
						double totalMCP_INC = 0;
						double totalMCP_DEC = 0;
						for (int h = 0; h < Settings.HOURS; h++) {
							totalMCP_DA += market.getMCP()[Settings.DA][h];
							totalMCP_INC += market.getMCP()[Settings.BM_INC][h];
							totalMCP_DEC += market.getMCP()[Settings.BM_DEC][h];
						}

						gen.averagePriceR[Settings.DA].add(step, totalMCP_DA
								/ Settings.HOURS);
						gen.averagePriceT[Settings.DA].add(step,
								gen.getTruePrice(Settings.DA) / Settings.HOURS);
						gen.averagePriceR[Settings.BM_INC].add(step,
								totalMCP_INC / Settings.HOURS);
						gen.averagePriceT[Settings.BM_INC].add(step,
								gen.getTruePrice(Settings.BM_INC)
								/ Settings.HOURS);
						gen.averagePriceR[Settings.BM_DEC].add(step,
								totalMCP_DEC / Settings.HOURS);
						gen.averagePriceT[Settings.BM_DEC].add(step,
								gen.getTruePrice(Settings.BM_DEC)
								/ Settings.HOURS);

						gen.dailyprofitR[Settings.DA].add(step,
								gen.getDailyProfit(Settings.DA));
						gen.dailyprofitT[Settings.DA].add(step,
								gen.getDailyTrueProfit(Settings.DA));
						gen.dailyprofitR[Settings.BM_INC].add(step,
								gen.getDailyProfit(Settings.BM_INC));
						gen.dailyprofitT[Settings.BM_INC].add(step,
								gen.getDailyTrueProfit(Settings.BM_INC));
						gen.dailyprofitR[Settings.BM_DEC].add(step,
								gen.getDailyProfit(Settings.BM_DEC));
						gen.dailyprofitT[Settings.BM_DEC].add(step,
								gen.getDailyTrueProfit(Settings.BM_DEC));
						gen.dailyprofitR[Settings.INC_DEC].add(step, (gen
								.getDailyProfit(Settings.BM_INC) + gen
								.getDailyProfit(Settings.BM_DEC)));
						gen.dailyprofitT[Settings.INC_DEC]
								.add(step,
										gen.getDailyTrueProfit(Settings.BM_INC)
										+ gen.getDailyTrueProfit(Settings.BM_DEC));
						// gen.wealthLevel.add(step, gen.getWealth());
						break;
					default:
						System.err.println(" The requested power market "
								+ "cannot be modelled in GUI");
					}
				}

				// for (int i = 0; i < market.getBranchList().length; i ++) {
				// market.branch[i].setCongestion(random.nextDouble());
				// }

				// congestion = random.nextDouble();

			}
		};
		scheduleRepeatingImmediatelyAfter(updater);
	}
}
