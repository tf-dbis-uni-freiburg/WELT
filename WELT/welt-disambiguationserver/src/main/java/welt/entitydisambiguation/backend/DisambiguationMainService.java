package welt.entitydisambiguation.backend;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.lucene.search.similarities.BM25Similarity;

import welt.entitydisambiguation.algorithms.AbstractDisambiguationAlgorithm;
import welt.entitydisambiguation.algorithms.DisambiguationHandler;
import welt.entitydisambiguation.knowledgebases.AbstractKnowledgeBase;
import welt.entitydisambiguation.knowledgebases.EntityCentricKBWikidata;
import welt.entitydisambiguation.knowledgebases.KnowledgeBaseIdentifiers;
import welt.entitydisambiguation.properties.Properties;

public final class DisambiguationMainService {

	public final static int MAXCLAUSECOUNT = 4096;

	private static final int TIMERPERIOD = 10000;

	private static DisambiguationMainService instance = null;

//	private Model hdtdbpediaCats;
//	private Model hdtdbpediaCats_ger;
//	private Model hdtdbpediaCatsL;
//	private Model hdtdbpediaCatsL_ger;
//	private Model hdtdbpediaDesc;
//	private Model hdtdbpediaLabels;
//	private Model hdtdbpediaLabels_ger;
//	private Model hdtdbpediaSkosCategories;
//	private Model hdtdbpediaInstanceTypes;
//	private Model hdtYagoCatsLab;
//	private Model hdtyagoTaxonomy;
//	private Model hdtyagoTransTypes;
//	private Model hdtdbpediaRedirects;

	public Map<KnowledgeBaseIdentifiers, AbstractKnowledgeBase> knowledgebases;

	private List<Timer> timerList;

	/**
	 * The DisambiguationMainService Constructor specifies a set of knowledge
	 * bases which are used for disambiguation. Dynamic knowledge bases will be
	 * initialized in a background thread loader. The static knowledge bases are
	 * initialized within the PriorLoader class. The Apache Lucene searchers and
	 * readers are created in the constructor of the EntityCentricDisambiguation
	 * class.
	 */
	private DisambiguationMainService() {
		super();
		this.knowledgebases = new EnumMap<KnowledgeBaseIdentifiers, AbstractKnowledgeBase>(
				KnowledgeBaseIdentifiers.class);
		this.knowledgebases.put(KnowledgeBaseIdentifiers.Standard,
				new EntityCentricKBWikidata(Properties.getInstance()
						.getEntityCentricKBWikidata(), false,
						new BM25Similarity()));
		/*this.knowledgebases.put(KnowledgeBaseIdentifiers.Biomed,
				new EntityCentricKBBiomed(Properties.getInstance()
						.getEntityCentricKBBiomed(), false,
						new DefaultSimilarity()));*/
		// this.knowledgebases.put(KnowledgeBaseIdentifiers.CSTable,
		// new EnCenKBCStable(Properties.getInstance().getCSTableIndex(),
		// false, new DefaultSimilarity()));
		// this.knowledgebases.put(KnowledgeBaseIdentifiers.DbPediaBiomedCopy,
		// new EntityCentricKnowledgeBaseDefault(Properties.getInstance()
		// .getDbPediaBiomedCopyKB(), true,
		// new DefaultSimilarity()));
		// this.knowledgebases.put(
		// KnowledgeBaseIdentifiers.DocumentCentricDefault,
		// new DocumentCentricKnowledgeBaseDefault(Properties
		// .getInstance().getDocumentCentricKB(), false,
		// new DefaultSimilarity()));

		// Create Timer thread, which periodically calls the IndexReader updates
		// for dynamic knowledge bases
		this.timerList = new ArrayList<Timer>();
		for (AbstractKnowledgeBase kb : this.knowledgebases.values()) {
			Timer timer = new Timer();
			this.timerList.add(timer);
			timer.scheduleAtFixedRate(kb, 0, TIMERPERIOD);
		}

		int threadSize = knowledgebases.size();
		if (threadSize > 0) {
			BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(
					threadSize);
			ThreadPoolExecutor ex = new ThreadPoolExecutor(threadSize,
					threadSize, 100, TimeUnit.SECONDS, queue);
			for (AbstractKnowledgeBase kb : knowledgebases.values()) {
				ex.execute(new KnowledgeBaseInitializationThread(kb));
			}
			ex.shutdown();
			try {
				while (!ex.awaitTermination(100, TimeUnit.SECONDS)) {
					Logger.getRootLogger().info(
							"InitializationPhase not completed yet! Still waiting "
									+ ex.getActiveCount());
				}
			} catch (InterruptedException e) {
				Logger.getRootLogger().warn(e.getStackTrace());
			}
		}

		// this.loadRelations();
//		try {
//			final HDT hdt = HDTManager.mapIndexedHDT(Properties.getInstance()
//					.getDBPediaArticleCategories(), null);
//			final HDT hdt3 = HDTManager.mapIndexedHDT(Properties.getInstance()
//					.getDBPediaCategoryLabels(), null);
//			final HDT hdt5 = HDTManager.mapIndexedHDT(Properties.getInstance()
//					.getDBPediaLabels(), null);
//			final HDT hdt6 = HDTManager.mapIndexedHDT(Properties.getInstance()
//					.getDBPediaDescriptions(), null);
//			final HDT hdt7 = HDTManager.mapIndexedHDT(Properties.getInstance()
//					.getDBpediaSkosCategories(), null);
//			final HDT hdt8 = HDTManager.mapIndexedHDT(Properties.getInstance()
//					.getDBpediaInstanceTypes(), null);
//			final HDT hdt9 = HDTManager.mapIndexedHDT(Properties.getInstance()
//					.getDBpediaRedirects(), null);
//			final HDT hdt10 = HDTManager.mapIndexedHDT(Properties.getInstance().getDBPediaLabels_GER(), null);
//			final HDT hdt11 = HDTManager.mapIndexedHDT(Properties.getInstance().getDBPediaArticleCategories_GER(), null);
//			final HDT hdt12 = HDTManager.mapIndexedHDT(Properties.getInstance().getDBPediaCategoryLabels_GER(), null);
//			final HDTGraph graph = new HDTGraph(hdt);
//			final HDTGraph graph3 = new HDTGraph(hdt3);
//			final HDTGraph graph5 = new HDTGraph(hdt5);
//			final HDTGraph graph6 = new HDTGraph(hdt6);
//			final HDTGraph graph7 = new HDTGraph(hdt7);
//			final HDTGraph graph8 = new HDTGraph(hdt8);
//			final HDTGraph graph9 = new HDTGraph(hdt9);
//			final HDTGraph graph10 = new HDTGraph(hdt10);
//			final HDTGraph graph11 = new HDTGraph(hdt11);
//			final HDTGraph graph12 = new HDTGraph(hdt12);
//			this.hdtdbpediaCats = ModelFactory.createModelForGraph(graph);
//			this.hdtdbpediaCatsL = ModelFactory.createModelForGraph(graph3);
//			this.hdtdbpediaLabels = ModelFactory.createModelForGraph(graph5);
//			this.hdtdbpediaDesc = ModelFactory.createModelForGraph(graph6);
//			this.hdtdbpediaSkosCategories = ModelFactory
//					.createModelForGraph(graph7);
//			this.hdtdbpediaInstanceTypes = ModelFactory
//					.createModelForGraph(graph8);
//			this.hdtdbpediaRedirects = ModelFactory
//					.createModelForGraph(graph9);
//			this.hdtdbpediaLabels_ger = ModelFactory.createModelForGraph(graph10);
//			this.hdtdbpediaCats_ger = ModelFactory.createModelForGraph(graph11);
//			this.hdtdbpediaCatsL_ger = ModelFactory.createModelForGraph(graph12);
//		} catch (final IOException e) {
//			e.printStackTrace();
//		}
	}

