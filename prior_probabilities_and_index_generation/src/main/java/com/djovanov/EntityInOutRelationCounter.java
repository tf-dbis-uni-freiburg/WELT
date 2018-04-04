package com.djovanov;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentProcessor;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;


/**
 * @author Dimitar
 * Used for the calculcation of prior probability for the Lucene index creation.
 * The whole HashMap is stored and serialized in an output file.
 */
public class EntityInOutRelationCounter implements EntityDocumentProcessor {

	// Variables
	private HashMap<String, Integer> degreeMap;
	int count;
	public static final String fileName = "vertex-degree-hash-map.ser";
	
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
		EntityInOutRelationCounter priorCalculation = new EntityInOutRelationCounter();
		
		Helpers.processEntitiesFromWikidataDump(priorCalculation);
		priorCalculation.writeOutput();
	}
	
	
	public EntityInOutRelationCounter() {
		this.degreeMap = new HashMap<>();
		this.count = 0;
	}
	
	@Override
	public void processItemDocument(ItemDocument itemDocument) {
		String idString = itemDocument.getEntityId().getId();
		boolean exists = this.degreeMap.containsKey(idString);

		// out-edges counter, counts the number of relations that will be iterated
		int statementCount = 0;
		for (StatementGroup sg : itemDocument.getStatementGroups()) {
			statementCount += sg.getStatements().size();
			for (Statement s : sg.getStatements()) {
				if(s.getClaim().getMainSnak() instanceof ValueSnak) {
					Value value = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
					if (value instanceof EntityIdValue) {
						String idValueString = ((EntityIdValue) value).getIri();
						boolean objectExists = this.degreeMap.containsKey(idValueString);
						// addition of in-edges for other entities
						if(objectExists) {
							int mapValue = this.degreeMap.get(idValueString);
							this.degreeMap.put(idValueString, mapValue + 1);
						} else {
							this.degreeMap.put(idValueString, 1);
						}
					}
				}
			}
		}
		
		// count the relations of the current entity and add the value to the map
		if(!exists) {
			this.degreeMap.put(idString, statementCount);
		} else {// if the value exists, increase it by the out-degree count
			int mapValue = this.degreeMap.get(idString);
			this.degreeMap.put(idString, mapValue + statementCount);
		}
		
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

			out.writeObject(degreeMap);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
