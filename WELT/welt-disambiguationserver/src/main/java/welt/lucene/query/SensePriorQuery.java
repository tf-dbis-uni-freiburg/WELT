package welt.lucene.query;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.Bits;

import welt.lucene.features.IEntityCentricExtFeatures;

/**
 * Due to major performance problems if we use an IndexReader request for every
 * single document, we create a <Concept, Occurence> Hashmap to improve the
 * overall performance.
 * 
 * Our StartupInformationLoader provides these necessary information much
 * faster.
 * 
 * @author Stefan Zwicklbauer
 * 
 */
public class SensePriorQuery extends Query {

	class PriorWeight extends Weight {

		class SensePriorScorer extends Scorer {

			private final AtomicReaderContext context;

			private int lastDoc = -1;

			SensePriorScorer(final Weight weight,
					final AtomicReaderContext context) {
				super(weight);
				this.context = context;
			}

			@Override
			public int advance(final int target) throws IOException {
				final int maxdoc = context.reader().numDocs();
				if (target > (maxdoc - 1)) {
					return NO_MORE_DOCS;
				}
				return lastDoc = target;
			}

			@Override
			public long cost() {
				return 0;
			}

			@Override
			public int docID() {
				return lastDoc;
			}

			@Override
			public int freq() throws IOException {
				return 1;
			}

			@Override
			public int nextDoc() throws IOException {
				if ((context.reader().numDocs() - 1) > lastDoc) {
					return ++lastDoc;
				} else {
					return NO_MORE_DOCS;
				}
			}

			@Override
			public float score() throws IOException {
				float res = 0.0f;
				res = kb.getSensePriorOfDocument(keyword, context.docBase
						+ lastDoc);
				return res;
			}

			@Override
			public String toString() {
				return "SensePrior";
			}

		}

		@Override
		public Explanation explain(final AtomicReaderContext context,
				final int doc) throws IOException {
			return null;
		}

		@Override
		public Query getQuery() {
			return SensePriorQuery.this;
		}

		@Override
		public float getValueForNormalization() throws IOException {
			return 0;
		}

		@Override
		public void normalize(final float norm, final float topLevelBoost) {
			// Do nothing here
		}

		@Override
		public Scorer scorer(AtomicReaderContext context, Bits acceptDocs)
				throws IOException {
			return new SensePriorScorer(this, context);
		}

	}

	private final IEntityCentricExtFeatures kb;

	private final String keyword;

	public SensePriorQuery(final String keyword, final IEntityCentricExtFeatures kb) {
		super();
		this.keyword = keyword;
		this.kb = kb;
	}

	@Override
	public Weight createWeight(final IndexSearcher searcher) throws IOException {
		return new PriorWeight();
	}

	@Override
	public String toString(final String field) {
		return "SensePriorQuery";
	}
}
