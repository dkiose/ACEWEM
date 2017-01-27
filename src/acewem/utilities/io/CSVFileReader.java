package acewem.utilities.io;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JOptionPane;

public class CSVFileReader {
	public static void main(final String[] args) {
		final CSVFileReader x = new CSVFileReader("data/Countries.csv");
		x.readFile();
		x.displayArrayList();
	}

	private String fileName = "data/oilAll.csv";

	public ArrayList<String> storeValues = new ArrayList<String>();

	public CSVFileReader() {
	}

	public CSVFileReader(final String fileName) {
		this.fileName = fileName;
	}

	public void displayArrayList() {
		for (int x = 0; x < storeValues.size(); x++) {
			// Make sure ALL rows have the same columns which specified by the
			// first row!
			if (storeValues.get(x).split(",").length != storeValues.get(0)
					.split(",").length) {
				JOptionPane.showMessageDialog(null,
						"error with number of colums");
				System.out.println((Arrays.toString(storeValues.get(x).split(
						","))));
				return;
			} else {
				// System.out.println(Arrays.toString(storeValues.get(x).split(",")));
				final String[] line = storeValues.get(x).split(",");

				for (int i = 0; i < line.length; i++) {
					System.out.println("Index:" + i + " text:" + line[i]);
				}
			}
		}
	}

	public String getFileName() {
		return fileName;
	}

	public ArrayList getFileValues() {
		return storeValues;
	}

	public void readFile() {
		try {
			storeValues.clear();// just in case this is the second call of the
								// ReadFile Method./
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			String line;
			while ((line = br.readLine()) != null) {
				storeValues.add(line);
			}
			br.close();
			br = null;
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}

	}

	// mutators and accesors
	public void setFileName(final String newFileName) {
		fileName = newFileName;
	}

}