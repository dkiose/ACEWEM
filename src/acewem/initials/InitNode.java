package acewem.initials;

import gamlss.utilities.oi.CSVFileReader;

import java.util.ArrayList;

import acewem.market.ACEWEMmodel;

public class InitNode {

	/**
	 * 
	 * @param market
	 *            - Object of ACEWEMmodel
	 */
	public InitNode(final ACEWEMmodel market) {

		switch (Settings.MODELTYPE) {
		case Settings.FIVE_NODE:
			initializeNodes(market, "data/FIVE_NODE_NODE.csv");
			break;
		case Settings.UK:
			initializeNodes(market, "data/UK/UK_NODE.csv");
			break;
		case Settings.SIX_NODE:
			initializeNodes(market, "data/SIX_NODE/SIX_NODE_NODE.csv");
			break;
		case Settings.FOUR_NODE:
			initializeNodes(market, "data/4-NODE_NODE.csv");
			break;
		default:
			System.out.println("Default NODE.SCV loaded");
			initializeNodes(market, "data/NODE.csv");

		}
	}

	private void initializeNodes(final ACEWEMmodel market, final String fileName) {

		final CSVFileReader readData = new CSVFileReader(fileName);
		readData.readFile();
		final ArrayList<String> data = readData.storeValues;

		String[][] nodes = null;
		if (market.isFromGUI()) {
			nodes = new String[data.size() - 1][Settings.THREE];
			for (int i = 1; i < data.size(); i++) {
				final String[] line = data.get(i).split(",");
				nodes[i - 1][Settings.NODE] = line[0];
				nodes[i - 1][Settings.X_LOCATION] = line[1];
				nodes[i - 1][Settings.Y_LOCATION] = line[2];
			}
		} else {
			nodes = new String[data.size() - 1][Settings.ONE];
			for (int i = 1; i < data.size(); i++) {
				final String[] line = data.get(i).split(",");
				nodes[i - 1][Settings.NODE] = line[0];
			}
		}
		market.setNodeList(nodes);
	}
}