package acewem.utilities.learning;

import gamlss.algorithm.Gamlss;
import gamlss.distributions.DistributionSettings;
import gamlss.distributions.GAMLSSFamilyDistribution;
import gamlss.utilities.MatrixFunctions;
import gamlss.utilities.exceptions.GamlssException;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.optimization.direct.BOBYQAOptimizer;
import org.apache.commons.math3.optimization.direct.BaseAbstractMultivariateSimpleBoundsOptimizer;
import org.apache.commons.math3.optimization.direct.CMAESOptimizer;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.util.FastMath;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import acewem.initials.Settings;
import acewem.market.ACEWEMmodel;
import acewem.market.GenCo;
import acewem.utilities.io.CSVFileReader;


/**
 * A class used to find aR and bR by loading the data, launching the GAMLSS and
 * solving the optimization problem.
 */
public class StochasticOptimisation {

	/** Object of MultivariateOptimizer intergace */
	private BaseAbstractMultivariateSimpleBoundsOptimizer optimizer;
	/** Object of ObjFunction inner class */
	private ObjFunction function;

	private GAMLSSFamilyDistribution[][] distr;
	private final HashMap[] designMatrices;
	private BlockRealMatrix[][] designMatrix;
	private ArrayRealVector[][] response;
	private double[][] lastResponse;

	double[][] mean;
	double[][] sd;
	double[] meanPower;

	/**
	 * Constructor.
	 * 
	 * @param supplyOffer
	 *            - list of true supply offer data
	 * @param idnum
	 *            - GenCo id number
	 * @param mkt
	 *            - object of ACEWEMmodel
	 */
	public StochasticOptimisation(final GenCo gen, final int numOfGenCos) {

		final int dist = DistributionSettings.NO;
		ArrayList<double[]> data = null;
		switch (Settings.MARKETS) {
		case Settings.SINGLE_DA:

			response = new ArrayRealVector[Settings.TWO][Settings.ONE];
			lastResponse = new double[Settings.TWO][Settings.ONE];
			designMatrix = new BlockRealMatrix[Settings.TWO][Settings.ONE];
			mean = new double[Settings.TWO][Settings.ONE];
			sd = new double[Settings.TWO][Settings.ONE];
			distr = new GAMLSSFamilyDistribution[Settings.TWO][Settings.ONE];

			data = setStoredData(Settings.DA, gen, numOfGenCos);
			buildResponseAndDesign(Settings.PRICE, Settings.DA, data, gen);
			buildResponseAndDesign(Settings.MW, Settings.DA, data, gen);

			gen.getMarket().getGamlss()[Settings.PRICE][Settings.DA]
					.initializeDistributions(response[Settings.PRICE][Settings.DA]);
			gen.getMarket().getGamlss()[Settings.MW][Settings.DA]
					.initializeDistributions(response[Settings.MW][Settings.DA]);
			gen.getMarket().getGamlss()[Settings.PRICE][Settings.DA]
					.setDistribution(dist);
			gen.getMarket().getGamlss()[Settings.MW][Settings.DA]
					.setDistribution(dist);

			distr[Settings.PRICE][Settings.DA] = gen.getMarket().getGamlss()[Settings.PRICE][Settings.DA]
					.getDistribution();
			distr[Settings.MW][Settings.DA] = gen.getMarket().getGamlss()[Settings.MW][Settings.DA]
					.getDistribution();

			break;
		case Settings.DA_BM:
			response = new ArrayRealVector[Settings.TWO][Settings.THREE];
			lastResponse = new double[Settings.TWO][Settings.THREE];
			designMatrix = new BlockRealMatrix[Settings.TWO][Settings.THREE];
			mean = new double[Settings.TWO][Settings.THREE];
			sd = new double[Settings.TWO][Settings.THREE];
			distr = new GAMLSSFamilyDistribution[Settings.TWO][Settings.THREE];

			for (int k = 0; k < Settings.THREE; k++) {
				data = setStoredData(k, gen, numOfGenCos);
				for (int i = 0; i < Settings.TWO; i++) {
					buildResponseAndDesign(i, k, data, gen);
					gen.getMarket().getGamlss()[i][k]
							.initializeDistributions(response[i][k]);
					gen.getMarket().getGamlss()[i][k].setDistribution(dist);
					distr[i][k] = gen.getMarket().getGamlss()[i][k]
							.getDistribution();
				}
			}
			break;
		default:
			System.err.println(" The requested power market "
					+ "cannot be modelled in StochasticOptimisation");
		}

		switch (Settings.NONLINEAR_OPTIMIZER) {
		case Settings.R:
			break;
		case Settings.BOBYQA:
			function = new ObjFunction();
			optimizer = new BOBYQAOptimizer(5);
			break;
		case Settings.CMAES:
			function = new ObjFunction();
			optimizer = new CMAESOptimizer();
			break;
		default:
			System.err.println("The requested nonlinear optimizer "
					+ "is not implemeted (StochasticOptimisation)");
		}
		designMatrices = new HashMap[Settings.TWO];
		designMatrices[Settings.PRICE] = new HashMap<Integer, BlockRealMatrix>();
		designMatrices[Settings.MW] = new HashMap<Integer, BlockRealMatrix>();
	}
	