	public synchronized static DisambiguationMainService getInstance() {
		if (instance == null) {
			instance = new DisambiguationMainService();
		}
		return instance;
	}

	public static void initialize() {
		instance = new DisambiguationMainService();
	}

	public void disambiguate(List<AbstractDisambiguationTask> taskList) {
		for (int i = 0; i < taskList.size(); i++) {
			AbstractDisambiguationTask task = taskList.get(i);
			AbstractDisambiguationAlgorithm algorithm = DisambiguationHandler
					.getInstance().getAlgorithm(task);
			if (algorithm != null) {
				task.setKb(this.knowledgebases.get(task.getKbIdentifier()));
				algorithm.disambiguate(task);
			}
		}
	}

//	public Model getDBPediaArticleCategories() {
//		return this.hdtdbpediaCats;
//	}
//	
//	public Model getDBPediaArticleCategories_GER() {
//		return this.hdtdbpediaCats_ger;
//	}
//
//	public Model getDBPediaCategoryLabels() {
//		return this.hdtdbpediaCatsL;
//	}
//	
//	public Model getDBPediaCategoryLabels_GER() {
//		return this.hdtdbpediaCatsL_ger;
//	}
//
//	public Model getDBPediaDescription() {
//		return this.hdtdbpediaDesc;
//	}
//
//	public Model getDBPediaInstanceTypes() {
//		return this.hdtdbpediaInstanceTypes;
//	}
//
//	public Model getDBPediaLabels() {
//		return this.hdtdbpediaLabels;
//	}
//	
//	public Model getDBPediaLabels_GER() {
//		return this.hdtdbpediaLabels_ger;
//	}
//
//	public Model getDBpediaSkosCategories() {
//		return this.hdtdbpediaSkosCategories;
//	}
//
//	public Model getYagoCategoryLabels() {
//		return this.hdtYagoCatsLab;
//	}
//
//	public Model getYagoTaxonomy() {
//		return this.hdtyagoTaxonomy;
//	}
//
//	public Model getYagoTransitiveTypes() {
//		return this.hdtyagoTransTypes;
//	}
//	
//	public Model getDBpediaRedirects() {
//		return this.hdtdbpediaRedirects;
//	}


	/**
	 * A seperate thread class to initialize our knowledgebases
	 * 
	 * @author Stefan Zwicklbauer
	 */
	class KnowledgeBaseInitializationThread implements Runnable {

		private AbstractKnowledgeBase kb;

		public KnowledgeBaseInitializationThread(AbstractKnowledgeBase kb) {
			super();
			this.kb = kb;
		}

		@Override
		public void run() {
			kb.initialize();
		}
	}

	public void shutDownDisambiguationService() {
		for (Timer timer : timerList) {
			timer.cancel();
		}
	}
}
