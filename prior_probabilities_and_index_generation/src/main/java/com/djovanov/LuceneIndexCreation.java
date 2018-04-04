package com.djovanov;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.document.IntField;
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

import doser.lucene.analysis.DoserIDAnalyzer;
import doser.lucene.analysis.DoserStandardAnalyzer;


/**
 * This script should be used for the generation of a new index of surface forms,
 * unless you want to write your own version of the process. It requiers at least 10GB ram
 * and should be run on a server instead of a local device. It iterates through a dump from the
 * knowledge base and extracts the relevant fields storing them in the index on the path located
 * in the {@value INDEX} variable. The {@value LANGUAGE} variable is a string representation of
 * the language that should be extracted in the index of surface forms.
 * 
 * @author Dimitar
 *
 */
public class LuceneIndexCreation implements EntityDocumentProcessor {

	class UsageStatistics {
		long count = 0;
	}
	
	public static final boolean ASC = true;
    public static final boolean DESC = false;
	public static final boolean IN_WIKI = true;
    public static final boolean NOT_IN_WIKI = false;
    public static final String INDEX = "./results/wikidatawiki-20170717/lucene";
    public static final String LANGUAGE = "fr";
    
    File newIndexFile;
    private HashMap<String, Integer> priorMap;
    private HashMap<String, Integer> degreeMap;
    private IndexWriter indexWriter;
	
	UsageStatistics itemStatistics = new UsageStatistics();
	
	public static final String DISAMBIGUATION_PAGE_ID = "Q4167410";

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
		LuceneIndexCreation.printDocumentation();

		LuceneIndexCreation mainClass = new LuceneIndexCreation();
		
		//initialize the Lucene index
		mainClass.initializeIndex();
		mainClass.loadMaps();
		
		Helpers.processEntitiesFromWikidataDump(mainClass);
		mainClass.writeFinalResults();
		
