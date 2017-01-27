package acewem.main;

import gamlss.utilities.Controls;
import acewem.gui.ACEWEMGUI;
import acewem.initials.Settings;
import acewem.market.ACEWEMmodel;

public class ACEWEMLauncher {

	public static void main(final String[] args) {

		Controls.IS_SVD = true;
		Controls.GAMLSS_TRACE = false;
		Controls.GLOB_DEVIANCE_TOL = Double.MAX_VALUE;
		new Controls();

		Settings.MODELTYPE = Settings.SIX_NODE;
		//Settings.MODELTYPE = Settings.UK;

		Settings.NONLINEAR_OPTIMIZER = Settings.R;
		//Settings.NONLINEAR_OPTIMIZER = Settings.BOBYQA;
		//Settings.NONLINEAR_OPTIMIZER = Settings.CMAES;

		Settings.QP_OPTIMIZER = Settings.QUADPROG_JAVA;
		//Settings.QP_OPTIMIZER = Settings.QUADPROG_MATLAB;
		//Settings.QP_OPTIMIZER = Settings.QUADPROG_R;
		//Settings.QP_OPTIMIZER = Settings.GUROBI;


		 Settings.DAYS_MAX = 365;
		//Settings.DAYS_MAX = 10;
		//Settings.DAYS_MAX = Integer.MAX_VALUE;

		//Settings.DEMAND_FIXED = true;
		Settings.DEMAND_FIXED = false;
		
		//Settings.WRITE_OBJECTIVE = true;

		//final boolean isFromGUI = true;
		final boolean isFromGUI = false;
		
		if (Settings.MODELTYPE == Settings.UK){
			Settings.HOURS = 48;
		} else {
			Settings.HOURS = 24;
			Settings.DEMAND_FIXED = true;
		}

		if (!isFromGUI) {
			
	      //Settings.MARKETS = Settings.SINGLE_DA;
		   Settings.MARKETS = Settings.DA_BM;
			
		   Settings.LEARNALG = Settings.REINF;
		   //Settings.LEARNALG = Settings.STOCH;
		} else {
			if (Settings.MODELTYPE == Settings.UK) {
				Settings.AGENT_WIDTH = 0.5;
				Settings.AGENT_HEIGHT = 0.5;
			}
		}
		new ACEWEMLauncher(isFromGUI);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	public ACEWEMLauncher(final boolean isFromGUI) {

		final ACEWEMmodel market = new ACEWEMmodel(System.currentTimeMillis(),
				isFromGUI);

		if (isFromGUI) {
			new ACEWEMGUI(market);
			market.finish();

		} else {
			market.initialiseMarket();
			market.start();
			market.schedule.step(market);

			final long jobsMax = 1;
			long days = 0;
			for (int jobs = 0; jobs < jobsMax; jobs++) {
				// if (days >= stepMax) {
				// year = 365 * jobs;
				// }

				do {
					if (!market.schedule.step(market)) {
						System.err.println("Error with the "
								+ "step function of the ACEWEM model");
						break;
					}
					// days = market.schedule.getSteps() - year;
					// System.out.println("----------------------  DAYS ---------------- "+days);
					days = market.schedule.getSteps();
				} while (days <= Settings.DAYS_MAX);
			}
			System.out.println("Done!!!");
			market.finish();
			
			if (Settings.NONLINEAR_OPTIMIZER == Settings.R || Settings.QP_OPTIMIZER == Settings.QUADPROG_R) {
				ACEWEMmodel.getrConnection().close();
			}
			// System.exit(0);
		}
	}
}