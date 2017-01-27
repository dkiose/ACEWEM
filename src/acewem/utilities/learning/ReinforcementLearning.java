package acewem.utilities.learning;

import java.util.ArrayList;

import acewem.initials.Settings;
import acewem.market.GenCo;
import edu.iastate.jrelm.core.SimpleAction;
import edu.iastate.jrelm.rl.SimpleStatelessLearner;
import edu.iastate.jrelm.rl.rotherev.REPolicy;
import edu.iastate.jrelm.rl.rotherev.variant.VREParameters;
import gamlss.distributions.DistributionSettings;

public class ReinforcementLearning {

	/**
	 * SimpleStatelessLearner combines all of JReLM’s pre-built
	 * ReinforcementLearners that implement stateless algorithms.
	 */
	private SimpleStatelessLearner learner;
	/**
	 * For SimpleStatelessLearner to act as a VRELearner, it must receive a
	 * VREParameters object.
	 */
	private VREParameters learningParams;
	/**
	 * Learning algorithm of agents: Stochastic optimization or reinforcement
	 * learning.
	 */
	private int learnAlg;
	/**
	 * GenCo action domain holds supply offer parameters in case of
	 * reinforcement learning, or distributions in case of Stochastic
	 * optimization.
	 */
	private ArrayList adList;

	/**
	 * Constructor.
	 * 
	 * @param initData
	 *            - initial data supplied from Genco
	 * @param whichMarket
	 *            - shows at wich market the operstion is happening: Day-ahead,
	 *            BM_inc or BM_decs.
	 */
	public ReinforcementLearning(final double[] initData,
			final int whichMarket, final GenCo gen) {

		switch (Settings.LEARNALG) {
		case Settings.REINF:
			learningParams = new VREParameters(initData[Settings.COOLING],
					initData[Settings.EXPEREMENTATION],
					initData[Settings.PROPENSITY], initData[Settings.RECENCY],
					(int) initData[Settings.RANADOMSEED]);
			adList = actionDomainConstructionABCap(initData[Settings.M1],
					initData[Settings.M2], initData[Settings.M3],
					initData[Settings.RIMaxL], initData[Settings.RIMaxU],
					initData[Settings.RIMinC]);
			adList = checkActionDomain(whichMarket, adList, gen, initData);
			break;
		case Settings.STOCH:
			// learningParams = new VREParameters(
			// (double) initData.get("coolingStoch"),
			// (double) initData.get("experimentationStoch"),
			// (double) initData.get("propensityStoch"),
			// (double) initData.get("recencyStoch"),
			// (int) initData.get("randomseed"));

			adList = actionDomainConstructionDistr((int) gen.getIdNum());
			break;
		default:
			System.err.println("The learning algorithm is not recognised"
					+ " (ReinforcementLearning class)");
		}
		learner = new SimpleStatelessLearner(learningParams, adList);
	}

	/**
	 * Constructs GenCo's action domain for Marginal cost curve coefficients and
	 * upper generating capacity.
	 * 
	 * @see DynTestAMES.JSLT.pdf values used in reinforcement learning process
	 * @return GenCo action domain for supply offer parameters
	 */
	private ArrayList<double[]> actionDomainConstructionABCap(final double m1,
			final double m2, final double m3, final double rIMaxL,
			final double rIMaxU, final double rIMinC) {
		final double someNumber = 10.0;
		final ArrayList<double[]> adMat = new ArrayList<double[]>();
		// double inc1 = rIMaxL / (m1 - 1);
		// double inc2 = rIMaxU / (m2 - 1);
		// double inc3 = (1 - rIMinC) / (m3 - 1);

		double inc1 = 0.0;
		double inc2 = 0.0;
		double inc3 = 0.0;

		if (m1 == 1) {
			inc1 = someNumber; // special case for only one choice of lower RI
		} else {
			inc1 = rIMaxL / (m1 - 1); // incremental step for lower RI
		}
		if (m2 == 1) {
			inc2 = someNumber; // special case for only one choice of upper RI
		} else {
			inc2 = rIMaxU / (m2 - 1); // incremental step for upper RI
		}
		if (m3 == 1) {
			inc3 = someNumber; // special case for only one choice of upper Cap
		} else {
			inc3 = (1 - rIMinC) / (m3 - 1); // incremental step for upper Cap
		}

		for (double i = 0; i <= rIMaxL; i = i + inc1) {
			for (double j = 0; j <= rIMaxU; j = j + inc2) {
				for (double k = 1 - rIMinC; k >= 0; k = k - inc3) {
					final double[] alpha = new double[3];
					alpha[0] = i;
					alpha[1] = j;
					alpha[2] = k + rIMinC;
					adMat.add(alpha);
				}
			}
		}
		return adMat;
	}

