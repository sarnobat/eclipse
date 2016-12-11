import java.io.IOException;
import java.util.HashMap;
import org.apache.mahout.text.SequenceFilesFromDirectory;
import org.apache.mahout.vectorizer.SparseVectorsFromSequenceFiles;
 
public class MahoutTutorial {
 
   
 
  private int textToSequenceFiles(String input,String output) throws Exception {
 
  /* Pass the arguments to the run method in the same way we invoke Mahout from the terminal. This may not be the best way in comparison to calling Mahout API but I struggled enough and failed to find good resources/tutorials that describe the standard workflow to use Mahout API.
All arguments are self explanatory, "--method sequential" asks Mahout not to run on Hadoop's MapReduce
 
*/
 
    String[] para = {"--input", input, "--output", output, "--overwrite", "--method", "sequential"};
    int x = new SequenceFilesFromDirectory().run(para);
    return x;
 
  }
 
  private int vectorize(String input,String output) throws Exception {
 
  /* 
      -wt tfidf --> Use the tfidf weighting method. 
      -ng 2 --> Use an n-gram size of 2 to generate both unigrams and bigrams. 
      -ml 50 --> Use a log-likelihood ratio (LLR) value of 50 to keep only very significant bigrams.   
  */
 
    String[] para = {"-o",output, "-i",input,"-wt","tfidf","-ow","-ml","50","-ng","2"};
    int x = new SparseVectorsFromSequenceFiles().run(para);
    return x;
  }
 
  private void readDictionaryAndFrequency(String path1, String path2) throws IOException {
    Configuration conf = new Configuration();
    SequenceFile.Reader read1 = new SequenceFile.Reader(FileSystem.get(conf), new Path(path1), conf);
    SequenceFile.Reader read2 = new SequenceFile.Reader(FileSystem.get(conf), new Path(path2), conf);
    IntWritable dictionaryKey = new IntWritable();
    Text text = new Text();
    LongWritable freq = new LongWritable();
    HashMap < Integer,Long> freqMap = new HashMap < Integer,Long > ();
    HashMap < Integer,String > dictionaryMap = new HashMap < Integer,String > ();
  
    /* 
           Read the contents of dictionary.file-0 and frequency.file-0 and write them to appropriate HashMaps
    */
 
    while (read1.next(text, dictionaryKey)) {
      dictionaryMap.put(Integer.parseInt(dictionaryKey.toString()), text.toString());
    }
    while (read2.next(dictionaryKey,freq)) {
      freqMap.put(Integer.parseInt(dictionaryKey.toString()),Long.parseLong(freq.toString()));
    }
 
    read1.close();
    read2.close();
 
    for(int i=0;i < dictionaryMap.size();i++){
        System.out.println("Key "+i + ": " + dictionaryMap.get(i));
    } 
 
    for(int i=0;i < freqMap.size();i++){ 
        System.out.println("Key "+i + ": " +freqMap.get(i));
    }
  }
 
  public static void main(String[] args) throws Exception {
        MahoutTutorial t = new MahoutTutorial();
        t.textToSequenceFiles("input-text-file-location", "sequence-file-location");
        t.vectorize("sequence-file-location", "/vector-file-location");
        t.readDictionaryAndFrequency("vector-file-location/dictionary.file-0", "vector-file-location/frequency.file-0");
 
  } 