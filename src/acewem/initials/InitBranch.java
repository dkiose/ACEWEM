package acewem.initials;

import gamlss.utilities.oi.CSVFileReader;

import java.util.ArrayList;

import acewem.market.ACEWEMmodel;

public class InitBranch {

	/**
	 * 
	 * @param market
	 *            - Object of ACEWEMmodel
	 */
	public InitBranch(final ACEWEMmodel market) {

		switch (Settings.MODELTYPE) {
		case Settings.FIVE_NODE:
			initializeBranches(market, "data/FIVE_NODE_BRANCH.csv");
			break;
		case Settings.UK:
			initializeBranches(market, "data/UK/UK_BRANCH.csv");
			break;
		case Settings.SIX_NODE:
			initializeBranches(market, "data/SIX_NODE/SIX_NODE_BRANCH.csv");
			break;
		case Settings.FOUR_NODE:
			initializeBranches(market, "data/4-NODE_BRANCH.csv");
			break;
		default:
			System.out.println("Default BRANCH.SCV loaded");
			initializeBranches(market, "data/BRANCH.csv");
		}
	}

	private void initializeBranches(final ACEWEMmodel market,
			final String fileName) {

		final CSVFileReader readData = new CSVFileReader(fileName);
		readData.readFile();
		final ArrayList<String> data = readData.storeValues;
		final double[][] initData = new double[data.size() - 1][Settings.FIVE];

		for (int i = 1; i < data.size(); i++) {
			final String[] line = data.get(i).split(",");

			initData[i - 1][Settings.BRANCH] = i;

			for (int n = 0; n < market.getNodeList().length; n++) {
				if (line[0].equals(market.getNodeList()[n][Settings.NODE])) {
					initData[i - 1][Settings.FROM] = n + 1;
					break;
				}
			}

			for (int n = 0; n < market.getNodeList().length; n++) {
				if (line[1].equals(market.getNodeList()[n][Settings.NODE])) {
					initData[i - 1][Settings.TO] = n + 1;
					break;
				}
			}

			initData[i - 1][Settings.REACTANCE] = Double.parseDouble(line[2]);
			initData[i - 1][Settings.CAPACITY] = Double.parseDouble(line[3]);
		}
		market.setBranchList(initData);
	}
}