	/**
	 * Constructs GenCo's action domain for distributions used in Gamlss.
	 * 
	 * @return GenCo distributions action domain list
	 */
	private ArrayList<Integer> actionDomainConstructionDistr(final int getIdNum) {
		final ArrayList<Integer> adMat = new ArrayList<Integer>();
		// adMat.add(DistributionSettings.BCPE);

		// if (getIdNum == 3) {
		// adMat.add(DistributionSettings.SST);
		// } else {
		adMat.add(DistributionSettings.NO);
		// }
		// if(getIdNum == 4){
		// adMat.add(DistributionSettings.TF);
		// adMat.add(DistributionSettings.GA);
		// adMat.add(DistributionSettings.TF2);
		// adMat.add(DistributionSettings.ST3);
		// adMat.add(DistributionSettings.ST4);
		// adMat.add(DistributionSettings.JSUo);
		// adMat.add(DistributionSettings.JSU);
		// adMat.add(DistributionSettings.SST);
		// adMat.add(DistributionSettings.PE);
		// }
		// for (int i = 0; i < 24; i++){
		// adMat.add(DistributionSettings.NO);
		// adMat.add(DistributionSettings.TF);
		// adMat.add(DistributionSettings.JSU);
		// adMat.add(DistributionSettings.SST);
		// }
		return adMat;

	}

	/**
	 * Returns a new feasible (MC is not higher than price cap) action domain
	 * list for supply offer parameters.
	 * 
	 * @param actionList
	 *            - GenCo action domain holds supply offer parameters
	 * @param priceCap
	 *            - maximum level of MC
	 * @param slopeStart
	 *            - GenCo Slope-start control parameter
	 * @return GenCo action domain for supply offer parameters
	 */
	private ArrayList checkActionDomain(final int whichMarket,
			final ArrayList actionList, final GenCo gen, final double[] initData) {

		final ArrayList newActionList = new ArrayList();
		if (whichMarket == Settings.BM_DEC) {
			for (int i = 0; i < actionList.size(); i++) {
				final double[] action = (double[]) actionList.get(i);
				final double[] newAction = action.clone();
				if (!checkOverPriceCapDec(newAction, gen.getaT(), gen.getbT(),
						gen.getcapTL(), gen.getcapTU(),
						initData[Settings.PRICECAP],
						initData[Settings.SLOPESTART])) {
					newActionList.add(newAction);
				}
			}
		} else {
			for (int i = 0; i < actionList.size(); i++) {
				final double[] action = (double[]) actionList.get(i);
				final double[] newAction = action.clone();
				if (!checkOverPriceCap(newAction, gen.getaT(), gen.getbT(),
						gen.getcapTL(), gen.getcapTU(),
						initData[Settings.PRICECAP],
						initData[Settings.SLOPESTART])) {
					newActionList.add(newAction);
				}
			}
		}
		return newActionList;
	}

	/**
	 * Checks whether the reported marginal curve is less than price cap .
	 * 
	 * @param action
	 *            - reported marginal curve parameters
	 * @param priceCap
	 *            - maximum level of MC
	 * @param slopeStart
	 *            - GenCo Slope-start control parameter
	 * @return boolean
	 */
	private boolean checkOverPriceCap(final double[] action, final double aTrue,
			final double bTrue, final double capTL, final double capTU,
			final double priceCap, final double slopeStart) {
		
		double aT = aTrue / 2.0;
		double bT = bTrue / 2.0;
		
		final double lowerRI = action[0];
		final double upperRI = action[1];
		final double upperRCap = action[2];
		// Step 0: To get capMaxCalculated
		final double capRU = upperRCap * (capTU - capTL) + capTL;

		// Step 1: To get lR
		final double lR = (aT + 2 * bT * capTL) / (1 - lowerRI);

		// Step 2: To get uStart
		final double u = aT + 2 * bT * capRU;
		double uStart;
		if (lR < u) {
			uStart = u;
		} else {
			uStart = lR + slopeStart;
		}
		if (uStart >= priceCap) {
			return true;
		}
		// Step 3: To get uR
		final double uR = uStart / (1 - upperRI);
		// Step 4: To get bReported
		action[1] = 0.5 * ((uR - lR) / (capRU - capTL));
		// Step 5: To get aReported
		action[0] = lR - 2 * action[1] * capTL;
		// for PriceCap
		final double maxPrice = action[0] + 2 * action[1] * capRU;
		if (maxPrice > priceCap) {
			action[2] = (priceCap - action[0]) / (2 * action[1]);
		} else {
			action[2] = capRU;
		}
		return false;
	}

