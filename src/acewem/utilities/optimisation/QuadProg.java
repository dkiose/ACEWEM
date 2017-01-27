package acewem.utilities.optimisation;

import java.util.Enumeration;
import java.util.Random;

import gamlss.utilities.MatrixFunctions;
import gurobi.GRB;
import gurobi.GRBConstr;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBQuadExpr;
import gurobi.GRBVar;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import QuadProgMatlab.Class1;
import quadprogj.QuadProgJ;
import acewem.initials.Settings;
import acewem.market.ACEWEMmodel;
import acewem.market.GenCo;
import acewem.market.LSE;
//import acewem.utilities.exceptions.ACEWEMdefaultSwitchStatement;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.jet.math.Functions;

import com.mathworks.toolbox.javabuilder.MWException;
import com.mathworks.toolbox.javabuilder.MWNumericArray;

public class QuadProg {

	private DoubleMatrix2D branchIndexM;
	private final DoubleFactory1D Matrix1d = DoubleFactory1D.dense;
	private final DoubleFactory2D Matrix2d = DoubleFactory2D.dense;
	private final ACEWEMmodel market;
	private  Class1 quadprogM;

	public QuadProg(final ACEWEMmodel mkt) {
		market = mkt;
		setBranchIndexM();

		if (Settings.QP_OPTIMIZER == Settings.QUADPROG_MATLAB) {
			try {
				quadprogM = new Class1();
			} catch (final MWException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	/** Calculates parameters for the Optimal Power Flow 
	 * @throws Exception */
	public void solveHourlyPowerFlowsConstainedDA( ) {// throws Exception {

		final int size = market.getGenCoList().size();
		final double[] aR = new double[size];
		final double[] bR = new double[size];
		final double[] capLower = new double[size];
		final double[] capUpper = new double[size];
		for (int i = 0; i < size; i++) {
			final GenCo gen = market.getGenCoList().get("genco" + (i + 1));
			aR[i] = gen.getaR(Settings.DA) * Settings.BASES;
			bR[i] = gen.getbR(Settings.DA) * Settings.BASES * Settings.BASES;
			capLower[i] = gen.getCapLR(Settings.DA) / Settings.BASES;
			capUpper[i] = gen.getCapUR(Settings.DA, 0) / Settings.BASES;
		}
		
		
/*
		for (int hour = 0; hour < Settings.HOURS; hour++) {
			
			double genCap = 0;
			double totalDemand = 0;
			
			for (int i = 0; i <  market.getGenCoList().size(); i++) {
				final GenCo gen = market.getGenCoList().get("genco" + (i + 1));
				genCap = genCap + gen.getCapUR(Settings.DA, 0);
			}
			
			for (int i = 0; i <  market.getLseList().size(); i++) {
				final LSE lse = market.getLseList().get("lse" + (i + 1));
				totalDemand = totalDemand + lse.getElectricityDemand(market.getDay())[hour];
			}
			
			System.out.println(genCap +"   "+totalDemand+"   "+ (genCap > totalDemand));
		} 
*/
		
		
		final DoubleMatrix2D gM = formGM(bR);
		final DoubleMatrix1D aV = formaM(aR);
		final DoubleMatrix2D ciqM = formCiqMlmp();
		final DoubleMatrix1D biqV = formbiqMlmp(capUpper, capLower);
		final DoubleMatrix2D ceqM = formCeqM();
		
		for (int hour = 0; hour < Settings.HOURS; hour++) {
			final DoubleMatrix1D beqV = formbeqM(hour);
		
		// System.out.println("G: " + gM);
		// System.out.println("a: " + aV);
		// System.out.println("Ceq: " + ceqM.viewDice());
		// System.out.println("beq: " + beqV);
		// System.out.println("Ciq: " + ciqM.viewDice().assign(Func.neg));
		// System.out.println("biq: " + biqV.assign(Func.neg));

		/*
		 * MatrixFunctions.matrixWriteCSV("results/GM.csv", new
		 * BlockRealMatrix(GM.toArray()), false);
		 * MatrixFunctions.vectorWriteCSVinColumn("results/aM.csv", new
		 * ArrayRealVector(aM.toArray()), false);
		 * MatrixFunctions.matrixWriteCSV("results/CeqM.csv", new
		 * BlockRealMatrix(CeqM.toArray()), false);
		 * MatrixFunctions.vectorWriteCSVinColumn("results/beqM.csv", new
		 * ArrayRealVector(beqM.toArray()), false);
		 * MatrixFunctions.matrixWriteCSV("results/CiqM.csv", new
		 * BlockRealMatrix(CiqM.toArray()), false);
		 * MatrixFunctions.vectorWriteCSVinColumn("results/biqM.csv", new
		 * ArrayRealVector(biqM.toArray()), false);
		 */
		 solve(hour, gM, aV, ceqM, beqV, ciqM, biqV);
		}

	}
	
	public void solveHourlyPowerFlowsUnconstainedDA( ){//{ throws Exception {
		final int size = market.getGenCoList().size();
		final double[] aR = new double[size];
		final double[] bR = new double[size];
		final double[] capLower = new double[size];
		final double[] capUpper = new double[size];
		for (int i = 0; i < size; i++) {
			final GenCo gen = market.getGenCoList().get("genco" + (i + 1));
			aR[i] = gen.getaR(Settings.DA) * Settings.BASES;
			bR[i] = gen.getbR(Settings.DA) * Settings.BASES * Settings.BASES;
			capLower[i] = gen.getCapLR(Settings.DA) / Settings.BASES;
			capUpper[i] = gen.getCapUR(Settings.DA, 0) / Settings.BASES;
		}

		final DoubleMatrix2D gM = formGM(bR);
		final DoubleMatrix1D aV = formaM(aR);
		final DoubleMatrix2D ciqM = formCiqMmcp();
		final DoubleMatrix1D biqV = formbiqMmcp(capUpper, capLower);
		final DoubleMatrix2D ceqM = formCeqM();
		
		for (int hour = 0; hour < Settings.HOURS; hour++) {
			
			final DoubleMatrix1D beqV = formbeqM(hour);		
			solve(hour, gM, aV, ceqM, beqV, ciqM, biqV);
		}
	}
	
	public void resolveCongestionBM() {//throws Exception {
		
		final int size = market.getGenCoList().size();
		final double[] aRInc = new double[size];
		final double[] aRDec = new double[size];
		final double[] bRInc = new double[size];
		final double[] bRDec = new double[size];
		final double[] capLowerInc = new double[size];
		final double[] capLowerDec = new double[size];
		final double[] capUpperInc = new double[size];
		final double[] capUpperDec = new double[size];
		
		for (int i = 0; i < size; i++) {
			final GenCo gen = market.getGenCoList().get("genco" + (i + 1));
			
			aRInc[i] = gen.getaR(Settings.BM_INC) * Settings.BASES;
			aRDec[i] = (gen.getaR(Settings.DA) - gen.getaR(Settings.BM_DEC)) * Settings.BASES;
			
			bRInc[i] = gen.getbR(Settings.BM_INC) * Settings.BASES * Settings.BASES;
			bRDec[i] = (gen.getbR(Settings.DA) - gen.getbR(Settings.BM_DEC)) * Settings.BASES * Settings.BASES;
			
			capLowerInc[i] = gen.getCapLR(Settings.BM_INC) / Settings.BASES;
			capLowerDec[i] = 0.0;
		}
		
		final DoubleMatrix1D aV = formaMbm(aRInc, aRDec);
		final DoubleMatrix2D gM = formGMbm(bRInc, bRDec);
		final DoubleMatrix2D ciqM = formCiqMbm();
		final DoubleMatrix2D ceqM = formCeqMbm();
		
		for (int hour = 0; hour < Settings.HOURS; hour++) {
			
			for (int i = 0; i < size; i++) {
				final GenCo gen = market.getGenCoList().get("genco" + (i + 1));
				
				double capDiff = gen.getCapUR(Settings.DA, 0) - gen.getCommitment(Settings.DA, hour);
				if(capDiff < 1) {
					capUpperInc[i] = 0;
				} else {
					capUpperInc[i] = capDiff / Settings.BASES;
				}
				capUpperDec[i] = gen.getCommitment(Settings.DA, hour) / Settings.BASES;
			}
			final DoubleMatrix1D biqV = formbiqMbm(capUpperInc, capLowerInc, capUpperDec, capLowerDec);
			final DoubleMatrix1D beqV = formbeqMbm(hour);
			solve(hour, gM, aV, ceqM, beqV, ciqM, biqV);
		}
	}

	public void solve(final int hour, final DoubleMatrix2D gM,
			final DoubleMatrix1D aV, final DoubleMatrix2D ceqM,
			final DoubleMatrix1D beqV, final DoubleMatrix2D ciqM,
			final DoubleMatrix1D biqV) {
		double objective = Double.NaN;
		switch (Settings.QP_OPTIMIZER) {
		case Settings.QUADPROG_JAVA:
			objective = solveOPFinJava(hour, gM, aV, ceqM, beqV, ciqM, biqV);
			break;
		case Settings.QUADPROG_MATLAB:
			objective = solveOPFinMatlab(hour, gM, aV, ceqM, beqV, ciqM, biqV);
			break;
		case Settings.QUADPROG_R:
			objective = solveOPFinR(hour, gM, aV, ceqM, beqV, ciqM, biqV);
			break;
		case Settings.GUROBI:
			objective = solveOPFinGurobi(hour, gM, aV, ceqM, beqV, ciqM, biqV);
			break;
		default:
			//throw new ACEWEMdefaultSwitchStatement();
		}
		
		if(Settings.WRITE_OBJECTIVE) {
			String file = null;
			switch (Settings.MARKET_SWITHCER) {
			case Settings.DA:
				file = "Results/ObjectiveDA.csv";
				break;
			case Settings.BM:
				file = "Results/ObjectiveBM.csv";
				break;
			default:
			//throw new ACEWEMdefaultSwitchStatement();
			}
			market.getWrtCSV().writeObjective(file, objective);
		}
	}


	// ------------------------------------------------------------------------------------------------------------------------------------------
	/**
	 * Forms Matrix of a coefficients from the supply offer
	 * 
	 * @see <f/> DC-OPF.IEEEPES2007.JSLT.pdf (pg 20) alpha*P+beta*P^2
	 */
	private DoubleMatrix1D formaM(final double[] aR) {
		final DoubleMatrix1D aV = new DenseDoubleMatrix1D(aR);
		final DoubleMatrix1D aM = new DenseDoubleMatrix1D(market.getGenCoList().size() + market.getNodeList().length - 1);
		aM.viewPart(0, market.getGenCoList().size()).assign(aV);
		return aM;
	}

	private DoubleMatrix1D formaMbm(final double[] aRInc, final double[] aRDec) {
		final DoubleMatrix1D aVInc = new DenseDoubleMatrix1D(aRInc);
		final DoubleMatrix1D aVDec = new DenseDoubleMatrix1D(aRDec);
		final DoubleMatrix1D aM = new DenseDoubleMatrix1D(2 * market.getGenCoList().size() + market.getNodeList().length - 1);
		
		DoubleMatrix1D aV = Matrix1d.append(aVInc, aVDec);
		aM.viewPart(0, 2 * market.getGenCoList().size()).assign(aV);
		return aM;
	}
	
	// ------------------------------------------------------------------------------------------------------------------------------------------
	/**
	 * Forms Adjacency Matrix
	 * 
	 * @see <f/> DC-OPF.IEEEPES2007.JSLT.pdf (pg 19)
	 */
	private DoubleMatrix2D formAM() {
		final DoubleMatrix2D aM = new DenseDoubleMatrix2D(
				market.getBranchList().length,
				market.getNodeList().length);
		for (int n = 0; n < market.getBranchList().length; n++) {
			for (int k = 0; k < market.getNodeList().length; k++) {
				if (k == branchIndexM.get(n, 0) - 1) {
					aM.set(n, k, 1);
				} else if (k == branchIndexM.get(n, 1) - 1) {
					aM.set(n, k, (-1));
				} else {
					aM.set(n, k, 0);
				}
			}
		}
		return aM;
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------
	/**
	 * Forms Bus Admittance Matrix.
	 * 
	 * @see <f/> DC-OPF.IEEEPES2007.JSLT.pdf (pg 18)
	 */
	private DoubleMatrix2D formBAM(final DoubleMatrix2D nsM) {
		final DoubleMatrix2D baM = new DenseDoubleMatrix2D(market
				.getNodeList().length,market.getNodeList().length);
		for (int i = 0; i <market.getNodeList().length; i++) {
			for (int j = 0; j < market.getNodeList().length; j++) {
				if (j == i) {
					for (int k = 0; k < market.getNodeList().length; k++) {
						if (k != i) {
							baM.set(i, j, (baM.get(i, j) + nsM.get(i, k)));
						}
					}
				} else {
					baM.set(i, j, (-nsM.get(i, j)));
				}
			}
		}
		return baM;
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------
	/**
	 * Forms an Equality Constraint Vector
	 * 
	 * @see <f/> DC-OPF.IEEEPES2007.JSLT.pdf (pg 20)
	 */
	private DoubleMatrix1D formbeqM(final int hour) {
		final DoubleMatrix1D beqV = new DenseDoubleMatrix1D(market
				.getNodeList().length);
		for (int k = 0; k < market.getNodeList().length; k++) {
			double demand = 0;
			for (int j = 0; j < market.getLseList().size(); j++) {
				final LSE lse = market.getLseList().get("lse" + (j + 1));
				if (lse.getNode() == k + 1) {
					demand = demand
							+ lse.getElectricityDemand(market.getDay())[hour];
				}
			}
			beqV.set(k, demand / Settings.BASES);
		}
		return beqV;
	}
	
	private DoubleMatrix1D formbeqMbm(final int hour) {
		
		final DoubleMatrix1D beqV = new DenseDoubleMatrix1D(market
				.getNodeList().length);
		for (int k = 0; k < market.getNodeList().length; k++) {	
			
			double nodeCommitment = 0;
			for (int i = 0; i < market.getGenCoList().size(); i++) {
				final GenCo gen = market.getGenCoList().get("genco" + (i + 1));
				if (gen.getNode() == (k + 1)) {
					nodeCommitment = nodeCommitment
							+ gen.getCommitment(Settings.DA, hour);
				}
			}							
			
			double demand = 0;
			for (int j = 0; j < market.getLseList().size(); j++) {
				final LSE lse = market.getLseList().get("lse" + (j + 1));
				if (lse.getNode() == k + 1) {
					demand = demand
							+ lse.getElectricityDemand(market.getDay())[hour];
				}
			}
			beqV.set(k, (demand - nodeCommitment) / Settings.BASES);
		}
		return beqV;
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------
	/**
	 * Forms (2N+2I)×1 the associated inequality constraint vector
	 * 
	 * @see <f/> DC-OPF.IEEEPES2007.JSLT.pdf (pg 21)
	 */
	// biq = (-pU, -pU, capL, -capU)
	private DoubleMatrix1D formbiqMlmp(final double[] capUpper,
			final double[] capLower) {
		final int size = market.getBranchList().length;
		final double[] capacity = new double[size];
		for (int n = 0; n < size; n++) {
			capacity[n] = market.getBranchList()[n][Settings.CAPACITY]
					/ Settings.BASES;
		}

		final DoubleMatrix1D bCapM = new DenseDoubleMatrix1D(capacity);
		final DoubleMatrix1D capTLM = new DenseDoubleMatrix1D(capLower);
		final DoubleMatrix1D capTUM = new DenseDoubleMatrix1D(capUpper);
		final DoubleMatrix1D[] parts = { bCapM.copy().assign(Functions.neg),
				bCapM.copy().assign(Functions.neg), capTLM,
				capTUM.copy().assign(Functions.neg) };
		final DoubleMatrix1D biqV = new DenseDoubleMatrix1D(2
				* market.getBranchList().length + 2
				* market.getGenCoList().size());
		biqV.assign(Matrix1d.make(parts));
		return biqV;
	}
	
	private DoubleMatrix1D formbiqMlmpMatlab(final double[] capUpper,
			final double[] capLower) {
		final int size = market.getBranchList().length;
		final double[] capacity = new double[size];
		for (int n = 0; n < size; n++) {
			capacity[n] = market.getBranchList()[n][Settings.CAPACITY]
					/ Settings.BASES;
		}

		final DoubleMatrix1D bCapM = new DenseDoubleMatrix1D(capacity);
		final DoubleMatrix1D capTLM = new DenseDoubleMatrix1D(capLower.length);
		final DoubleMatrix1D capTUM = new DenseDoubleMatrix1D(capUpper.length);
		final DoubleMatrix1D[] parts = { bCapM.copy().assign(Functions.neg), bCapM.copy().assign(Functions.neg), capTLM, capTUM };
		final DoubleMatrix1D biqV = new DenseDoubleMatrix1D(2
				* market.getBranchList().length + 2
				* market.getGenCoList().size());
		biqV.assign(Matrix1d.make(parts));
		return biqV;
	}
	

/*	private DoubleMatrix1D formbiqMlmpAngleCon(final double[] capUpper,
			final double[] capLower) {
		final int size = market.getBranchList().length;
		final double[] capacity = new double[size];
		for (int n = 0; n < size; n++) {
			capacity[n] = market.getBranchList()[n][Settings.CAPACITY]
					/ Settings.BASES;
		}
		
		DoubleMatrix1D angLower = new DenseDoubleMatrix1D(market.getNodeList().length - 1).assign(- 5 * Math.PI);
		DoubleMatrix1D angUpper = new DenseDoubleMatrix1D(market.getNodeList().length - 1).assign(- 5 * Math.PI);

		final DoubleMatrix1D bCapM = new DenseDoubleMatrix1D(capacity);
		final DoubleMatrix1D capTLM = new DenseDoubleMatrix1D(capLower);
		final DoubleMatrix1D capTUM = new DenseDoubleMatrix1D(capUpper);
		final DoubleMatrix1D[] parts = { bCapM.copy().assign(Functions.neg),
				bCapM.copy().assign(Functions.neg), capTLM,
				capTUM.copy().assign(Functions.neg),
				angLower, angUpper};
		final DoubleMatrix1D biqV = new DenseDoubleMatrix1D(2
				* market.getBranchList().length + 2
				* market.getGenCoList().size() + 2 * (market.getNodeList().length - 1));
		biqV.assign(Matrix1d.make(parts));
		return biqV;
	}
*/	
	private DoubleMatrix1D formbiqMmcp(final double[] capUpper,
			final double[] capLower) {
		final int size = market.getBranchList().length;
		final double[] capacity = new double[size];

		final DoubleMatrix1D bCapM = new DenseDoubleMatrix1D(capacity);
		final DoubleMatrix1D capTLM = new DenseDoubleMatrix1D(capLower);
		final DoubleMatrix1D capTUM = new DenseDoubleMatrix1D(capUpper);
		final DoubleMatrix1D[] parts = { bCapM, bCapM, capTLM,
				capTUM.copy().assign(Functions.neg) };
		final DoubleMatrix1D biqV = new DenseDoubleMatrix1D(2
				* market.getBranchList().length + 2
				* market.getGenCoList().size());
		biqV.assign(Matrix1d.make(parts));
		return biqV;
	}
	
	

/*  Angle min and max constraint add!!!!!!!!!!!!	
	private DoubleMatrix1D formbiqMbm(final double[] capUpperInc,
									  final double[] capLowerInc,
									  final double[] capUpperDec,
									  final double[] capLowerDec) {
		
		final int size = market.getBranchList().length;
		final double[] capacity = new double[size];
		for (int n = 0; n < size; n++) {
			capacity[n] = market.getBranchList()[n][Settings.CAPACITY]
					/ Settings.BASES;
		}

		final DoubleMatrix1D bCapM = new DenseDoubleMatrix1D(capacity);
		final DoubleMatrix1D capTLMinc = new DenseDoubleMatrix1D(capLowerInc);
		final DoubleMatrix1D capTUMinc = new DenseDoubleMatrix1D(capUpperInc);
		final DoubleMatrix1D capTLMdec = new DenseDoubleMatrix1D(capLowerDec);
		final DoubleMatrix1D capTUMdec = new DenseDoubleMatrix1D(capUpperDec);
		
		DoubleMatrix1D angLower = new DenseDoubleMatrix1D(market.getNodeList().length - 1).assign(- 5 * Math.PI);
		DoubleMatrix1D angUpper = new DenseDoubleMatrix1D(market.getNodeList().length - 1).assign(- 5 * Math.PI);
		
		
		final DoubleMatrix1D[] parts = { bCapM.copy().assign(Functions.neg), bCapM.copy().assign(Functions.neg), 
				capTLMdec, capTUMdec.copy().assign(Functions.neg),
				capTLMinc, capTUMinc.copy().assign(Functions.neg),
				angLower, angUpper};
		
		
		final DoubleMatrix1D biqV = new DenseDoubleMatrix1D(2
				* market.getBranchList().length + 4
				* market.getGenCoList().size() + 2 * (market.getNodeList().length - 1));
		biqV.assign(Matrix1d.make(parts));
		return biqV;
	}
*/

private DoubleMatrix1D formbiqMbm(final double[] capUpperInc,
									  final double[] capLowerInc,
									  final double[] capUpperDec,
									  final double[] capLowerDec) {
		
		final int size = market.getBranchList().length;
		final double[] capacity = new double[size];
		for (int n = 0; n < size; n++) {
		capacity[n] = market.getBranchList()[n][Settings.CAPACITY]
		/ Settings.BASES;
		}
		
		final DoubleMatrix1D bCapM = new DenseDoubleMatrix1D(capacity);
		final DoubleMatrix1D capTLMinc = new DenseDoubleMatrix1D(capLowerInc);
		final DoubleMatrix1D capTUMinc = new DenseDoubleMatrix1D(capUpperInc);
		final DoubleMatrix1D capTLMdec = new DenseDoubleMatrix1D(capLowerDec);
		final DoubleMatrix1D capTUMdec = new DenseDoubleMatrix1D(capUpperDec);
		
		final DoubleMatrix1D[] parts = { bCapM.copy().assign(Functions.neg), bCapM.copy().assign(Functions.neg), 
		capTLMdec, capTUMdec.copy().assign(Functions.neg),
		capTLMinc, capTUMinc.copy().assign(Functions.neg)};
		
		
		final DoubleMatrix1D biqV = new DenseDoubleMatrix1D(2
		* market.getBranchList().length + 4
		* market.getGenCoList().size());
		biqV.assign(Matrix1d.make(parts));
return biqV;
}

	// ------------------------------------------------------------------------------------------------------------------------------------------
	/**
	 * Forms Equality Constraint Matrix
	 * 
	 * @see <f/> DC-OPF.IEEEPES2007.JSLT.pdf (pg 19)
	 */
	// CeqTranspose = (II, -Br'); Ceq = CeqTranspose'; where Br' is rBusAdm here
	private DoubleMatrix2D formCeqM() {
		final DoubleMatrix2D nsM = formNSM();
		final DoubleMatrix2D baM = formBAM(nsM);
		final DoubleMatrix2D rbaM = formRBAM(baM);
		final DoubleMatrix2D iiM = formIIM();

		final DoubleMatrix2D[][] parts = { { iiM,
			rbaM.viewDice().assign(Functions.neg) } };
		final DoubleMatrix2D CeqTMatrix = new DenseDoubleMatrix2D(
				market.getNodeList().length, market.getGenCoList()
				.size() + market.getNodeList().length - 1);
		CeqTMatrix.assign(Matrix2d.compose(parts));
		final DoubleMatrix2D ceqM = new DenseDoubleMatrix2D(market
				.getGenCoList().size()
				+ market.getNodeList().length - 1,
				market.getNodeList().length);
		ceqM.assign(CeqTMatrix.viewDice());
		return ceqM;

	}
	
	private DoubleMatrix2D formCeqMbm( ) {
		final DoubleMatrix2D nsM = formNSM();
		final DoubleMatrix2D baM = formBAM(nsM);
		final DoubleMatrix2D rbaM = formRBAM(baM);
		final DoubleMatrix2D iiM = formIIM();

		final DoubleMatrix2D[][] parts = { { iiM, iiM.copy().assign(Functions.neg),
			rbaM.viewDice().assign(Functions.neg) } };
		final DoubleMatrix2D CeqTMatrix = new DenseDoubleMatrix2D(
				market.getNodeList().length, 
				2 * market.getGenCoList().size() + market.getNodeList().length - 1);
		CeqTMatrix.assign(Matrix2d.compose(parts));
		final DoubleMatrix2D ceqM = new DenseDoubleMatrix2D(2 * market
				.getGenCoList().size()
				+ market.getNodeList().length - 1,
				market.getNodeList().length);
		ceqM.assign(CeqTMatrix.viewDice());
		return ceqM;

	}

	// ------------------------------------------------------------------------------------------------------------------------------------------
	/**
	 * Forms an Inequality Constraint Matrix Ciq
	 * 
	 * @see <f/> DC-OPF.IEEEPES2007.JSLT.pdf (pg 20)
	 */
	private DoubleMatrix2D formCiqMlmp() {
		final DoubleMatrix2D otM = formOtM();
		final DoubleMatrix2D opM = formOpM();
		final DoubleMatrix2D daM = formDAM();
		final DoubleMatrix2D aM = formAM();
		final DoubleMatrix2D raM = formRAM(aM);
		final DoubleMatrix2D ipM = formIpM();

		final DoubleMatrix2D[][] parts = { { otM, daM.zMult(raM, null) },
				{ otM, daM.copy().assign(Functions.neg).zMult(raM, null) },
				{ ipM, opM }, { ipM.copy().assign(Functions.neg), opM } };

		final DoubleMatrix2D CiqTMatrix = new DenseDoubleMatrix2D(2
				* market.getBranchList().length + 2
				* market.getGenCoList().size(), market.getGenCoList().size()
				+ market.getNodeList().length - 1);
		CiqTMatrix.assign(Matrix2d.compose(parts));
		final DoubleMatrix2D ciqM = new DenseDoubleMatrix2D(market
				.getGenCoList().size() + market.getNodeList().length - 1, 2
				* market.getBranchList().length + 2
				* market.getGenCoList().size());
		ciqM.assign(CiqTMatrix.viewDice());
		return ciqM;
	}
	
	private DoubleMatrix2D formCiqMlmpMatlab() {
		final DoubleMatrix2D otM = formOtM();
		final DoubleMatrix2D opM = formOpM();
		final DoubleMatrix2D daM = formDAM();
		final DoubleMatrix2D aM = formAM();
		final DoubleMatrix2D raM = formRAM(aM);
		
		final DoubleMatrix2D ipM = new DenseDoubleMatrix2D(market.getGenCoList().size(), market.getGenCoList().size());
		final DoubleMatrix2D[][] parts = { { otM, daM.zMult(raM, null) },
				{ otM, daM.copy().assign(Functions.neg).zMult(raM, null) },
				{ ipM, opM },{ ipM, opM}};

		final DoubleMatrix2D CiqTMatrix = new DenseDoubleMatrix2D(2
				* market.getBranchList().length + 2
				* market.getGenCoList().size(), market.getGenCoList().size()
				+ market.getNodeList().length - 1);
		CiqTMatrix.assign(Matrix2d.compose(parts));
		final DoubleMatrix2D ciqM = new DenseDoubleMatrix2D(market
				.getGenCoList().size() + market.getNodeList().length - 1, 2
				* market.getBranchList().length + 2
				* market.getGenCoList().size());
		ciqM.assign(CiqTMatrix.viewDice());
		return ciqM;
	}

	
/*	private DoubleMatrix2D formCiqMlmpAngleCon() {
		final DoubleMatrix2D otM = formOtM();
		final DoubleMatrix2D opM = formOpM();
		final DoubleMatrix2D daM = formDAM();
		final DoubleMatrix2D aM = formAM();
		final DoubleMatrix2D raM = formRAM(aM);
		final DoubleMatrix2D ipM = formIpM();
		

		DoubleMatrix2D anglConM = Matrix2d.identity(market.getNodeList().length - 1);
		DoubleMatrix2D angleZeorM = new DenseDoubleMatrix2D(market.getNodeList().length - 1, market.getGenCoList().size());

		final DoubleMatrix2D[][] parts = { { otM, daM.zMult(raM, null) },
				{ otM, daM.copy().assign(Functions.neg).zMult(raM, null) },
				{ ipM, opM }, { ipM.copy().assign(Functions.neg), opM },
				{ angleZeorM,  anglConM},{ angleZeorM, anglConM.copy().assign(Functions.neg)}};

		final DoubleMatrix2D CiqTMatrix = new DenseDoubleMatrix2D(2
				* market.getBranchList().length + 2
				* market.getGenCoList().size() + 2 * (market.getNodeList().length - 1), market.getGenCoList().size()
				+ market.getNodeList().length - 1);
		CiqTMatrix.assign(Matrix2d.compose(parts));
		final DoubleMatrix2D ciqM = new DenseDoubleMatrix2D(market
				.getGenCoList().size() + market.getNodeList().length - 1, 2
				* market.getBranchList().length + 2
				* market.getGenCoList().size() + 2 * (market.getNodeList().length - 1));
		ciqM.assign(CiqTMatrix.viewDice());
		return ciqM;
	}
*/
	
	private DoubleMatrix2D formCiqMbm() {
		final DoubleMatrix2D otM = formOtM();
		final DoubleMatrix2D opM = formOpM();
		final DoubleMatrix2D daM = formDAM();
		final DoubleMatrix2D aM = formAM();
		final DoubleMatrix2D raM = formRAM(aM);
		final DoubleMatrix2D ipMInc = formIpM();
		final DoubleMatrix2D ipMDec = formIpM();
		
		DoubleMatrix2D ooM = new DenseDoubleMatrix2D(opM.rows(), otM.columns());

		final DoubleMatrix2D[][] parts = { { otM, otM, daM.zMult(raM, null) },{ otM, otM, daM.copy().assign(Functions.neg).zMult(raM, null) },
				{ooM, ipMInc, opM }, {ooM, ipMInc.copy().assign(Functions.neg), opM },
				{ ipMDec, ooM, opM }, { ipMDec.copy().assign(Functions.neg), ooM, opM }};

		final DoubleMatrix2D CiqTMatrix = new DenseDoubleMatrix2D(2
				* market.getBranchList().length + 4
				* market.getGenCoList().size(), 2 * market.getGenCoList().size()
				+ market.getNodeList().length - 1);
		CiqTMatrix.assign(Matrix2d.compose(parts));
		final DoubleMatrix2D ciqM = new DenseDoubleMatrix2D(2 * market.getGenCoList().size() + market.getNodeList().length - 1, 2
				* market.getBranchList().length + 4 * market.getGenCoList().size());
		ciqM.assign(CiqTMatrix.viewDice());
		return ciqM;
	}
	
/*  Angle min and max value constraint added !!!!	
	private DoubleMatrix2D formCiqMbm() {
		final DoubleMatrix2D otM = formOtM();
		final DoubleMatrix2D opM = formOpM();
		final DoubleMatrix2D daM = formDAM();
		final DoubleMatrix2D aM = formAM();
		final DoubleMatrix2D raM = formRAM(aM);
		final DoubleMatrix2D ipMInc = formIpM();
		final DoubleMatrix2D ipMDec = formIpM();
		
		DoubleMatrix2D ooM = new DenseDoubleMatrix2D(opM.rows(), otM.columns());
		DoubleMatrix2D anglConM = Matrix2d.identity(market.getNodeList().length - 1);
		
		DoubleMatrix2D angleZeorM = new DenseDoubleMatrix2D(market.getNodeList().length - 1, market.getGenCoList().size());

		final DoubleMatrix2D[][] parts = { { otM, otM, daM.zMult(raM, null) },{ otM, otM, daM.copy().assign(Functions.neg).zMult(raM, null) },
				{ooM, ipMInc, opM }, {ooM, ipMInc.copy().assign(Functions.neg), opM },
				{ ipMDec, ooM, opM }, { ipMDec.copy().assign(Functions.neg), ooM, opM },
				{ angleZeorM, angleZeorM,  anglConM},{ angleZeorM, angleZeorM, anglConM.assign(Functions.neg)}};

		final DoubleMatrix2D CiqTMatrix = new DenseDoubleMatrix2D(2
				* market.getBranchList().length + 4
				* market.getGenCoList().size() + 2 * (market.getNodeList().length - 1), 2 * market.getGenCoList().size()
				+ market.getNodeList().length - 1);
		CiqTMatrix.assign(Matrix2d.compose(parts));
		final DoubleMatrix2D ciqM = new DenseDoubleMatrix2D(2 * market.getGenCoList().size() + market.getNodeList().length - 1, 2
				* market.getBranchList().length + 4 * market.getGenCoList().size() + 2 * (market.getNodeList().length - 1));
		ciqM.assign(CiqTMatrix.viewDice());
		return ciqM;
	}
*/	
	
	private DoubleMatrix2D formCiqMmcp() {
		final DoubleMatrix2D otM = formOtM();
		final DoubleMatrix2D opM = formOpM();
		final DoubleMatrix2D ipM = formIpM();
		
		final DoubleMatrix2D zeroM = new DenseDoubleMatrix2D(otM.rows(), opM.columns());
		final DoubleMatrix2D[][] parts = { { otM, zeroM },
										   { otM, zeroM },
										   { ipM, opM }, 
										   { ipM.copy().assign(Functions.neg), opM } };
				
		final DoubleMatrix2D CiqTMatrix = new DenseDoubleMatrix2D(2
				* market.getBranchList().length + 2
				* market.getGenCoList().size(), market.getGenCoList().size()
				+ market.getNodeList().length - 1);
		CiqTMatrix.assign(Matrix2d.compose(parts));
		final DoubleMatrix2D ciqM = new DenseDoubleMatrix2D(market
				.getGenCoList().size() + market.getNodeList().length - 1, 2
				* market.getBranchList().length + 2
				* market.getGenCoList().size());
		ciqM.assign(CiqTMatrix.viewDice());
		return ciqM;
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------
	/**
	 * Forms Diagonal Admittance Matrix, where Bkm=1/Xkm
	 * 
	 * @see <f/> DC-OPF.IEEEPES2007.JSLT.pdf (pg 21)
	 */
	private DoubleMatrix2D formDAM() {
		final int size = market.getBranchList().length;
		final DoubleMatrix1D reactanceInv = new DenseDoubleMatrix1D(size);
		for (int n = 0; n < size; n++) {
			reactanceInv.set(n,
					1 / market.getBranchList()[n][Settings.REACTANCE]);
		}

		DoubleMatrix2D daM = new DenseDoubleMatrix2D(
				market.getBranchList().length, market.getBranchList().length);
		daM = Matrix2d.diagonal(reactanceInv);
		return daM;

	}

	/**
	 * Forms Voltage Angle Difference Weight Matrix
	 * 
	 * @see <f/> DC-OPF.IEEEPES2007.JSLT.pdf (pg 16)
	 */
	private DoubleMatrix2D formGM(final double[] bR) {
		final DoubleMatrix2D vadwM = formVADWM();
		final DoubleMatrix2D rvadwM = formRVADWM(vadwM);

		final DoubleMatrix1D bM = new DenseDoubleMatrix1D(bR);
		final DoubleMatrix2D uM = new DenseDoubleMatrix2D(Matrix2d.diagonal(
				bM.assign(Functions.mult(2))).toArray()); // Matrix U has 2*B
		// diagonal and 0
		// for other
		// elements
		final DoubleMatrix2D wrrM = rvadwM;
		final DoubleMatrix2D gM = new DenseDoubleMatrix2D(market.getGenCoList()
				.size() + market.getNodeList().length - 1, market
				.getGenCoList().size()
				+ market.getNodeList().length - 1);
		gM.assign(Matrix2d.composeDiagonal(uM, wrrM));
		return gM;
	}
	
	
	private DoubleMatrix2D formGMbm(final double[] bRInc, final double[] bRDec) {
		final DoubleMatrix2D vadwM = formVADWM();
		final DoubleMatrix2D rvadwM = formRVADWM(vadwM);

		final DoubleMatrix1D bMInc = new DenseDoubleMatrix1D(bRInc);
		final DoubleMatrix1D bMDec = new DenseDoubleMatrix1D(bRDec);
		final DoubleMatrix2D uMInc = new DenseDoubleMatrix2D(Matrix2d.diagonal(bMInc.assign(Functions.mult(2))).toArray()); 
		final DoubleMatrix2D uMDec = new DenseDoubleMatrix2D(Matrix2d.diagonal(bMDec.assign(Functions.mult(2))).toArray());
		final DoubleMatrix2D wrrM = rvadwM;
		final DoubleMatrix2D gM = new DenseDoubleMatrix2D(2 * market.getGenCoList()
				.size() + market.getNodeList().length - 1, 2 * market
				.getGenCoList().size()
				+ market.getNodeList().length - 1);
		gM.assign(Matrix2d.composeDiagonal(uMInc, uMDec, wrrM));
		return gM;
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------
	/**
	 * Forms II Matrix which represents K Nodes(rows) and I Generators(columns)
	 * located on them. <f/> If generator Ii is located at node Ki then the
	 * matrix element has value of 1
	 * 
	 * @see <f/> DC-OPF.IEEEPES2007.JSLT.pdf (pg 19)
	 */
	private DoubleMatrix2D formIIM() {
		final DoubleMatrix2D iiM = new DenseDoubleMatrix2D(market
				.getNodeList().length, market.getGenCoList().size());
		for (int k = 0; k < market.getNodeList().length; k++) {
			for (int i = 0; i < market.getGenCoList().size(); i++) {
				final int id = i + 1;
				if (market.getGenCoList().get("genco" + id).getNode() == k + 1) {
					iiM.set(k, i, 1);
				}
			}
		}
		return iiM;
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------
	/**
	 * Forms I×I Identity Matrix
	 * 
	 * @see <f/> DC-OPF.IEEEPES2007.JSLT.pdf (pg 21)
	 */
	private DoubleMatrix2D formIpM() {
		final DoubleMatrix2D ipM = new DenseDoubleMatrix2D(market
				.getGenCoList().size(), market.getGenCoList().size())
		.assign(Matrix2d.identity(market.getGenCoList().size()));
		return ipM;
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------
	/**
	 * Forms Negative Susceptance Matrix.
	 * 
	 * @see <f/> DC-OPF.IEEEPES2007.JSLT.pdf (pg 18)
	 */
	private DoubleMatrix2D formNSM() {
		final DoubleMatrix2D nsM = new DenseDoubleMatrix2D(market
				.getNodeList().length, market.getNodeList().length);
		for (int n = 0; n < market.getBranchList().length; n++) {
			final double[] branchData = market.getBranchList()[n];

			nsM.set((int) branchIndexM.get(n, 0) - 1,
					(int) branchIndexM.get(n, 1) - 1,
					1 / branchData[Settings.REACTANCE]);
			nsM.set((int) branchIndexM.get(n, 1) - 1,
					(int) branchIndexM.get(n, 0) - 1,
					1 / branchData[Settings.REACTANCE]);
		}
		return nsM;
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------
	/**
	 * Forms O an (I x K-1) zero Matrix
	 * 
	 * @see <f/> DC-OPF.IEEEPES2007.JSLT.pdf (pg 21)
	 */
	private DoubleMatrix2D formOpM() {
		final DoubleMatrix2D opM = new DenseDoubleMatrix2D(market
				.getGenCoList().size(), market.getNodeList().length - 1);
		return opM;
	}

	/**
	 * Forms O an N × I zero Matrix
	 * 
	 * @see <f/> DC-OPF.IEEEPES2007.JSLT.pdf (pg 21)
	 */
	private DoubleMatrix2D formOtM() {
		final DoubleMatrix2D otM = new DenseDoubleMatrix2D(
				market.getBranchList().length, market.getGenCoList().size());
		return otM;
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------
	/**
	 * Forms Reduced Adjacency Matrix
	 * 
	 * @see <f/> DC-OPF.IEEEPES2007.JSLT.pdf (pg 19)
	 */
	private DoubleMatrix2D formRAM(final DoubleMatrix2D aM) {
		DoubleMatrix2D raM = new DenseDoubleMatrix2D(
				market.getBranchList().length,
				market.getNodeList().length - 1);
		raM = aM.viewPart(0, 1, market.getBranchList().length,
				market.getNodeList().length - 1);
		return raM;
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------
	/**
	 * Forms Reduced Bus Admittance Matrix.
	 * 
	 * @see <f/> DC-OPF.IEEEPES2007.JSLT.pdf (pg 18)
	 */
	private DoubleMatrix2D formRBAM(final DoubleMatrix2D baM) {
		DoubleMatrix2D rbaM = new DenseDoubleMatrix2D(market
				.getNodeList().length - 1,
				market.getNodeList().length);
		rbaM = baM.viewPart(1, 0, market.getNodeList().length - 1,
				market.getNodeList().length);
		return rbaM;
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------
	/**
	 * Forms Reduced Voltage Angle Difference Weight Matrix by deleting of
	 * VADWMatrix first row and first column
	 * 
	 * @see <f/> DC-OPF.IEEEPES2007.JSLT.pdf (pg 17)
	 */
	private DoubleMatrix2D formRVADWM(final DoubleMatrix2D vadwM) {
		final DoubleMatrix2D rvadwM = new DenseDoubleMatrix2D(market
				.getNodeList().length - 1,
				market.getNodeList().length - 1);
		for (int i = 0; i < market.getNodeList().length - 1; i++) {
			for (int j = 0; j < market.getNodeList().length - 1; j++) {
				rvadwM.set(i, j, vadwM.get(i + 1, j + 1));
			}
		}
		return rvadwM;
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------
	/**
	 * Forms Voltage Angle Difference Weight Matrix
	 * 
	 * @see <f/> DC-OPF.IEEEPES2007.JSLT.pdf (pg 16)
	 */
	private DenseDoubleMatrix2D formVADWM() {
		final DenseDoubleMatrix2D vadwM = new DenseDoubleMatrix2D(
				market.getNodeList().length, market
						.getNodeList().length);
		for (int n = 0; n < market.getBranchList().length; n++) {
			vadwM.set((int) branchIndexM.get(n, 0) - 1,
					(int) branchIndexM.get(n, 1) - 1, -2 * Settings.PENALTY); // NOTE:
			vadwM.set((int) branchIndexM.get(n, 1) - 1,
					(int) branchIndexM.get(n, 0) - 1, -2 * Settings.PENALTY);
		}
		for (int i = 0; i < market.getNodeList().length; i++) {
			for (int j = 0; j < market.getNodeList().length; j++) {
				if (j == i) {
					for (int k = 0; k < market.getNodeList().length; k++) {
						if (k != i) {
							vadwM.set(i, j, (vadwM.get(i, j) - vadwM.get(i, k)));
						}
					}
				}
			}
		}
		return vadwM;
	}

	private void setBranchIndexM() {
		branchIndexM = new DenseDoubleMatrix2D(market.getBranchList().length,
				Settings.TWO);
		final int size = market.getBranchList().length;

		market.getBranchList();

		for (int n = 0; n < size; n++) {
			branchIndexM.set(n, 0, market.getBranchList()[n][Settings.FROM]);
			branchIndexM.set(n, 1, market.getBranchList()[n][Settings.TO]);
		}
	}

	private double  solveOPFinJava(final int hour, final DoubleMatrix2D gM,
			final DoubleMatrix1D aV, final DoubleMatrix2D ceqM,
			final DoubleMatrix1D beqV, final DoubleMatrix2D ciqM,
			final DoubleMatrix1D biqV) {//throws Exception {
		QuadProgJ qpj = new QuadProgJ(gM, aV, ceqM, beqV, ciqM, biqV);
		
		//17 x 38 matrix and 11 x 38 matrix
		
		if (!qpj.getIsFeasibleAndOptimal()) {
			System.err.println("QuadProgJ failed to solve QCP");
		}
		
		switch (Settings.MARKET_SWITHCER) {
		case Settings.DA:
			assignDAresults(hour, qpj.getMinX(), qpj.getEqMultipliers());
			break;
		case Settings.BM:
			assignBMresults(hour, qpj.getMinX());
			break;
		default:
			//throw new ACEWEMdefaultSwitchStatement();
		}			
	return  qpj.getMinF();
	}
		
	private void assignDAresults(int hour, double[] solution, double[] price) {
		// OPF solution for (p_{G1},...,p_{GI}) in SI
		for (int i = 0; i < market.getGenCoList().size(); i++) {
			final GenCo gen = market.getGenCoList().get("genco" + (i + 1));
			gen.setCommitment(Settings.DA, hour, solution[i]
					* Settings.BASES);
		}

		// OPF solution for (delta_2,...,delta_K)
		for (int k = market.getGenCoList().size(); k < market
				.getGenCoList().size() + market.getNodeList().length - 1; k++) {

			final double angle = solution[k];
			market.setAngleList(Settings.DA, k - market.getGenCoList().size() + 1, hour,
					angle);
		}

		// LMP: locational marginal prices in SI
		if(Settings.QP_OPTIMIZER == Settings.QUADPROG_MATLAB) {
			switch (Settings.MARKETS) {
			case Settings.SINGLE_DA:
				for (int k = 0; k < market.getNodeList().length; k++) {
				market.getLMP()[k][hour] = - price[k] / Settings.BASES;
				}
			break;
			case Settings.DA_BM:
				//System.out.println(new ArrayRealVector(price));
				market.getMCP()[Settings.DA][hour] = - price[0] / Settings.BASES;
				break;
			default:
				//throw new ACEWEMdefaultSwitchStatement();
			}	 
		} else {
			switch (Settings.MARKETS) {
			case Settings.SINGLE_DA:
				for (int k = 0; k < market.getNodeList().length; k++) {
				market.getLMP()[k][hour] = price[k] / Settings.BASES;
				}
			break;
			case Settings.DA_BM:
				//System.out.println(new ArrayRealVector(price));
				market.getMCP()[Settings.DA][hour] = price[0] / Settings.BASES;
				break;
			default:
				//throw new ACEWEMdefaultSwitchStatement();
			}	 
		}
	}
	
/*	private void assignDAresultsUnconstrained(int hour, double[] solution) {
		// OPF solution for (p_{G1},...,p_{GI}) in SI
		for (int i = 0; i < market.getGenCoList().size(); i++) {
			final GenCo gen = market.getGenCoList().get("genco" + (i + 1));			
			gen.setCommitment(Settings.DA, hour, solution[i]
					* Settings.BASES);
		}

		// OPF solution for (delta_2,...,delta_K)
		for (int k = market.getGenCoList().size(); k < market
				.getGenCoList().size() + market.getNodeList().length - 1; k++) {

			final double angle = solution[k];
			market.setAngleList(Settings.DA, k - market.getGenCoList().size() + 1, hour,
					angle);
		}
		
		final double mcp = getMarketClearingPrice(market,
				hour, Settings.DA);
		market.getMCP()[Settings.DA][hour] = mcp;
		
	}
*/
	
	private void assignBMresults(int hour, double[] solution) {
		// OPF solution for (p_{G1},...,p_{GI}) in SI
		for (int i = 0; i < market.getGenCoList().size(); i++) {
			final GenCo gen = market.getGenCoList().get("genco" + (i + 1));
			gen.setCommitment(Settings.BM_INC, hour, solution[i]
					* Settings.BASES);
		}
		
		for (int i = 0; i < market.getGenCoList().size(); i++) {
			final GenCo gen = market.getGenCoList().get("genco" + (i + 1));
			gen.setCommitment(Settings.BM_DEC, hour, solution[i + market.getGenCoList().size()]
					* Settings.BASES);
		}

		// OPF solution for (delta_2,...,delta_K)
		int index = 2 * market.getGenCoList().size();
		for (int k = 0; k <  market.getNodeList().length - 1; k++) {

			final double angle = solution[index + k];
			market.setAngleList(Settings.BM, k + 1, hour,
					angle);
		}
		
		final double mcp_INC = getMarketClearingPrice(
				market, hour, Settings.BM_INC);
		final double mcp_DEC = getMarketClearingPrice(
				market, hour, Settings.BM_DEC);

		market.getMCP()[Settings.BM_INC][hour] = mcp_INC;

		if (mcp_DEC == Double.MAX_VALUE) {
			market.getMCP()[Settings.BM_DEC][hour] = 0.0;
		} else {
			market.getMCP()[Settings.BM_DEC][hour] = mcp_DEC;
		}
	}
		

	private double solveOPFinMatlab(final int hour, final DoubleMatrix2D gM,
			final DoubleMatrix1D aV, final DoubleMatrix2D ceqM,
			final DoubleMatrix1D beqV, final DoubleMatrix2D ciqM,
			final DoubleMatrix1D biqV) {//throws Exception {


		/* MatrixFunctions.matrixWriteCSV("results/mat/gM.csv", new
		 BlockRealMatrix(gM.toArray()), false);
		 MatrixFunctions.vectorWriteCSVinRow("results/mat/aV.csv", new
		 ArrayRealVector(aV.toArray()), false);
		 MatrixFunctions.matrixWriteCSV("results/mat/ceqM.csv", new
		 BlockRealMatrix(ceqM.copy().viewDice().toArray()), false);
		 MatrixFunctions.vectorWriteCSVinRow("results/mat/beqV.csv", new
		 ArrayRealVector(beqV.toArray()), false);
		 MatrixFunctions.matrixWriteCSV("results/mat/ciqM.csv", new
		 BlockRealMatrix(ciqM.copy().viewDice().assign(Functions.neg).toArray()), false);
		 MatrixFunctions.vectorWriteCSVinRow("results/mat/biqV.csv", new
		 ArrayRealVector(biqV.copy().assign(Functions.neg).toArray()), false);
		 */
		// MatrixFunctions.vectorWriteCSVinRow("results/mat/lb.csv", new
		// ArrayRealVector(lb), false);
		// MatrixFunctions.vectorWriteCSVinRow("results/mat/ub.csv", new
		// ArrayRealVector(ub), false);

		/*
		 * result = quadprog.solveQCP(1, new MWNumericArray(gM.toArray(),
		 * MWClassID.DOUBLE), new MWNumericArray(aV.toArray(),
		 * MWClassID.DOUBLE), new
		 * MWNumericArray(ciqM.viewDice().assign(Functions.neg).toArray(),
		 * MWClassID.DOUBLE), new
		 * MWNumericArray(biqV.assign(Functions.neg).toArray(),
		 * MWClassID.DOUBLE), new MWNumericArray(ceqM.viewDice().toArray(),
		 * MWClassID.DOUBLE), new MWNumericArray(beqV.toArray(),
		 * MWClassID.DOUBLE))
		 * 
		 * MWNumericArray out = (MWNumericArray) result[0];
		 */ 
		
		int size = - 1;
		double[] lb = null;
		double[] ub = null;
		if (Settings.MARKET_SWITHCER == Settings.DA) {
			size = market.getGenCoList().size() + market.getNodeList().length - 1;
			lb = new double[size];
			ub = new double[size];

			for (int i = 0; i < market.getGenCoList().size(); i++) {
				final GenCo gen = market.getGenCoList().get("genco" + (i + 1));
				lb[i] = gen.getCapLR(Settings.DA) / Settings.BASES;
				ub[i] = gen.getCapUR(Settings.DA, 0) / Settings.BASES;
			}
			
			for (int i = 0; i < market.getNodeList().length - 1; i++) {
				lb[i + market.getGenCoList().size()] =  Double.NEGATIVE_INFINITY; //Math.PI;rge -5 * Math.PI;/
				ub[i + market.getGenCoList().size()] =  Double.POSITIVE_INFINITY; //Math.PI; e  5 * Math.PI;
			}
		} else {
			size = 2 * market.getGenCoList().size() + market.getNodeList().length - 1;
			lb = new double[size];
			ub = new double[size];
			
			for (int k = 0; k < market.getGenCoList().size(); k++) {
				final GenCo gen = market.getGenCoList().get("genco" + (k + 1));
				lb[k] = gen.getCapLR(Settings.BM_INC) / Settings.BASES;
				
				ub[k] = (gen.getCapUR(Settings.DA, 0) - gen.getCommitment(Settings.DA, hour)) / Settings.BASES;
				if(ub[k] < 0.0) {
					ub[k] = 0.0;
				} 				
								
				lb[k +  market.getGenCoList().size()] = gen.getCapLR(Settings.BM_DEC) / Settings.BASES;
				ub[k +  market.getGenCoList().size()] = gen.getCommitment(Settings.DA, hour) / Settings.BASES;
				if(ub[k +  market.getGenCoList().size()] < 0.0) {
					ub[k +  market.getGenCoList().size()] = 0.0;
				} 	

			}
			
			int index = 2 * market.getGenCoList().size();
			for (int i = 0; i < market.getNodeList().length - 1; i++) {
				lb[i + index] = Double.NEGATIVE_INFINITY;
				ub[i + index] = Double.POSITIVE_INFINITY; 
			}
		}
		
		if(Settings.MARKET_SWITHCER == Settings.BM) {
			int stop = 0;
		}
		
		 Object[] result = null;
		 try {			 
		/*	 result = quadprogM.activeSetQCP(4, 
					 						 gM.toArray(),
					 						 aV.toArray(),
					 						 ciqM.copy().viewDice().assign(Functions.neg).toArray(), 
					 						 biqV.copy().assign(Functions.neg).toArray(),
					 						 ceqM.copy().viewDice().toArray(),
					 						 beqV.toArray(), 
					 						 lb,
					 						 ub,
					 						 "off", 
					 						 "off", 
					 						 200);
			 */
					 						 
			 
			 // This optimisation method gives same results as other optimisers, R, Java, Gurobi
			 result = quadprogM.interiorPointConvexQCP(4, 
					 								   gM.toArray(),
					 								   aV.toArray(), 
					 								   ciqM.copy().viewDice().assign(Functions.neg).toArray(),
					 								   biqV.copy().assign(Functions.neg).toArray(),
					 								   ceqM.copy().viewDice().toArray(),
					 								   beqV.toArray(), 
					 								   lb,
					 								   ub,
					 								   "off", 
					 								   "off", 
					 								   200,
					 								   1e-08,
					 								   1e-08,
					 								   1e-08);
			 

		 printOptimisationStatus(((MWNumericArray) result[3]).getInt(), hour);
		 } catch (final Exception e) {
			 System.out.println("Matlab failed at hour  " + hour);
			 System.out.println("Exception: " + e.toString());
		 }
		 
		 switch (Settings.MARKET_SWITHCER) {
			case Settings.DA:
				assignDAresults(hour, ((MWNumericArray) result[0]).getDoubleData(), ((MWNumericArray) result[1]).getDoubleData());
				break;
			case Settings.BM:
				assignBMresults(hour, ((MWNumericArray) result[0]).getDoubleData());
				break;
			default:
				//throw new ACEWEMdefaultSwitchStatement();
			}			
		return ((MWNumericArray) result[2]).getDoubleData()[0];
	}

	private double solveOPFinR(final int hour, final DoubleMatrix2D gM,
			final DoubleMatrix1D aV, final DoubleMatrix2D ceqM,
			final DoubleMatrix1D beqV, final DoubleMatrix2D ciqM,
			final DoubleMatrix1D biqV) {//throws Exception {

		final double[] gm = new double[gM.columns() * gM.rows()];
		int e = 0;
		for (int c = 0; c < gM.columns(); c++) {
			for (int r = 0; r < gM.rows(); r++) {
				gm[e] = gM.get(r, c);
				e++;
			}
		}

		final double[] ceq = new double[ceqM.columns() * ceqM.rows()];
		e = 0;
		for (int c = 0; c < ceqM.columns(); c++) {
			for (int r = 0; r < ceqM.rows(); r++) {
				ceq[e] = ceqM.get(r, c);
				e++;
			}
		}

		final double[] ciq = new double[ciqM.columns() * ciqM.rows()];
		e = 0;
		for (int c = 0; c < ciqM.columns(); c++) {
			for (int r = 0; r < ciqM.rows(); r++) {
				ciq[e] = ciqM.get(r, c);
				e++;
			}
		}

		double objective = Double.NaN;
		double[] solution = null;
		double[] price = null;
		try {
			final RConnection rConnection = ACEWEMmodel.getrConnection();

			rConnection.assign("gm", gm);
			rConnection.assign("numCols", new int[] { gM.rows() });
			rConnection.voidEval("GM <- matrix(gm, numCols)");

			rConnection.assign("ceq", ceq);
			rConnection.assign("numCols", new int[] { ceqM.rows() });
			rConnection.voidEval("Ceq <- matrix(ceq, numCols)");

			rConnection.assign("ciq", ciq);
			rConnection.assign("numCols", new int[] { ciqM.rows() });
			rConnection.voidEval("Ciq <- matrix(ciq, numCols)");

			rConnection.assign("aM", aV.copy().assign(Functions.neg).toArray());
			rConnection.assign("beq", beqV.toArray());
			rConnection.assign("biq", biqV.toArray());

			rConnection.voidEval("Amat = cbind(Ceq, Ciq)");
			rConnection.voidEval("bvec = c(beq, biq)");

			rConnection
			.voidEval("result = solve.QP(GM, aM, Amat, bvec, ncol(Ceq))");

			solution = rConnection.eval("result$solution").asDoubles();
			objective = rConnection.eval("result$value").asDoubles()[0];
			

			price = rConnection.eval("result$Lagrangian").asDoubles();
			// rConnection.eval("write.csv(bvec, file = \"C:/Users/Daniil/Desktop/bvec.csv\")");

		} catch (final RserveException e1) {
			e1.printStackTrace();
		} catch (final REngineException e1) {
			e1.printStackTrace();
		} catch (final REXPMismatchException e1) {
			e1.printStackTrace();
		}

		for (int i = 0; i < market.getGenCoList().size(); i++) {
			final int id = i + 1;
			final GenCo gen = market.getGenCoList().get("genco" + id);
			gen.setCommitment(Settings.DA, hour, solution[i] * Settings.BASES);
		}

		for (int k = market.getGenCoList().size(); k < market.getGenCoList()
				.size() + market.getNodeList().length - 1; k++) {

			final double angle = solution[k];
			market.setAngleList(Settings.DA, k - market.getGenCoList().size() + 1, hour,
					angle);
		}
		
		 switch (Settings.MARKET_SWITHCER) {
			case Settings.DA:
				assignDAresults(hour, solution, price);
				break;
			case Settings.BM:
				assignBMresults(hour, solution);
				break;
			default:
				//throw new ACEWEMdefaultSwitchStatement();
		}			
		return objective;
	}
	
	private double solveOPFinGurobi(final int hour, final DoubleMatrix2D gM,
			final DoubleMatrix1D aV,  DoubleMatrix2D ceqM,
			final DoubleMatrix1D beqV, DoubleMatrix2D ciqM,
			DoubleMatrix1D biqV) {//throws Exception {		
		

		ciqM = ciqM.copy().viewDice().assign(Functions.neg);
		biqV = biqV.copy().assign(Functions.neg);
		ceqM = ceqM.copy().viewDice();
		
		GRBModel model = null;
		double objective = Double.NaN;
		try {
			final GRBEnv env = new GRBEnv();
			env.set(GRB.IntParam.LogToConsole, 0);
			model = new GRBModel(env);
			
			
			int size = - 1;
			double[] lb = null;
			double[] ub = null;
			double[] solution = null;
			if (Settings.MARKET_SWITHCER == Settings.DA) {
				size = market.getGenCoList().size() + market.getNodeList().length - 1;
				lb = new double[size];
				ub = new double[size];

				for (int i = 0; i < market.getGenCoList().size(); i++) {
					final GenCo gen = market.getGenCoList().get("genco" + (i + 1));
					lb[i] = gen.getCapLR(Settings.DA) / Settings.BASES;
					ub[i] = gen.getCapUR(Settings.DA, 0) / Settings.BASES;
				}
				
				for (int i = 0; i < market.getNodeList().length - 1; i++) {
					lb[i + market.getGenCoList().size()] =  - GRB.INFINITY; //Math.PI;rge -5 * Math.PI;/
					ub[i + market.getGenCoList().size()] =    GRB.INFINITY; //Math.PI; e  5 * Math.PI;
				}
				
			    solution = new double[market.getGenCoList().size() + market.getNodeList().length - 1];
			} else {
				size = 2 * market.getGenCoList().size() + market.getNodeList().length - 1;
				lb = new double[size];
				ub = new double[size];
				
				for (int k = 0; k < market.getGenCoList().size(); k++) {
					final GenCo gen = market.getGenCoList().get("genco" + (k + 1));
					lb[k] = gen.getCapLR(Settings.BM_INC) / Settings.BASES;
					ub[k] = (gen.getCapUR(Settings.DA, 0) - gen.getCommitment(
							Settings.DA, hour)) / Settings.BASES;
					
					lb[k +  market.getGenCoList().size()] = gen.getCapLR(Settings.BM_DEC) / Settings.BASES;
					ub[k +  market.getGenCoList().size()] = gen.getCommitment(Settings.DA, hour) / Settings.BASES;
				}
			
				int index = 2 * market.getGenCoList().size();
				for (int i = 0; i < market.getNodeList().length - 1; i++) {
					lb[i + index] = - GRB.INFINITY;
					ub[i + index] =   GRB.INFINITY; 
				}
				
			    solution = new double[2 * market.getGenCoList().size() + market.getNodeList().length - 1];
			}

			GRBVar[] vars = model.addVars(lb, ub, null, null, null);
			model.update();
			

       	 	GRBQuadExpr obj = new GRBQuadExpr();
       		 for (int i = 0; i < gM.rows(); i++) {
       			 for (int j = 0; j < gM.columns(); j++) {
       				 if (gM.get(i, j) != 0) {
       					 obj.addTerm(gM.get(i, j) / 2.0, vars[i], vars[j]);
       				 }
       			 }
       		 }
       		 
       		 for (int j = 0; j < aV.size(); j++) {
       		 			if (aV.get(j) != 0) {
       		 				obj.addTerm(aV.get(j), vars[j]); 
       		 			}
       		 }
       		 model.setObjective(obj);
       		 model.update();
       	
			
       		
          	for (int i = 0; i < ciqM.rows(); i++) {
          		GRBLinExpr expr = new GRBLinExpr();
          		for (int j = 0; j < ciqM.columns(); j++) {
          				if (ciqM.get(i, j) != 0) {
          					 expr.addTerm(ciqM.get(i, j), vars[j]);
          				}
          		}
          		model.addConstr(expr, GRB.LESS_EQUAL, biqV.get(i) , null);
          	}
          	model.update();
			
          	GRBConstr[]constrArray = new GRBConstr[market.getNodeList().length];
          	for (int i = 0; i < ceqM.rows(); i++) {
          		GRBLinExpr expr = new GRBLinExpr();
          		for (int j = 0; j < ceqM.columns(); j++) {
          				if (ceqM.get(i, j) != 0) {
          					 expr.addTerm(ceqM.get(i, j), vars[j]);
          				}
          		}
          		constrArray[i] = model.addConstr(expr, GRB.EQUAL, beqV.get(i) , null);
          	}
          	model.update();
          	
    		model.optimize();
    		printOptimisationStatus(model.get(GRB.IntAttr.Status), hour);
    		    
    		//printObjectiveValue(model);
    		// OptimUtilsGurobi.updateVoltageAnglesDA(vars, market);
    			 
    		
    		for (int i = 0; i < solution.length; i++){   				     			
    			solution[i] = vars[i].get(GRB.DoubleAttr.X);
    		}
    			 
    			
    		double[] price = null;
    		price = new double[constrArray.length];
    		for (int i = 0; i < price.length; i++){ 
    			price[i] = constrArray[i].get(GRB.DoubleAttr.Pi);// / Settings.BASES;
    		}
    		
    		
	   		 switch (Settings.MARKET_SWITHCER) {
	   			case Settings.DA:
    				assignDAresults(hour, solution, price);
	   				break;
	   			case Settings.BM:
					assignBMresults(hour, solution);
	   				break;
	   			default:
	   				//throw new ACEWEMdefaultSwitchStatement();
	   		}	    		
    		objective = model.get(GRB.DoubleAttr.ObjVal);
    			     	
    		//MatrixFunctions.vectorWriteCSVinRow("results/mat/solutionGurobi.csv", (ArrayRealVector) out, false);
		} catch (final GRBException e) {
			System.out.println("Error code: " + e.getErrorCode() + ". "
					+ e.getMessage());
		}
		return objective;
	}
	
	private double getMarketClearingPrice(final ACEWEMmodel market,
			final int hour, final int whichMarket) {

		final double[] marginalCosts = new double[market.getGenCoList().size()];
		final double[] commitments = new double[market.getGenCoList().size()];
		final Enumeration<String> e = market.getGenCoList().keys();
		int q = 0;
		while (e.hasMoreElements()) {
			final GenCo gen = market.getGenCoList().get(e.nextElement());
			commitments[q] = gen.getCommitment(whichMarket, hour);
			marginalCosts[q] = gen.getaR(whichMarket) + 2.0
					* gen.getbR(whichMarket) * commitments[q];
			q++;
		}

		double marketClearingPrice = Double.NaN;
		if (whichMarket == Settings.BM_DEC) {
			marketClearingPrice = Double.MAX_VALUE;
			for (int i = 0; i < marginalCosts.length; i++) {
				// if genCo does not buy power than it cannot be marginal
				// generator
				if (marginalCosts[i] < marketClearingPrice
						&& commitments[i] != 0.0) {
					marketClearingPrice = marginalCosts[i];
				}
			}
		} else {
			marketClearingPrice = 0.0;
			for (int i = 0; i < marginalCosts.length; i++) {
				// if genCo does not produce power than it cannot be marginal
				// generator
				if (marginalCosts[i] > marketClearingPrice
						&& commitments[i] >= 1.0) {
					marketClearingPrice = marginalCosts[i];
				}
			}
		}
		return marketClearingPrice;
	}
	
	private  void printObjectiveValue(final GRBModel model) {
		try {
			System.out.println(model.get(GRB.DoubleAttr.ObjVal));
		} catch (final GRBException e) {
			e.printStackTrace();
		}
	}
	
	private  void printOptimisationStatus(int status, int hour) {
		
		 switch (Settings.QP_OPTIMIZER) {
			case Settings.GUROBI:
			 switch (status) {
	  			case 1:
	  				System.out.println("sp = "+hour+". Model is loaded, but no solution information is available.");
	  				break;
	  			case 3:
	  				System.out.println("sp = "+hour+". Model was proven to be infeasible.");
	  				break;
	  			case 4:
	  				System.out.println("sp = "+hour+". Model was proven to be either infeasible or unbounded. To obtain a more definitive conclusion, set the DualReductions parameter to 0 and reoptimize.");
	  				break;
	  			case 5:
	  				System.out.println("sp = "+hour+". Model was proven to be unbounded. Important note: an unbounded status indicates the presence of an unbounded ray that allows the objective to improve without limit. It says nothing about whether the model has a feasible solution. If you require information on feasibility, you should set the objective to zero and reoptimize.");
	  				break;
	  			case 6:
	  				System.out.println("sp = "+hour+". Optimal objective for model was proven to be worse than the value specified in the Cutoff parameter. No solution information is available.");
	  				break;
	  			case 7:
	  				System.out.println("sp = "+hour+". Optimization terminated because the total number of simplex iterations performed exceeded the value specified in the IterationLimit parameter, or because the total number of barrier iterations exceeded the value specified in the BarIterLimit parameter.");
	  				break;
	  			case 8:
	  				System.out.println("sp = "+hour+". Optimization terminated because the total number of branch-and-cut nodes explored exceeded the value specified in the NodeLimit parameter.");
	  				break;
	  			case 9:
	  				System.out.println("sp = "+hour+". Optimization terminated because the time expended exceeded the value specified in the TimeLimit parameter.");
	  				break;
	  			case 10:
	  				System.out.println("sp = "+hour+". Optimization terminated because the number of solutions found reached the value specified in the SolutionLimit parameter.");
	  				break;
	  			case 11:
	  				System.out.println("sp = "+hour+". Optimization was terminated by the user.");
	  				break;
	  			case 12:
	  				System.out.println("sp = "+hour+". Optimization was terminated due to unrecoverable numerical difficulties.");
	  				break;
	  			case 13:
	  				System.out.println("sp = "+hour+". Unable to satisfy optimality tolerances; a sub-optimal solution is available.");;
	  				break;
				case 14:
	  				System.out.println("sp = "+hour+". A non-blocking optimization call was made (by setting the NonBlocking parameter to 1 in a Gurobi Compute Server environment), but the associated optimization run is not yet complete.");;
	  				break;
	  			default:
	  				//throw new ACEWEMdefaultSwitchStatement();
	  		}
			break;
			case Settings.QUADPROG_MATLAB:
				 switch (status) {
		  			case 0:
		  				System.out.println("sp = "+hour+". Number of iterations exceeded options.MaxIter.");
		  				break;
		  			case -2:
		  				System.out.println("sp = "+hour+". Problem is infeasible.");
		  				break;
		  			case -3:
		  				System.out.println("sp = "+hour+". Problem is unbounded.");
		  				break;
		  			case -6:
		  				System.out.println("sp = "+hour+". Nonconvex problem detected.");
		  				break;
		  			case 4:
		  				System.out.println("sp = "+hour+". Local minimizer was found.");
		  				break;
		  			case -7:
		  				System.out.println("sp = "+hour+". Magnitude of search direction became too small. No further progress could be made. The problem is ill-posed or badly conditioned.");
		  				break;
		  			default:
		  				//throw new ACEWEMdefaultSwitchStatement();
		  		}
  				break;
		default:
			//throw new ACEWEMdefaultSwitchStatement();
		}
	}
	
	
}
