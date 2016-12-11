import java.io.IOException;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.mahout.common.Pair;
import org.apache.mahout.vectorizer.DictionaryVectorizer;
import org.apache.mahout.vectorizer.DocumentProcessor;
import org.apache.mahout.vectorizer.common.PartialVectorMerger;
import org.apache.mahout.vectorizer.tfidf.TFIDFConverter;

public class MahoutExample {

	public static void main(String[] args) throws ClassNotFoundException, IOException,
			InterruptedException {

		Configuration configuration = new Configuration();

		String tokenizedDocumentsPath = "/sarnobat.garagebandbroken/trash/tfidf/out";
		String outputFolder = "/sarnobat.garagebandbroken/trash/tfidf/out";

		Path documentsSequencePath = new Path(outputFolder, "sequence");

		Path input = new Path(tokenizedDocumentsPath,
				DocumentProcessor.TOKENIZED_DOCUMENT_OUTPUT_FOLDER);
		// Tokenize the documents using Apache Lucene StandardAnalyzer
		DocumentProcessor.tokenizeDocuments(documentsSequencePath, StandardAnalyzer.class, input,
				configuration);

		Path termFrequencyVectorsPath = new Path(outputFolder);
		DictionaryVectorizer.createTermFrequencyVectors(input, termFrequencyVectorsPath,
				DictionaryVectorizer.DOCUMENT_VECTOR_OUTPUT_FOLDER, configuration, 1, 1, 0.0f,
				PartialVectorMerger.NO_NORMALIZING, true, 1, 100, false, false);

		Path tfidfPath = new Path(outputFolder + "tfidf");

		Pair<Long[], List<Path>> documentFrequencies = TFIDFConverter.calculateDF(
				termFrequencyVectorsPath, tfidfPath, configuration, 100);

		TFIDFConverter.processTfIdf(termFrequencyVectorsPath, tfidfPath, configuration,
				documentFrequencies, 1, 100, PartialVectorMerger.NO_NORMALIZING, false, false,
				false, 1);
	}

}
