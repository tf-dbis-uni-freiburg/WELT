package com.djovanov.analysis;


import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentProcessor;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.SiteLink;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;

/**
 * A simple class that processes EntityDocuments to compute basic
 * statistics that are printed to the standard output. Moreover, it stores
 * further statistics in several files:
 * Take a look @writeStatisticsToFile() method for further information.
 *
 * @author Dimitar Jovanov
 *
 */
class ItemAnalysis implements EntityDocumentProcessor {

	class UsageStatistics {
		long count = 0;
		
		long enWikiSiteLinksCount = 0;
		long enWikiGeneralSiteLinksCount = 0;
		long allLanguageWikiGeneralSiteLinksCount = 0;
		long notInWikiItemsCount = 0;
		long itemsWithoutLabelsAndDescriptionsCount = 0;
		long itemsWithoutLabelsCount = 0;
		long itemsWithoutDescriptionsCount = 0;
		long itemsWithLabelsAndDescriptionsCount = 0;
		
		final HashSet<Long> enWikiSiteLinksSet = new HashSet<>();
		final HashSet<Long> enWikiGeneralSiteLinksSet = new HashSet<>();
		final HashSet<Long> allLanguageWikiGeneralSiteLinksSet = new HashSet<>();
		final HashSet<Long> notInWikiItemsSet = new HashSet<>();
		
		// INFO: modified for English items
		final HashSet<Long> itemsWithoutLabelsAndDescriptionsSet = new HashSet<>();
		final HashSet<Long> itemsWithoutLabelsSet = new HashSet<>();
		final HashSet<Long> itemsWithoutDescriptionsSet = new HashSet<>();
		final HashSet<Long> itemsWithLabelsAndDescriptionsSet = new HashSet<>();
		

		final HashMap<String, ClassRecord> classRecordsInWiki = new HashMap<>();
		final HashMap<String, ClassRecord> classRecordsNotInWiki = new HashMap<>();

	}
	
	public static final boolean ASC = true;
    public static final boolean DESC = false;
	public static final boolean IN_WIKI = true;
    public static final boolean NOT_IN_WIKI = false;
	
	private class ClassRecord {

		public String itemId = null;
		public Integer itemCount = 0;
		public Integer subclassCount = 0;
		
		public ClassRecord(String entityIdValue) {
			this.itemId = entityIdValue;
			this.itemCount = 0;
			this.subclassCount = 0;
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
		ItemAnalysis.printDocumentation();

		ItemAnalysis entityStatisticsProcessor = new ItemAnalysis();
		Helpers.processEntitiesFromWikidataDump(entityStatisticsProcessor);
		entityStatisticsProcessor.writeFinalResults();
	}

