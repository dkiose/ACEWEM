package acewem.market;

import sim.engine.SimState;
import sim.engine.Steppable;
import acewem.initials.Settings;
import acewem.utilities.optimisation.QuadProg;

public class ISO implements Steppable {

	private QuadProg qcp;
	private ACEWEMmodel market;

	public ISO(final ACEWEMmodel market) {
		qcp = new QuadProg(market);
		this.market = market;
	}

	public final void step(final SimState state) {
				

		switch (Settings.MARKETS) {
		case Settings.SINGLE_DA:
			System.out.println("------------- day "+market.getDay()+" market DA -------------");
			qcp.solveHourlyPowerFlowsConstainedDA();

			break;
		case Settings.DA_BM:

			switch (Settings.MARKET_SWITHCER) {
			case Settings.DA:
				System.out.println("------------- day "+market.getDay()+" market DA -------------");
				qcp.solveHourlyPowerFlowsUnconstainedDA();
				break;
			case Settings.BM:
				System.out.println("------------- day "+market.getDay()+" market BM -------------");
				qcp.resolveCongestionBM();
				break;
			default:
				System.err.println(" The requested power market "
						+ "is not implemented");
			}
			break;
		default:
			System.err.println(" The requested power market "
					+ "cannot be modelled in ISO");
		}
	}
	
	public QuadProg getQCP() {
		return qcp;
	}
}