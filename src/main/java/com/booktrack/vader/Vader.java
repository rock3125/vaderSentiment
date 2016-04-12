/*
    The MIT License (MIT)

    Copyright (c) 2014 cjhutto
    Copyright (c) 2016 Rock de Vocht, booktrack.com

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.

 */
package com.booktrack.vader;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Created by Rock de Vocht on 18/03/16 for Booktrack.com
 *
 * the vader emotional analysis system
 *
 */
public class Vader {

    private static Logger logger = LoggerFactory.getLogger(Vader.class);

    // empirically derived mean sentiment intensity rating increase for booster words
    private static final double B_INCR = 0.293;
    private static final double B_DECR = -0.293;

    // alhpa normalization value
    private static final double ALPHA = 15.0;

    // empirically derived mean sentiment intensity rating increase for using ALLCAPs to emphasize a word
    private static final double c_INCR = 0.733;

    // the maximum number of words in an idiom
    private static final int idiomMaxSize = 5;

    // negations of mood
    private static final String[] NEGATE = new String[] {
            "aint", "arent", "cannot", "cant", "couldnt", "darent", "didnt", "doesnt",
            "ain't", "aren't", "can't", "couldn't", "daren't", "didn't", "doesn't",
            "dont", "hadnt", "hasnt", "havent", "mightnt", "mustnt", "neither",
            "don't", "hadn't", "hasn't", "haven't", "isn't", "isnt", "mightn't", "mustn't",
            "neednt", "needn't", "never", "none", "nope", "nor", "not", "nothing", "nowhere",
            "oughtnt", "shant", "shouldnt", "uhuh", "wasnt", "werent",
            "oughtn't", "shan't", "shouldn't", "uh-uh", "wasn't", "weren't",
            "without", "wont", "wouldnt", "won't", "wouldn't", "rarely", "seldom", "despite"};

    // items in the boosterMap that need an increment in sentiment when seen
    private static final String[] BoosterIncrementList = new String[] {
            "absolutely", "amazingly", "awfully", "completely", "considerably",
            "decidedly", "deeply", "effing", "enormously",
            "entirely", "especially", "exceptionally", "extremely",
            "fabulously", "flipping", "flippin",
            "fricking", "frickin", "frigging", "friggin", "fully", "fucking",
            "greatly", "hella", "highly", "hugely", "incredibly",
            "intensely", "majorly", "more", "most", "particularly",
            "purely", "quite", "really", "remarkably",
            "so",  "substantially",
            "thoroughly", "totally", "tremendously",
            "uber", "unbelievably", "unusually", "utterly",
            "very"};

    // items in the boosterMap that need an decrement in sentiment when seen
    private static final String[] BoosterDecreaseList = new String[] {
            "almost", "barely", "hardly", "just enough",
            "kind of", "kinda", "kindof", "kind-of",
            "less", "little", "marginally", "occasionally", "partly",
            "scarcely", "slightly", "somewhat",
            "sort of", "sorta", "sortof", "sort-of"};

    private static final Map<String, Double> boosterMap = new HashMap<>();

    // check for special case idioms using a sentiment-laden keyword known to SAGE
    private static final Map<String, Double> idiomMap = new HashMap<>();

    // set of vectors keyed on word + tag e {n,v,a}
    private Map<String, Double> moodSet;
    private HashSet<String> negatedSet;

    public Vader() {
    }

