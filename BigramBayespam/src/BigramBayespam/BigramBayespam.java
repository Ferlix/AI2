package BigramBayespam;
import java.io.*;
import java.util.*;

public class BigramBayespam
{
    // This defines the two types of messages we have.
    static enum MessageType
    {
        NORMAL, SPAM
    }

    // This a class with two counters (for regular and for spam)
    static class Multiple_Counter
    {
        int counter_spam    = 0;
        int counter_regular = 0;
        double likelihood_regular = 0;
        double likelihood_spam = 0;

        // Increase one of the counters by one
        public void incrementCounter(MessageType type)
        {
            if ( type == MessageType.NORMAL ){
                ++counter_regular;
            } else {
                ++counter_spam;
            }
        }
        
        /// Set the likelihood for each word
        public void setLikelihoodSpam(double n)
        {
        	likelihood_spam = n;
        }
        public void setLikelihoodRegular(double n)
        {
        	likelihood_regular = n;
        }
    }

    // Listings of the two subdirectories (regular/ and spam/)
    private static File[] listing_regular = new File[0];
    private static File[] listing_spam = new File[0];

    // A hash table for the vocabulary (word searching is very fast in a hash table)
    private static Hashtable <String, Multiple_Counter> vocab = new Hashtable <String, Multiple_Counter> ();

    
    // Add a word to the vocabulary
    private static void addWord(String word, MessageType type)
    {
        Multiple_Counter counter = new Multiple_Counter();

        if ( vocab.containsKey(word) ){                  // if word exists already in the vocabulary..
            counter = vocab.get(word);                  // get the counter from the hashtable
        }
        counter.incrementCounter(type);                 // increase the counter appropriately
        vocab.put(word, counter);                       // put the word with its counter into the hashtable
    }
    
    // Add a word to the vocabulary
    private static void cleanVocab(int threshold)
    {
        Multiple_Counter counter = new Multiple_Counter();

        for (Enumeration<String> e = vocab.keys() ; e.hasMoreElements() ;)
        {   
            String word;
            
            word = e.nextElement();
            counter  = vocab.get(word);
            if(vocab.get(word).counter_regular + vocab.get(word).counter_spam < threshold)	
            	vocab.remove(word);
        }
    }
    

    // List the regular and spam messages
    private static void listDirs(File dir_location)
    {
        // List all files in the directory passed
        File[] dir_listing = dir_location.listFiles();

        // Check that there are 2 subdirectories
        if ( dir_listing.length != 2 )
        {
            System.out.println( "- Error: specified directory does not contain two subdirectories.\n" );
            Runtime.getRuntime().exit(0);
        }

        listing_regular = dir_listing[0].listFiles();
        listing_spam    = dir_listing[1].listFiles();
        /// System.out.println(listing_regular.length + " " + listing_spam.length);		
    }

    
    // Print the current content of the vocabulary
    private static void printVocab()
    {
        Multiple_Counter counter = new Multiple_Counter();

        for (Enumeration<String> e = vocab.keys() ; e.hasMoreElements() ;)
        {   
            String word;
            
            word = e.nextElement();
            counter  = vocab.get(word);
            
            System.out.println( word + " | in regular: " + counter.counter_regular + 
                                " in spam: "    + counter.counter_spam +
                                "\nregular likelihood: " + counter.likelihood_regular +
                                "   spam likelihood: " + counter.likelihood_spam);
        }
    }
    
    


    // Read the words from messages and add them to your vocabulary. The boolean type determines whether the messages are regular or not  
    /// The function also returns the number of emails scanned 
    static String word2;
    private static void readMessages(MessageType type, int minimalSize)
    throws IOException
    {
        File[] messages = new File[0];
        if (type == MessageType.NORMAL){
            messages = listing_regular;
        } else {
            messages = listing_spam;
        }
        for (int i = 0; i < messages.length; ++i)
        {
            FileInputStream i_s = new FileInputStream( messages[i] );
            BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
            String line;
            String word;
            Scanner sc;
            while ((line = in.readLine()) != null)                      // read a line
            {
            	/// Scanner go through the lines and takes the single words
            	/// Omitting symbols and words shorter than 4 characters
            	sc = new Scanner(line);
            	while(sc.hasNext()){
            	 word = sc.next().replaceAll("[^a-zA-Z]", "").toLowerCase();
       
            	 if(sc.hasNext()){
            		 word2 = sc.next().replaceAll("[^a-zA-Z]", "").toLowerCase();
            	 }
            	 if(!word.isEmpty() && word.length() >= minimalSize)
            		 addWord(word.concat(word2.toString()), type);    // add them to the vocabulary
            	}
                
            }
            in.close();
        }
    }
    
