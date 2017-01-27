package acewem.utilities.io;

import java.io.BufferedWriter;
import java.io.FileWriter;

import acewem.initials.Settings;
import acewem.market.ACEWEMmodel;
import acewem.market.GenCo;

public class WriteToCSV {

	public WriteToCSV(final ACEWEMmodel market) {
		try {
			// Create file
			FileWriter fstream = null;
			BufferedWriter out = null;
			switch (Settings.MARKETS) {
			case Settings.SINGLE_DA:

				fstream = new FileWriter("Results/DA.csv", false);
				out = new BufferedWriter(fstream);
				out.close();
				makeHead("Results/DA.csv", market);

				break;
			case Settings.DA_BM:

				fstream = new FileWriter("Results/DA.csv", false);
				out = new BufferedWriter(fstream);
				out.close();
				makeHead("Results/DA.csv", market);

				fstream = new FileWriter("Results/BM_INCS.csv", false);
				out = new BufferedWriter(fstream);
				out.close();
				makeHead("Results/BM_INCS.csv", market);

				fstream = new FileWriter("Results/BM_DECS.csv", false);
				out = new BufferedWriter(fstream);
				out.close();
				makeHead("Results/BM_DECS.csv", market);
				break;
			default:
				System.err.println(" The requested power market "
						+ "cannot be modelled");
			}
			
			if(Settings.WRITE_OBJECTIVE) {
				String opt = null;
				switch (Settings.QP_OPTIMIZER) {
				case Settings.QUADPROG_JAVA:
					opt = "Java";
					break;
				case Settings.QUADPROG_MATLAB:
					opt = "Matlab";
					break;
				case Settings.QUADPROG_R:
					opt = "R";
					break;
				case Settings.GUROBI:
					opt = "Gurobi";
					break;
				default:
					//throw new ACEWEMdefaultSwitchStatement();
			}
				
				fstream = new FileWriter("Results/ObjectiveDA.csv", false);
				out = new BufferedWriter(fstream);
				out.write(opt);
				out.append(',');
				out.newLine();
				out.close();
				
				if (Settings.MARKETS == Settings.DA_BM) {
					fstream = new FileWriter("Results/ObjectiveBM.csv", false);
					out = new BufferedWriter(fstream);
					out.write(opt);
					out.append(',');
					out.newLine();
					out.close();
				}								
			}
		} catch (final Exception e) { // Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}

	public void makeHead(final String fileName, final ACEWEMmodel market) {
		try {

			final int numGencos = market.getGenCoList().size();
			// int numGencos = 1;

			// Create file
			final FileWriter fstream = new FileWriter(fileName, true);
			final BufferedWriter out = new BufferedWriter(fstream);

			out.write("time");
			out.append(',');

			for (int i = 0; i < numGencos; i++) {
				out.write("aR" + Integer.toString(i + 1));
				out.append(',');
			}

			for (int i = 0; i < numGencos; i++) {
				out.write("bR" + Integer.toString(i + 1));
				out.append(',');
			}

			if (Settings.MARKETS == Settings.SINGLE_DA) {
				for (int i = 0; i < numGencos; i++) {
					out.write("avgLMP" + Integer.toString(i + 1));
					out.append(',');
				}
			} else {
				for (int i = 0; i < numGencos; i++) {
					out.write("pricePaidToGen" + Integer.toString(i + 1));
					out.append(',');
				}
				out.write("avgMCP");
				out.append(',');
			}

			for (int i = 0; i < numGencos; i++) {
				out.write("avgMW " + Integer.toString(i + 1));
				out.append(',');
			}

			for (int i = 0; i < numGencos; i++) {
				out.write("daily Profit " + Integer.toString(i + 1));
				out.append(',');
			}
			
			for (int i = 0; i < numGencos; i++) {
				out.write("avgAngle " + Integer.toString(i + 1));
				out.append(',');
			}

			if (Settings.LEARNALG == Settings.STOCH) {

				for (int i = 0; i < numGencos; i++) {
					out.write("MCforecast " + Integer.toString(i + 1));
					out.append(',');
				}

				for (int i = 0; i < numGencos; i++) {
					out.write("MWforecast " + Integer.toString(i + 1));
					out.append(',');
				}

				for (int i = 0; i < numGencos; i++) {
					out.write("distr " + Integer.toString(i + 1));
					out.append(',');
					out.write("mu " + Integer.toString(i + 1));
					out.append(',');
					out.write("sigma " + Integer.toString(i + 1));
					out.append(',');
					out.write("nu " + Integer.toString(i + 1));
					out.append(',');
					out.write("tau " + Integer.toString(i + 1));
					out.append(',');
				}

				for (int i = 0; i < numGencos; i++) {
					out.write("Obj Value " + Integer.toString(i + 1));
					out.append(',');
				}

				for (int i = 0; i < numGencos; i++) {
					out.write("Prob " + Integer.toString(i + 1));
					out.append(',');
				}
			}

			out.newLine();
			// Close the output stream
			out.close();
		} catch (final Exception e) { // Catch exception if any
			System.err.println("Error in makeHead method: " + e.getMessage());
		}
	}
	
	public void writeObjective(String file, double objective) {
		try {
			FileWriter fstream = new FileWriter(file, true);
			final BufferedWriter out = new BufferedWriter(fstream);
			out.write(Double.toString(objective));
			out.append(',');
			out.newLine();
			out.close();
		} catch (final Exception e) { // Catch exception if any
			System.err.println("Error in writeObjective : " + e.getMessage());
		}
	}

	public void writeResults(final int whichMarket, final ACEWEMmodel market,
			final boolean isTrueSupply) {

		try {
			FileWriter fstream = null;
			switch (whichMarket) {
			case Settings.DA:
				fstream = new FileWriter("Results/DA.csv", true);
				break;
			case Settings.BM_INC:
				fstream = new FileWriter("Results/BM_INCS.csv", true);
				break;
			case Settings.BM_DEC:
				fstream = new FileWriter("Results/BM_DECS.csv", true);
				break;

			default:
				System.err.println(" The requested power market "
						+ "cannot be modelled WriteCSV");
			}

			final BufferedWriter out = new BufferedWriter(fstream);
			final int numGencos = market.getGenCoList().size();
			// int numGencos = 1;

			// time
			out.write(Long.toString(market.schedule.getSteps()));
			out.append(',');

			// aR
			for (int i = 0; i < numGencos; i++) {
				out.write(Double.toString(market.getGenCoList()
						.get("genco" + (i + 1)).getaR(whichMarket)));
				out.append(',');
			}

			// bR
			for (int i = 0; i < numGencos; i++) {
				out.write(Double.toString(market.getGenCoList()
						.get("genco" + (i + 1)).getbR(whichMarket)));
				out.append(',');
			}

			// LMPs or MCs that are paid to each genco
			for (int i = 0; i < numGencos; i++) {
				out.write(Double.toString(market.getGenCoList()
						.get("genco" + (i + 1)).getTotalDailyPrice(whichMarket)
						/ Settings.HOURS));
				out.append(',');
			}

			// MCP when congestion is not resolved by LMPs at DA
			if (Settings.MARKETS != Settings.SINGLE_DA) {
				double totalMCP = 0.0;
				for (int h = 0; h < Settings.HOURS; h++) {
					totalMCP += market.getMCP()[whichMarket][h];
				}
				// avgMCP
				out.write(Double.toString(totalMCP / Settings.HOURS));
				out.append(',');

			}

			// avgMW
			for (int i = 0; i < numGencos; i++) {
				out.write(Double.toString(market.getGenCoList()
						.get("genco" + (i + 1))
						.getTotalDailyCommitment(whichMarket)
						/ Settings.HOURS));
				out.append(',');
			}

			// Profit
			for (int i = 0; i < numGencos; i++) {
				out.write(Double.toString(market.getGenCoList()
						.get("genco" + (i + 1)).getDailyProfit(whichMarket)));
				out.append(',');
			}
			
			// Angles 
			int whichMarketAngle = -1;
			if(whichMarket ==  Settings.DA) {
				whichMarketAngle = Settings.DA;
			} else {
				whichMarketAngle = Settings.BM;
			}
			for (int i = 0; i < numGencos; i++) {
				int node = (int) market.getGenCoList().get("genco" + (i + 1)).getNode() - 1;
				double avgAngle = 0.0;
				for (int h = 0; h < Settings.HOURS; h++) {
					avgAngle += market.getAngleList(whichMarketAngle)[node][h];
				}
				out.write(Double.toString(avgAngle / Settings.HOURS));
				out.append(',');
			}

			if (Settings.LEARNALG == Settings.STOCH) {
				if (!isTrueSupply) {

					// forecasted MC
					for (int i = 0; i < numGencos; i++) {
						final GenCo gen = market.getGenCoList().get("genco" + (i + 1));
						out.write(Double.toString(gen.getForecast(Settings.PRICE,
								whichMarket)));
						out.append(',');
					}

					// forecasted MW
					for (int i = 0; i < numGencos; i++) {
						final GenCo gen = market.getGenCoList().get("genco" + (i + 1));
						out.write(Double.toString(gen.getForecast(Settings.MW,
								whichMarket)));
						out.append(',');
					}

					// Distribution parameters
					for (int i = 0; i < numGencos; i++) {
						out.write(Double.toString(market.getGenCoList()
								.get("genco" + (i + 1))
								.getDistrOfChoice(whichMarket)));
						out.append(',');

						final double[] forecastedParameters = market
								.getGenCoList().get("genco" + (i + 1))
								.getForecastedParameters(whichMarket);
						switch (forecastedParameters.length) {
						case 2:
							out.write(Double.toString(forecastedParameters[0]));
							out.append(',');
							out.write(Double.toString(forecastedParameters[1]));
							out.append(',');
							out.write("---");
							out.append(',');
							out.write("---");
							out.append(',');
							break;
						case 3:
							out.write(Double.toString(forecastedParameters[0]));
							out.append(',');
							out.write(Double.toString(forecastedParameters[1]));
							out.append(',');
							out.write(Double.toString(forecastedParameters[2]));
							out.append(',');
							out.write("---");
							out.append(',');
							break;
						case 4:
							out.write(Double.toString(forecastedParameters[0]));
							out.append(',');
							out.write(Double.toString(forecastedParameters[1]));
							out.append(',');
							out.write(Double.toString(forecastedParameters[2]));
							out.append(',');
							out.write(Double.toString(forecastedParameters[3]));
							out.append(',');
							break;
						default:
							System.err.println(" number of parameters less"
									+ " than 2 or greater than 4 in WriteCSV");
						}
					}

					// Objective function value
					for (int i = 0; i < numGencos; i++) {
						out.write(Double.toString(market.getGenCoList()
								.get("genco" + (i + 1))
								.getObjectiveValue(whichMarket)));
						out.append(',');
					}

					// Probability
					for (int i = 0; i < numGencos; i++) {
						out.write(Double.toString(market.getGenCoList()
								.get("genco" + (i + 1))
								.getProbability(whichMarket)));
						out.append(',');
					}
				}
			}
			out.newLine();
			out.close();
		} catch (final Exception e) { // Catch exception if any
			System.err.println("Error in writeResults method: "
					+ e.getMessage());
		}
	}
}