    /**
     * Analyse a sentence using Vader's algorithm and return a score for that sentence
     * @param sentence the sentence to analyse
     * @return the vader score
     */
    public VScore analyseSentence( List<Token> sentence ) {
        if ( sentence != null ) {

            boolean isCapsDifferential = isAllCAPDifferential(sentence);
            List<Double> sentiments = new ArrayList<>();
            int i = 0;
            List<Token> snt = filterPunctuation(sentence);
            for ( Token item : snt ) {

                double v = 0.0;
                String itemLowercase = item.getValue().toLowerCase();

                // skip "kind of" and any value already in the booster dictionary
                if ( ((i + 1) < snt.size() && itemLowercase.equals("kind") && wordInSentenceEquals(snt, i+1, "of")) ||
                        boosterMap.containsKey(itemLowercase) ) {
                    sentiments.add(v);
                    i++; // next index
                    continue;
                }

                if ( moodSet.containsKey(itemLowercase) ) {

                    // get sentiment value
                    v = moodSet.get(itemLowercase);

                    // check if sentiment laden word is in ALLCAPS (while others aren't)
                    if ( isCapsDifferential && isUpper(item.getValue()) ) {
                        if ( v > 0.0 ) {
                            v = v + c_INCR;
                        } else {
                            v = v - c_INCR;
                        }
                    }

                    double nScalar = -0.74; // negative scalar

                    if ( i > 0 && !moodSetContainsSentenceIndex(snt,i-1) ) {
                        double s1 = scalarIncDec(snt.get(i-1).getValue(), v, isCapsDifferential);
                        v = v + s1;

                    }

                    if ( i > 1 && !moodSetContainsSentenceIndex(snt,i-2) ) {

                        double s2 = scalarIncDec(snt.get(i-2).getValue(), v, isCapsDifferential);
                        v = v + (s2 * 0.95);

                        // check for special use of 'never' as valence modifier instead of negation
                        if ( wordInSentenceEquals(snt,i-2,"never") && (wordInSentenceEquals(snt,i-1,"so") || wordInSentenceEquals(snt,i-1,"this")) ) {

                            v = v * 1.5;

                        } else if ( negated(snt,i-2) ) { //  otherwise, check for negation/nullification

                            v = v * nScalar;

                        }
                    }

                    if ( i > 2 && !moodSetContainsSentenceIndex(snt, i-3) ) {

                        double s3 = scalarIncDec(snt.get(i-3).getValue(), v, isCapsDifferential);
                        v = v + (s3 * 0.9);

                        // check for special use of 'never' as valence modifier instead of negation
                        if ( wordInSentenceEquals(snt,i-3,"never") &&
                                ( (wordInSentenceEquals(snt,i-2,"so") || wordInSentenceEquals(snt,i-2,"this")) ||
                                        (wordInSentenceEquals(snt,i-1,"so") || wordInSentenceEquals(snt,i-1,"this")) ) ) {
                            v = v * 1.25;
                        } else if ( negated(snt, i-3) ) {

                            v = v * nScalar;

                        }

                        // test the special case idioms
                        StringBuilder idiom = new StringBuilder();
                        for ( int index = 0; index < idiomMaxSize && index < snt.size(); index++ ) {
                            idiom.append(getLcaseWordAt(snt, index + i));
                            String idiomStr = idiom.toString();
                            if ( idiomMap.containsKey(idiomStr) ) {
                                v = idiomMap.get(idiomStr);
                            }
                            if ( boosterMap.containsKey(idiomStr) ) {
                                v = v + B_DECR;
                            }
                            idiom.append(" ");
                        }

                    }

                    // check for negation case using "least"
                    if ( i > 1 && !moodSetContainsSentenceIndex(snt, i-1) &&
                            wordInSentenceEquals(snt, i-1, "least") ) {
                        if ( !wordInSentenceEquals(snt,i-2,"at") && !wordInSentenceEquals(snt,i-2,"very") ) {
                            v = v * nScalar;
                        }
                    } else if ( i > 0 && !moodSetContainsSentenceIndex(snt, i-1) &&
                            wordInSentenceEquals(snt, i-1, "least") ) {
                        v = v * nScalar;
                    }

                } // if moodSet contains word

                sentiments.add(v);
                i++; // next index

            } // for each item in snt

            // set the sentiment on the tokens
            if ( snt.size() == sentiments.size() ) {
                for ( int j = 0; j < snt.size(); j++ ) {
                    snt.get(j).setWordScore( sentiments.get(j) );
                }
            }

            // find but in the sentence
            int butIndex = -1;
            for ( int j = 0; j < sentence.size(); j++ ) {
                Token t = sentence.get(j);
                if ( t.getValue().equals("but") || t.getValue().equals("BUT")) {
                    butIndex = j;
                    break;
                }
            }
            if ( butIndex >= 0 ) {
                List<Double> newSentiments = new ArrayList<>();
                for ( int j = 0; j < sentiments.size(); j++ ) {
                    if ( j < butIndex ) {
                        newSentiments.add( sentiments.get(j) * 0.5);
                    } else if ( j > butIndex ) {
                        newSentiments.add( sentiments.get(j) * 1.5);
                    } else {
                        newSentiments.add( sentiments.get(j) );
                    }
                }
                sentiments = newSentiments;
            }

            // do the sum of the total
            double sum = 0.0;
            for ( double value : sentiments ) {
                sum = sum + value;
            }

            // count the number of exclamation marks
            int epCount = 0;
            for ( Token t : sentence ) {
                if ( t.getValue().equals("!") ) {
                    epCount = epCount + 1;
                }
            }
            if ( epCount > 4 ) {
                epCount = 4;
            }
            double emAmplifier = (double)epCount * 0.292; // empirically derived mean sentiment intensity rating increase for exclamation points

            if ( sum > 0.0 ) {
                sum = sum + emAmplifier;
            } else if ( sum < 0.0 ) {
                sum = sum - emAmplifier;
            }

            // count the number of question marks
            int qmCount = 0;
            for ( Token t : sentence ) {
                if ( t.getValue().equals("?") ) {
                    qmCount = qmCount + 1;
                }
            }

            // check for added emphasis resulting from question marks (2 or 3+)
            double qmAmplifier = 0.0;
            if ( qmCount > 1 ) {
                if ( qmCount <= 3 ) {
                    qmAmplifier = (double)qmCount * 0.18;
                } else {
                    qmAmplifier = 0.96;
                }
                if ( sum > 0.0 ) {
                    sum = sum + qmAmplifier;
                } else if ( sum < 0.0 ) {
                    sum = sum - qmAmplifier;
                }
            }

            double compound = normalize(sum);

            double posSum = 0.0;
            double negSum = 0.0;
            double neutralCount = 0.0;
            for ( double sentimentScore : sentiments ) {
                if ( sentimentScore > 0.0 ) {
                    posSum = posSum + sentimentScore + 1.0; // compensates for neutral words that are counted as 1
                }
                if ( sentimentScore < 0.0 ) {
                    negSum = negSum + sentimentScore - 1.0; // when used with math.fabs(), compensates for neutrals
                }
                if ( sentimentScore == 0.0 ) {
                    neutralCount = neutralCount + 1;
                }
            }

            // adjust amplifiers
            if ( posSum > Math.abs(negSum) ) {
                posSum = posSum + qmAmplifier + emAmplifier;
            } else if ( posSum < Math.abs(negSum) ) {
                negSum = negSum - (qmAmplifier + emAmplifier);
            }

            double total = posSum + Math.abs(negSum) + neutralCount;
            if ( total > 0.0 ) { // make sure values are valid
                posSum = Math.abs(posSum / total);
                negSum = Math.abs(negSum / total);
                neutralCount = Math.abs(neutralCount / total);
            } else {
                posSum = 0.0;
                negSum = 0.0;
                neutralCount = 0.0;
            }

            return new VScore(posSum, neutralCount, negSum, compound);

        } // if sentence != null

        return new VScore(); // empty score
    }

