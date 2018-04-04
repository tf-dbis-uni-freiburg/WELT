package welt.tools;

import java.io.File;
import java.util.Iterator;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import welt.entitydisambiguation.properties.Properties;
import welt.lucene.query.TermQuery;

public class LuceneTest {

	public static void main(String [] args) throws Exception{
		IndexReader reader = IndexReader.open(FSDirectory.open(new File("F:\\Dimitar\\School Materials\\UoF\\master_thesis\\prior_probabilities_and_index_generation\\results\\wikidatawiki-20170717\\lucene")));
		// IndexReader reader = IndexReader.open(FSDirectory.open(new File(Properties.getInstance().getEntityCentricKBWikidata())));
		// IndexReader reader = IndexReader.open(FSDirectory.open(new File("/home/stefan/Arbeitsfläche/mnt/ssd1/disambiguation/LuceneIndex/Wikipedia_Default_AidaNew/")));
		
		IndexSearcher searcher = new IndexSearcher(reader);
		//TermQuery q = new TermQuery(new Term("Mainlink", "http://www.wikidata.org/entity/Q187126"));
		//TermQuery q = new TermQuery(new Term("Mainlink", "http://www.wikidata.org/entity/Q15136093"));
		TermQuery q = new TermQuery(new Term("UniqueLabel", "basel"));
		TopDocs docs = searcher.search(q, 10);
		ScoreDoc[] doc = docs.scoreDocs;
		for (int i = 0; i < doc.length; i++) {
			Document document = reader.document(doc[i].doc);
			Iterator<IndexableField> iterator = document.getFields().iterator();
			System.out.println("------ Item " + i + " --------------");
			while(iterator.hasNext()) {
				IndexableField field = iterator.next();
				System.out.println(field.name() + ": " + field.stringValue());
			}
		}
		

		// STATISTICS
		/*System.out.println("Processing entities for statistics...");
		long uniqueLabelCount = 0;
		for (int i = 0; i < reader.numDocs(); i++) {
			Document document = reader.document(i);
			Iterator<IndexableField> iterator = document.getFields().iterator();
			if(i%100000 == 0) {
				System.out.println("Entities processed:" + i);
			}
			while(iterator.hasNext()) {
				IndexableField field = iterator.next();
				if(field.name().equals("UniqueLabel")) {
					uniqueLabelCount++;
				}
			}
		}
		System.out.println("Number of entities:" + reader.numDocs());
		System.out.println("Number of surface forms:" + uniqueLabelCount);*/
	}
	
}
