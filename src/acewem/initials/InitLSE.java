package acewem.initials;

import gamlss.utilities.oi.CSVFileReader;

import java.util.ArrayList;

import acewem.market.ACEWEMmodel;
import acewem.market.LSE;

public class InitLSE {

	/**
	 * 
	 * @param market
	 *            - Object of ACEWEMmodel
	 */
	public InitLSE(final ACEWEMmodel market) {

		switch (Settings.MODELTYPE) {
		case Settings.FIVE_NODE:
			initializeLSE(market, "data/FIVE_NODE_LSE.csv");
			break;
		case Settings.UK:
			if (Settings.DEMAND_FIXED) {
				initializeLSE(market, "data/UK/UK_LSE_fixed.csv");
			} else {
				initializeLSE(market, "data/UK/UK_LSE.csv");
			}
			break;
		case Settings.SIX_NODE:
			initializeLSE(market, "data/SIX_NODE/SIX_NODE_LSE.csv");
			break;
		case Settings.FOUR_NODE:
			initializeLSE(market, "data/4-NODE_LSE.csv");
			break;
		default:
			System.out.println("Default LSE.SCV loaded");
			initializeLSE(market, "data/LSE.csv");
		}
	}

	private void initializeLSE(final ACEWEMmodel market, final String fileName) {

		final CSVFileReader readData = new CSVFileReader(fileName);
		readData.readFile();
		final ArrayList<String> data = readData.storeValues;

		for (int i = 1; i < data.size(); i++) {
			final String[] line = data.get(i).split(",");
			final double[] initData = new double[Settings.THREE];
			final double[][] load = new double[(line.length - 2)
					/ Settings.HOURS][Settings.HOURS];

			initData[Settings.LSE] = i;

			for (int n = 0; n < market.getNodeList().length; n++) {
				if (line[0].equals(market.getNodeList()[n][Settings.NODE])) {
					initData[Settings.LSE_AT_NODE] = n + 1;
					break;
				}
			}

			int day = 0;
			int h = 0;
			for (int j = 2; j < line.length; j++) {

				load[day][h] = Double.parseDouble(line[j]);
				if (h == (Settings.HOURS - 1)) {
					h = 0;
					day++;
				} else {
					h++;
				}
			}

			final LSE lse = new LSE(initData, load);
			market.getLseList().put("lse" + i, lse);
		}
	}
}