    /**
     * check if the word in sentence @ index is in the moodSet or not
     * @param sentence the sentence to check
     * @param index the index
     * @return true if the word is in the mood-set
     */
    private boolean moodSetContainsSentenceIndex( List<Token> sentence, int index ) {
        if ( index >= 0 && index < sentence.size() ) {
            Token t = sentence.get(index);
            return moodSet.containsKey(t.getValue().toLowerCase());
        }
        return false;
    }

    /**
     * return true if the word in sentence @ index equals wordStr (case insensitive)
     * @param sentence the sentence to check
     * @param index the index of the word
     * @param wordStr the word to check for
     * @return true if the word is there
     */
    private boolean wordInSentenceEquals( List<Token> sentence, int index, String wordStr ) {
        if ( index >= 0 && index < sentence.size() ) {
            Token t = sentence.get(index);
            return t.getValue().compareToIgnoreCase(wordStr) == 0;
        }
        return false;
    }

    /**
     * return a word at index lowercased
     * @param sentence the sentence to get it from
     * @param index the index of the word
     * @return the word at index, lower-cased, or empty string if not a word
     */
    private String getLcaseWordAt( List<Token> sentence, int index ) {
        if ( index >= 0 && index < sentence.size() ) {
            Token t = sentence.get(index);
            return t.getValue().toLowerCase();
        }
        return "";
    }

    /**
     * load vader from class-path
     * @throws IOException
     */
    public void init() throws IOException {

        logger.debug("Vader: init lexicon(vader_sentiment_lexicon.txt)");
        moodSet = new HashMap<>();
        try ( InputStream vaderIn = getClass().getResourceAsStream("vader_sentiment_lexicon.txt") ) {
            if (vaderIn == null) {
                throw new IOException("vader_sentiment_lexicon.txt not found on class-path");
            }
            String vaderLexicon = new String(IOUtils.toByteArray(vaderIn));
            if (vaderLexicon.length() > 0) {
                for (String line : vaderLexicon.split("\n")) {
                    String[] items = line.split("\t");
                    if (items.length > 2) {
                        moodSet.put(items[0].trim(), Double.parseDouble(items[1].trim()));
                    } else {
                        logger.debug("skipping invalid Vader line: " + line);
                    }
                }
            }
        }

        // setup booster dict
        for ( String incr : BoosterIncrementList) {
            boosterMap.put( incr, B_INCR);
        }
        for ( String decr : BoosterDecreaseList) {
            boosterMap.put( decr, B_DECR);
        }

        // add the special case idioms
        logger.debug("Vader: init idioms(vader_idioms.txt)");
        try ( InputStream vaderIdiomsIn = getClass().getResourceAsStream("vader_idioms.txt") ) {
            if (vaderIdiomsIn == null) {
                throw new IOException("vader_idioms.txt not found on class-path");
            }
            String vaderIdiomContent = new String(IOUtils.toByteArray(vaderIdiomsIn));
            if (vaderIdiomContent.length() > 0) {
                for (String line : vaderIdiomContent.split("\n")) {
                    String[] items = line.split(",");
                    if ( items.length == 2 ) {
                        idiomMap.put(items[0].trim(), Double.parseDouble(items[1].trim()));
                    }
                }
            }
        }

        negatedSet = new HashSet<>();
        for ( String str : NEGATE) {
            negatedSet.add(str);
        }
    }

