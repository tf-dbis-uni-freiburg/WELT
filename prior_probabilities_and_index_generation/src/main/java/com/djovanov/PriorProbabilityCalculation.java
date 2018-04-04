package com.djovanov;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentProcessor;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;


/**
 * @author Dimitar
 * Used for the calculcation of prior probability for the Lucene index creation.
 * The whole HashMap is stored and serialized in an output file.
 */
public class PriorProbabilityCalculation implements EntityDocumentProcessor {

	// Variables
	private HashMap<String, Integer> priorMap;
	int count;
	public static final String fileName = "prior-probability-hash-map.ser";
	
	/**
	 * Main method. Processes the whole dump using this processor and writes the
	 * results to a file. To change which dump file to use and whether to run in
	 * offline mode, modify the settings in {@link ExampleHelpers}.
	 *
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Helpers.configureLogging();
		PriorProbabilityCalculation priorCalculation = new PriorProbabilityCalculation();
		
		Helpers.processEntitiesFromWikidataDump(priorCalculation);
		priorCalculation.writeOutput();
	}
	
	
	public PriorProbabilityCalculation() {
		this.priorMap = new HashMap<>();
		this.count = 0;
	}
	
	@Override
	public void processItemDocument(ItemDocument itemDocument) {
		String idString = itemDocument.getEntityId().getId();
		this.priorMap.put(idString, itemDocument.getSiteLinks().values().size());
		this.count++;
		
		// Print a report every 100000 items:
		if (this.count % 100000 == 0) {
			printStatus();
		}
	}

	@Override
	public void processPropertyDocument(PropertyDocument propertyDocument) {
		
	}
	
	private void printStatus() {
		System.out.println("---");
		System.out.println(this.count + " items processed.");
	}
	
	public void writeOutput() {
		try (ObjectOutputStream out = new ObjectOutputStream(
			Helpers.openExampleFileOuputStream(fileName))) {

			out.writeObject(priorMap);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
