package acewem.market;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Random;

import org.jfree.data.xy.XYSeries;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.SimplePortrayal2D;
import acewem.initials.Settings;
import acewem.utilities.learning.ReinforcementLearning;
import acewem.utilities.learning.StochasticOptimisation;

public class GenCo extends SimplePortrayal2D implements Steppable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/** Daily Net Earnings = sum of HourlyNetEarning over 24 hours. */
	private double[] dailyNetEarnings;
	/** dailyProfit = sum of HourlyProfit over 24 hours. */
	private double[] dailyProfit;
	private double[] dailyTrueProfit;
	/** Fixed cost of production. */
	private double fixedCost;
	/**
	 * GenCo's accumulative money holding, money (new) = money(previous) +
	 * DailyProfit(new) (array).
	 */
	private double wealth;

	private String[][] data;
	private String[][] model;
	private double[][] commitment;

	private double powerToSellinBM;
	private double powerToBuyinBM;
	private double[][] forecast;
	HashMap<Integer, double[]> forecastedParameters = new HashMap<Integer, double[]>();

	private Random random;

	// this is public to avoid showing
	// them in the GUI (no need for get() and set() methods).
	public XYSeries[] slopeR;
	public XYSeries[] interceptR;
	public XYSeries slopeT;
	public XYSeries interceptT;
	public XYSeries[] powerCommitmentR;
	public XYSeries[] powerCommitmentT;
	public XYSeries[] averagePriceR;
	public XYSeries[] averagePriceT;
	public XYSeries[] dailyprofitR;
	public XYSeries[] dailyprofitT;

	public XYSeries wealthLevel;
	public XYSeries[] distribution;

	private double totalDailyPrice[];
	private double[] totalDailyTruePrice;
	private double[] totalDailyTrueCommitment;
	private double[] totalDailyCommitment;

	private double[] aR;
	private double[] bR;
	private double[][] capUR;
	private double[] capLR;

	private StochasticOptimisation stchOpt;
	private ReinforcementLearning[] reinfLearn;
	private final ACEWEMmodel market;

	private int[] distrOfChoice;
	private double[] objectiveValue;
	private double[] probability;

	private final double[] genCoData;

	public GenCo(final ACEWEMmodel market, final double[] initData,
			final int numOfGenCos) {

		genCoData = initData;
		this.market = market;

		if (market.isFromGUI()) {
			setGraphAxis();
		}
		
		if(Settings.SUPPLY_SHOCK){
			random = new Random();
		}

		switch (Settings.MARKETS) {
		case Settings.SINGLE_DA:

			data = new String[Settings.TWO][Settings.ONE];
			data[Settings.PRICE][Settings.DA] = Settings.GROWTH_RATE;
			data[Settings.MW][Settings.DA] = Settings.GROWTH_RATE;

			model = new String[Settings.TWO][Settings.ONE];
			model[Settings.PRICE][Settings.DA] = Settings.AUTOREGRESSIVE;
			model[Settings.MW][Settings.DA] = Settings.AUTOREGRESSIVE;

			aR = new double[Settings.ONE];
			aR[Settings.DA] = genCoData[Settings.INTERCEPT];

			bR = new double[Settings.ONE];
			bR[Settings.DA] = genCoData[Settings.SLOPE];

			capUR = new double[Settings.ONE][Settings.ONE];
			capUR[Settings.DA][0] = genCoData[Settings.GENCO_CAPACITY_UPPER];

			capLR = new double[Settings.ONE];
			capLR[Settings.DA] = genCoData[Settings.GENCO_CAPACITY_LOWER];

			// forecastPower = new double[Settings.ONE];
			forecast = new double[Settings.TWO][Settings.ONE];
			dailyProfit = new double[Settings.ONE];
			dailyNetEarnings = new double[Settings.ONE];
			totalDailyPrice = new double[Settings.ONE];
			totalDailyTruePrice = new double[Settings.ONE];
			dailyTrueProfit = new double[Settings.ONE];
			totalDailyCommitment = new double[Settings.ONE];
			totalDailyTrueCommitment = new double[Settings.ONE];
			commitment = new double[Settings.ONE][Settings.HOURS];

			if (Settings.LEARNALG == Settings.STOCH) {
				stchOpt = new StochasticOptimisation(this, numOfGenCos);
				distrOfChoice = new int[Settings.ONE];
				objectiveValue = new double[Settings.ONE];
				probability = new double[Settings.ONE];
			} else {
				final double[] reinfLearningParameters = new double[Settings.THIRTEEN];
				final Random random = new Random();

				reinfLearningParameters[Settings.COOLING] = 5529.4906;
				reinfLearningParameters[Settings.EXPEREMENTATION] = 0.983;
				reinfLearningParameters[Settings.PROPENSITY] = 552949.06;
				reinfLearningParameters[Settings.RECENCY] = 0.017;
				reinfLearningParameters[Settings.RANADOMSEED] = (getIdNum() / 100) * 10e8;
				reinfLearningParameters[Settings.M1] = 10.0;
				reinfLearningParameters[Settings.M2] = 10.0;
				reinfLearningParameters[Settings.M3] = 1.0;
				reinfLearningParameters[Settings.RIMaxL] = 0.75;
				reinfLearningParameters[Settings.RIMaxU] = 0.75;
				reinfLearningParameters[Settings.RIMinC] = 1.0;
				reinfLearningParameters[Settings.PRICECAP] = 1000.0;
				reinfLearningParameters[Settings.SLOPESTART] = 0.0001;

				reinfLearn = new ReinforcementLearning[Settings.ONE];
				reinfLearn[Settings.DA] = new ReinforcementLearning(
						reinfLearningParameters, Settings.DA, this);
			}

			break;
		case Settings.DA_BM:

			data = new String[Settings.TWO][Settings.THREE];
			data[Settings.PRICE][Settings.DA] = Settings.GROWTH_RATE;
			data[Settings.PRICE][Settings.BM_INC] = Settings.GROWTH_RATE;
			data[Settings.PRICE][Settings.BM_DEC] = Settings.GROWTH_RATE;

			data[Settings.MW][Settings.DA] = Settings.GROWTH_RATE;
			data[Settings.MW][Settings.BM_INC] = Settings.GROWTH_RATE;
			data[Settings.MW][Settings.BM_DEC] = Settings.GROWTH_RATE;

			model = new String[Settings.TWO][Settings.THREE];
			model[Settings.PRICE][Settings.DA] = Settings.AUTOREGRESSIVE;
			model[Settings.PRICE][Settings.BM_INC] = Settings.AUTOREGRESSIVE;
			model[Settings.PRICE][Settings.BM_DEC] = Settings.AUTOREGRESSIVE;

			model[Settings.MW][Settings.DA] = Settings.AUTOREGRESSIVE;
			model[Settings.MW][Settings.BM_INC] = Settings.AUTOREGRESSIVE;
			model[Settings.MW][Settings.BM_DEC] = Settings.AUTOREGRESSIVE;

			aR = new double[Settings.THREE];
			aR[Settings.DA] = genCoData[Settings.INTERCEPT];
			aR[Settings.BM_INC] = genCoData[Settings.INTERCEPT];
			aR[Settings.BM_DEC] = genCoData[Settings.INTERCEPT];

			bR = new double[Settings.THREE];
			bR[Settings.DA] = genCoData[Settings.SLOPE];
			bR[Settings.BM_INC] = genCoData[Settings.SLOPE];
			bR[Settings.BM_DEC] = genCoData[Settings.SLOPE] / 2.0;

			// here true capacity is assigned to DA market only, the upper
			// capacities for incs and decs are left zeros and will be assigned
			// when true DA
			// market is cleared in initialiseACEWEMmodel() method in
			// ACEWEMmodel class
			capUR = new double[Settings.THREE][Settings.HOURS];
			capUR[Settings.DA][0] = genCoData[Settings.GENCO_CAPACITY_UPPER];

			capLR = new double[Settings.THREE];
			capLR[Settings.DA] = genCoData[Settings.GENCO_CAPACITY_LOWER];
			capLR[Settings.BM_INC] = genCoData[Settings.GENCO_CAPACITY_LOWER];
			capLR[Settings.BM_DEC] = genCoData[Settings.GENCO_CAPACITY_LOWER];

			forecast = new double[Settings.THREE][Settings.THREE];
			dailyProfit = new double[Settings.THREE];
			dailyNetEarnings = new double[Settings.THREE];
			totalDailyPrice = new double[Settings.THREE];
			totalDailyTruePrice = new double[Settings.THREE];
			dailyTrueProfit = new double[Settings.THREE];
			totalDailyCommitment = new double[Settings.THREE];
			totalDailyTrueCommitment = new double[Settings.THREE];
			commitment = new double[Settings.THREE][Settings.HOURS];

			if (Settings.LEARNALG == Settings.STOCH) {
				stchOpt = new StochasticOptimisation(this, numOfGenCos);
				distrOfChoice = new int[Settings.THREE];
				objectiveValue = new double[Settings.THREE];
				probability = new double[Settings.THREE];
			} else {

				final double[] reinfLearningParameters = new double[Settings.THIRTEEN];
				final Random random = new Random();

				reinfLearningParameters[Settings.COOLING] = 5529.4906;
				reinfLearningParameters[Settings.EXPEREMENTATION] = 0.95;
				reinfLearningParameters[Settings.PROPENSITY] = 552949.06;
				reinfLearningParameters[Settings.RECENCY] = 0.05;
				reinfLearningParameters[Settings.RANADOMSEED] = (getIdNum() / 100) * 10e8;
				reinfLearningParameters[Settings.M1] = 10.0;
				reinfLearningParameters[Settings.M2] = 10.0;
				reinfLearningParameters[Settings.M3] = 1.0;
				reinfLearningParameters[Settings.RIMaxL] = 0.75;
				reinfLearningParameters[Settings.RIMaxU] = 0.75;
				reinfLearningParameters[Settings.RIMinC] = 1.0;
				reinfLearningParameters[Settings.PRICECAP] = 1000.0;
				reinfLearningParameters[Settings.SLOPESTART] = 0.0001;

				reinfLearn = new ReinforcementLearning[Settings.THREE];
				reinfLearn[Settings.DA] = new ReinforcementLearning(
						reinfLearningParameters, Settings.DA, this);
				reinfLearn[Settings.BM_INC] = new ReinforcementLearning(
						reinfLearningParameters, Settings.BM_INC, this);
				reinfLearn[Settings.BM_DEC] = new ReinforcementLearning(
						reinfLearningParameters, Settings.BM_DEC, this);

			}
			break;
		default:
			System.err.println(" The requested power market "
					+ "cannot be modelled in GenCo");
		}
	}

	public void appendLastResponseValue(final int whichMarket) {

		double lastPrice;
		if (Settings.MARKETS == Settings.SINGLE_DA) {
			lastPrice = totalDailyPrice[whichMarket] / Settings.HOURS;
		} else {

			double totalMCP = 0.0;
			for (int h = 0; h < Settings.HOURS; h++) {
				totalMCP += market.getMCP()[whichMarket][h];
			}
			lastPrice = totalMCP / Settings.HOURS;
		}
		double lastMW = totalDailyCommitment[whichMarket]
				/ Settings.HOURS;
		
/*		if (lastPrice < 1.0) {
			if (lastPrice >  0.1) {
				lastPrice = 1.0;
			} else {
				lastPrice = 0;
			}
		}
		
		if (lastMW < 1.0) {
			if (lastMW >  0.1) {
				lastMW = 1.0;
			} else {
				lastMW = 0;
			}
		}
*/	
		if (lastPrice < 0.0) {
			lastPrice = 0.0;
		}
		
		if (lastMW < 0.0) {
			lastMW = 0.0;
		}

		final boolean[] decision = getStchOpt()
				.appendLastObservationToResponse(whichMarket,
						new double[] { lastPrice, lastMW }, this);

		if (decision[Settings.PRICE]) {
			getStchOpt().appendLastObservationToDesign(Settings.PRICE,
					whichMarket, this);
			getStchOpt()
					.setLastResponse(Settings.PRICE, whichMarket, lastPrice);
		}

		if (decision[Settings.MW]) {
			getStchOpt().appendLastObservationToDesign(Settings.MW,
					whichMarket, this);
			getStchOpt().setLastResponse(Settings.MW, whichMarket, lastMW);
		}
	}

	public double getaR(final int whichMarket) {
		return aR[whichMarket];
	}

	public double getaT() {
		return genCoData[Settings.INTERCEPT];
	}

	public double getbR(final int whichMarket) {
		return bR[whichMarket];
	}

	public double getbT() {
		return genCoData[Settings.SLOPE];
	}

	public double getCapLR(final int whichMarket) {
		return capLR[whichMarket];
	}

	public double getcapTL() {
		return genCoData[Settings.GENCO_CAPACITY_LOWER];
	}

	public double getcapTU() {
		return genCoData[Settings.GENCO_CAPACITY_UPPER];
	}

	public double getCapUR(final int whichMarket, final int hour) {
		return capUR[whichMarket][hour];
	}

	public double getCommitment(final int whichMarket, final int hour) {
		return commitment[whichMarket][hour];
	}

	public double getDailyProfit(final int whichMarket) {
		return dailyProfit[whichMarket];
	}

	public double getDailyTrueProfit(final int whichMarket) {
		return dailyTrueProfit[whichMarket];
	}

	public String getData(final int fittingWhat, final int whichMarket) {
		return data[fittingWhat][whichMarket];
	}

	public int getDistrOfChoice(final int whichMarket) {
		return distrOfChoice[whichMarket];
	}

	public double getForecast(final int what, final int whichMarket) {
		return forecast[what][whichMarket];
	}

	public double[] getForecastedParameters(final int whichMarket) {
		return forecastedParameters.get(whichMarket);
	}

	public double getIdNum() {
		return genCoData[Settings.GENCO];
	}

	public ACEWEMmodel getMarket() {
		return market;
	}
	
	public boolean hideMarket() {
		return true;
	}

	public String[][] getModel() {
		return model;
	}
	
	public boolean hideModel() {
		return true;
	}

	public double getNode() {
		return genCoData[Settings.GENCO_AT_NODE];
	}

	public double getObjectiveValue(final int whichMarket) {
		return objectiveValue[whichMarket];
	}

	public double getPowerToBuyinBM() {
		return powerToBuyinBM;
	}

	public double getPowerToSellinBM() {
		return powerToSellinBM;
	}

	public double getProbability(final int whichMarket) {
		return probability[whichMarket];
	}

	public ReinforcementLearning getReinfLearnerPrice(final int whichMarket) {
		return reinfLearn[whichMarket];
	}

	private StochasticOptimisation getStchOpt() {
		return stchOpt;
	}

	public double getTotalDailyCommitment(final int whichMarket) {
		return totalDailyCommitment[whichMarket];
	}

	public double getTotalDailyPrice(final int whichMarket) {
		return totalDailyPrice[whichMarket];
	}

	public double getTotalDailyTrueCommitment(final int whichMarket) {
		return totalDailyTrueCommitment[whichMarket];
	}

	public double getTruePrice(final int whichMarket) {
		return totalDailyTruePrice[whichMarket];
	}

	public double getWealth() {
		return wealth;
	}

	/**
	 * GenCo's learning (updating propensity based on current period DailyProfit
	 * or net Earnings).
	 */
	public void learn(final int whichMarket) {

		// switch (rewardSelection) {
		// case Settings.PROFIT:
		reinfLearn[whichMarket].updateLearning(dailyProfit[whichMarket]);
		// break;
		// case Settings.NETEARN:
		// reinfLearn[whichMarket].updateLearning(dailyNetEarnings[whichMarket]);
		// break;
		// default:
		// System.err.println("The reward is not recognised");
		// }
	}

	/**
	 * Sets dailyNetEarnings, dailyProfit, dailyRevenue equal to zero and
	 * initiates a new SimpleStatelessLearner.
	 */
	public final void resetLerner() {
		dailyNetEarnings = null;
		dailyProfit = null;

		switch (Settings.MARKET_SWITHCER) {
		case Settings.DA:

			reinfLearn[Settings.DA].updateLearner();
			break;
		case Settings.BM:

			reinfLearn[Settings.BM_INC].updateLearner();
			reinfLearn[Settings.BM_DEC].updateLearner();
			break;
		default:
			System.err.println(" The requested power market "
					+ "is not implemented");
		}
	}

	public void setCapUR(final int whichMarket, final int hour, final double cap) {
		capUR[whichMarket][hour] = cap;
	}

	public void setCommitment(final int whichMarket, final int hour,
			final double value) {
		commitment[whichMarket][hour] = value;
	}

	public void setDailyTrueProfit(final int whichMarket, final double profit) {
		dailyTrueProfit[whichMarket] = profit;
	}

	public void setDistrOfChoice(final int whichMarket, final int distrOfChoice) {
		this.distrOfChoice[whichMarket] = distrOfChoice;
	}

	public void setForecast(final int fittingWhat, final int whichMarket,
			final double value) {
		forecast[fittingWhat][whichMarket] = value;
	}

	public void setForecastedParameters(final int whichMarket,
			final double[] fparam) {
		forecastedParameters.put(whichMarket, fparam);
	}

	private void setGraphAxis() {

		if (Settings.LEARNALG == Settings.STOCH) {
			switch (Settings.MARKETS) {
			case Settings.SINGLE_DA:
				distribution = new XYSeries[Settings.ONE];
				distribution[Settings.DA] = new XYSeries("Distribution DA");
				break;
			case Settings.DA_BM:
				distribution = new XYSeries[Settings.THREE];
				distribution[Settings.DA] = new XYSeries("Distribution DA");
				distribution[Settings.BM_INC] = new XYSeries(
						"Distribution BM INCs");
				distribution[Settings.BM_DEC] = new XYSeries(
						"Distribution BM DECs");
				break;
			default:
				System.err.println(" The requested power market "
						+ "cannot be modelled in GenCo");
			}
		}
		interceptT = new XYSeries("True intercept of MC");
		slopeT = new XYSeries("True slope of MC");
		wealthLevel = new XYSeries("Wealth");

		switch (Settings.MARKETS) {
		case Settings.SINGLE_DA:

			interceptR = new XYSeries[Settings.ONE];
			slopeR = new XYSeries[Settings.ONE];
			dailyprofitR = new XYSeries[Settings.ONE];
			dailyprofitT = new XYSeries[Settings.ONE];
			powerCommitmentR = new XYSeries[Settings.ONE];
			powerCommitmentT = new XYSeries[Settings.ONE];
			averagePriceR = new XYSeries[Settings.ONE];
			averagePriceT = new XYSeries[Settings.ONE];

			interceptR[Settings.DA] = new XYSeries(
					"DA intercept of MC (learning)");
			slopeR[Settings.DA] = new XYSeries("DA slope of MC (learning)");
			dailyprofitR[Settings.DA] = new XYSeries("DA profit (learning)");
			dailyprofitT[Settings.DA] = new XYSeries("DA profit (no learning)");
			powerCommitmentR[Settings.DA] = new XYSeries(
					"DA electricity dispatch (learning)");
			powerCommitmentT[Settings.DA] = new XYSeries(
					"DA electricity dispatch (no learning)");
			averagePriceR[Settings.DA] = new XYSeries("Average LMP (learning)");
			averagePriceT[Settings.DA] = new XYSeries(
					"Average LMP (no learning)");

			break;
		case Settings.DA_BM:

			interceptR = new XYSeries[Settings.THREE];
			slopeR = new XYSeries[Settings.THREE];
			dailyprofitR = new XYSeries[Settings.FOUR];
			dailyprofitT = new XYSeries[Settings.FOUR];
			powerCommitmentR = new XYSeries[Settings.THREE];
			powerCommitmentT = new XYSeries[Settings.THREE];
			averagePriceR = new XYSeries[Settings.THREE];
			averagePriceT = new XYSeries[Settings.THREE];

			interceptR[Settings.DA] = new XYSeries(
					"DA intercept of MC (learning)");
			interceptR[Settings.BM_INC] = new XYSeries(
					"BM Incs intercept of MC (learning)");
			interceptR[Settings.BM_DEC] = new XYSeries(
					"BM Decs intercept of MC (learning)");
			slopeR[Settings.DA] = new XYSeries("DA slope of MC (learning)");
			slopeR[Settings.BM_INC] = new XYSeries(
					"BM Incs slope of MC (learning)");
			slopeR[Settings.BM_DEC] = new XYSeries(
					"BM Decs slope of MC (learning)");
			dailyprofitR[Settings.DA] = new XYSeries("DA profit (learning)");
			dailyprofitT[Settings.DA] = new XYSeries("DA profit (no learning)");
			dailyprofitR[Settings.BM_INC] = new XYSeries(
					"BM Inc profit (learning)");
			dailyprofitT[Settings.BM_INC] = new XYSeries(
					"BM Inc profit (no learning)");
			dailyprofitR[Settings.BM_DEC] = new XYSeries(
					"BM Decs profit (learning)");
			dailyprofitT[Settings.BM_DEC] = new XYSeries(
					"BM Decs profit (no learning)");
			dailyprofitR[Settings.INC_DEC] = new XYSeries(
					"Sum of Incs and Decs profit (learning)");
			dailyprofitT[Settings.INC_DEC] = new XYSeries(
					"Sum of Incs and Decs profit (no learning)");
			powerCommitmentR[Settings.DA] = new XYSeries(
					"DA electricity dispatch (learning)");
			powerCommitmentR[Settings.BM_INC] = new XYSeries(
					"BM electricity Incs (learning)");
			powerCommitmentR[Settings.BM_DEC] = new XYSeries(
					"BM electricity Decs (learning)");
			powerCommitmentT[Settings.DA] = new XYSeries(
					"DA electricity dispatch (no learning)");
			powerCommitmentT[Settings.BM_INC] = new XYSeries(
					"DA electricity Incs (no learning)");
			powerCommitmentT[Settings.BM_DEC] = new XYSeries(
					"DA electricity Decs (no learning)");
			averagePriceR[Settings.DA] = new XYSeries(
					"Average MCP DA (learning)");
			averagePriceR[Settings.BM_INC] = new XYSeries(
					"Average MCP INCs (learning)");
			averagePriceR[Settings.BM_DEC] = new XYSeries(
					"Average MCP DECs (learning)");
			averagePriceT[Settings.DA] = new XYSeries(
					"Average MCP DA (no learning)");
			averagePriceT[Settings.BM_INC] = new XYSeries(
					"Average MCP INCs (no learning)");
			averagePriceT[Settings.BM_DEC] = new XYSeries(
					"Average MCP DECs (no learning)");

			break;
		default:
			System.err.println(" The requested power market "
					+ "cannot be modelled in GenCo");
		}
	}

	public void setObjectiveValue(final int whichMarket, final double objValue) {
		objectiveValue[whichMarket] = objValue;
	}

	public void setProbability(final int whichMarket, final double prob) {
		probability[whichMarket] = prob;
	}

	public void setTotalDailyCommitment(final int whichMarket,
			final double commit) {
		totalDailyCommitment[whichMarket] = commit;
	}

	public void setTotalDailyTrueCommitment(final int whichMarket,
			final double price) {
		totalDailyTrueCommitment[whichMarket] = price;
	}

	public void setTotalDailyTruePrice(final int whichMarket, final double price) {
		totalDailyTruePrice[whichMarket] = price;
	}

	/**
	 * This method is called by SimState when the agent is stepped.
	 * 
	 * @param state
	 *            - object of SimState
	 */
	public final void step(final SimState state) {

		switch (Settings.LEARNALG) {
		case Settings.REINF:
			submitReportedSupplyOfferReinfLearning();
			break;
		case Settings.STOCH:
			submitReportedOfferStochasticOptimisation();
			break;
		default:
			System.err.println("The learning algorithm is not recognised"
					+ " (GenCo class)");
		}
	}

	/**
	 * Rewrites reportedSupplyOffer hashtable with new reported supply offer
	 * parameters: aR,bR,capRU,capRL determined by Gamlss stochastic
	 * optimization algorithm.
	 */
	private void submitReportedOfferStochasticOptimisation() {

		double[] pair = new double[Settings.TWO];
		switch (Settings.MARKET_SWITHCER) {
		case Settings.DA:

			if(Settings.MARKET_SWITHCER == Settings.DA && market.getDay() > 1 && getIdNum() == 83) {
				int tt = 0;
			}	
			
			pair = stchOpt.submitReportedOfferStochasticOptimisation(this,
					Settings.DA);
			aR[Settings.DA] = pair[0];
			bR[Settings.DA] = pair[1];
//System.out.println(pair[0]+"  "+pair[1]);			
			capLR[Settings.DA] = getcapTL();
			
		

			if (Settings.SUPPLY_SHOCK && getIdNum() == 5) {
				capUR[Settings.DA][0] = getcapTU()
						* (0.3 + 0.7 * random.nextDouble());
			} else {
				capUR[Settings.DA][0] = getcapTU();
			}
				
			
			// if (flag){
			// capUR[Settings.DA][0] = getForecastPower()+1;
			// flag = false;
			// }
			// capUR[Settings.DA][0] = getForecastPower();

			// Print some results here to be deleted
			// int choice = getDistrOfChoice(Settings.DA);
			// int prob = (int) reinfLearn[Settings.DA].getChoiceID();
			// double[] t1 = reinfLearn[Settings.DA].getDProbability();
			// System.out.println();
			// for (int i = 0; i < t1.length; i++){
			// System.out.print(Support.roundOff(t1[i], 3)+" ");

			// if(Double.isNaN(t1[i])){
			// System.err.println("Probability is NaN for " + id);
			// }
			// }
			// System.out.print(t1.length);
			// System.out.println();
			// System.out.println(market.schedule.getSteps()+"  "+id+"  "+choice
			// +"  "+//prob+"   "+
			// Support.roundOff(reinfLearn[Settings.DA].getChoiceProbability(),
			// 3));
			//System.out.println("-----------------");

			// }else {
			// aR[Settings.DA] = getaT();
			// bR[Settings.DA] = getbT();
			// capLR[Settings.DA] = getcapTL();
			// capUR[Settings.DA][0] = getcapTU();
			// }
			// ------------------------------------------
			break;
		case Settings.BM:

			pair = stchOpt.submitReportedOfferStochasticOptimisation(this,
					Settings.BM_INC);
			aR[Settings.BM_INC] = pair[0];
			bR[Settings.BM_INC] = pair[1];
			capLR[Settings.BM_INC] = getcapTL();
			for (int hour = 0; hour < Settings.HOURS; hour++) {
				capUR[Settings.BM_INC][hour] = capUR[Settings.DA][0]
						- commitment[Settings.DA][hour];
			}
//ystem.out.println(pair[0]+"  "+pair[1]);	

			pair = stchOpt.submitReportedOfferStochasticOptimisation(this,
					Settings.BM_DEC);
			// reported aR and bR reasonably should be less or
			// at least equal to the true coefficients
			// otherwise it makes no sense to bid them
			aR[Settings.BM_DEC] = pair[0];
			bR[Settings.BM_DEC] = pair[1];
			capLR[Settings.BM_DEC] = getcapTL();
			for (int hour = 0; hour < Settings.HOURS; hour++) {
				capUR[Settings.BM_DEC][hour] = commitment[Settings.DA][hour];
			}
//System.out.println("*****************************  "+pair[0]+"  "+pair[1]);				
			break;
		default:
			System.err.println(" The requested power market "
					+ "is not implemented");
		}
	}

	/**
	 * Rewrites reportedSupplyOffer hashtable with new reported supply offer
	 * parameters: aR,bR,capRU,capRL determined by Reinforcement learning
	 * algorithm.
	 */
	private void submitReportedSupplyOfferReinfLearning() {

		double[] triplet = null;
		switch (Settings.MARKET_SWITHCER) {
		case Settings.DA:
//System.out.println("DA  " + market.getDay()+"   "+(int) getIdNum());
//System.out.println("---------------------");			
			
			triplet = (double[]) reinfLearn[Settings.DA].getLearner()
					.chooseActionRaw();

			if (triplet[0] < getaT()) {
				aR[Settings.DA] = getaT();
			} else {
				aR[Settings.DA] = triplet[0];
			}

			if (triplet[1] < getbT()) {
				bR[Settings.DA] = getbT();
			} else {
				bR[Settings.DA] = triplet[1];
			}

			capLR[Settings.DA] = getcapTL();
			capUR[Settings.DA][0] = getcapTU();
			break;
		case Settings.BM:
//System.out.println("BM  " + market.getDay()+"   "+(int) getIdNum());
//System.out.println("---------------------");		

			
			triplet = (double[]) reinfLearn[Settings.BM_INC].getLearner()
					.chooseActionRaw();

			if (triplet[0] < getaT()) {
				aR[Settings.BM_INC] = getaT();
			} else {
				aR[Settings.BM_INC] = triplet[0];
			}

			if (triplet[1] < getbT()) {
				bR[Settings.BM_INC] = getbT();
			} else {
				bR[Settings.BM_INC] = triplet[1];
			}

			capLR[Settings.BM_INC] = getcapTL();
			for (int hour = 0; hour < Settings.HOURS; hour++) {
				capUR[Settings.BM_INC][hour] = getcapTU()
						- commitment[Settings.DA][hour];
			}

			triplet = (double[]) reinfLearn[Settings.BM_DEC].getLearner()
					.chooseActionRaw();
			// reported aR and bR reasonably should be less or
			// at least equal to the true coefficents
			// otherwise it makes no sense to bid them
			if (triplet[0] > getaT()) {
				aR[Settings.BM_DEC] = getaT();
			} else {
				aR[Settings.BM_DEC] = triplet[0];
			}

			if (triplet[1] > getbT() / 2.0) {
				bR[Settings.BM_DEC] = getbT() / 2.0;
			} else {
				bR[Settings.BM_DEC] = triplet[1];
			}
			capLR[Settings.BM_DEC] = getcapTL();
			for (int hour = 0; hour < Settings.HOURS; hour++) {
				capUR[Settings.BM_DEC][hour] = commitment[Settings.DA][hour];
			}
			break;
		default:
			System.err.println(" The requested power market "
					+ "is not implemented");
		}
	}

	public void updatePerformanceBM() {

		dailyProfit[Settings.BM_INC] = 0.0;
		dailyProfit[Settings.BM_DEC] = 0.0;
		totalDailyPrice[Settings.BM_INC] = 0.0;
		totalDailyPrice[Settings.BM_DEC] = 0.0;
		totalDailyCommitment[Settings.BM_INC] = 0.0;
		totalDailyCommitment[Settings.BM_DEC] = 0.0;
		// dailyNetEarnings[Settings.BM_INC] = 0.0;
		// dailyNetEarnings[Settings.BM_DEC] = 0.0;

		for (int hour = 0; hour < Settings.HOURS; hour++) {

			final double cInc = commitment[Settings.BM_INC][hour];
			final double cDec = commitment[Settings.BM_DEC][hour];

			final double hourlyVariableCostInc = cInc * getaT() + getbT()
					* cInc * cInc;
			// double hourlyVariableCostDec = cDec * getaR(Settings.DA) +
			// getbR(Settings.DA) * cDec * cDec;
			final double hourlyVariableCostDec = (getaT() + getbT() * cDec)
					* cDec;

			final double hourlyTotalCostInc = hourlyVariableCostInc;
			final double hourlyTotalCostDec = hourlyVariableCostDec;

			// GenCos are paid what they ask for
			// double price_INC = getaR(Settings.BM_INC) + 2 *
			// getbR(Settings.BM_INC) * cInc;
			// double price_DEC = getaR(Settings.BM_DEC) + 2 *
			// getbR(Settings.BM_DEC) * cDec;

			double price_INC = 0;
			if (cInc != Settings.ZERO) {
				price_INC = getaR(Settings.BM_INC) + 2 * getbR(Settings.BM_INC)
						* cInc;
			}

			double price_DEC = 0;
			if (cDec != Settings.ZERO) {
				price_DEC = getaR(Settings.BM_DEC) + 2 * getbR(Settings.BM_DEC)
						* cDec;
			}

			final double hourlyProfitInc = price_INC * cInc
					- hourlyTotalCostInc;
			final double hourlyProfitDec = hourlyTotalCostDec - price_DEC
					* cDec;

			// double hourlyNetEarningInc =
			// double hourlyNetEarningDec =

			dailyProfit[Settings.BM_INC] += hourlyProfitInc;
			dailyProfit[Settings.BM_DEC] += hourlyProfitDec;

			// dailyNetEarnings[Settings.BM_INC] += hourlyNetEarningInc;
			// dailyNetEarnings[Settings.BM_DEC] += hourlyNetEarningDec;

			totalDailyCommitment[Settings.BM_INC] += cInc;
			totalDailyCommitment[Settings.BM_DEC] += cDec;

			totalDailyPrice[Settings.BM_INC] += price_INC;
			totalDailyPrice[Settings.BM_DEC] += price_DEC;
		}
		wealth += dailyProfit[Settings.BM_INC]
				+ dailyNetEarnings[Settings.BM_DEC];

	}

	public void updatePerformanceDA() {

		dailyProfit[Settings.DA] = 0.0;
		dailyNetEarnings[Settings.DA] = 0.0;
		totalDailyCommitment[Settings.DA] = 0.0;
		powerToSellinBM = 0.0;
		powerToBuyinBM = 0.0;
		totalDailyPrice[Settings.DA] = 0.0;
		final int genAtNode = (int) genCoData[Settings.GENCO_AT_NODE];

		for (int hour = 0; hour < Settings.HOURS; hour++) {

			final double c = commitment[Settings.DA][hour];
			final double hourlyVariableCost = c * getaT() + getbT() * c * c;
			final double hourlyTotalCost = hourlyVariableCost + fixedCost;

			double price = Double.NaN;
			if (Settings.MARKETS == Settings.SINGLE_DA) {
				price = market.getLMP()[genAtNode - 1][hour];
			} else {
				// Get paid what asked for
				if (c == 0) {
					price = 0;
				} else {
					price = getaR(Settings.DA) + 2 * getbR(Settings.DA) * c;
				}
				powerToSellinBM += getCapUR(Settings.DA, 0) - c;
				powerToBuyinBM += c;
			}
			final double hourlyRevenue = c * price;
			final double hourlyProfit = hourlyRevenue - hourlyTotalCost;
			final double hourlyNetEarning = hourlyRevenue - hourlyVariableCost;

			dailyProfit[Settings.DA] += hourlyProfit;
			dailyNetEarnings[Settings.DA] += hourlyNetEarning;
			totalDailyCommitment[Settings.DA] += c;

			totalDailyPrice[Settings.DA] += price;
		}
		wealth = wealth + dailyProfit[Settings.DA];
	}
	
	public void draw(final Object object, final Graphics2D graphics,
			final DrawInfo2D info) {
		double width = info.draw.width * Settings.AGENT_WIDTH;
		double height = info.draw.height * Settings.AGENT_HEIGHT;
		final int x = (int) (info.draw.x - width / 2.0);
		final int y = (int) (info.draw.y - height / 2.0);
		final int w = (int) (width);
		final int h = (int) (height);
		
		graphics.setColor(Color.green);
		graphics.fillRect(x, y, w, h);
		
		if (Settings.MODELTYPE != Settings.UK) {
			final String label = "G" + Integer.toString((int) getIdNum()); 
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