	/**
	 * The function estimates expected profit, determines best aR and bR and
	 * submits them to the market.
	 * 
	 * @return pair of best values (aR, bR)
	 */
	public final double[] submitReportedOfferStochasticOptimisation(
			final GenCo gen, final int whichMarket) {

//		System.out.println(whichMarket + "     " + gen.getMarket().getDay()
//				+ "     " + "genCo " + gen.getIdNum());

		final double[][] distrParameters = new double[Settings.TWO][1];
		for (int fittingWhat = 0; fittingWhat < Settings.TWO; fittingWhat++) {
			if (gen.getModel().equals(Settings.FIXED)) {
				// We don't need to fit data when the parameters are fixed
				gen.getMarket().getGamlss()[fittingWhat][whichMarket]
						.setDistribution(DistributionSettings.NO);
				// gen.getMarket().getGamlss()[Settings.MW][whichMarket].setDistribution(DistributionSettings.NO);
			} else {
				boolean fittingFails = true;
				int errorLoop = 0;
				while (fittingFails) {
					// get distribution from action domain of reinforcement
					// learner
					// int distrOfChoice = (int)
					// gen.getReinfLearnerPrice(whichMarket).getLearner().chooseActionRaw();
					// gen.setDistrOfChoice(whichMarket, distrOfChoice);
					// gamlss[Settings.PRICE][whichMarket].setDistribution(distrOfChoice);

					// gamlss[Settings.MW][whichMarket].setDistribution(distrOfChoice);
					// Always NOrmal distribution for power commitments
					// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
					// gamlss[Settings.MW][whichMarket].setDistribution(DistributionSettings.NO);
					// distr[Settings.PRICE] =
					// gamlss[Settings.PRICE][whichMarket].getDistribution();
					// distr[Settings.MW] =
					// gamlss[Settings.MW][whichMarket].getDistribution();

					final int size = response[fittingWhat][whichMarket]
							.getDimension();
					designMatrices[fittingWhat].clear();
					for (int i = 0; i < distr[fittingWhat][whichMarket]
							.getNumberOfDistribtionParameters(); i++) {
						// extends distribution parameters to the length of the
						// response variable
						distr[fittingWhat][whichMarket]
								.setDistributionParameter(
										i + 1,
										extendVector(
												distr[fittingWhat][whichMarket]
														.getDistributionParameter(i + 1),
												size));
						// assign same design matrix for each of distribution
						// parameters
						if ((i + 1) == DistributionSettings.MU) {
							designMatrices[fittingWhat].put(i + 1,
									designMatrix[fittingWhat][whichMarket]);
						} else {
							designMatrices[fittingWhat].put(i + 1, null);
						}
					}

					/*
					 * size = response[Settings.MW][whichMarket].getDimension();
					 * designMatrices[Settings.MW].clear(); for (int i = 0; i <
					 * distr
					 * [Settings.MW][whichMarket].getNumberOfDistribtionParameters
					 * (); i++) { // extends distribution parameters to the
					 * length of the response variable
					 * distr[Settings.MW][whichMarket
					 * ].setDistributionParameter(i+1,
					 * extendVector(distr[Settings
					 * .MW][whichMarket].getDistributionParameter(i + 1),
					 * size)); //assign same design matrix for each of
					 * distribution parameters if ((i+1) ==
					 * DistributionSettings.MU) {
					 * designMatrices[Settings.MW].put(i+1,
					 * designMatrix[Settings.MW][whichMarket]); } else {
					 * designMatrices[Settings.MW].put(i+1, null); } }
					 */
					gen.getMarket().getGamlss()[fittingWhat][whichMarket]
							.setResponseVariable(response[fittingWhat][whichMarket]);
					// gen.getMarket().getGamlss()[Settings.MW][whichMarket].setResponseVariable(response[Settings.MW][whichMarket]);

					gen.getMarket().getGamlss()[fittingWhat][whichMarket]
							.setDesignMatrices(designMatrices[fittingWhat]);
					// gen.getMarket().getGamlss()[Settings.MW][whichMarket].setDesignMatrices(designMatrices[Settings.MW]);

					gen.getMarket().getGamlss()[fittingWhat][whichMarket]
							.setDistribution(distr[fittingWhat][whichMarket]);
					// gen.getMarket().getGamlss()[Settings.MW][whichMarket].setDistribution(distr[Settings.MW][whichMarket]);

					gen.getMarket().getGamlss()[fittingWhat][whichMarket]
							.setWeights(null);
					// gen.getMarket().getGamlss()[Settings.MW][whichMarket].setWeights(null);
					// // to delete if only 365 days are used for fitting

					try {

						gen.getMarket().getGamlss()[fittingWhat][whichMarket]
								.fit();
						// gen.getMarket().getGamlss()[Settings.MW][whichMarket].fit();
						fittingFails = false;

					} catch (final Exception e) {
						
						String fittingWhatStr = null;
						switch (fittingWhat) {
			  			case Settings.PRICE:
			  				fittingWhatStr = "Price";
			  				break; 
			  			case Settings.MW:
			  				fittingWhatStr = "Power";
			  				break; 
			  			default:
			  				//throw new ACEWEMdefaultSwitchStatement();
						}
						
						String whichMarketStr = null;
						switch (whichMarket) {
			  			case Settings.DA:
			  				whichMarketStr = "DA";
			  				break; 
			  			case Settings.BM_INC:
			  				whichMarketStr = "BM for INCs";
			  				break; 
			  			case Settings.BM_DEC:
			  				whichMarketStr = "BM for DECs";
			  				break; 
			  			default:
			  				//throw new ACEWEMdefaultSwitchStatement();
						}
						if(errorLoop++ > 10){
							System.out.println("re-initilise "+fittingWhatStr
									+" distribution at "+whichMarketStr+" for GenCo "+gen.getIdNum());
						}	
						gen.getMarket().getGamlss()[fittingWhat][whichMarket].initializeDistributions(response[fittingWhat][whichMarket]);
						gen.getMarket().getGamlss()[fittingWhat][whichMarket].setDistribution(DistributionSettings.NO);
						distr[fittingWhat][whichMarket] = gen.getMarket().getGamlss()[fittingWhat][whichMarket].getDistribution();
					//	MatrixFunctions.vectorPrint(gen.getMarket().getGamlss()[fittingWhat][whichMarket].getResponseVariable());
					//	MatrixFunctions.matrixPrint(gen.getMarket().getGamlss()[fittingWhat][whichMarket].getDesignMatrices().get(DistributionSettings.MU));
						// gen.getReinfLearnerPrice(whichMarket).getActionDomain().remove(gen.getReinfLearnerPrice(whichMarket).getChoiceID());
						// gen.getReinfLearnerPrice(whichMarket).initialiseLearner();
					}
				}
			}

			distrParameters[fittingWhat] = calculateDistributionParameters(gen,
					fittingWhat, whichMarket);
			// distrParameters[Settings.MW] =
			// calculateDistributionParameters(gen, Settings.MW, whichMarket);

			double forecast = Double.NaN;
			// double forecastPrice = Double.NaN;
			// when fixed no fitting happens and distr parameters are not
			// updated
			if (gen.getModel().equals(Settings.FIXED)) {
				// forecastPower = distrParameters[Settings.MW][0];
				forecast = distrParameters[fittingWhat][0];
			} else {
				switch (gen.getData(fittingWhat, whichMarket)) {
				case Settings.STRAIGHT:
					// forecastPower = distrParameters[Settings.MW][0];
					forecast = distrParameters[fittingWhat][0];
					break;
				case Settings.LOGGED:
					// forecastPower =
					// FastMath.exp(distrParameters[Settings.MW][0]);
					forecast = FastMath.exp(distrParameters[fittingWhat][0]);
					break;
				case Settings.GROWTH_RATE:
					// forecastPower =
					// FastMath.exp(distrParameters[Settings.MW][0]) *
					// lastResponse[Settings.MW][whichMarket];
					forecast = FastMath.exp(distrParameters[fittingWhat][0])
							* lastResponse[fittingWhat][whichMarket];
					break;
				default:
					System.err
							.println(" The data processing format is undefined "
									+ "in StochasticOptimisation class");
				}
			}

			// GenCo will not produce less than 1 MW
			// if(forecastPower < 1.0) {
			// forecastPower = 1.0;
			// }
			gen.setForecast(fittingWhat, whichMarket, forecast);
			// gen.setForecastPrice(whichMarket, forecastPrice);
		}

		double[] startPoint;
		double[] lowerLimit;
		double[] upperLimit;
		if (whichMarket == Settings.BM_DEC) {

			startPoint = new double[] { gen.getaT(), gen.getbT() };
			// lowerLimit = new double[]{Double.MIN_VALUE, Double.MIN_VALUE};
			lowerLimit = new double[] { gen.getaT() / 1000, gen.getbT() / 1000 };
			upperLimit = new double[] { gen.getaT(), gen.getbT() }; // Add above margianl  cost!!!!!!!!!!!!!!!!!!!!!!!

		} else {

			// startPoint = new double[]{ gen.getaR(Settings.DA),
			// gen.getbR(Settings.DA)};
			startPoint = new double[] { gen.getaT(), gen.getbT() };
			lowerLimit = new double[] { gen.getaT(), gen.getbT() };   // Add below true  margianl  cost!!!!!!!!!!!!!!!!!!!!!!
			// upperLimit = new double[]{gen.getaT()*Double.MAX_VALUE,
			// gen.getbT()*Double.MAX_VALUE};
			//upperLimit = new double[] { gen.getaT() * 1000, gen.getbT() * 1000 };
			//upperLimit = new double[] {  gen.getaT()*10e10, gen.getbT()*10e10, };
			//upperLimit = new double[] {gen.getaT()*1000000, gen.getbT()*1000000};
			upperLimit = new double[] {Double.MAX_VALUE, Double.MAX_VALUE };  
		}

		double[] aRbR = null;
		switch (Settings.NONLINEAR_OPTIMIZER) {
		case Settings.R:
			aRbR = nonlinearOptimizationWithR(gen, whichMarket, startPoint,
					lowerLimit, upperLimit,
					gen.getForecast(Settings.MW, whichMarket),
					distrParameters[Settings.PRICE]);
			break;
		case Settings.BOBYQA:
			aRbR = nonlinearOptimizationWithJava(gen, whichMarket, startPoint,
					lowerLimit, upperLimit,
					gen.getForecast(Settings.MW, whichMarket),
					distrParameters[Settings.PRICE]);
			break;
		case Settings.CMAES:
			aRbR = nonlinearOptimizationWithJava(gen, whichMarket, startPoint,
					lowerLimit, upperLimit,
					gen.getForecast(Settings.MW, whichMarket),
					distrParameters[Settings.PRICE]);
			break;
		default:
			System.err.println("The requested nonlinear optimizer "
					+ "is not implemeted (StochasticOptimisation)");
		}
		return aRbR;
	}