		mainClass.closeIndex();
		

	}

	private void loadMaps() {
		this.priorMap = loadPriorMap();
		this.degreeMap = loadDegreeMap();
	}

	public void closeIndex() {
		try {
			indexWriter.close();
		} catch (IOException e) {
			System.out.println("Error closing index.");
			e.printStackTrace();
		}
		System.out.println("Index is closed.");
	}

	@Override
	public void processItemDocument(ItemDocument itemDocument) {
		// Count items:
		this.itemStatistics.count++;

		// Fetch wikidata values
		String idString = itemDocument.getEntityId().getId();
		String uriString = ((EntityIdValue) itemDocument.getEntityId()).getIri();
		MonolingualTextValue englishLabel = itemDocument.getLabels().get(LANGUAGE);
		MonolingualTextValue englishDescription = itemDocument.getDescriptions().get(LANGUAGE);
		List<MonolingualTextValue> aliasList = itemDocument.getAliases().get(LANGUAGE);
		
		if(englishLabel != null) {
			Document doc = new Document();
			StringBuilder typeValues = new StringBuilder();
			int typeSeparatorCounter = 0;
			StringBuilder subclassValues = new StringBuilder();
			int subclassSeparatorCounter = 0;
			StringBuilder relationValues = new StringBuilder();
			int relationCounter = 0;
			boolean disambiguationPageFlag = false;
			int notInWikipedia = 1;
			
			for (SiteLink siteLink : itemDocument.getSiteLinks().values()) {
				if(siteLink.getSiteKey().contains("wiki") || 
						siteLink.getSiteKey().equals("wikti")) {
					notInWikipedia = 0;
				}
			}
			
			for (StatementGroup sg : itemDocument.getStatementGroups()) {
				boolean isInstanceOf = "P31".equals(sg.getProperty().getId());
				// uncomment if needed for subclass of
				boolean isSubclassOf = "P279".equals(sg.getProperty().getId());
				
				for (Statement s : sg.getStatements()) {
					// Relations combining
					String property = sg.getProperty().getId().toString();
					
					if (s.getClaim().getMainSnak() instanceof ValueSnak) {
						Value value = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
						if(value instanceof EntityIdValue) {
							// if entity is a disambiguation page, skip
							String idValue = ((EntityIdValue) value).getId().toString();
							if(idValue.equals(DISAMBIGUATION_PAGE_ID)) {
								disambiguationPageFlag = true;
								break;
							}

							// Process value of instance of for Type
							if (isInstanceOf) {
								if(typeSeparatorCounter != 0) {
									typeValues.append(";;;");
								}
								typeValues.append(((EntityIdValue) value).getId().toString());
								typeSeparatorCounter++;
							}
							
							// Process value of subclass of for Type
							if (isSubclassOf) {
								if(subclassSeparatorCounter != 0) {
									subclassValues.append(";;;");
								}
								subclassValues.append(((EntityIdValue) value).getId().toString());
								subclassSeparatorCounter++;
							}
							
							// add entity relations to the Relations field in the index
							if(relationCounter != 0) {
								relationValues.append(";;;");
							}
							relationValues.append(property);
							relationValues.append(":::");
							relationValues.append(idValue);
							relationCounter++;
						}
					}
				}
			}
			
			// if not a disambiguation page, then add to index
			if(!disambiguationPageFlag) {
				// Add ID
				doc.add(new StringField("ID", idString, Store.YES));
				// Add Mainlink
				doc.add(new StringField("Mainlink", uriString, Store.YES));
				// Add labels
				doc.add(new TextField("Label", englishLabel.getText().toLowerCase(), Store.YES));
				// Add labels
				doc.add(new StringField("StringLabel", englishLabel.getText().toLowerCase(), Store.YES));
				if(englishDescription != null) {
					// Add Description
					doc.add(new TextField("ShortDescription", englishDescription.getText().toLowerCase(),
							Store.YES));
					// Add Description - same as previous description
					doc.add(new TextField("LongDescription", englishDescription.getText().toLowerCase(),
							Store.YES));
				}else {
					// Add Description
					doc.add(new TextField("ShortDescription", "",
							Store.YES));
					// Add Description - same as previous description
					doc.add(new TextField("LongDescription", "",
							Store.YES));
				}
				// Add type
				doc.add(new StringField("Type", typeValues.toString(),
						Store.YES));
				// Add relations (statements)
				doc.add(new TextField("Relations", relationValues.toString(),
						Store.YES));
				// Add unique labels (aliases)
				doc.add(new StringField("UniqueLabel", englishLabel.getText().toLowerCase(),
						Store.YES));
				// Build occurence count string based on links
				int priorProbability = 0;
				if(this.priorMap.containsKey(idString)) {
					priorProbability = this.priorMap.get(idString);
				}
				StringBuilder occurencesString = 
						new StringBuilder(englishLabel.getText().toLowerCase() + ":::" + priorProbability);
				if(aliasList != null) {
				Iterator<MonolingualTextValue> iterator = aliasList.iterator();
					while(iterator.hasNext()) {
						MonolingualTextValue alias = iterator.next();
						doc.add(new StringField("UniqueLabel", alias.getText().toString().toLowerCase(),
								Store.YES));
						occurencesString.append(";;;" + alias.getText().toString().toLowerCase() +  ":::" + priorProbability);
					}
				} 
				// Add occurence count based on links
				doc.add(new StringField("Occurrences", occurencesString.toString(),
						Store.YES));
				// Add patty relations if needed
				doc.add(new StringField("PattyRelations", "",
						Store.YES));
				// Add patty freebase relations if needed
				doc.add(new StringField("PattyFreebaseRelations", "",
						Store.YES));
				// Add DBpediaVertexDegree if needed
				int degree = 0;
				if(this.degreeMap.containsKey(idString)) {
					degree = this.degreeMap.get(idString);
				}
				doc.add(new IntField("WikidataVertexDegree", degree,
						Store.YES));
				doc.add(new IntField("NotInWikipedia", notInWikipedia,
						Store.YES));
				
				//println check!!!
				//printStore(doc.toString());
				
				try {
					indexWriter.addDocument(doc);
				} catch (IOException e) {
					System.out.println("Index write failed.");
					e.printStackTrace();
				}
			}
		}
		
		// Print a report every 100000 items:
		if (this.itemStatistics.count % 100000 == 0) {
			printStatus();
		}
	}

	private HashMap<String, Integer> loadPriorMap() {
		System.out.println("Loading prior probability hash map.");
		HashMap<String, Integer> map = null;
		try
		{
			FileInputStream fis = new FileInputStream("./results/prior-probability-hash-map.ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			map = (HashMap) ois.readObject();
			ois.close();
			fis.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
		}catch(ClassNotFoundException c){
			System.out.println("Class not found");
			c.printStackTrace();
		}
		return map;
	}
	
	private HashMap<String, Integer> loadDegreeMap() {
		System.out.println("Loading degree hash map.");
		HashMap<String, Integer> map = null;
		try
		{
			FileInputStream fis = new FileInputStream("./results/vertex-degree-hash-map.ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			map = (HashMap) ois.readObject();
			ois.close();
			fis.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
		}catch(ClassNotFoundException c){
			System.out.println("Class not found");
			c.printStackTrace();
		}
		return map;
	}

	@Override
	public void processPropertyDocument(PropertyDocument propertyDocument) {
		// Count properties:
	}

	private void initializeIndex() {
		System.out.println("Initializing index.");
		this.newIndexFile = new File(INDEX);

		Directory newDir;
		try {
			newDir = FSDirectory.open(newIndexFile);
	
			Map<String, Analyzer> analyzerPerField = new HashMap<String, Analyzer>();
			analyzerPerField.put("Label", new DoserStandardAnalyzer());
			/*analyzerPerField.put("PattyRelations", new DoserIDAnalyzer());
			analyzerPerField.put("PattyFreebaseRelations",
					new DoserIDAnalyzer());*/
			analyzerPerField.put("Relations", new DoserIDAnalyzer());
			analyzerPerField.put("Occurrences", new DoserIDAnalyzer());
	
			PerFieldAnalyzerWrapper aWrapper = new PerFieldAnalyzerWrapper(
					new StandardAnalyzer(), analyzerPerField);
	
			IndexWriterConfig config = new IndexWriterConfig(
					Version.LATEST, aWrapper);
			indexWriter = new IndexWriter(newDir, config);
			System.out.println("Index initialized.");
		} catch (IOException e) {
			System.out.println("Writer cannot be opened, directory for index or index file might be wrong.");
			e.printStackTrace();
		}

	}
	

	/**
	 * Prints an output of what the stored values would look like.
	 */
	private void printStore(String doc) {
		System.out.println(doc);
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

	}
	
	private void printStatus() {
		System.out.println("---");
		printStatisticsItems(this.itemStatistics, "items");
	}

	private void printStatisticsItems(UsageStatistics itemStatistics,
			String entityLabel) {
		System.out.println("Processed " + itemStatistics.count + " "
				+ entityLabel + ":");
	}
	
}
