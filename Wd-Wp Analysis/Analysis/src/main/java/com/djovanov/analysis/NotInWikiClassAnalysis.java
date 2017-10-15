package com.djovanov.analysis;

import java.io.FileInputStream;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentProcessor;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;

/**
 * A simple class that processes EntityDocuments to compute class level statistics
 * of not in Wikipedia Entities that are printed to the standard output. Moreover, it stores
 * further statistics in several files:
 * Take a look @writeStatisticsToFile() method for further information.
 *
 * @author Dimitar Jovanov
 *
 */
class NotInWikiClassAnalysis implements EntityDocumentProcessor {
	
	class UsageStatistics {
		long count = 0;
	}
	

	HashMap<Long, ItemRecord> itemRecordsNotInWiki;
	
	public static final boolean ASC = true;
    public static final boolean DESC = false;
	public static final boolean IN_WIKI = true;
    public static final boolean NOT_IN_WIKI = false;
    public static final String INPUT = "input/list-of-not-in-wiki-ids.csv";
    public static final int SCIENTIFIC_ARTICLE = 1;
    public static final int GENE = 2;
    public static final int PROTEIN = 3;
    public static final int HUMAN = 4;
    public static final int CHEMICAL_COMPOUND = 5;
	
	private class ItemRecord {

		public String itemId;
		public String enLabel;
		public Integer labelCount;
		public Integer descriptionCount;
		public Integer attributeCount;
		public Integer instanceOf;
		
		public ItemRecord(String entityIdValue) {
			this.itemId = entityIdValue;
			this.enLabel = null;
			this.labelCount = 0;
			this.descriptionCount = 0;
			this.attributeCount = 0;
			this.instanceOf = 0;
		}
		
		public ItemRecord() {
			this.itemId = null;
			this.enLabel = null;
			this.labelCount = 0;
			this.descriptionCount = 0;
			this.attributeCount = 0;
			this.instanceOf = 0;
		}
		
	}

	UsageStatistics itemStatistics = new UsageStatistics();

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
		NotInWikiClassAnalysis.printDocumentation();