	/**
	 * Append last observation of response variable to the vector of response
	 * variable values.
	 * 
	 * @param whichMarket
	 *            -
	 * @param lastObseravition
	 *            - average electricity price of the current day
	 */
	public boolean[] appendLastObservationToResponse(final int whichMarket,
			final double[] lastObseravition, final GenCo gen) {

		final boolean[] out = new boolean[] { false, false };
		for (int fittingWhat = 0; fittingWhat < Settings.TWO; fittingWhat++) {
			if (lastObseravition[fittingWhat] != 0) {
				switch (gen.getData(fittingWhat, whichMarket)) {
				case Settings.STRAIGHT:
					response[fittingWhat][whichMarket] = (ArrayRealVector) response[fittingWhat][whichMarket]
							.append(lastObseravition[fittingWhat]);
					break;
				case Settings.LOGGED:
					response[fittingWhat][whichMarket] = (ArrayRealVector) response[fittingWhat][whichMarket]
							.append(FastMath.log(lastObseravition[fittingWhat]));

					break;
				case Settings.GROWTH_RATE:
					response[fittingWhat][whichMarket] = (ArrayRealVector) response[fittingWhat][whichMarket]
							.append(FastMath.log(lastObseravition[fittingWhat]
									/ lastResponse[fittingWhat][whichMarket]));
					break;
				default:
					System.err
							.println(" The data processing format is undefined "
									+ "in StochasticOptimisation class");
				}
				out[fittingWhat] = true;
			}

			/*
			 * if(lastObseravition[Settings.MW] != 0) { switch
			 * (gen.getData(whichMarket)) { case Settings.STRAIGHT:
			 * response[Settings.MW][whichMarket] = (ArrayRealVector)
			 * response[Settings.MW][whichMarket].append(
			 * lastObseravition[Settings.MW]); break; case Settings.LOGGED:
			 * this.response[Settings.MW][whichMarket] = (ArrayRealVector)
			 * response[Settings.MW][whichMarket].append(
			 * FastMath.log(lastObseravition[Settings.MW])); break; case
			 * Settings.GROWTH_RATE: this.response[Settings.MW][whichMarket] =
			 * (ArrayRealVector) response[Settings.MW][whichMarket].append(
			 * FastMath.log(lastObseravition[Settings.MW] /
			 * lastResponse[Settings.MW][whichMarket])); break; default:
			 * System.err.println(" The data processing format is undefined " +
			 * "in StochasticOptimisation class"); } out[Settings.MW] = true; }
			 */
		}
		return out;
	}