	private boolean checkOverPriceCapDec(final double[] action,
			final double aTrue, final double bTrue, final double capTL,
			final double capTU, final double priceCap, final double slopeStart) {
		final double lowerRI = action[0];
		final double upperRI = action[1];
		final double upperRCap = action[2];
		
		double aT = aTrue * 2.0;
		double bT = bTrue * 2.0;
		
		// Step 0: To get capMaxCalculated
		final double capRU = upperRCap * (capTU - capTL) + capTL;

		// Step 1: To get lR
		final double lR = (aT / 4.0 + 2 * (bT / 4.0) * capTL) / (1 - lowerRI);

		// Step 2: To get uStart
		final double u = aT / 4.0 + 2 * (bT / 10.0) * capRU;
		double uStart;
		if (lR < u) {
			uStart = u;
		} else {
			uStart = lR + slopeStart;
		}
		if (uStart >= aT + 2 * bT * capTU) {
			return true;
		}
		// Step 3: To get uR
		final double uR = uStart / (1 - upperRI);
		// Step 4: To get bReported
		action[1] = 0.5 * ((uR - lR) / (capRU - capTL));
		// Step 5: To get aReported
		action[0] = lR - 2 * action[1] * capTL;
		// for PriceCap

		final double maxPrice = action[0] + 2 * action[1] * capRU;
		if (maxPrice > aT + 2 * bT * capTU) {

			action[1] = (aT + 2 * bT * capTU - action[0]) / (2 * capRU);
		}
		action[2] = capRU;

		// System.out.println(action[0] + 2 * action[1] * action[2]
		// +"   "+(trueSupplyOffer.get("aT") + 2 * trueSupplyOffer.get("bT") *
		// trueSupplyOffer.get("capTU")));
		// System.out.println(action[0] +"   "+action[1]+"  "+action[2]);
		return false;
	}

	public double[] getAct() {
		final SimpleAction lastAction = (SimpleAction) learner.getPolicy()
				.getLastAction();
		return (double[]) lastAction.getAct();
	}

	public ArrayList getActionDomain() {
		return adList;
	}

	public int getChoiceID() {
		final SimpleAction lastAction = (SimpleAction) learner.getPolicy()
				.getLastAction();
		return lastAction.getID();
	}

	public double getChoiceProbability() {
		final REPolicy policy = (REPolicy) learner.getPolicy();
		final int choiceID = getChoiceID();
		return policy.getProbability(choiceID);
	}

	public double getChoicePropensity() {
		final REPolicy policy = (REPolicy) learner.getPolicy();
		final int choiceID = getChoiceID();
		return policy.getPropensity(choiceID);
	}

	public double[] getDProbability() {
		final REPolicy policy = (REPolicy) learner.getPolicy();
		return policy.getProbabilities();
	}

	public double[] getDPropensity() {
		final REPolicy policy = (REPolicy) learner.getPolicy();
		return policy.getPropensities();
	}

	public SimpleAction getLastAction() {
		return (SimpleAction) learner.getPolicy().getLastAction();
	}

	public SimpleStatelessLearner getLearner() {
		return learner;
	}

	public VREParameters getLearningParams() {
		return learningParams;
	}

	public REPolicy getPolicy() {
		return (REPolicy) learner.getPolicy();
	}

	public void initialiseLearner() {
		learner = new SimpleStatelessLearner(learningParams, adList);
	}

	/**
	 * Resets the learner.
	 */
	public final void updateLearner() {
		switch (learnAlg) {
		case Settings.REINF:
			learner = new SimpleStatelessLearner(learningParams, adList);
			break;
		case Settings.STOCH:
			learner = new SimpleStatelessLearner(learningParams, adList);
			break;
		default:
			System.err.println("The learning algorithm is not recognised"
					+ " (ReinforcementLearning class)");
		}

	}

	/**
	 * GenCo learns based on daily profits or net daily earnings.
	 * 
	 * @param tradeResult
	 *            - the profit obtained by GenCo over the market day or the net
	 *            earnings
	 */
	public final void updateLearning(final double tradeResult) {
		learner.update(tradeResult);
	}

}