		NotInWikiClassAnalysis entityStatisticsProcessor = new NotInWikiClassAnalysis();
		entityStatisticsProcessor.loadNotInWikiIdsFromFile();
		entityStatisticsProcessor.printFileLoaded();
		Helpers.processEntitiesFromWikidataDump(entityStatisticsProcessor);
		entityStatisticsProcessor.writeFinalResults();
	}

	@Override
	public void processItemDocument(ItemDocument itemDocument) {
		// Count items:
		this.itemStatistics.count++;

		String uriValue = ((EntityIdValue) itemDocument.getEntityId()).getIri();
		String idString = itemDocument.getEntityId().getId();
		Long id = Long.parseLong(idString.substring(1, idString.length()));
		
		if(this.itemRecordsNotInWiki.containsKey(id)) {
			ItemRecord itemRecord = this.itemRecordsNotInWiki.get(id);
			itemRecord.itemId = uriValue;
			String value = itemDocument.findLabel("en");
			if(value != null) {
				itemRecord.enLabel = value.toString();
			}
			itemRecord.labelCount = itemDocument.getLabels().size();
			itemRecord.descriptionCount = itemDocument.getDescriptions().size();
			itemRecord.attributeCount = itemDocument.getStatementGroups().size();
			for (StatementGroup sg : itemDocument.getStatementGroups()) {
				boolean isInstanceOf = "P31".equals(sg.getProperty().getId());
				for (Statement s : sg.getStatements()) {
					if (isInstanceOf
							&& s.getClaim().getMainSnak() instanceof ValueSnak) {
						Value snakValue = ((ValueSnak) s.getClaim().getMainSnak())
								.getValue();
						if (snakValue instanceof EntityIdValue) {
							String entityId = ((EntityIdValue) snakValue).getId();
							boolean isScientificArticle = "Q13442814".equals(entityId);
							boolean isGene = "Q7187".equals(entityId);
							boolean isProtein = "Q8054".equals(entityId);
							boolean isHuman = "Q5".equals(entityId);
							boolean isChemicalCompound = "Q11173".equals(entityId);
							
							if(isScientificArticle) {
								itemRecord.instanceOf = SCIENTIFIC_ARTICLE;
							}else if(isGene) {
								itemRecord.instanceOf = GENE;
							}else if(isProtein) {
								itemRecord.instanceOf = PROTEIN;
							}else if(isHuman) {
								itemRecord.instanceOf = HUMAN;
							}else if(isChemicalCompound) {
								itemRecord.instanceOf = CHEMICAL_COMPOUND;
							}
						}
					}
				}
			}
		}

		// Print a report every 100000 items:
		if (this.itemStatistics.count % 100000 == 0) {
			printStatus();
		}
	}

	@Override
	public void processPropertyDocument(PropertyDocument propertyDocument) {
		// Count properties:
	}

	/**
	 * Prints some basic documentation about this program.
	 */
	public static void printDocumentation() {
		System.out
				.println("********************************************************************");
		System.out.println("*** Wikidata Toolkit: EntityStatisticsProcessor");
		System.out.println("*** ");
		System.out
				.println("*** This program will download and process dumps from Wikidata.");
		System.out
				.println("*** It will print progress information and some simple statistics.");
		System.out
				.println("*** Results about property usage will be stored in a CSV file.");
		System.out.println("*** See source code for further details.");
		System.out
				.println("********************************************************************");
	}

	private void writeFinalResults() {
		// Print a final report:
		printStatus();

		writeStatisticsToFile();
	}
	
	private void writeStatisticsToFile() {

		Map<Long, ItemRecord> sortedItemRecordsNotInWiki = 
				sortByComparator(this.itemRecordsNotInWiki, DESC);
		
		
		String fileName = "list-of-not-in-wiki-items-with-info.csv";
		String fileName1 = "scientific-articles-of-not-in-wiki-with-info.csv";
		String fileName2 = "genes-of-not-in-wiki-with-info.csv";
		String fileName3 = "proteins-of-not-in-wiki-with-info.csv";
		String fileName4 = "humans-of-not-in-wiki-with-info.csv";
		String fileName5 = "chemical-compounds-of-not-in-wiki-with-info.csv";
		try {
			PrintStream out = new PrintStream(
					Helpers.openExampleFileOuputStream(fileName));
			PrintStream out1 = new PrintStream(
					Helpers.openExampleFileOuputStream(fileName1));
			PrintStream out2 = new PrintStream(
					Helpers.openExampleFileOuputStream(fileName2));
			PrintStream out3 = new PrintStream(
					Helpers.openExampleFileOuputStream(fileName3));
			PrintStream out4 = new PrintStream(
					Helpers.openExampleFileOuputStream(fileName4));
			PrintStream out5 = new PrintStream(
					Helpers.openExampleFileOuputStream(fileName5));
			
			out.println("Label(en),URI,labelCount,descriptionCount,attributeCount");
			for(ItemRecord itemRecord : sortedItemRecordsNotInWiki.values()) {
				out.println(itemRecord.enLabel + "," +
					itemRecord.itemId + "," +
					itemRecord.labelCount + "," +
					itemRecord.descriptionCount + "," +
					itemRecord.attributeCount);
				if(itemRecord.instanceOf == 1) {
					out1.println(itemRecord.enLabel + "," +
							itemRecord.itemId + "," +
							itemRecord.labelCount + "," +
							itemRecord.descriptionCount + "," +
							itemRecord.attributeCount);
				}else if(itemRecord.instanceOf == 2) {
					out2.println(itemRecord.enLabel + "," +
							itemRecord.itemId + "," +
							itemRecord.labelCount + "," +
							itemRecord.descriptionCount + "," +
							itemRecord.attributeCount);
				}else if(itemRecord.instanceOf == 3) {
					out3.println(itemRecord.enLabel + "," +
							itemRecord.itemId + "," +
							itemRecord.labelCount + "," +
							itemRecord.descriptionCount + "," +
							itemRecord.attributeCount);
				}else if(itemRecord.instanceOf == 4) {
					out4.println(itemRecord.enLabel + "," +
							itemRecord.itemId + "," +
							itemRecord.labelCount + "," +
							itemRecord.descriptionCount + "," +
							itemRecord.attributeCount);
				}else if(itemRecord.instanceOf == 5) {
					out5.println(itemRecord.enLabel + "," +
							itemRecord.itemId + "," +
							itemRecord.labelCount + "," +
							itemRecord.descriptionCount + "," +
							itemRecord.attributeCount);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private void printStatus() {
		System.out.println("---");
		printStatisticsItems(this.itemStatistics, "items");
	}
	
	private void printFileLoaded() {
		System.out.println("Input file with " + this.itemRecordsNotInWiki.size() + " items loaded.");
	}

	private void printStatisticsItems(UsageStatistics itemStatistics,
			String entityLabel) {
		System.out.println("Processed " + itemStatistics.count + " "
				+ entityLabel + ".");
	}
	
	private static Map<Long, ItemRecord> sortByComparator(Map<Long, ItemRecord> unsortMap, final boolean order)
    {

        List<Entry<Long, ItemRecord>> list = new LinkedList<Entry<Long, ItemRecord>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Entry<Long, ItemRecord>>()
        {

			@Override
			public int compare(Entry<Long, ItemRecord> o1, Entry<Long, ItemRecord> o2) {
				if (order)
                {
                    return o1.getValue().attributeCount.compareTo(o2.getValue().attributeCount);
                }
                else
                {
                    return o2.getValue().attributeCount.compareTo(o1.getValue().attributeCount);

                }
			}
        });

        // Maintaining insertion order with the help of LinkedList
        Map<Long, ItemRecord> sortedMap = new LinkedHashMap<>();
        for (Entry<Long, ItemRecord> entry : list)
        {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
	
	private void loadNotInWikiIdsFromFile() {
		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream(INPUT);
			String inputFileString = IOUtils.toString(inputStream);
			String [] idParts = inputFileString.split(",");
			itemRecordsNotInWiki = new HashMap<>();
			for(String part : idParts) {
				Long id = Long.parseLong(part.trim());
				itemRecordsNotInWiki.put(id, new ItemRecord());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(inputStream != null) {
		    try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