	/**
	 * To set vectror of response variable, design matrix and in case of
	 * GrowRate of data the last observation.
	 * 
	 * @param whichModel
	 * @param whichMarket
	 * @param data
	 * @param gen
	 */
	private void buildResponseAndDesign(final int whichModel,
			final int whichMarket, final ArrayList<double[]> data,
			final GenCo gen) {
		final double[] modelVariable = data.get(whichModel);
		int size = -1;
		switch (gen.getModel()[whichModel][whichMarket]) {
		case Settings.MARGINAL:
			switch (gen.getData(whichModel, whichMarket)) {

			case Settings.STRAIGHT:
				response[whichModel][whichMarket] = new ArrayRealVector(
						modelVariable, false);
				break;
			case Settings.LOGGED:
				size = modelVariable.length;
				response[whichModel][whichMarket] = new ArrayRealVector(size);
				for (int i = 0; i < size; i++) {
					response[whichModel][whichMarket].setEntry(i,
							FastMath.log(modelVariable[i]));
				}
				break;
			case Settings.GROWTH_RATE:
				size = modelVariable.length - 1;
				response[whichModel][whichMarket] = new ArrayRealVector(size);
				for (int i = 0; i < size; i++) {
					response[whichModel][whichMarket].setEntry(
							i,
							FastMath.log(modelVariable[i + 1]
									/ modelVariable[i]));
				}
				lastResponse[whichModel][whichMarket] = modelVariable[modelVariable.length - 1];
				break;
			default:
				System.err.println(" The data processing format is undefined "
						+ "in StochasticOptimisation class");
			}
			designMatrix[whichModel][whichMarket] = null;
			break;

		case Settings.AUTOREGRESSIVE:
			switch (gen.getData(whichModel, whichMarket)) {
			case Settings.STRAIGHT:
				size = modelVariable.length - 1;
				response[whichModel][whichMarket] = new ArrayRealVector(size);
				for (int i = 0; i < size; i++) {
					response[whichModel][whichMarket].setEntry(i,
							modelVariable[i + 1]);
				}

				designMatrix[whichModel][whichMarket] = new BlockRealMatrix(
						size, 1);
				for (int i = 0; i < size; i++) {
					designMatrix[whichModel][whichMarket].setEntry(i, 0,
							modelVariable[i]);
				}
				break;
			case Settings.LOGGED:
				size = modelVariable.length - 1;
				response[whichModel][whichMarket] = new ArrayRealVector(size);
				for (int i = 0; i < size; i++) {
					response[whichModel][whichMarket].setEntry(i,
							FastMath.log(modelVariable[i + 1]));
				}

				designMatrix[whichModel][whichMarket] = new BlockRealMatrix(
						size, 1);
				for (int i = 0; i < size; i++) {
					designMatrix[whichModel][whichMarket].setEntry(i, 0,
							FastMath.log(modelVariable[i]));
				}
				break;
			case Settings.GROWTH_RATE:
				size = modelVariable.length - 2;
				response[whichModel][whichMarket] = new ArrayRealVector(size);
				for (int i = 0; i < size; i++) {
					response[whichModel][whichMarket].setEntry(
							i,
							FastMath.log(modelVariable[i + 2]
									/ modelVariable[i + 1]));
				}

				designMatrix[whichModel][whichMarket] = new BlockRealMatrix(
						size, 1);
				for (int i = 0; i < size; i++) {
					designMatrix[whichModel][whichMarket].setEntry(
							i,
							0,
							FastMath.log(modelVariable[i + 1]
									/ modelVariable[i]));
				}
				lastResponse[whichModel][whichMarket] = modelVariable[modelVariable.length - 1];
				break;
			default:
				System.err.println(" The data processing format is undefined "
						+ "in StochasticOptimisation class");
			}
			break;

		case Settings.FIXED:
			switch (gen.getData(whichModel, whichMarket)) {
			case Settings.STRAIGHT:
				response[whichModel][whichMarket] = new ArrayRealVector(
						modelVariable, false);
				break;
			case Settings.LOGGED:
				size = modelVariable.length;
				response[whichModel][whichMarket] = new ArrayRealVector(size);
				for (int i = 0; i < size; i++) {
					response[whichModel][whichMarket].setEntry(i,
							FastMath.log(modelVariable[i]));
				}
				break;
			case Settings.GROWTH_RATE:
				size = modelVariable.length - 1;
				response[whichModel][whichMarket] = new ArrayRealVector(size);
				for (int i = 0; i < size; i++) {
					response[whichModel][whichMarket].setEntry(
							i,
							FastMath.log(modelVariable[i + 1]
									/ modelVariable[i]));
				}

				lastResponse[whichModel][whichMarket] = modelVariable[modelVariable.length - 1];
				break;
			default:
				System.err.println(" The data processing format is undefined "
						+ "in StochasticOptimisation class");
			}
			designMatrix[whichModel][whichMarket] = null;
			mean[whichModel][whichMarket] = new Mean().evaluate(modelVariable);
			sd[whichModel][whichMarket] = new StandardDeviation()
					.evaluate(modelVariable);
			break;
		default:
			System.err.println(" The requested stochastic model is "
					+ "not implemented (StochasticOptimisation calss) ");
		}

	}

