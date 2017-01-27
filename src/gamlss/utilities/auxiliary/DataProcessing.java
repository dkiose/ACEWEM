package gamlss.utilities.auxiliary;

import gamlss.utilities.oi.CSVFileReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

public class DataProcessing {
	
	 private int fromNode = 0;
private int toNode = 1;
private int branchReactance = 2;
private int branchCapacity = 3;
public HashMap<Integer, Object>  branchListTemp = new HashMap<Integer, Object>() ;
private int bus = 0;

HashMap<Integer, Object>branchListcleared = new HashMap<Integer, Object>();

	public DataProcessing(){
		
		 String fileName = "Data/UK/UK_BRANCH.csv";	
		 CSVFileReader readData = new CSVFileReader(fileName);
		 readData.readFile();
		 ArrayList<String> data = readData.storeValues;
		 
		 fileName = "Data/UK/UK_NODE.csv";	
		 readData = new CSVFileReader(fileName);
		 readData.readFile();
		 ArrayList<String> nodeData = readData.storeValues;

		 for (int i = 0; i < data.size(); i++) {
			 String[] line = data.get(i).split(",");
			Hashtable<String, String> branchData =  
					new Hashtable<String, String>();
			branchData.put("from", line[fromNode]);
			branchData.put("to", line[toNode]);
			branchData.put("reactance", line[branchReactance]);
			branchData.put("capacity", line[branchCapacity]);
			branchListTemp.put((i + 1), branchData);	
		 }	
		 			 
		 for (int i = 0; i < branchListTemp.size(); i++) {
			 Hashtable<String, String> temp1 = 
					 	(Hashtable) branchListTemp.get((i + 1));
			 
			 Double r1 = Double.parseDouble(temp1.get("reactance"));
			 Double c1 = Double.parseDouble(temp1.get("capacity"));		 
			 
			 for (int j = i; j < branchListTemp.size(); j++) {
				 Hashtable<String, String> temp2 = 
						 (Hashtable) branchListTemp.get((j + 1));
				 
				 if (i != j) {

					 if (temp1.get("from").equals(temp2.get("from"))) {
						 if (temp1.get("to").equals(temp2.get("to"))) {
							 
							 
							 double r2 = Double.parseDouble(temp2.get("reactance"));
							 r1 =  (r1 * r2) / (r1 + r2);
							 
							 double c2 = Double.parseDouble(temp2.get("capacity"));
							 c1 =  c1 + c2;
							 
							 Hashtable<String, String> tempHash2 =  
										new Hashtable<String, String>();
							 
							 tempHash2.put("from", "TF" + i);
							 tempHash2.put("to", "TT" + i);
							 tempHash2.put("reactance", "1"); 
							 tempHash2.put("capacity", "0");
							 branchListTemp.put((j + 1), tempHash2);
							 
						 }
					 }
				 }		 
			 }
			 
			 Hashtable<String, String> tempHash =  
						new Hashtable<String, String>();
			 
			 for (Integer j = 0; j < nodeData.size(); j++) {
					String[] lineNode = nodeData.get(j).split(",");
					
					if (temp1.get("from").equals(lineNode[bus])) {
						temp1.put("from", lineNode[bus]);
					}
					if (temp1.get("to").equals(lineNode[bus])) {
						temp1.put("to", lineNode[bus]);
					}
				}
			 
			 tempHash.put("from", temp1.get("from"));
			 tempHash.put("to", temp1.get("to"));
			 tempHash.put("reactance", r1.toString()); 
			 tempHash.put("capacity",  c1.toString());
			 branchListTemp.put((i + 1), tempHash);				 
		 }
		 
			int k = 0;	
			for (int i = 0; i < branchListTemp.size(); i++) {
			 
				Hashtable<String, String> temp = 
						(Hashtable) branchListTemp.get((i + 1));
		 
				if (Double.parseDouble(temp.get("capacity")) == 0) { 
					k++;
				}
				else {
					
					branchListcleared.put((i + 1 - k), temp);
				}
			}
			for (int i = 0; i < branchListcleared.size(); i++) {
									
				 Hashtable<String, String> temp11 = (Hashtable<String, String>) branchListcleared.get(i + 1);
				 Hashtable<String, Double> temp2 =  new Hashtable<String, Double>();				 					
System.out.println(temp11.get("from")+"  "+temp11.get("to")+"  "+temp11.get("reactance")+"   "+temp11.get("capacity"));	
			}
	}
	
	public static void main(final String[] args) {
		new DataProcessing();
	}

}
