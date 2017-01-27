package acewem.market;

import gamlss.algorithm.Gamlss;

import java.util.Enumeration;
import java.util.Hashtable;

import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import sim.engine.SimState;
import sim.engine.Steppable;
import acewem.initials.InitBranch;
import acewem.initials.InitGenCo;
import acewem.initials.InitLSE;
import acewem.initials.InitNode;
import acewem.initials.Settings;
import acewem.utilities.io.StartRserve;
import acewem.utilities.io.WriteToCSV;

public class ACEWEMmodel extends SimState {


	/** Stores LMPs for each node and each market time interval. */
	private double[][] lmp;
	/** Stores MCPs for each market time interval. */
	private double[][] mcp;
	/** Stores LSE objects. */
	private Hashtable<String, LSE> lseList;
	/** Stores GenCo objects. */
	private Hashtable<String, GenCo> genCoList;
	/** Stores branches information. */
	private double[][] branchList;
	/** Stores values of voltage angles. */
	private double[][][] angleList;
	/** Stores nodes information. */
	private String[][] nodeList;
	/** Whether a call is from ACEWEMGUI class. */
	private boolean isFromGUI = false;
	/** Object of ISO. */
	private ISO iso;
	/** Object of WriteToCSV. */
	private WriteToCSV wrtCSV;
	/** Object of Gamlss class. */
	private Gamlss[][] gamlss;

//	public boolean learningSelected = false;

	private static RConnection rConnection;

	/**
	 * Constructor.
	 * @param seed - random number
	 * @param isfromGUI
	 */
	public ACEWEMmodel(final long seed, final boolean isfromGUI) {
		super(seed);
		isFromGUI = isfromGUI;
	}

	/**
	 * Whether a call is from ACEWEMGUI class.
	 * 
	 * @return boolean
	 */
	public final boolean isFromGUI() {
		return isFromGUI;
	}

	/**
	 * Cleares some model parameters.
	 */
	public final void resetModelParameters() {

		genCoList.clear();
		lseList.clear();
		branchList = null;
	}

	public final void initialiseMarket() {

		new InitNode(this);
		if (Settings.QP_OPTIMIZER == Settings.QUADPROG_R
				|| Settings.NONLINEAR_OPTIMIZER == Settings.R) {
			try {

				StartRserve.checkLocalRserve();
				rConnection = new RConnection();

				if (Settings.QP_OPTIMIZER == Settings.QUADPROG_R) {

					//	if (rConnection
					//			.eval("\"quadprog\" %in% rownames(installed.packages())")
					//			.asString().equals("FALSE")) {
					//		rConnection.voidEval("install.packages(\"quadprog\")");
					//	}
					rConnection.voidEval("library(quadprog)");
				}

				if (Settings.NONLINEAR_OPTIMIZER == Settings.R) {
					rConnection.voidEval("library(gamlss.add)");
				}
			} catch (final RserveException e1) {
				e1.printStackTrace();
			}
		}

		// if (Settings.LEARNALG == Settings.STOCH) {
		// fit = new Fitting(getGenCoList().size());
		// }
		lseList = new Hashtable<String, LSE>();
		genCoList = new Hashtable<String, GenCo>();
		switch (Settings.MARKETS) {
		case Settings.SINGLE_DA:
			lmp = new double[getNodeList().length][Settings.HOURS];
			gamlss = new Gamlss[Settings.TWO][Settings.ONE];
			angleList = new double[Settings.ONE][getNodeList().length][Settings.HOURS];
			gamlss[Settings.PRICE][Settings.DA] = new Gamlss();
			gamlss[Settings.MW][Settings.DA] = new Gamlss();
			break;
		case Settings.DA_BM:
			mcp = new double[Settings.THREE][Settings.HOURS];
			gamlss = new Gamlss[Settings.TWO][Settings.THREE];
			angleList = new double[Settings.TWO][getNodeList().length][Settings.HOURS];
			for (int k = 0; k < Settings.THREE; k++) {
				for (int i = 0; i < Settings.TWO; i++) {
					gamlss[i][k] = new Gamlss();
				}
			}
			break;
		default:
			System.err.println(" The requested power market "
					+ "cannot be modelled in ACEWEMmodel");
		}

		new InitGenCo(this);
		new InitLSE(this);
		new InitBranch(this);
		iso = new ISO(this);
		// just to supply true MCs and calculate true profits, do it once at
		// very beginning
		wrtCSV = new WriteToCSV(this);
	}