	private double[] calculateDistributionParameters(final GenCo gen,
			final int whichModel, final int whichMarket) {
		final int numDistPar = gen.getMarket().getGamlss()[whichModel][whichMarket]
				.getDistribution().getNumberOfDistribtionParameters();
		final double[] distrParameters = new double[numDistPar];

		for (int i = 1; i < numDistPar + 1; i++) {
			final ArrayRealVector beta = gen.getMarket().getGamlss()[whichModel][whichMarket]
					.getDistribution().getBeta(i);

			switch (gen.getModel()[whichModel][whichMarket]) {
			case Settings.MARGINAL:

				switch (gen.getMarket().getGamlss()[whichModel][whichMarket]
						.getDistribution().getDistributionParameterLink(i)) {
				case DistributionSettings.IDENTITY:
					distrParameters[i - 1] = beta.getEntry(0);
					break;
				case DistributionSettings.LOG:
					distrParameters[i - 1] = FastMath.exp(beta.getEntry(0));
					break;
				case DistributionSettings.LOGSHIFTTO2:
					distrParameters[i - 1] = FastMath.exp(beta.getEntry(0)) + 2;
					break;
				default:
					System.err
							.println(" No link function exist for a provided "
									+ "link (StochasticOptimisation class)");
				}
				break;
			case Settings.AUTOREGRESSIVE:
				if (i == DistributionSettings.MU) {
					switch (gen.getMarket().getGamlss()[whichModel][whichMarket]
							.getDistribution().getDistributionParameterLink(i)) {
					case DistributionSettings.IDENTITY:
						distrParameters[i - 1] = beta.getEntry(0)
								+ beta.getEntry(1)
								* response[whichModel][whichMarket]
										.getEntry(response[whichModel][whichMarket]
												.getDimension() - 1);
						break;
					case DistributionSettings.LOG:
						distrParameters[i - 1] = FastMath
								.exp(beta.getEntry(0)
										+ beta.getEntry(1)
										* response[whichModel][whichMarket]
												.getEntry(response[whichModel][whichMarket]
														.getDimension() - 1));
						break;
					case DistributionSettings.LOGSHIFTTO2:
						distrParameters[i - 1] = FastMath
								.exp(beta.getEntry(0)
										+ beta.getEntry(1)
										* response[whichModel][whichMarket]
												.getEntry(response[whichModel][whichMarket]
														.getDimension() - 1)) + 2;
						break;
					default:
						System.err
								.println(" No link function exist for a provided "
										+ "link (StochasticOptimisation class)");
					}
				} else {
					switch (gen.getMarket().getGamlss()[whichModel][whichMarket]
							.getDistribution().getDistributionParameterLink(i)) {
					case DistributionSettings.IDENTITY:
						distrParameters[i - 1] = beta.getEntry(0);
						break;
					case DistributionSettings.LOG:
						distrParameters[i - 1] = FastMath.exp(beta.getEntry(0));
						break;
					case DistributionSettings.LOGSHIFTTO2:
						distrParameters[i - 1] = FastMath.exp(beta.getEntry(0)) + 2;
						break;
					default:
						System.err
								.println(" No link function exist for a provided "
										+ "link (StochasticOptimisation class)");
					}
				}
				break;
			case Settings.FIXED:
				distrParameters[0] = mean[whichModel][whichMarket];
				distrParameters[1] = sd[whichModel][whichMarket];
				break;
			default:
				System.err.println(" The requested stochastic model is "
						+ "not implemented (StochasticOptimisation calss) ");
			}
		}
		return distrParameters;
	}

	/**
	 * Extends vector by copying its values consequently until it reaches
	 * dimension maxDim
	 * 
	 * @param x
	 *            - vector
	 * @param maxDim
	 *            - desired dimension of the vector
	 * @return extended vector
	 */
	private ArrayRealVector extendVector(final ArrayRealVector v,
			final int maxDim) {
		int n = 0;
		final int vDim = v.getDimension();
		final double[] vArr = v.getDataRef();
		final double[] out = new double[maxDim];
		for (int i = 0; i < maxDim; i++) {
			out[i] = vArr[n];
			n++;
			if (n == vDim) {
				n = n - vDim;
			}
		}
		return new ArrayRealVector(out, false);
	}

	private double[] nonlinearOptimizationWithJava(final GenCo gen,
			final int whichMarket, final double[] startPoint,
			final double[] lowerLimit, final double[] upperLimit,
			final double power, final double[] distrParameters) {

		function.setGamlss(gen.getMarket().getGamlss()[Settings.PRICE][whichMarket]);
		function.setLastprice(lastResponse[Settings.PRICE][whichMarket]);
		function.setPower(power);
		function.setDistrParameters(distrParameters);
		function.setGen(gen);
		function.setMarket(whichMarket);

		double[] aRbR = null;
		double objValue = Double.NaN;
		PointValuePair pair = null;
		double profit;
		switch (whichMarket) {
		case Settings.DA:
			try {
				pair = optimizer.optimize(1000, function, GoalType.MAXIMIZE,
						startPoint, lowerLimit, upperLimit);
				aRbR = pair.getPointRef();
				objValue = pair.getValue();
			} catch (final Exception e) {
				e.printStackTrace();
				aRbR = new double[] { function.getaR(), function.getbR() };
				objValue = function.getObjectiveValue();
			}
			gen.setForecastedParameters(whichMarket, distrParameters);
			gen.setObjectiveValue(whichMarket, objValue);
			profit = ((aRbR[0] + 2 * aRbR[1] * power) * power - (gen.getaT()
					* power + gen.getbT() * power * power));
			gen.setProbability(whichMarket, objValue / profit);
			break;
		case Settings.BM_INC:
			try {
				pair = optimizer.optimize(1000, function, GoalType.MAXIMIZE,
						startPoint, lowerLimit, upperLimit);

				aRbR = pair.getPointRef();
				objValue = pair.getValue();
			} catch (final Exception e) {
				e.printStackTrace();
				aRbR = new double[] { function.getaR(), function.getbR() };
				objValue = function.getObjectiveValue();
			}
			gen.setForecastedParameters(whichMarket, distrParameters);
			gen.setObjectiveValue(whichMarket, objValue);
			profit = ((aRbR[0] + 2 * aRbR[1] * power) * power - (gen.getaT()
					* power + gen.getbT() * power * power));
			gen.setProbability(whichMarket, objValue / profit);
			break;
		case Settings.BM_DEC:
			try {
				pair = optimizer.optimize(1000, function, GoalType.MAXIMIZE,
						startPoint, lowerLimit, upperLimit);

				aRbR = pair.getPointRef();
				objValue = pair.getValue();
			} catch (final Exception e) {
				e.printStackTrace();
				aRbR = new double[] { function.getaR(), function.getbR() };
				objValue = function.getObjectiveValue();
			}
			gen.setForecastedParameters(whichMarket, distrParameters);
			gen.setObjectiveValue(whichMarket, objValue);
			profit = ((gen.getaR(Settings.DA) + gen.getbR(Settings.DA) * power)
					* power - (aRbR[0] + 2 * aRbR[1] * power) * power);
			gen.setProbability(whichMarket, objValue / profit);
			break;
		default:
			System.err.println(" The requested power market "
					+ "cannot be modelled (StochAlg)");
		}
		return aRbR;
	}

