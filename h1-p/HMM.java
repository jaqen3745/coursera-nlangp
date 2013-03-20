import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.collections.map.MultiKeyMap;

/**
 * Estimates the parameters for Trigram Hidden Markov model.</br>
 */
public class HMM {

  private final static String WORDTAG = "WORDTAG";
  private final static String[] tags = {"O", "I-GENE"};
  private final PrintWriter writer;

  Map<String, Integer> wordCounts;
  Map<String, Integer> tagCounts;
  MultiKeyMap emissionCounts;
  MultiKeyMap emissionProbabilities;

  public HMM(String filename) throws FileNotFoundException{
    wordCounts = new HashMap<String, Integer>();
    tagCounts = new HashMap<String, Integer>();
    emissionCounts = new MultiKeyMap();
    emissionProbabilities = new MultiKeyMap();
    
    writer = new PrintWriter(new File(filename));
  }

  private void readCounts() throws IOException{
    InputStream in = getClass().getResourceAsStream("./h1-p/gene-rare.counts");
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    String line;
    while((line = reader.readLine()) != null){
      String[] str = line.split(" ");
      if(str[1].equals(WORDTAG)){
        String word    = str[3];
        String tag     = str[2];
        int freq       = Integer.parseInt(str[0]);
        emissionCounts.put(tag, word, freq);

        Integer cntTag = (tagCounts.get(tag) == null)?0 : tagCounts.get(tag);
        tagCounts.put(tag, cntTag+freq);
      }
    }
//    String tag = tags[1];
//    String word = "_RARE_";
//    System.out.println(emissionCounts.get(tag, "_RARE_"));
//    System.out.println(tagCounts.get(tag));
//    System.out.println(((double)(int)emissionCounts.get(tag, word))/((double)(int)tagCounts.get(tag)));
    computeEmissionProbabilities();
//    System.out.println(emissionProbabilities.get(word, tag));
  }

  private void computeEmissionProbabilities(){
    for(Object k: emissionCounts.keySet()){
      MultiKey key = (MultiKey)k;
      String y = (String)key.getKey(0); //the tag
      String x = (String)key.getKey(1); //the word
      double prob = ((double)(int)emissionCounts.get(y, x))/((double)(int)tagCounts.get(y));
      emissionProbabilities.put(x, y, prob);
    }
  }

  /**
   * Returns the precomputed emission probabilities.
   * @param y the tag  {0, I-GENE}
   * @param x the word {Galphao, TFII, ... etc}
   */
  public double e(String x, String y){
//    System.out.println("Called e("+x+", "+y+")");
    if (emissionProbabilities.containsKey(x, y)){
      return (double)emissionProbabilities.get(x, y);
    }
    return (double)emissionProbabilities.get("_RARE_", y);
  }

  public void train() throws IOException{
    readCounts();
    computeEmissionProbabilities();
  }

  public void classify(InputStream in) throws IOException{
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    String line;
    while((line = reader.readLine()) != null){
      String word = line.trim();
      if(word.equals("")){
        writer.println();
        continue;
      }
      String maxTag = tags[0];
      double maxEmissionProb = Double.MIN_VALUE;
      for(String tag:tags){
        if( e(word.toLowerCase(), tag) > maxEmissionProb){
          maxEmissionProb = e(word, tag);
          maxTag = tag;
        }
      }
      writer.println(word+" "+maxTag);
    }
  }

  public static void main(String[] args) throws IOException {
    String filename = "./h1-p/gene_dev.p1.out";
    HMM hmm = new HMM(filename);
    hmm.train();

    InputStream devIn = hmm.getClass().getResourceAsStream("./h1-p/gene.dev");
    hmm.classify(devIn);
  }

}