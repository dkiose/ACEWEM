package acewem.initials;

import gamlss.utilities.oi.CSVFileReader;

import java.util.ArrayList;

import acewem.market.ACEWEMmodel;
import acewem.market.GenCo;

/**
 * Holds the methods for loading the initial data of power genrators from csv
 * file.
 * 
 * @author Daniil
 * 
 */
public class InitGenCo {

	/**
	 * Constructor.
	 * 
	 * @param market
	 *            - Object of ACEWEMmodel class
	 */
	public InitGenCo(final ACEWEMmodel market) {

		switch (Settings.MODELTYPE) {
		case Settings.FIVE_NODE:
			initializeGenCos(market, "data/FIVE_NODE_GENCO.csv");
			break;
		case Settings.UK:
			initializeGenCos(market, "data/UK/UK_GENCO.csv");
			break;
		case Settings.SIX_NODE:
			initializeGenCos(market, "data/SIX_NODE/SIX_NODE_GENCO.csv");
			break;
		case Settings.FOUR_NODE:
			initializeGenCos(market, "data/4-NODE_GENCO.csv");
			break;
		default:
			System.out.println("Default GENCO.SCV loaded");
			initializeGenCos(market, "data/GENCO.csv");
		}
	}

	/**
	 * 
	 * @param market
	 *            - Object of ACEWEMmodel class
	 */
	private void initializeGenCos(final ACEWEMmodel market,
			final String fileName) {

		final CSVFileReader readData = new CSVFileReader(fileName);
		readData.readFile();
		final ArrayList<String> data = readData.storeValues;
		final int numOfGenCos = data.size() - 1;

		for (int i = 1; i < data.size(); i++) {
			final String[] line = data.get(i).split(",");
			final double[] initData = new double[Settings.SIX];
			initData[Settings.GENCO] = i;

			for (int n = 0; n < market.getNodeList().length; n++) {
				if (line[0].equals(market.getNodeList()[n][Settings.NODE])) {
					initData[Settings.GENCO_AT_NODE] = n + 1;
					break;
				}
			}

			initData[Settings.GENCO_CAPACITY_UPPER] = Double
					.parseDouble(line[2]) * Double.parseDouble(line[3]);
			initData[Settings.GENCO_CAPACITY_LOWER] = 0.0;
			initData[Settings.INTERCEPT] = Double.parseDouble(line[4]);
			initData[Settings.SLOPE] = Double.parseDouble(line[5]);

			final GenCo gen = new GenCo(market, initData, numOfGenCos);
			market.getGenCoList().put("genco" + i, gen);
		}
	}
}