	private double[] nonlinearOptimizationWithR(final GenCo gen,
			final int whichMarket, final double[] startPoint,
			final double[] lowerLimit, final double[] upperLimit,
			final double power, final double[] distrParameters) {

		String lowerTail = "F";
		String profit = null;
		switch (whichMarket) {
		case Settings.DA:
			// lowerTail = "F";
			profit = "(marginalCost * power - (aT * power + bT * power * power));";
			break;
		case Settings.BM_INC:
			// lowerTail = "F";
			profit = "(marginalCost * power - (aT * power + bT * power * power));";
			break;
		case Settings.BM_DEC:
			lowerTail = "T";
			// Price_DA x MW - marginalCost x MW, Price_DA = MC bid to DA
			profit = "-(marginalCost * power - (aT * power + bT * power * power));";
			break;
		default:
			System.err.println(" The requested power market "
					+ "cannot be modelled ObjFunction");
		}

		String mc = null;
		// switch (Settings.DATA) {
		switch (gen.getData(Settings.PRICE, whichMarket)) {
		case Settings.STRAIGHT:
			mc = "marginalCost";
			break;
		case Settings.LOGGED:
			mc = "log(marginalCost)";
			break;
		case Settings.GROWTH_RATE:
			mc = "log(marginalCost/lastPrice)";
			break;
		default:
			System.err.println(" The data processing format is undefined "
					+ "in StochasticOptimisation class");
		}

		// double DELETE =
		// FastMath.log((gen.getaT()+2*gen.getbT()*power)/lastPrice[Settings.DA]);

		String cumProb = null;
		gen.getMarket();
		final RConnection rConnection = ACEWEMmodel.getrConnection();
		switch (gen.getMarket().getGamlss()[Settings.PRICE][whichMarket]
				.getDistribution().getFamilyOfDistribution()) {
		case DistributionSettings.NO:
			cumProb = "pNO(" + mc + ", parametersPrice[1], parametersPrice[2],"
					+ lowerTail + ", F);";
			break;
		case DistributionSettings.GA:
			cumProb = "pGA(" + mc + ", parametersPrice[1], parametersPrice[2],"
					+ lowerTail + ", F);";
			break;
		case DistributionSettings.TF:
			cumProb = "pTF("
					+ mc
					+ ", parametersPrice[1], parametersPrice[2], parametersPrice[3],"
					+ lowerTail + ", F);";
			break;
		case DistributionSettings.TF2:
			cumProb = "pTF2("
					+ mc
					+ ", parametersPrice[1], parametersPrice[2], parametersPrice[3],"
					+ lowerTail + ", F);";
			break;
		case DistributionSettings.JSU:
			cumProb = "pJSU("
					+ mc
					+ ", parametersPrice[1], parametersPrice[2], parametersPrice[3], parametersPrice[4],"
					+ lowerTail + ", F);";
			break;
		case DistributionSettings.SST:
			cumProb = "pSST("
					+ mc
					+ ", parametersPrice[1], parametersPrice[2], parametersPrice[3], parametersPrice[4],"
					+ lowerTail + ", F);";
			break;
		case DistributionSettings.ST4:
			cumProb = "pST4("
					+ mc
					+ ", parametersPrice[1], parametersPrice[2], parametersPrice[3], parametersPrice[4],"
					+ lowerTail + ", F);";
			break;
		default:
			System.err.println("distr");
		}

		if (whichMarket == Settings.BM_DEC) {
			// cumProb = "1 -" + cumProb;
		}

		try {
			rConnection.voidEval("objFunc <- function(par)"
					+ "{marginalCost = par[1] + 2 * par[2] * power;" +
					// "logMC = log(marginalCost);" +
					"p =" + cumProb +
					// "p <- ifelse(" + mc +
					// "> parametersPrice[1], 0, 1);" +
					"profit = " + profit + "obj = profit^degree * p;" +
					// "obj = obj^util - obj;" +
					// "if(p < 0.1) {obj = 0.0};" +
					// "if(obj <= objPrev) {obj = -100000.0};" +
					"-obj}");
		} catch (final RserveException e1) {
			e1.printStackTrace();
		}

		double objValue = Double.NaN;
		double[] aRbR = null;

		final double degree = 1.0;

		try {

			if (gen.getData(Settings.PRICE, whichMarket).equals(
					Settings.GROWTH_RATE)) {
				rConnection
						.assign("lastPrice",
								new double[] { lastResponse[Settings.PRICE][whichMarket] });
			}
			if (whichMarket == Settings.BM_DEC) {
				rConnection.assign("aDA",
						new double[] { gen.getaR(Settings.DA) });
				rConnection.assign("bDA",
						new double[] { gen.getbR(Settings.DA) });
			}
			rConnection.assign("degree", new double[] { degree });
			rConnection.assign("parametersPrice", distrParameters);
			rConnection.assign("power", new double[] { power });
			rConnection.assign("aT", new double[] { gen.getaT() });
			rConnection.assign("bT", new double[] { gen.getbT() });
			rConnection
					.assign("par", new double[] { gen.getaT(), gen.getbT() });
			rConnection.assign("startPoint", startPoint);
			rConnection.assign("lowerLimit", lowerLimit);
			rConnection.assign("upperLimit", upperLimit);

			rConnection
					.voidEval("out <- nlminb(start = startPoint, objective = objFunc, lower = lowerLimit,  upper = upperLimit)");

			aRbR = new double[Settings.TWO];
			aRbR[0] = rConnection.eval("out$par[1]").asDouble();
			aRbR[1] = rConnection.eval("out$par[2]").asDouble();
			objValue = rConnection.eval("out$objective[1]").asDouble();

		} catch (final REngineException e) {
			e.printStackTrace();
		} catch (final REXPMismatchException e) {
			e.printStackTrace();
		}
		gen.setForecastedParameters(whichMarket, distrParameters);
		gen.setObjectiveValue(whichMarket, -objValue);

		switch (whichMarket) {
		case Settings.DA:
			gen.setProbability(
					whichMarket,
					-objValue
							/ FastMath.pow(
									((aRbR[0] + 2 * aRbR[1] * power) * power - (gen
											.getaT() * power + gen.getbT()
											* power * power)), degree));
			break;
		case Settings.BM_INC:
			gen.setProbability(
					whichMarket,
					-objValue
							/ FastMath.pow(
									((aRbR[0] + 2 * aRbR[1] * power) * power - (gen
											.getaT() * power + gen.getbT()
											* power * power)), degree));
			break;
		case Settings.BM_DEC:
			gen.setProbability(
					whichMarket,
					-objValue
							/ FastMath.pow(
									-((aRbR[0] + 2 * aRbR[1] * power) * power - (gen
											.getaT() * power + gen.getbT()
											* power * power)), degree));
			break;
		default:
			System.err.println(" The requested power market "
					+ "cannot be modelled ObjFunction");
		}
		return aRbR;
	}