    private static int getNumberMessages(MessageType type){
		File[] messages = new File[1];
		
        if (type == MessageType.NORMAL){
            messages = listing_regular;
        } else {
            messages = listing_spam;
        }
         return messages.length;				
    } 
    
    /// This function reads the test-email, computes the posteri class probability 
    /// and returns the total number of misclassifications for that category
	private static int readTest(MessageType type, int alpha, double priorProb) throws IOException{
			int errors = 0;
            double postRegular = alpha;			// ALpha is the tuning parameter for the post-probabilities
 			double postSpam = alpha;
			File[] messages = new File[1];
			
	        if (type == MessageType.NORMAL){
	            messages = listing_regular;
	        } else {
	            messages = listing_spam;
	        }
	         /// int n = messages.length;				// Check if the folder is right 
	         /// System.out.printf(type + " %d \n", n);	
	       
			for (int i = 0; i < messages.length; ++i)
	        {
	            FileInputStream i_s = new FileInputStream( messages[i] );
	            BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
	            String text;
	            String word;
	            Scanner sc;
				while ((text = in.readLine()) != null)                      // read a line
	            {
	            	/// Scanner go through the lines and takes the single words
	            	/// Omitting words which are not saved in the hash-table
	            	sc = new Scanner(text);
	            	while(sc.hasNext()){
	            	 word = sc.next().replaceAll("[^a-zA-Z]", "").toLowerCase();    
	            	 if(sc.hasNext()){
	            		 word2 = sc.next().replaceAll("[^a-zA-Z]", "").toLowerCase();
	            	 }
	            	 if(vocab.containsKey(word.concat(word2.toString()))){
	            		 	// Add the probability only if the word is in the training set
		         	        postRegular += vocab.get(word.concat(word2.toString())).likelihood_regular;
		        	        postSpam  += vocab.get(word.concat(word2.toString())).likelihood_spam;
		            	}
		            	 
	            	}
	            }
	            in.close();
	            postRegular *= priorProb; 		// Post probabilities are multiplied by the prior probability
 	            postSpam *= priorProb;
	            if(postRegular < postSpam && type == MessageType.NORMAL){			// Misclassification for normal messages
	            	errors += 1;
	            }
	            if(postRegular > postSpam && type == MessageType.SPAM){				// Misclassification for spam messages
	            	errors += 1;
	            }
	            //System.out.println(postRegular + " " + postSpam + " " +errors );
	            postRegular = 0;
	            postSpam = 0;
	        }
			return errors;
	}
	
	private static void printConfusionMatrix(int[][] confusionMatrix) {
		System.out.println("True Positives: " + confusionMatrix[0][0] + " False Positive: " + confusionMatrix[0][1]);
		System.out.println("True negatives: " + confusionMatrix[1][0] + " False negatives: " + confusionMatrix[1][1]);
	}
   
