package Bayespam;
import java.io.*;
import java.util.*;

public class Bayespam
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
    
    /// Return the frequency of the word in the Normal emails
    private static int getNormalCounter(String word)
    {
        Multiple_Counter counter = new Multiple_Counter();

        if ( vocab.containsKey(word) ){                  // if word exists already in the vocabulary..
        	return counter.counter_regular;                       // get the counter from the hashtable
        }
        return 1;                    // put the word with its counter into the hashtable
    }

    /// Return the frequency of the word in the spam emails
    private static int getSpamCounter(String word)
    {
        Multiple_Counter counter = new Multiple_Counter();

        if ( vocab.containsKey(word) ){                  // if word exists already in the vocabulary..
        	return counter.counter_spam;                    // get the counter from the hashtable
        }
        return 1;                    // put the word with its counter into the hashtable
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
    private static int readMessages(MessageType type)
    throws IOException
    {
        File[] messages = new File[0];

        if (type == MessageType.NORMAL){
            messages = listing_regular;
        } else {
            messages = listing_spam;
        }
        
        int counterFile = 0;
        for (int i = 0; i < messages.length; ++i)
        {
			counterFile ++;
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
            	 
            	 if(!word.isEmpty() && word.length() > 4){
            		 // System.out.println(word.toString());		// Print the tokenized word
            		 addWord(word, type);    // add them to the vocabulary
            	 }
            	}
                
            }

            in.close();
        }
		return counterFile;
    }
   
    public static void main(String[] args)
    throws IOException
    {
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
        int nMessagesRegular = readMessages(MessageType.NORMAL);
        int nMessagesSpam = readMessages(MessageType.SPAM);

        ///  Compute the prior probability
        int priorRegular = nMessagesRegular/ nMessagesRegular+nMessagesSpam;
        int priorSpam = nMessagesSpam/ nMessagesRegular+nMessagesSpam;

        /// Compute the likelihood
        /// First find the number of words in each class of email...
        double e = 100;		/// tuning parameter 3
    	Iterator<String> words = vocab.keySet().iterator();
    	float nWordsRegular = 0;
    	float nWordsSpam = 0;
        while( words.hasNext() ){
        	String word = words.next();
        	int regular_freq = vocab.get(word).counter_regular;
        	int spam_freq = vocab.get(word).counter_spam;
        	
        	if(regular_freq != 0){
        		nWordsRegular +=  regular_freq;
        	}else{
        		nWordsRegular +=  e;		// If there are no occurances of a word, its frequency is replaced by e
        	}
        	
        	if(spam_freq != 0){
        		nWordsSpam +=  spam_freq;
        	}else{
        		nWordsSpam +=  e;		// If there are no occurances of a word, its frequency is replaced by e
        	}
        }
        System.out.println(nWordsRegular + " " + nWordsSpam);
        
        /// ... then computes the relative frequencies of each word for each type of email and
        /// find the class conditional probability
        words = vocab.keySet().iterator();
        while( words.hasNext() ){
        	String word = words.next();
        	double regular_freq = vocab.get(word).counter_regular;
        	if(regular_freq != 0)
        		vocab.get(word).setLikelihoodRegular(-Math.log10(regular_freq/ nWordsRegular)); /// the prob is log-normalized
        	else
        		vocab.get(word).setLikelihoodRegular(-Math.log10(e/(nWordsSpam+nWordsRegular)));
        	double spam_freq = vocab.get(word).counter_spam;
        	if(spam_freq != 0)
        		vocab.get(word).setLikelihoodSpam(-Math.log10(spam_freq/ nWordsSpam));
        	else
        		vocab.get(word).setLikelihoodSpam(-Math.log10(e/(nWordsSpam+nWordsRegular)));
        }
        
        // Print out the hash table
        printVocab();

        // Now all students must continue from here:
        //
        // 1) A priori class probabilities must be computed from the number of regular and spam messages
        // 2) The vocabulary must be clean: punctuation and digits must be removed, case insensitive
        // 3) Conditional probabilities must be computed for every word
        // 4) A priori probabilities must be computed for every word
        // 5) Zero probabilities must be replaced by a small estimated value
        // 6) Bayes rule must be applied on new messages, followed by argmax classification
        // 7) Errors must be computed on the test set (FAR = false accept rate (misses), FRR = false reject rate (false alarms))
        // 8) Improve the code and the performance (speed, accuracy)
        //
        // Use the same steps to create a class BigramBayespam which implements a classifier using a vocabulary consisting of bigrams
    }
}