	public void setLastResponse(final int whichModel, final int whichMarket,
			final double value) {
		lastResponse[whichModel][whichMarket] = value;
	}

	private ArrayList<double[]> setStoredData(final int whichMarket,
			final GenCo gen, final int numOfGenCos) {
		String fileName = null;
		switch (whichMarket) {
		case Settings.DA:
			switch (Settings.MODELTYPE) {
			case Settings.FIVE_NODE:
				if (Settings.MARKETS == Settings.SINGLE_DA) {
					fileName = "data/FIVE_NODE_DA_LMP.csv";
				} else {
					fileName = "data/FIVE_NODE_DA_MCP.csv";
				}
				break;
			case Settings.UK:
				if (Settings.MARKETS == Settings.SINGLE_DA) {
					fileName = "data/UK/UK_DA_LMP.csv";
				} else {
					fileName = "data/UK/UK_DA_MCP.csv";
				}
				break;
			case Settings.SIX_NODE:
				if (Settings.MARKETS == Settings.SINGLE_DA) {
					fileName = "data/SIX_NODE/SIX_NODE_DA_LMP.csv";
				} else {
					fileName = "data/SIX_NODE/SIX_NODE_DA_MCP.csv";
				}
				break;
			case Settings.FOUR_NODE:
				if (Settings.MARKETS == Settings.SINGLE_DA) {
					fileName = "data/4-NODE_DA_LMP.csv";
				} else {
					fileName = "data/4-NODE_DA_MCP.csv";
				}
				break;
			default:
				if (Settings.MARKETS == Settings.SINGLE_DA) {
					fileName = "data/DA_LMP.csv";
				} else {
					fileName = "data/DA_MCP.csv";
				}
			}
			break;
		case Settings.BM_INC:
			switch (Settings.MODELTYPE) {
			case Settings.FIVE_NODE:
				fileName = "data/FIVE_NODE_BM_INC.csv";
				break;
			case Settings.UK:
				fileName = "data/UK/UK_BM_INC.csv";
				break;
			case Settings.SIX_NODE:
				fileName = "data/SIX_NODE/SIX_NODE_BM_INC.csv";
				break;
			case Settings.FOUR_NODE:
				fileName = "data/4-NODE_BM_INC.csv";
				break;
			default:
				fileName = "data/BM_INC.csv";
			}
			break;
		case Settings.BM_DEC:
			switch (Settings.MODELTYPE) {
			case Settings.FIVE_NODE:
				fileName = "data/FIVE_NODE_BM_DEC.csv";
				break;
			case Settings.UK:
				fileName = "data/UK/UK_BM_DEC.csv";
				break;
			case Settings.SIX_NODE:
				fileName = "data/SIX_NODE/SIX_NODE_BM_DEC.csv";
				break;
			case Settings.FOUR_NODE:
				fileName = "data/4-NODE_BM_DEC.csv";
				break;
			default:
				fileName = "data/BM_DEC.csv";
			}
			break;
		default:
			System.err.println("The model type is not recognised");
		}
		final CSVFileReader readData = new CSVFileReader(fileName);
		readData.readFile();
		final ArrayList<String> data = readData.storeValues;
		// setting data for MW
		final double[] yMW = new double[data.size() - 1];
		int indexMW = -1;
		if (Settings.MARKETS == Settings.SINGLE_DA) {
			indexMW = (int) (3 * numOfGenCos - 1 + gen.getIdNum());
		} else {
			indexMW = (int) (2 * numOfGenCos + gen.getIdNum());
		}

		for (int i = 1; i < data.size(); i++) {
			final String[] line = data.get(i).split(",");
			yMW[i - 1] = Double.parseDouble(line[indexMW]);
		}
		int j = 0;
		for (int i = 0; i < yMW.length; i++) {
			if (yMW[i] != 0) {
				yMW[j++] = yMW[i];
			}
		}
		final double[] outMW = new double[j];
		System.arraycopy(yMW, 0, outMW, 0, j);

		// Setting data for price
		final double[] yPrice = new double[data.size() - 1];
		int indexPrice = -1;
		if (Settings.MARKETS == Settings.SINGLE_DA) {
			indexPrice = (int) (2 * numOfGenCos - 1 + gen.getIdNum());
		} else {
			indexPrice = 2 * numOfGenCos;
		}

		for (int i = 1; i < data.size(); i++) {
			final String[] line = data.get(i).split(",");
			yPrice[i - 1] = Double.parseDouble(line[indexPrice]);
		}
		int k = 0;
		for (int i = 0; i < yPrice.length; i++) {
			if (yPrice[i] != 0) {
				yPrice[k++] = yPrice[i];
			}
		}
		final double[] outPrice = new double[k];
		System.arraycopy(yPrice, 0, outPrice, 0, k);

		final ArrayList<double[]> list = new ArrayList<double[]>();
		list.add(yPrice);
		list.add(outMW);
		return list;
	}
	