    public static void main(String[] args)
    throws IOException
    {
    	int minimalSize = 4;	/// minimal size of the word in the vocabulary
    	double e = 0.1;			/// tuning parameter for computing the likelihood
    	int threshold = 5;		/// minimum frequency allowed for a word in the vocabulary 
    	
        // Location of the directory (the path) taken from the cmd line (first arg)
        File dir_location = new File( args[0] );
        
        // Check if the cmd line arg is a directory
        if ( !dir_location.isDirectory() )
        {
            System.out.println( "- Error: cmd line arg not a directory.\n" );
            Runtime.getRuntime().exit(0);
        }

        // Initialize the regular and spam lists
        listDirs(dir_location);

        // Read the e-mail messages and count the numbers of email
        double nMessagesRegular = getNumberMessages(MessageType.NORMAL);
        double nMessagesSpam = getNumberMessages(MessageType.SPAM);
        double nMessagesTotal = nMessagesRegular+nMessagesSpam;
        
        // Build vocab
        readMessages(MessageType.NORMAL, minimalSize);
        readMessages(MessageType.SPAM, minimalSize);
        cleanVocab(threshold);

        ///  Compute the prior probability
        double priorRegular = nMessagesRegular/ nMessagesTotal;
        double priorSpam = nMessagesSpam/ nMessagesTotal;
        
        /// Compute the likelihood
        /// First find the total number of words in each class of email (zero-terms are substituted by e)...
    	Iterator<String> line = vocab.keySet().iterator();
    	float nWordsRegular = 0;
    	float nWordsSpam = 0;
        while( line.hasNext() ){
        	String word = line.next();
        	int regular_freq = vocab.get(word).counter_regular;
        	int spam_freq = vocab.get(word).counter_spam;
        	nWordsRegular +=  regular_freq;      	
       		nWordsSpam +=  spam_freq;
        }
        int nWordsTotal = (int) (nWordsRegular + nWordsSpam);
        //System.out.println("number of regular words: " + nWordsRegular + "  number of spam words:  " + nWordsSpam);		
        
        /// ... then computes the relative frequencies of each word for each type of email and
        /// find the class conditional probability
        line = vocab.keySet().iterator();
        while( line.hasNext() ){
        	String word = line.next();
        	
        	/// Take the number of occurrances of a word
        	double regular_freq = vocab.get(word).counter_regular;
        	double spam_freq = vocab.get(word).counter_spam;
        	
        	if(regular_freq != 0)
        		vocab.get(word).setLikelihoodRegular(Math.log10(regular_freq) + Math.log10(nWordsRegular)); /// the prob is log-normalized
        	else
        		vocab.get(word).setLikelihoodRegular(Math.log10(e) + Math.log10(nWordsRegular + nWordsSpam)); // zero probabilities are estimated with a small value
  
        	if(spam_freq != 0)
        		vocab.get(word).setLikelihoodSpam(Math.log10(spam_freq) + Math.log10(nWordsSpam));
        	else
        		vocab.get(word).setLikelihoodSpam(Math.log10(e) + Math.log10(nWordsRegular + nWordsSpam)); // zero probabilities are estimated with a small value
        }
        
        // Print out the hash table
        printVocab();
         
        // Find the post-class probabilities and computes the error per category
        int alpha = 0;
        int FAR = 0;  // FAR = false accept rate (misses)
        int FRR = 0;  // FRR = false reject rate (false alarms)
        File dir_location2 = new File( args[1] );	// Move to the 2nd folder
        listDirs(dir_location2);	// Initialize the regular and spam lists for the test
        FAR = readTest(MessageType.NORMAL, alpha, priorRegular);
        FRR = readTest(MessageType.SPAM, alpha, priorSpam);
        
        // Make confusion matrix 
		int [][] ConfusionMatrix = {{getNumberMessages(MessageType.NORMAL) - FAR,FAR},{getNumberMessages(MessageType.SPAM) - FRR,FRR}};
		printConfusionMatrix(ConfusionMatrix);
		
        

        

        // Now all students must continue from here:
        //
        // 1) A priori class probabilities must be computed from the number of regular and spam messages DONE
        // 2) The vocabulary must be clean: punctuation and digits must be removed, case insensitive DONE
        // 3) Conditional probabilities must be computed for every word DONE
        // 4) A priori probabilities must be computed for every word DONE
        // 5) Zero probabilities must be replaced by a small estimated value DONE
        // 6) Bayes rule must be applied on new messages, followed by argmax classification DONE
        // 7) Errors must be computed on the test set (FAR = false accept rate (misses), FRR = false reject rate (false alarms)) DONE
        // 8) Improve the code and the performance (speed, accuracy) DONE
        //
        // Use the same steps to create a class BigramBayespam which implements a classifier using a vocabulary consisting of bigrams
    }



}