	/**
	 * Fires the market agents.
	 */
	public final void start() {
		// prepare the scheduler of the simulation
		super.start();

		Enumeration<String> e = null;
		LSE lse = null;
		switch (Settings.MARKETS) {
		case Settings.SINGLE_DA:
			
			try {
				iso.getQCP().solveHourlyPowerFlowsConstainedDA();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			GenCo gen = null;
			e = getGenCoList().keys();
			while (e.hasMoreElements()) {
				gen = getGenCoList().get(e.nextElement());
				gen.updatePerformanceDA();
				gen.setDailyTrueProfit(Settings.DA,
						gen.getDailyProfit(Settings.DA));
				gen.setTotalDailyTruePrice(Settings.DA,
						gen.getTotalDailyPrice(Settings.DA));
				gen.setTotalDailyTrueCommitment(Settings.DA,
						gen.getTotalDailyCommitment(Settings.DA));

				// System.out.println(gen.getID()+"  "+gen.getDailyTrueProfit(Settings.DA)+"  "+gen.getTruePrice(Settings.DA)/Settings.HOURS+"  "+gen.getTotalDailyTrueCommitment(Settings.DA));
			}
			getWrtCSV().writeResults(Settings.DA, this, true);
			break;
		case Settings.DA_BM:

			try {
				iso.getQCP().solveHourlyPowerFlowsUnconstainedDA();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			double totalMCP = 0;
			for (int h = 0; h < Settings.HOURS; h++) {
				totalMCP += getMCP()[Settings.DA][h];
			}
			e = getGenCoList().keys();
			while (e.hasMoreElements()) {
				gen = getGenCoList().get(e.nextElement());
				gen.updatePerformanceDA();
				gen.setDailyTrueProfit(Settings.DA,
						gen.getDailyProfit(Settings.DA));
				gen.setTotalDailyTruePrice(Settings.DA, totalMCP);

				for (int hour = 0; hour < Settings.HOURS; hour++) {
					// calculate upper capacity for increments for BM market
					final double capInc = gen.getCapUR(Settings.DA, 0)
							- gen.getCommitment(Settings.DA, hour);
					gen.setCapUR(Settings.BM_INC, hour, capInc);
					// calculate upper capacity for decrements for BM market
					final double capDec = gen.getCommitment(Settings.DA, hour);
					gen.setCapUR(Settings.BM_DEC, hour, capDec);
				}
			}
			getWrtCSV().writeResults(Settings.DA, this, true);
			Settings.MARKET_SWITHCER = Settings.BM;

			try {
				iso.getQCP().resolveCongestionBM();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			double totalMCP_inc = 0;
			double totalMCP_dec = 0;
			for (int h = 0; h < Settings.HOURS; h++) {
				totalMCP_inc += getMCP()[Settings.BM_INC][h];
				totalMCP_dec += getMCP()[Settings.BM_DEC][h];
			}
			e = getGenCoList().keys();
			while (e.hasMoreElements()) {
				gen = getGenCoList().get(e.nextElement());
				gen.updatePerformanceBM();
				gen.setDailyTrueProfit(Settings.BM_INC,
						gen.getDailyProfit(Settings.BM_INC));
				gen.setDailyTrueProfit(Settings.BM_DEC,
						gen.getDailyProfit(Settings.BM_DEC));
				gen.setTotalDailyTruePrice(Settings.BM_INC, totalMCP_inc);
				gen.setTotalDailyTruePrice(Settings.BM_DEC, totalMCP_dec);
			}
			getWrtCSV().writeResults(Settings.BM_INC, this, true);
			getWrtCSV().writeResults(Settings.BM_DEC, this, true);
			Settings.MARKET_SWITHCER = Settings.DA;
			break;
		default:
			System.err.println(" The requested power market "
					+ "cannot be modelled in ACEWEMmodel");
		}

		e = null;
		int order = -1;

		// fitting
		// if (Settings.MARKETS == Settings.DA_BM &&
		// Settings.LEARNALG == Settings.STOCH) {
		// order++;
		// this.schedule.scheduleRepeating(fit, order, 1.0);
		// }

		// Fire GenCos agents for Day-ahead market
		GenCo gen = null;
		e = genCoList.keys();
		order++;
		while (e.hasMoreElements()) {
			gen = genCoList.get(e.nextElement());
			schedule.scheduleRepeating(gen, order, 1.0);
		}

		// Fire LSEs agents for Day-ahead market
		lse = null;
		e = lseList.keys();
		order++;
		while (e.hasMoreElements()) {
			lse = lseList.get(e.nextElement());
			schedule.scheduleRepeating(lse, order, 1.0);
		}

		// Fire ISO agent for Day-ahead market
		schedule.scheduleRepeating(iso, order++, 1.0);

		// Fire updater for Day-ahead market
		updaterDA(order++, this);

		if (Settings.MARKETS == Settings.DA_BM) {

			// fitting
			// if (Settings.LEARNALG == Settings.STOCH) {
			// order++;
			// this.schedule.scheduleRepeating(fit, order, 1.0);
			// }

			// Fire GenCos agents for Balancing Mechanism market
			e = genCoList.keys();
			order++;
			while (e.hasMoreElements()) {
				gen = genCoList.get(e.nextElement());
				schedule.scheduleRepeating(gen, order, 1.0);
			}

			// Fire LSEs agents for Balancing Mechanism market
			e = lseList.keys();
			order++;
			while (e.hasMoreElements()) {
				lse = lseList.get(e.nextElement());
				schedule.scheduleRepeating(lse, order, 1.0);
			}

			// Fire ISO agents for Balancing Mechanism market
			schedule.scheduleRepeating(iso, order++, 1.0);

			// Fire updater for Balancing Mechanism market
			updaterBM(order++, this);
			// Write the results
		}
	}

	private final void updaterBM(final int order, final ACEWEMmodel market) {

		final Steppable updaterBM = new Steppable() {
			private static final long serialVersionUID = 1L;

			public void step(final SimState simState) {

				Enumeration<String> e = null;

				// GenCo gen = null;
				// e = getGenCoList().keys();
				// while (e.hasMoreElements()) {
				// gen = getGenCoList().get(e.nextElement());
				// if (gen.getWealth() < 0.0) {
				// getGenCoList().remove(gen.getID());
				// }
				// }

				GenCo gen = null;
				e = getGenCoList().keys();
				while (e.hasMoreElements()) {
					gen = getGenCoList().get(e.nextElement());
					// update learning
					gen.updatePerformanceBM();
					if (Settings.LEARNALG == Settings.REINF) {
						gen.learn(Settings.BM_INC);
						gen.learn(Settings.BM_DEC);
					}
					if (Settings.LEARNALG == Settings.STOCH) {
						gen.appendLastResponseValue(Settings.BM_INC);
						gen.appendLastResponseValue(Settings.BM_DEC);
					}
				}
				getWrtCSV().writeResults(Settings.BM_INC, market, false);
				getWrtCSV().writeResults(Settings.BM_DEC, market, false);
				
				Settings.MARKET_SWITHCER = Settings.DA;
			}
		};
		schedule.scheduleRepeating(updaterBM, order, 1.0);
	}

	/**
	 * Forces GenCo agents to learn based on current day profits(revenues).
	 */
	/*
	 * public final void makeGenCoLearn() { Enumeration<String> e =
	 * genCoList.keys(); while (e.hasMoreElements()) { gen =
	 * genCoList.get(e.nextElement()); gen.learn(); } }
	 */

	private final void updaterDA(final int order, final ACEWEMmodel market) {

		final Steppable updaterDA = new Steppable() {
			private static final long serialVersionUID = 1L;

			public void step(final SimState simState) {

				GenCo gen = null;
				Enumeration<String> e = getGenCoList().keys();

				gen = null;
				e = getGenCoList().keys();
				while (e.hasMoreElements()) {
					gen = getGenCoList().get(e.nextElement());
					// update learning
					gen.updatePerformanceDA();
					if (Settings.LEARNALG == Settings.REINF) {
						gen.learn(Settings.DA);
					}
					if (Settings.LEARNALG == Settings.STOCH) {
						gen.appendLastResponseValue(Settings.DA);
					}
				}

				// Write the results
				getWrtCSV().writeResults(Settings.DA, market, false);

				if (Settings.MARKETS == Settings.DA_BM) {
					Settings.MARKET_SWITHCER = Settings.BM;
				}
			}
		};
		schedule.scheduleRepeating(updaterDA, order, 1.0);
	}

	public static RConnection getrConnection() {
		return rConnection;
	}

	public Object domLearning() {
		return new String[] { "Stochastic learning with GAMLSS",
		"Reinforcement learning" };
	}

	public Object domLseShock() {
		return new sim.util.Interval(0.0, 2.0);
	}

	/**
	 * Get a list of voltage angles.
	 * 
	 * @return voltage angles
	 */
	public double[][] getAngleList(int whichMarket) {
		return angleList[whichMarket];
	}

	/**
	 * Get a list of transmission grid branches.
	 * 
	 * @return Transmission grid branches
	 */
	public final double[][] getBranchList() {
		return branchList;
	}

	public final int getDay() {
		return (int) schedule.getSteps();
	}

	public Gamlss[][] getGamlss() {
		return gamlss;
	}

	/**
	 * Get a list of GenCo objects.
	 * 
	 * @return GenCo objects
	 */
	public final Hashtable<String, GenCo> getGenCoList() {
		return genCoList;
	}

	public int getLearning() {
		return Settings.LEARNALG;
	}

	/**
	 * Get a list of LMPs.
	 * 
	 * @return LMPs
	 */
	public final double[][] getLMP() {
		return lmp;
	}

	/**
	 * Get a list of LSE objects.
	 * 
	 * @return LSE objects
	 */
	public final Hashtable<String, LSE> getLseList() {
		return lseList;
	}

	public double getLseShock() {
		return Settings.DEMAND_SHOCK_MAGNITUDE;
	}

	public final double[][] getMCP() {
		return mcp;
	}

	public String[][] getNodeList() {
		return nodeList;
	}

	public boolean getShock() {
		return Settings.DEMAND_SHOCK;
	}

	public int getShockDay() {
		return Settings.DEMAND_SHOCK_DAY;
	}

	/**
	 * Get an object of WriteToCSV class.
	 * 
	 * @return object of WriteToCSV
	 */
	public final WriteToCSV getWrtCSV() {
		return wrtCSV;
	}

	public boolean hideAngleList() {
		return true;
	}

	public boolean hideBranchList() {
		return true;
	}

	public boolean hideDay() {
		return true;
	}

	public boolean hideFromGUI() {
		return true;
	}

	public boolean hideGamlss() {
		return true;
	}

	public boolean hideGenCoList() {
		return true;
	}

	public boolean hideLMP() {
		return true;
	}

	public boolean hideLseList() {
		return true;
	}

	public boolean hideMCP() {
		return true;
	}

	public boolean hideNodeList() {
		return true;
	}

	public boolean hideNumberOfGencos() {
		return true;
	}

	public boolean hideNumNodes() {
		return true;
	}

	public boolean hiderConnection() {
		return true;
	}

	public boolean hideWrtCSV() {
		return true;
	}


	public void setAngleList(final int whichMarket, final int node, final int hour, final double angle) {
		angleList[whichMarket][node][hour] = angle;
	}

	public final void setBranchList(final double[][] branchData) {
		branchList = branchData;
	}

	public void setLearning(final int val) {

		if (val == 0) {
			Settings.LEARNALG = Settings.STOCH;

		} else if (val == 1) {
			Settings.LEARNALG = Settings.REINF;
		} else {
			Settings.LEARNALG = Settings.STOCH;
		}
	}
	
	public void setCongestionManagement(final int val) {

		if (val == 0) {
			Settings.MARKETS = Settings.SINGLE_DA;

		} else if (val == 1) {
			Settings.MARKETS = Settings.DA_BM;
		} else {
			Settings.MARKETS = Settings.SINGLE_DA;
		}
	}
	
	public int getCongestionManagement() {
		return Settings.MARKETS;
	}
	
	public Object domCongestionManagement() {
		return new String[] { "Locational Marginal Pricing",
		"Power Re-dispatch" };
	}

	public void setLseShock(final double shock) {

		Settings.DEMAND_SHOCK_MAGNITUDE = shock;
	}

	public void setNodeList(final String[][] nodeList) {
		this.nodeList = nodeList;
	}

	public void setShock(final boolean shock) {
		Settings.DEMAND_SHOCK = shock;
	}

	public void setShockDay(final int shockDay) {
		Settings.DEMAND_SHOCK_DAY = shockDay;
	}

}
