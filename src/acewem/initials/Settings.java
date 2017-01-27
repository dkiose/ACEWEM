package acewem.initials;

public final class Settings {

	/** Base apparent power value (MVA). */
	public static final double BASES = 100;;
	/** Base voltage value (kV). */
	// public static final double BASEV = 10;
	/** 180 degrees. */
	public static final double DEG180 = 180.0;
	/** Soft penalty weight for voltage angle differences. */
	public static final double PENALTY = 0.05;
	/** Abstract model = 0. */
	public static final int FIVE_NODE = 0;
	/** Uk model = 1. */
	public static final int UK = 1;
	/** 6-Node case = 3. */
	public static final int SIX_NODE = 3;
	public static final int FOUR_NODE = 4;
	/** Twenty-four hours. */
	public static int HOURS = 24;

	public static boolean DEMAND_FIXED;

	public static final int YEAR = 365;
	/** Reinforcement learning algorithm of agents. */
	public static final int REINF = 1;
	/** Stochastic optimization algorithm with Gamlss. */
	public static final int STOCH = 0;
	/**
	 * First 365 market days, Reinforcement learning is used to accumulate the
	 * data, after day 365 Stochastic optimization is used.
	 */
	public static final int MIXED365 = 2;
	/**
	 * The agent should learn taking into account accumulated daily profits:
	 * Revenue - Total costs.
	 */
	public static final int PROFIT = 1;
	/**
	 * The agent should learn taking into account net daily: earnings Revenue -
	 * Variable costs.
	 */
	public static final int NETEARN = 0;
	/** Small system optimisator */
	public static final int SMALL_SCALE_OPTIM = 0;
	/** Large system optimisator */
	public static final int LARGE_SCALE_OPTIM = 1;

	public static boolean IS_INITIALIZED = false;

	public static final int DA = 0;
	public static final int BM = 1;
	public static final int BM_INC = 1;
	public static final int BM_DEC = 2;
	public static final int INC_DEC = 3;
	public static final int SINGLE_DA = 0;
	public static final int DA_BM = 1;

	public static final int A_R = 0;
	public static final int B_R = 1;
	public static final int CAP_RL = 2;
	public static final int CAP_RU = 3;

	public static boolean REWRITE_INPUT_DATA = false;
	// public static int DATA;
	public static final String STRAIGHT = "s";
	public static final String LOGGED = "l";
	public static final String GROWTH_RATE = "g";

	public static final String AUTOREGRESSIVE = "a";
	public static final String MARGINAL = "m";
	public static final String FIXED = "f";

	public static int MARKET_SWITHCER = DA;
	public static int MARKETS = 0;
	/** Models switcher. */
	public static int MODELTYPE = -1;
	/** Learning algorithm of agents. */
	public static int LEARNALG = 0;
	/** Optimisatar */
	// public static int OPTIM;

	public final static double ZERO = 0.0;
	public final static int ONE = 1;
	public final static int TWO = 2;
	public final static int THREE = 3;
	public final static int FOUR = 4;
	public final static int FIVE = 5;
	public final static int SIX = 6;
	public final static int THIRTEEN = 13;

	public static int DEMAND_SHOCK_DAY = 15;
	public static int CYCLIC_SHOCK_DAY = 0;
	public static int SUPPLY_SHOCK_DAY = 15;
	public static boolean DEMAND_SHOCK = false;
	public static boolean SUPPLY_SHOCK = false;
	public static double DEMAND_SHOCK_MAGNITUDE = 1.0;
	public static double SUPPLY_SHOCK_MAGNITUDE = 1.0;

	public static final int R = 8;
	public static final int BOBYQA = 9;
	public static final int CMAES = 10;
	public static final int QUADPROG_JAVA = 11;
	public static final int QUADPROG_MATLAB = 12;
	public static final int QUADPROG_R = 13;
	public static final int GUROBI = 14;
	public static int QP_OPTIMIZER = GUROBI;
	public static int NONLINEAR_OPTIMIZER = R;

	public static int DAYS_MAX = 3550;

	public static final int PRICE = 0;
	public static final int MW = 1;

	public static final int BRANCH = 0;
	public static final int FROM = 1;
	public static final int TO = 2;
	public static final int REACTANCE = 3;
	public static final int CAPACITY = 4;

	public static final int GENCO = 0;
	public static final int GENCO_AT_NODE = 1;
	public static final int GENCO_CAPACITY_UPPER = 2;
	public static final int GENCO_CAPACITY_LOWER = 3;
	public static final int INTERCEPT = 4;
	public static final int SLOPE = 5;

	public static final int LSE = 0;
	public static final int LSE_AT_NODE = 1;

	public static final int COOLING = 0;
	public static final int EXPEREMENTATION = 1;
	public static final int PROPENSITY = 2;
	public static final int RECENCY = 3;
	public static final int RANADOMSEED = 4;
	public static final int M1 = 5;
	public static final int M2 = 6;
	public static final int M3 = 7;
	public static final int RIMaxL = 8;
	public static final int RIMaxU = 9;
	public static final int RIMinC = 10;
	public static final int PRICECAP = 11;
	public static final int SLOPESTART = 12;

	public static final int NODE = 0;
	public static final int X_LOCATION = 1;
	public static final int Y_LOCATION = 2;
		
	public static  double AGENT_WIDTH = 3.0;
	public static  double AGENT_HEIGHT = 3.5;
	
	public static boolean WRITE_OBJECTIVE = false;
}
