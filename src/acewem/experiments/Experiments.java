package acewem.experiments;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import gamlss.utilities.Controls;
import gamlss.utilities.oi.CSVFileReader;
import acewem.initials.Settings;
import acewem.main.ACEWEMLauncher;


public class Experiments {
	
	private int n = 1;

	public Experiments() {
		
	//	allAutoregressiveModelsLMP();		
	//	allAutoregressiveModelsMCP();
		supplyDemandShockLargeLMP();
	//	supplyDemandShockLargeMCP();	
	}
	
	private void supplyDemandShockLargeMCP() {
		  
	  	System.out.println(" +~+~+~+~+~~+~+~+~+~+~ Running experiment "+(n++)+" +~+~+~+~+~~+~+~+~+~+~ ");
	  	Controls.IS_SVD = true;
		Controls.GAMLSS_TRACE = false;
		Controls.GLOB_DEVIANCE_TOL = Double.MAX_VALUE;
		new Controls();

		Settings.MODELTYPE = Settings.SIX_NODE;
		//Settings.MODELTYPE = Settings.UK;

		Settings.NONLINEAR_OPTIMIZER = Settings.R;
		//Settings.NONLINEAR_OPTIMIZER = Settings.BOBYQA;
		// Settings.NONLINEAR_OPTIMIZER = Settings.CMAES;

		Settings.QP_OPTIMIZER = Settings.QUADPROG_JAVA;
		//Settings.QP_OPTIMIZER = Settings.QUADPROG_MATLAB;
		//Settings.QP_OPTIMIZER = Settings.QUADPROG_R;
		//Settings.QP_OPTIMIZER = Settings.GUROBI;

		Settings.HOURS = 24;
		// Settings.HOURS = 48;

		Settings.DAYS_MAX = 3650;
		//Settings.DAYS_MAX = Integer.MAX_VALUE;

		Settings.DEMAND_FIXED = true;
		// Settings.DEMAND_FIXED = false;

		//final boolean isFromGUI = true;
		final boolean isFromGUI = false;
		
		Settings.DEMAND_SHOCK = true;
		Settings.SUPPLY_SHOCK = true;

		if (!isFromGUI) {
			
			//Settings.MARKETS = Settings.SINGLE_DA;
			Settings.MARKETS = Settings.DA_BM;
			
			//Settings.LEARNALG = Settings.REINF;
			Settings.LEARNALG = Settings.STOCH;
		}
		new ACEWEMLauncher(isFromGUI);
			
			//copy results from file to file			
			overwriteResults("results/DA.csv", "results/DA_MCP_supply_deamnd_shock_large.csv");
			overwriteResults("results/BM_INCS.csv", "results/INC_MCP_supply_deamnd_shock_large.csv");
			overwriteResults("results/BM_DECS.csv", "results/DEC_MCP_supply_deamnd_shock_large.csv");
	  }
	
	
	private void supplyDemandShockLargeLMP() {
		  
		System.out.println(" +~+~+~+~+~~+~+~+~+~+~ Running experiment "+(n++)+" +~+~+~+~+~~+~+~+~+~+~ ");
	  	Controls.IS_SVD = true;
		Controls.GAMLSS_TRACE = false;
		Controls.GLOB_DEVIANCE_TOL = Double.MAX_VALUE;
		new Controls();

		Settings.MODELTYPE = Settings.SIX_NODE;
		//Settings.MODELTYPE = Settings.UK;

		Settings.NONLINEAR_OPTIMIZER = Settings.R;
		//Settings.NONLINEAR_OPTIMIZER = Settings.BOBYQA;
		// Settings.NONLINEAR_OPTIMIZER = Settings.CMAES;

		Settings.QP_OPTIMIZER = Settings.QUADPROG_JAVA;
		//Settings.QP_OPTIMIZER = Settings.QUADPROG_MATLAB;
		//Settings.QP_OPTIMIZER = Settings.QUADPROG_R;
		//Settings.QP_OPTIMIZER = Settings.GUROBI;

		Settings.HOURS = 24;
		// Settings.HOURS = 48;

		Settings.DAYS_MAX = 3650;
		//Settings.DAYS_MAX = Integer.MAX_VALUE;

		Settings.DEMAND_FIXED = true;
		// Settings.DEMAND_FIXED = false;

		//final boolean isFromGUI = true;
		final boolean isFromGUI = false;
		
		Settings.DEMAND_SHOCK = true;
		Settings.SUPPLY_SHOCK = true;

		if (!isFromGUI) {
			
			//Settings.MARKETS = Settings.SINGLE_DA;
			Settings.MARKETS = Settings.SINGLE_DA;
			
			//Settings.LEARNALG = Settings.REINF;
			Settings.LEARNALG = Settings.STOCH;
		}
		new ACEWEMLauncher(isFromGUI);
			//copy results from file to file
			overwriteResults("results/DA.csv", "results/DA_LMP_supply_demand_shock_large.csv");
	  }	  
	  