	public void appendLastObservationToDesign(final int whichModel,
			final int whichMarket, final GenCo gen) {
		designMatrices[whichModel].clear();
		switch (gen.getModel()[whichModel][whichMarket]) {
		case Settings.MARGINAL:
			designMatrix[whichModel][whichMarket] = null;
			break;
		case Settings.AUTOREGRESSIVE:
			final BlockRealMatrix toAppend = new BlockRealMatrix(1, 1);
			toAppend.setEntry(0, 0, response[whichModel][whichMarket].getEntry(
			// since we added new value to response, the design has to be added
			// with response value before new one: response.getDimension() - 2
					response[whichModel][whichMarket].getDimension() - 2));
			designMatrix[whichModel][whichMarket] = MatrixFunctions
					.appendMatricesRows(designMatrix[whichModel][whichMarket],
							toAppend);
			break;
		case Settings.FIXED:
			designMatrix[whichModel][whichMarket] = null;
			break;
		default:
			System.err.println(" The requested stochastic model is "
					+ "not implemented (StochasticOptimisation calss) ");
		}
	}
	
	/**
	 * Objective function class
	 * 
	 */
	class ObjFunction implements MultivariateFunction {
		/** GenCo true supply offer. */
		private GenCo gen;
		/** object of Gamlss. */
		private Gamlss gamlss;
		/** Intercept. */
		private double a;
		/** Slope. */
		private double b;
		private int whichMarket;
		private double objValue;
		private double power;
		private double lastPrice;
		private double[] distrParameters;

		public double getaR() {
			return a;
		}

		public double getbR() {
			return b;
		}

		public double getObjectiveValue() {
			return objValue;
		}

		public void setDistrParameters(final double[] distrParameters) {
			this.distrParameters = distrParameters;
		}

		public void setGamlss(final Gamlss gamlss) {
			this.gamlss = gamlss;
		}

		public void setGen(final GenCo gen) {
			this.gen = gen;
		}

		public void setLastprice(final double lastPrice) {
			this.lastPrice = lastPrice;
		}

		public void setMarket(final int whichMarket) {
			this.whichMarket = whichMarket;
		}

		public void setPower(final double power) {
			this.power = power;
		}

		/**
		 * Compute the value for the function at the given point.
		 * 
		 * @param point
		 *            - the function value for the given point.
		 * @return the function value for the given point.
		 */
		public double value(final double[] point) {

			final double marginalCost = point[0] + 2 * point[1] * power;
			if (marginalCost < 0.0) {
				System.err.println("MC is < than 0 for distr:"
						+ gamlss.getDistribution().getFamilyOfDistribution()
						+ "  genCo: " + gen.getIdNum());
			}

			double p = Double.NaN;
			boolean lowerTail = false;
			if (whichMarket == Settings.BM_DEC) {
				lowerTail = true;
			}

			try {
				// switch (Settings.DATA) {
				switch (gen.getData(Settings.PRICE, whichMarket)) {
				case Settings.STRAIGHT:
					p = gamlss.getDistribution().p(marginalCost, distrParameters,
							lowerTail, false);
					break;
				case Settings.LOGGED:
					p = gamlss.getDistribution().p(FastMath.log(marginalCost),
							distrParameters, lowerTail, false);
					break;
				case Settings.GROWTH_RATE:
					p = gamlss.getDistribution().p(
							FastMath.log(marginalCost / lastPrice),
							distrParameters, lowerTail, false);
					break;
				default:
					System.err.println(" The data processing format is undefined "
							+ "in StochasticOptimisation class");
				}
			} catch (final GamlssException e1) {
				e1.printStackTrace();
			}

			if (Double.isNaN(p)) {
				System.err.println("CDF (p) is NaN for distr:"
						+ gamlss.getDistribution().getFamilyOfDistribution()
						+ "  genCo: " + gen.getIdNum());
			}

			objValue = Double.NaN;
			switch (whichMarket) {
			case Settings.DA:
				objValue = (marginalCost * power - (gen.getaT() * power + gen
						.getbT() * power * power))
						* p;
				break;
			case Settings.BM_INC:
				objValue = (marginalCost * power - (gen.getaT() * power + gen
						.getbT() * power * power))
						* p;
				break;
			case Settings.BM_DEC:
				objValue = ((gen.getaR(Settings.DA) * power + gen
						.getbR(Settings.DA) * power * power) - marginalCost * power)
						* p;
				break;
			default:
				System.err.println(" The requested power market "
						+ "cannot be modelled ObjFunction");
			}

			a = point[0];
			b = point[1];

			return objValue;
		}
	}
}