    /**
     * return true if the sentence has a negation in it
     * @param sentence the sentence to check
     * @return true if negated
     */
    private boolean negated(List<Token> sentence, int index ) {
        if ( sentence != null ) {
            Token t = sentence.get(index);
            String lcaseWord = t.getValue().toLowerCase();

            // anything in the negatedSet is a negator
            if (negatedSet.contains(lcaseWord)) {

                // exceptions for don't and dont "know", or "like"
                // can't take/feel
                if ( index + 1 < sentence.size() ) {
                    String lcaseWord2 = sentence.get(index+1).getValue().toLowerCase();
                    if ( lcaseWord2.equals("know") || lcaseWord2.equals("take") || lcaseWord2.equals("feel") || lcaseWord2.equals("like") ||
                            lcaseWord2.equals("want") || lcaseWord2.equals("wanna") ) {
                        return false;
                    }
                }

                return true;
            }
            // any "couldn't" modal is a negator
            if ( lcaseWord.contains("n't") ) {
                return true;
            }

            // "at least" is a negator
            if ( lcaseWord.equals("least") && index > 0 ) {
                if ( wordInSentenceEquals(sentence, index-1, "at") ) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * normalize a score using an alpha magic value
     * @param score the score to normalize
     * @return the normalized score
     */
    private double normalize( double score ) {
        return score / Math.sqrt( (score *score) + ALPHA );
    }

    /**
     * return true if str does not contain any lower case characters a..z
     * @param str the string to check
     * @return true if there are no lower case characters in this string
     */
    private boolean isUpper( String str ) {
        if ( str != null ) {
            for ( char ch : str.toCharArray() ) {
                if ( ch >= 'a' && ch <= 'z' ) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * return a differential of all the caps, meaning that if the sentence is all
     * caps it doesn't count, but if any one word (or n-1 words) are caps, return true
     * @param sentence the sentence to check
     * @return true if one or more, but not all words are caps
     */
    private boolean isAllCAPDifferential( List<Token> sentence ) {
        if ( sentence != null ) {
            int countAllCaps = 0;
            for (Token t : sentence) {
                if ( isUpper(t.getValue()) ) {
                    countAllCaps = countAllCaps + 1;
                }
            }
            int capsDifferential = sentence.size() - countAllCaps;
            return capsDifferential > 0 && capsDifferential < sentence.size();
        }
        return false;
    }

    /**
     * word out an individual word's scalar given a valance and a the sentence's isCaps diff
     * @param word the word to check
     * @param valence its valence value
     * @param isCapsDifferential the is diff of the sentence
     * @return an emotional scalar value for this word
     */
    private double scalarIncDec( String word, double valence, boolean isCapsDifferential ) {
        double scalar = 0.0;
        String lcase = word.toLowerCase();
        if ( boosterMap.containsKey(lcase) ) {
            scalar = boosterMap.get(lcase);
            if ( valence < 0 ) {
                scalar = scalar * -1.0;
            }
            // check if booster/dampener word is in ALLCAPS (while others aren't)
            if ( isUpper(word) && isCapsDifferential ) {
                if ( valence > 0.0 ) {
                    scalar = scalar + c_INCR;
                } else {
                    scalar = scalar - c_INCR;

                }
            }
        }
        return scalar;
    }

    /**
     * return a sentence without any punctuation in it - assume that all
     * punctuation are characters of length 1, not entirely correct but it
     * helps filter out all the little niggly noise words like "a" and "i" too
     * @param sentence the sentence to check
     * @return a sentence without any of the punctuation marks in it
     */
    private List<Token> filterPunctuation( List<Token> sentence ) {
        if (sentence != null ) {
            List<Token> newSentence = new ArrayList<>();
            for ( Token t : sentence ) {
                if ( t.getValue().length() > 1) {
                    newSentence.add(t);
                }
            }
            return newSentence;
        }
        return null;
    }


}