	  private void allAutoregressiveModelsMCP() {
		  
		  	System.out.println(" +~+~+~+~+~~+~+~+~+~+~ Running experiment "+(n++)+" +~+~+~+~+~~+~+~+~+~+~ ");
		  	Controls.IS_SVD = true;
			Controls.GAMLSS_TRACE = false;
			Controls.GLOB_DEVIANCE_TOL = Double.MAX_VALUE;
			new Controls();

			Settings.MODELTYPE = Settings.SIX_NODE;
			//Settings.MODELTYPE = Settings.UK;

			Settings.NONLINEAR_OPTIMIZER = Settings.R;
			//Settings.NONLINEAR_OPTIMIZER = Settings.BOBYQA;
			// Settings.NONLINEAR_OPTIMIZER = Settings.CMAES;

			Settings.QP_OPTIMIZER = Settings.QUADPROG_JAVA;
			//Settings.QP_OPTIMIZER = Settings.QUADPROG_MATLAB;
			//Settings.QP_OPTIMIZER = Settings.QUADPROG_R;
			//Settings.QP_OPTIMIZER = Settings.GUROBI;

			Settings.HOURS = 24;
			// Settings.HOURS = 48;

			Settings.DAYS_MAX = 3650;
			//Settings.DAYS_MAX = Integer.MAX_VALUE;

			Settings.DEMAND_FIXED = true;
			// Settings.DEMAND_FIXED = false;

			//final boolean isFromGUI = true;
			final boolean isFromGUI = false;

			if (!isFromGUI) {
				
				//Settings.MARKETS = Settings.SINGLE_DA;
				Settings.MARKETS = Settings.DA_BM;
				
				//Settings.LEARNALG = Settings.REINF;
				Settings.LEARNALG = Settings.STOCH;
			}
			new ACEWEMLauncher(isFromGUI);
			
			//copy results from file to file
			overwriteResults("results/DA.csv", "results/DA_MCP_all_autoregressive.csv");
			overwriteResults("results/BM_INCS.csv", "results/INC_MCP_all_autoregressive.csv");
			overwriteResults("results/BM_DECS.csv", "results/DEC_MCP_all_autoregressive.csv");
	  }
	
	  private void allAutoregressiveModelsLMP() {
		  
		  System.out.println(" +~+~+~+~+~~+~+~+~+~+~ Running experiment "+(n++)+" +~+~+~+~+~~+~+~+~+~+~ ");
			Controls.IS_SVD = true;
			Controls.GAMLSS_TRACE = false;
			Controls.GLOB_DEVIANCE_TOL = Double.MAX_VALUE;
			new Controls();

			Settings.MODELTYPE = Settings.SIX_NODE;
			//Settings.MODELTYPE = Settings.UK;

			Settings.NONLINEAR_OPTIMIZER = Settings.R;
			//Settings.NONLINEAR_OPTIMIZER = Settings.BOBYQA;
			// Settings.NONLINEAR_OPTIMIZER = Settings.CMAES;

			Settings.QP_OPTIMIZER = Settings.QUADPROG_JAVA;
			//Settings.QP_OPTIMIZER = Settings.QUADPROG_MATLAB;
			//Settings.QP_OPTIMIZER = Settings.QUADPROG_R;
			//Settings.QP_OPTIMIZER = Settings.GUROBI;

			Settings.HOURS = 24;
			// Settings.HOURS = 48;

			Settings.DAYS_MAX = 3650;
			//Settings.DAYS_MAX = Integer.MAX_VALUE;

			Settings.DEMAND_FIXED = true;
			// Settings.DEMAND_FIXED = false;

			//final boolean isFromGUI = true;
			final boolean isFromGUI = false;

			if (!isFromGUI) {
				
				Settings.MARKETS = Settings.SINGLE_DA;
				//Settings.MARKETS = Settings.DA_BM;
				
				//Settings.LEARNALG = Settings.REINF;
				Settings.LEARNALG = Settings.STOCH;
			}
			new ACEWEMLauncher(isFromGUI);
			
			//copy results from file to file
			overwriteResults("results/DA.csv", "results/DA_LMP_all_autoregressive.csv");
	  }
	  
	  private void overwriteResults(String from, String to) {
			try {
				FileWriter fstream = new FileWriter(to, false);
				BufferedWriter out = new BufferedWriter(fstream);
				
				CSVFileReader readData = new CSVFileReader(from);
				readData.readFile();
				ArrayList<String> data = readData.storeValues;	
								
				for (int line = 0; line < data.size(); line++) {
					String[] lineCurrent = data.get(line).split(",");
					for (int i = 0; i < lineCurrent.length; i++) {
						out.write(lineCurrent[i]);
						out.append(',');
					}
					out.newLine();
				}
				out.close();
			} catch (Exception e) { //Catch exception if any
				System.err.println("Error: " + e.getMessage());
			}
		  }
	  
		public static void main(String[] args) {
			System.out.println("Call from Experiments class");
			new Experiments(); 
		}
}
