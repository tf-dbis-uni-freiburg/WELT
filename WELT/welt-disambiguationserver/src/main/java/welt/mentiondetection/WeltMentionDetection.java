package welt.mentiondetection;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import welt.entitydisambiguation.knowledgebases.AbstractKnowledgeBase;
import welt.entitydisambiguation.knowledgebases.EntityCentricKBWikidata;
import welt.lucene.query.TermQuery;
import welt.models.Mention;

/**
 * 
 * @author Dimitar
 * 
 * The WELT mention detection class that uses the Stanford NLP packages to parse the sentence
 * and produce N-grams. The N-grams are from 1-5 and are then used to query the Surface Form Index
 * (Lucene Index) and cross-check the Wikidata entities.
 *
 */
public class WeltMentionDetection implements MentionDetection {
	private String text;
	private ArrayList<Mention> mentions;
	
	private EntityCentricKBWikidata knowledgeBase;
	
	private int[] oneGrams;
	private int[][] twoGrams;
	private int[][] threeGrams;
	private int[][] fourGrams;
	private int[][] fiveGrams;
	
	private String[] oneGramsHits;
	private String[] twoGramsHits;
	private String[] threeGramsHits;
	private String[] fourGramsHits;
	private String[] fiveGramsHits;
	
	private int availableGrams = 0;
	
	public static String[] stopwords = {";", ":", ",", ".", "!", "?", "\\", "a", "as", "able", "about", "above", "according", "accordingly", "across", "actually", "after", "afterwards", "again", "against", "aint", "all", "allow", "allows", "almost", "alone", "along", "already", "also", "although", "always", "am", "among", "amongst", "an", "and", "another", "any", "anybody", "anyhow", "anyone", "anything", "anyway", "anyways", "anywhere", "apart", "appear", "appreciate", "appropriate", "are", "arent", "around", "as", "aside", "ask", "asking", "associated", "at", "available", "away", "awfully", "be", "became", "because", "become", "becomes", "becoming", "been", "before", "beforehand", "behind", "being", "believe", "below", "beside", "besides", "best", "better", "between", "beyond", "both", "brief", "but", "by", "cmon", "cs", "came", "can", "cant", "cannot", "cant", "cause", "causes", "certain", "certainly", "changes", "clearly", "co", "com", "come", "comes", "concerning", "consequently", "consider", "considering", "contain", "containing", "contains", "corresponding", "could", "couldnt", "course", "currently", "definitely", "described", "despite", "did", "didnt", "different", "do", "does", "doesnt", "doing", "dont", "done", "down", "downwards", "during", "each", "edu", "eg", "eight", "either", "else", "elsewhere", "enough", "entirely", "especially", "et", "etc", "even", "ever", "every", "everybody", "everyone", "everything", "everywhere", "ex", "exactly", "example", "except", "far", "few", "ff", "fifth", "first", "five", "followed", "following", "follows", "for", "former", "formerly", "forth", "four", "from", "further", "furthermore", "get", "gets", "getting", "given", "gives", "go", "goes", "going", "gone", "got", "gotten", "greetings", "had", "hadnt", "happens", "hardly", "has", "hasnt", "have", "havent", "having", "he", "hes", "hello", "help", "hence", "her", "here", "heres", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "hi", "him", "himself", "his", "hither", "hopefully", "how", "howbeit", "however", "i", "id", "ill", "im", "ive", "ie", "if", "ignored", "immediate", "in", "inasmuch", "inc", "indeed", "indicate", "indicated", "indicates", "inner", "insofar", "instead", "into", "inward", "is", "is a", "isnt", "it", "itd", "itll", "its", "its", "itself", "just", "keep", "keeps", "kept", "know", "knows", "known", "last", "lately", "later", "latter", "latterly", "least", "less", "lest", "let", "lets", "like", "liked", "likely", "little", "look", "looking", "looks", "ltd", "mainly", "many", "may", "maybe", "me", "mean", "meanwhile", "merely", "might", "more", "moreover", "most", "mostly", "much", "must", "my", "myself", "name", "namely", "nd", "near", "nearly", "necessary", "need", "needs", "neither", "never", "nevertheless", "new", "next", "nine", "no", "nobody", "non", "none", "noone", "nor", "normally", "not", "nothing", "novel", "now", "nowhere", "obviously", "of", "off", "often", "oh", "ok", "okay", "old", "on", "once", "one", "ones", "only", "onto", "or", "other", "others", "otherwise", "ought", "our", "ours", "ourselves", "out", "outside", "over", "overall", "own", "particular", "particularly", "per", "perhaps", "placed", "please", "plus", "possible", "presumably", "probably", "provides", "que", "quite", "qv", "rather", "rd", "re", "really", "reasonably", "regarding", "regardless", "regards", "relatively", "respectively", "right", "said", "same", "saw", "say", "saying", "says", "second", "secondly", "see", "seeing", "seem", "seemed", "seeming", "seems", "seen", "self", "selves", "sensible", "sent", "serious", "seriously", "seven", "several", "shall", "she", "should", "shouldnt", "since", "six", "so", "some", "somebody", "somehow", "someone", "something", "sometime", "sometimes", "somewhat", "somewhere", "soon", "sorry", "specified", "specify", "specifying", "still", "sub", "such", "sup", "sure", "ts", "take", "taken", "tell", "tends", "th", "than", "thank", "thanks", "thanx", "that", "thats", "thats", "the", "their", "theirs", "them", "themselves", "then", "thence", "there", "theres", "thereafter", "thereby", "therefore", "therein", "theres", "thereupon", "these", "they", "theyd", "theyll", "theyre", "theyve", "think", "third", "this", "thorough", "thoroughly", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together", "too", "took", "toward", "towards", "tried", "tries", "truly", "try", "trying", "twice", "two", "un", "under", "unfortunately", "unless", "unlikely", "until", "unto", "up", "upon", "us", "use", "used", "useful", "uses", "using", "usually", "value", "various", "very", "via", "viz", "vs", "want", "wants", "was", "wasnt", "way", "we", "wed", "well", "were", "weve", "welcome", "well", "went", "were", "werent", "what", "whats", "whatever", "when", "whence", "whenever", "where", "wheres", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whos", "whoever", "whole", "whom", "whose", "why", "will", "willing", "wish", "with", "within", "without", "wont", "wonder", "would", "would", "wouldnt", "yes", "yet", "you", "youd", "youll", "youre", "youve", "your", "yours", "yourself", "yourselves", "zero"};
	