	@Override
	public void processItemDocument(ItemDocument itemDocument) {
		// Count items:
		this.itemStatistics.count++;

		String idString = itemDocument.getEntityId().getId();
		Long id = Long.parseLong(idString.substring(1, idString.length()));
		boolean enWiki = false;
		boolean enWikiSub = false;
		boolean siteLinked = false;
		for (SiteLink siteLink : itemDocument.getSiteLinks().values()) {
			siteLinked = true;
			if(siteLink.getSiteKey().equals("enwiki")) {
				enWiki = true;
			}
			if(siteLink.getSiteKey().equals("enwikiversity") ||
					siteLink.getSiteKey().equals("enwikisource") ||
					siteLink.getSiteKey().equals("enwikinews") ||
					siteLink.getSiteKey().equals("enwikibooks") ||
					siteLink.getSiteKey().equals("enwikivoyage") ||
					siteLink.getSiteKey().equals("enwikiquote")) {
				enWikiSub = true;
			}
		}
		if(enWiki) {
			countEnWikiSiteLinks(this.itemStatistics, id, 1);
		} else if(enWikiSub) {
			countEnWikiGeneralSiteLinks(this.itemStatistics, id, 1);
		} else if(siteLinked) {
			countAllLanguageWikiGeneralSiteLinks(this.itemStatistics, id, 1);
		} else {
			countNotInWikiItem(this.itemStatistics, id, 1);
		}
		
		/*
		INFO: Used for any type of label processing. 
		if(itemDocument.getLabels().size() == 0 && 
				itemDocument.getDescriptions().size() == 0) {
			countItemsWithoutLabelsAndDescriptions(itemStatistics, id, 1);
		}else if(itemDocument.getLabels().size() == 0) {
			countItemsWithoutLabels(itemStatistics, id, 1);
		}else if(itemDocument.getDescriptions().size() == 0) {
			countItemsWithoutDescriptions(itemStatistics, id, 1);
		}
		*/
		
		MonolingualTextValue englishLabel = itemDocument.getLabels().get("en");
		MonolingualTextValue englishDescription = itemDocument.getDescriptions().get("en");
			
		if(englishLabel == null && englishDescription == null) {
			countItemsWithoutLabelsAndDescriptions(itemStatistics, id, 1);
		}else if(englishLabel == null) {
			countItemsWithoutLabels(itemStatistics, id, 1);
		}else if(englishDescription == null) {
			countItemsWithoutDescriptions(itemStatistics, id, 1);
		}else {
			countItemsWithLabelsAndDescriptions(itemStatistics, id, 1);
		}
		
		for (StatementGroup sg : itemDocument.getStatementGroups()) {
			
			if(!itemStatistics.classRecordsInWiki.containsKey(idString)) {
				boolean isInstanceOf = "P31".equals(sg.getProperty().getId());
				boolean isSubclassOf = "P279".equals(sg.getProperty().getId());
				
				for (Statement s : sg.getStatements()) {
					// Process value of instance of/subclass of:
					if ((isInstanceOf || isSubclassOf)
							&& s.getClaim().getMainSnak() instanceof ValueSnak) {
						Value value = ((ValueSnak) s.getClaim().getMainSnak())
								.getValue();
						if (value instanceof EntityIdValue) {
							ClassRecord otherClassRecord = null;
							if(enWiki || enWikiSub || siteLinked) {
								otherClassRecord = 
										getClassRecord(((EntityIdValue) value).getIri(),
												itemStatistics, IN_WIKI);
							}else {
								otherClassRecord = 
										getClassRecord(((EntityIdValue) value).getIri(),
												itemStatistics, NOT_IN_WIKI);
							}
							if (isInstanceOf) {
								otherClassRecord.itemCount++;
							} else {
								otherClassRecord.subclassCount++;
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
	
	protected void countEnWikiSiteLinks(UsageStatistics usageStatistics, Long itemId, int count){
		if(!usageStatistics.enWikiSiteLinksSet.contains(itemId)) {
			usageStatistics.enWikiSiteLinksSet.add(itemId);
			usageStatistics.enWikiSiteLinksCount += count;
		}
	}
	
	protected void countEnWikiGeneralSiteLinks(UsageStatistics usageStatistics, Long itemId, int count){
		if(!usageStatistics.enWikiGeneralSiteLinksSet.contains(itemId)) {
			usageStatistics.enWikiGeneralSiteLinksSet.add(itemId);
			usageStatistics.enWikiGeneralSiteLinksCount += count;
		}
	}
	
	protected void countAllLanguageWikiGeneralSiteLinks(UsageStatistics usageStatistics, Long itemId, int count){
		if(!usageStatistics.allLanguageWikiGeneralSiteLinksSet.contains(itemId)) {
			usageStatistics.allLanguageWikiGeneralSiteLinksSet.add(itemId);
			usageStatistics.allLanguageWikiGeneralSiteLinksCount += count;
		}
	}
	
	protected void countNotInWikiItem(UsageStatistics usageStatistics, Long itemId, int count){
		if(!usageStatistics.notInWikiItemsSet.contains(itemId)) {
			usageStatistics.notInWikiItemsSet.add(itemId);
			usageStatistics.notInWikiItemsCount += count;
		}
	}
	
	protected void countItemsWithoutLabelsAndDescriptions(UsageStatistics usageStatistics, Long itemId, int count){
		if(!usageStatistics.itemsWithoutLabelsAndDescriptionsSet.contains(itemId)) {
			usageStatistics.itemsWithoutLabelsAndDescriptionsSet.add(itemId);
			usageStatistics.itemsWithoutLabelsAndDescriptionsCount += count;
		}
	}
	
	protected void countItemsWithoutLabels(UsageStatistics usageStatistics, Long itemId, int count){
		if(!usageStatistics.itemsWithoutLabelsSet.contains(itemId)) {
			usageStatistics.itemsWithoutLabelsSet.add(itemId);
			usageStatistics.itemsWithoutLabelsCount += count;
		}
	}
	
	protected void countItemsWithoutDescriptions(UsageStatistics usageStatistics, Long itemId, int count){
		if(!usageStatistics.itemsWithoutDescriptionsSet.contains(itemId)) {
			usageStatistics.itemsWithoutDescriptionsSet.add(itemId);
			usageStatistics.itemsWithoutDescriptionsCount += count;
		}
	}
	
	protected void countItemsWithLabelsAndDescriptions(UsageStatistics usageStatistics, Long itemId, int count){
		if(!usageStatistics.itemsWithLabelsAndDescriptionsSet.contains(itemId)) {
			usageStatistics.itemsWithLabelsAndDescriptionsSet.add(itemId);
			usageStatistics.itemsWithLabelsAndDescriptionsCount += count;
		}
	}
	
	private ClassRecord getClassRecord(String entityIdValue, UsageStatistics itemStatistics,
				boolean inWiki) {
		if(inWiki) {
			if (!itemStatistics.classRecordsInWiki.containsKey(entityIdValue)) {
				ClassRecord classRecord = new ClassRecord(entityIdValue);
				itemStatistics.classRecordsInWiki.put(entityIdValue, classRecord);
				return classRecord;
			} else {
				return itemStatistics.classRecordsInWiki.get(entityIdValue);
			}
		}else {
			if (!itemStatistics.classRecordsNotInWiki.containsKey(entityIdValue)) {
				ClassRecord classRecord = new ClassRecord(entityIdValue);
				itemStatistics.classRecordsNotInWiki.put(entityIdValue, classRecord);
				return classRecord;
			} else {
				return itemStatistics.classRecordsNotInWiki.get(entityIdValue);
			}
		}
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

		writeStatisticsToFile(this.itemStatistics);
	}
	
	private void writeStatisticsToFile(UsageStatistics itemStatistics) {
		
		String fileName = "analysis-report.csv";
		try (PrintStream out = new PrintStream(
				Helpers.openExampleFileOuputStream(fileName))) {

			out.println("Key,Value");
			out.println("Number of items processed:" + "," + itemStatistics.count);
			out.println("Number of enwiki items with site links:" + "," + itemStatistics.enWikiSiteLinksCount);
			out.println("Number of enwiki(general) with site links:" + ","  + itemStatistics.enWikiGeneralSiteLinksCount);
			out.println("Number of items containing site links:" + ","  + itemStatistics.allLanguageWikiGeneralSiteLinksCount);
			out.println("Number of items without links:" + ","  + itemStatistics.notInWikiItemsCount);
			out.println("Number of EN items without label and description:" + ","  + itemStatistics.itemsWithoutLabelsAndDescriptionsCount);
			out.println("Number of EN items only without label:" + ","  + itemStatistics.itemsWithoutLabelsCount);
			out.println("Number of EN items only without description:" + ","  + itemStatistics.itemsWithoutDescriptionsCount);
			out.println("Number of EN items with label and description:" + ","  + itemStatistics.itemsWithoutDescriptionsCount);
			out.println("Number of InWiki classes found:" + ","  + itemStatistics.classRecordsInWiki.size());
			out.println("Number of NotInWiki classes found:" + ","  + itemStatistics.classRecordsNotInWiki.size());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String fileName1 = "list-of-enwiki-ids.csv";
		try (PrintStream out = new PrintStream(
				Helpers.openExampleFileOuputStream(fileName1))) {
			out.println("En Wiki Ids\n");
			out.println(StringUtils.join(itemStatistics.enWikiSiteLinksSet, ","));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String fileName2 = "list-of-enwiki-general-ids.csv";
		try (PrintStream out = new PrintStream(
				Helpers.openExampleFileOuputStream(fileName2))) {
			out.println(StringUtils.join(itemStatistics.enWikiGeneralSiteLinksSet, ","));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String fileName3 = "list-of-wiki-general-ids.csv";
		try (PrintStream out = new PrintStream(
				Helpers.openExampleFileOuputStream(fileName3))) {
			out.println(StringUtils.join(itemStatistics.allLanguageWikiGeneralSiteLinksSet, ","));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String fileName4 = "list-of-en-items-without-label-and-description.csv";
		try (PrintStream out = new PrintStream(
				Helpers.openExampleFileOuputStream(fileName4))) {
			out.println(StringUtils.join(itemStatistics.itemsWithoutLabelsAndDescriptionsSet, ","));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String fileName5 = "list-of-en-items-without-label.csv";
		try (PrintStream out = new PrintStream(
				Helpers.openExampleFileOuputStream(fileName5))) {
			out.println(StringUtils.join(itemStatistics.itemsWithoutLabelsSet, ","));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String fileName6 = "list-of-en-items-without-description.csv";
		try (PrintStream out = new PrintStream(
				Helpers.openExampleFileOuputStream(fileName6))) {
			out.println(StringUtils.join(itemStatistics.itemsWithoutDescriptionsSet, ","));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String fileName7 = "list-of-not-in-wiki-ids.csv";
		try (PrintStream out = new PrintStream(
				Helpers.openExampleFileOuputStream(fileName7))) {
			out.println(StringUtils.join(itemStatistics.notInWikiItemsSet, ","));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// sorting of the hash maps for classes
		Map<String, ClassRecord> inWikiMap = sortByComparator(itemStatistics.classRecordsInWiki, DESC);
		Map<String, ClassRecord> notInWikiMap = sortByComparator(itemStatistics.classRecordsNotInWiki, DESC);
		
		String fileName8 = "list-of-in-wiki-class-usage.csv";
		try (PrintStream out = new PrintStream(
				Helpers.openExampleFileOuputStream(fileName8))) {
			out.println("EntityID,instanceOf(in links),subclassOf(in links)");
			for (Entry<String, ClassRecord> entry : inWikiMap.entrySet())
	        {
				out.println(entry.getKey() + "," + entry.getValue().itemCount + "," 
	            		+ entry.getValue().subclassCount);
	        }
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String fileName9 = "list-of-in-wiki-class-usage-top-50.csv";
		try (PrintStream out = new PrintStream(
				Helpers.openExampleFileOuputStream(fileName9))) {
			out.println("EntityID,instanceOf(in links),subclassOf(in links)");
			int i = 0;
			for (Entry<String, ClassRecord> entry : inWikiMap.entrySet())
	        {
				if(i == 50) {
					break;
				}
				out.println(entry.getKey() + "," + entry.getValue().itemCount + "," 
	            		+ entry.getValue().subclassCount);
				i++;
	        }
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String fileName10 = "list-of-not-in-wiki-class-usage.csv";
		try (PrintStream out = new PrintStream(
				Helpers.openExampleFileOuputStream(fileName10))) {
			out.println("EntityID,instanceOf(in links),subclassOf(in links)");
			for (Entry<String, ClassRecord> entry : notInWikiMap.entrySet())
	        {
				out.println(entry.getKey() + "," + entry.getValue().itemCount + "," 
	            		+ entry.getValue().subclassCount);
	        }
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String fileName11 = "list-of-not-in-wiki-class-usage-top-50.csv";
		try (PrintStream out = new PrintStream(
				Helpers.openExampleFileOuputStream(fileName11))) {
			out.println("EntityID,instanceOf(in links),subclassOf(in links)");
			int i = 0;
			for (Entry<String, ClassRecord> entry : notInWikiMap.entrySet())
	        {
				if(i == 50) {
					break;
				}
	            out.println(entry.getKey() + "," + entry.getValue().itemCount + "," 
	            		+ entry.getValue().subclassCount);
				i++;
	        }
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String fileName12 = "list-of-en-items-with-label-and-description.csv";
		try (PrintStream out = new PrintStream(
				Helpers.openExampleFileOuputStream(fileName12))) {
			out.println(StringUtils.join(itemStatistics.itemsWithoutLabelsAndDescriptionsSet, ","));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void printStatus() {
		System.out.println("---");
		printStatisticsItems(this.itemStatistics, "items");
	}

	private void printStatisticsItems(UsageStatistics itemStatistics,
			String entityLabel) {
		System.out.println("Processed " + itemStatistics.count + " "
				+ entityLabel + ":");
		System.out.println("With any type of site links: " + itemStatistics.allLanguageWikiGeneralSiteLinksCount
				+ ", with general enwiki site links: " + itemStatistics.enWikiGeneralSiteLinksCount
				+ ", with enwiki site links: " + itemStatistics.enWikiSiteLinksCount
				+ ", without site links: " + itemStatistics.notInWikiItemsCount);
		System.out.println("Without EN a label and descriptions: " + itemStatistics.itemsWithoutLabelsAndDescriptionsCount
				+ ", without EN description: " + itemStatistics.itemsWithoutDescriptionsCount
				+ ", without EN label: " + itemStatistics.itemsWithoutLabelsCount
				+ ", with EN label and description: " + itemStatistics.itemsWithLabelsAndDescriptionsCount);
		System.out.println("InWiki classes found:" + itemStatistics.classRecordsInWiki.size());
		System.out.println("NotInWiki classes found:" + itemStatistics.classRecordsNotInWiki.size());
	}
	
	private static Map<String, ClassRecord> sortByComparator(Map<String, ClassRecord> unsortMap, final boolean order)
    {

        List<Entry<String, ClassRecord>> list = new LinkedList<Entry<String, ClassRecord>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Entry<String, ClassRecord>>()
        {

			@Override
			public int compare(Entry<String, ClassRecord> o1, Entry<String, ClassRecord> o2) {
				if (order)
                {
                    return o1.getValue().itemCount.compareTo(o2.getValue().itemCount);
                }
                else
                {
                    return o2.getValue().itemCount.compareTo(o1.getValue().itemCount);

                }
			}
        });

        // Maintaining insertion order with the help of LinkedList
        Map<String, ClassRecord> sortedMap = new LinkedHashMap<String, ClassRecord>();
        for (Entry<String, ClassRecord> entry : list)
        {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

}