	public WeltMentionDetection(String text, AbstractKnowledgeBase abstractKnowledgeBase) {
		try {
			this.text = URLDecoder.decode(text, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			this.text = "";
		}
		this.mentions = new ArrayList<>();
		this.knowledgeBase = (EntityCentricKBWikidata) abstractKnowledgeBase;
	}
	
	@Override
	public String detectMentions() {
		
		//text manipulation
		buildMentionList();
		
		//ngrams
		buildNGrams();
		queryIndex();
		
		
		extractSurfaceForms();
		

		//xml building
		StringBuilder xmlBuilder = new StringBuilder("<annotation text=\""+StringEscapeUtils.escapeXml(text)+"\">");
		for(Mention mention : this.mentions) {
			xmlBuilder.append("<surfaceForm name=\""+StringEscapeUtils.escapeXml(mention.getMentionText())+"\" offset=\""+mention.getOffset()+"\"/>");
		}
		xmlBuilder.append("</annotation>");
		
		return xmlBuilder.toString();
	}

	private void extractSurfaceForms() {
		ArrayList<Mention> temp = new ArrayList<>();
		if(fiveGramsHits != null) {
			for(int i = 0; i < fiveGramsHits.length; i++) {
				if(fiveGramsHits[i] != null && !isStopWord(fiveGramsHits[i].toLowerCase())) {
					temp.add(new Mention(fiveGramsHits[i], this.mentions.get(i).getOffset()));
				}
			}
		}
		if(fourGramsHits != null) {
			for(int i = 0; i < fourGramsHits.length; i++) {
				if(fourGramsHits[i] != null && !isStopWord(fourGramsHits[i].toLowerCase())) {
					temp.add(new Mention(fourGramsHits[i], this.mentions.get(i).getOffset()));
				}
			}
		}
		if(threeGramsHits != null) {
			for(int i = 0; i < threeGramsHits.length; i++) {
				if(threeGramsHits[i] != null && !isStopWord(threeGramsHits[i].toLowerCase())) {
					temp.add(new Mention(threeGramsHits[i], this.mentions.get(i).getOffset()));
				}
			}
		}
		if(twoGramsHits != null) {
			for(int i = 0; i < twoGramsHits.length; i++) {
				if(twoGramsHits[i] != null && !isStopWord(twoGramsHits[i].toLowerCase())) {
					temp.add(new Mention(twoGramsHits[i], this.mentions.get(i).getOffset()));
				}
			}
		}
		for(int i = 0; i < oneGramsHits.length; i++) {
			if(oneGramsHits[i] != null && !isStopWord(oneGramsHits[i].toLowerCase())) {
				temp.add(new Mention(oneGramsHits[i], this.mentions.get(i).getOffset()));
			}
		}
		Collections.sort(temp);
		this.mentions = temp;
	}

	private void buildNGrams() {
		int mentionSize = this.mentions.size();
		
		oneGrams = new int [mentionSize];
		oneGramsHits = new String [mentionSize];
		for(int i = 0; i < mentionSize; i++) {
			oneGrams[i] = i;
		}
		
		if(mentionSize > 1) {
			twoGrams = new int[mentionSize - 1] [2];
			twoGramsHits = new String [mentionSize - 1];
			for(int i = 0; i < mentionSize - 1; i++) {
				twoGrams[i][0] = i;
				twoGrams[i][1] = i + 1;
			}
			this.availableGrams = 2;
		}
		
		if(mentionSize > 2) {
			threeGrams = new int[mentionSize - 2] [3];
			threeGramsHits = new String [mentionSize - 2];
			for(int i = 0; i < mentionSize - 2; i++) {
				threeGrams[i][0] = i;
				threeGrams[i][1] = i + 1;
				threeGrams[i][2] = i + 2;
			}
			this.availableGrams = 3;
		}
		
		if(mentionSize > 3) {
			fourGrams = new int[mentionSize - 3] [4];
			fourGramsHits = new String [mentionSize - 3];
			for(int i = 0; i < mentionSize - 3; i++) {
				fourGrams[i][0] = i;
				fourGrams[i][1] = i + 1;
				fourGrams[i][2] = i + 2;
				fourGrams[i][3] = i + 3;
			}
			this.availableGrams = 4;
		}
		
		if(mentionSize > 4) {
			fiveGrams = new int[mentionSize - 4] [5];
			fiveGramsHits = new String [mentionSize - 4];
			for(int i = 0; i < mentionSize - 4; i++) {
				fiveGrams[i][0] = i;
				fiveGrams[i][1] = i + 1;
				fiveGrams[i][2] = i + 2;
				fiveGrams[i][3] = i + 3;
				fiveGrams[i][4] = i + 4;
			}
			this.availableGrams = 5;
		}
	}

	private void buildMentionList() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Annotation annotation = pipeline.process(this.text);
        for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
            	int offset = token.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
            	String mentionText = token.get(CoreAnnotations.TextAnnotation.class);
        		this.mentions.add(new Mention(mentionText, offset));
            }
        }
        
	}

	private boolean isStopWord(String word) {
		boolean isStopWord = false;
		for(String stopWord : stopwords) {
			if(word.equals(stopWord)) {
				isStopWord = true;
			}
		}
		return isStopWord;
	}
	
	private void queryIndex() {
		querySignleNGramList(this.oneGrams);
		if(availableGrams > 1) {
			querySignleNGramList(this.twoGrams);
		}
		if(availableGrams > 2) {
			querySignleNGramList(this.threeGrams);
		}
		if(availableGrams > 3) {
			querySignleNGramList(this.fourGrams);
		}
		if(availableGrams > 4) {
			querySignleNGramList(this.fiveGrams);
		}
	}
	
	private void querySignleNGramList(int[] ngram) {
		for(int gramIndex : ngram) {
			Mention mention = this.mentions.get(gramIndex);
			if(mention.getMentionText().length() > 1) {
				TermQuery query = new TermQuery(new Term("UniqueLabel", mention.getMentionText().toLowerCase()));
				final IndexSearcher searcher = this.knowledgeBase.getSearcher();
				TopDocs docs = null;
				try {
					docs = searcher.search(query, 1);
				} catch (IOException e) {
					
				}
				if(docs != null) {
					ScoreDoc[] doc = docs.scoreDocs;
					if (doc.length > 0) {
						oneGramsHits[gramIndex] = mention.getMentionText();
					}
				}
			}
		}
	}
	
	private void querySignleNGramList(int[][] ngram) {
		int ngramSize = ngram[0].length;
		for(int i = 0; i < ngram.length; i++) {
			StringBuilder fullMention = new StringBuilder();
			for(int j = 0; j < ngram[i].length; j++) {
				Mention current = this.mentions.get(ngram[i][j]);
				
				String mentionText = current.getMentionText();
				// if the ngram begins with the word "the", ignore it.
				if(j == 0 && mentionText.equals("the")) {
					break;
				}
				if(j > 0) {
					Mention previous = this.mentions.get(ngram[i][j - 1]);
					
					if(current.getOffset() == (previous.getOffset() + previous.getMentionText().length() + 1)) {
						fullMention.append(' ');
					}
				}
				fullMention.append(mentionText);
			}

			TermQuery query = new TermQuery(new Term("UniqueLabel", fullMention.toString().toLowerCase()));
			final IndexSearcher searcher = this.knowledgeBase.getSearcher();
			TopDocs docs = null;
			try {
				docs = searcher.search(query, 1);
			} catch (IOException e) {
				
			}
			if(docs != null) {
				ScoreDoc[] doc = docs.scoreDocs;
				if (doc.length > 0) {
					if(ngramSize == 2) {
						twoGramsHits[i] = fullMention.toString();
						//wipe previous grams
						oneGramsHits[i] = null;
						oneGramsHits[i + 1] = null;
					} else if(ngramSize == 3) {
						threeGramsHits[i] = fullMention.toString();
						//wipe previous grams
						twoGramsHits[i] = null;
						twoGramsHits[i + 1] = null;
						oneGramsHits[i] = null;
						oneGramsHits[i + 1] = null;
						oneGramsHits[i + 2] = null;
					} else if(ngramSize == 4) {
						fourGramsHits[i] = fullMention.toString();
						//wipe previous grams
						threeGramsHits[i] = null;
						threeGramsHits[i + 1] = null;
						twoGramsHits[i] = null;
						twoGramsHits[i + 1] = null;
						twoGramsHits[i + 2] = null;
						oneGramsHits[i] = null;
						oneGramsHits[i + 1] = null;
						oneGramsHits[i + 2] = null;
						oneGramsHits[i + 3] = null;
					} else if(ngramSize == 5) {
						fiveGramsHits[i] = fullMention.toString();
						//wipe previous grams
						fourGramsHits[i] = null;
						fourGramsHits[i + 1] = null;
						threeGramsHits[i] = null;
						threeGramsHits[i + 1] = null;
						threeGramsHits[i + 2] = null;
						twoGramsHits[i] = null;
						twoGramsHits[i + 1] = null;
						twoGramsHits[i + 2] = null;
						twoGramsHits[i + 3] = null;
						oneGramsHits[i] = null;
						oneGramsHits[i + 1] = null;
						oneGramsHits[i + 2] = null;
						oneGramsHits[i + 3] = null;
						oneGramsHits[i + 4] = null;
					}
				}
			}
		}
	}
	
	private void printNGrams() {
		System.out.println("One grams:");
		printSignleNGramList(this.oneGrams);
		if(availableGrams > 1) {
			System.out.println("Two grams:");
			printSignleNGramList(this.twoGrams);
		}
		if(availableGrams > 2) {
			System.out.println("Three grams:");
			printSignleNGramList(this.threeGrams);
		}
		if(availableGrams > 3) {
			System.out.println("Four grams:");
			printSignleNGramList(this.fourGrams);
		}
		if(availableGrams > 4) {
			System.out.println("Five grams:");
			printSignleNGramList(this.fiveGrams);
		}
	}
	
	private void printSignleNGramList(int[] ngram) {
		for(int gram : ngram) {
			System.out.println(this.mentions.get(gram).getMentionText());
		}
		System.out.println();
	}
	
	private void printSignleNGramList(int[][] ngram) {
		for(int i = 0; i < ngram.length; i++) {
			for(int j = 0; j < ngram[i].length; j++) {
				System.out.print(this.mentions.get(ngram[i][j]).getMentionText() + " ");
			}
			System.out.println();
		}
		System.out.println();
	}
}
