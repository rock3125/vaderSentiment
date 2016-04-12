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

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rock de Vocht @ booktrack.com on 13/04/16.
 *
 * simple tokenizer and pos tagger using OpenNLP
 *
 */
public class VaderNLP {

    private static Logger logger = LoggerFactory.getLogger(VaderNLP.class);

    // the apache-nlp sentence boundary detector
    private SentenceDetector sentenceDetector = null;

    // the apache-nlp penn-tree tagger
    private POSTaggerME posTagger = null;

    // apache-nlp tokenizer
    private Tokenizer tokenizer;

    public VaderNLP() {
    }

    /**
     * convert a piece of text to a list of parsed tokens with POS tags
     * @param text the text to parse
     * @return a list of sentences (each sentence a list of tokens itself) that is the entire text
     * @throws IOException if things don't go as planned
     */
    public List<List<Token>> parse( String text ) throws IOException {

        if ( text != null ) {

            List<List<Token>> sentenceList = new ArrayList<>();

            // this is how it works boys and girls - apache-opennlp
            String[] sentenceArray = getSentences(text);
            for (String sentenceStr : sentenceArray) {

                List<Token> sentence = new ArrayList<>();

                // get the results of the syntactic parse
                String[] words = getTokens(sentenceStr);
                String[] posTags = getTags(words);

                // the number of tags should always match the number of words - a little primitive
                // how open-nlp treats it
                if ( words.length != posTags.length ) {
                    throw new IOException("unmatched words / posTags in nlp-parser");
                }

                // add this sentence - the first word in the sentence gets the "is a sentence start" marker
                for ( int i = 0; i < words.length; i++ ) {
                    sentence.add( new Token( words[i], posTags[i]) );
                }

                sentenceList.add( sentence );
            }

            return sentenceList;
        }
        return null;
    }

    /**
     * invoke the OpenNLP sentence detector to split text into sentences
     * @param text the text to split
     * @return a set of string representing nlp sentences
     */
    private String[] getSentences(String text) {
        return sentenceDetector.sentDetect(text);
    }

    /**
     * turn a sentence into a set of tokens (split words and punctuation etc)
     * @param sentence a string that is a sentence
     * @return a set of tokens from that sentence in order
     */
    private String[] getTokens(String sentence) {
        return tokenizer.tokenize(sentence);
    }

    /**
     * use a pos-tagger to get the set of penn-tree tags for a given set of tokens
     * that form a sentence
     * @param tokens a sentence split into tokens
     * @return a set of penn-tags
     */
    private String[] getTags(String[] tokens) {
        return posTagger.tag(tokens);
    }

    /**
     * initialise the parser and its constituents - called from spring init
     * @throws IOException
     */
    public void init() throws IOException {

        logger.debug("VaderNLP: init()");

        // setup the sentence splitter
        {
            logger.debug("VaderNLP: loading en-sent.bin");
            try (InputStream modelIn = getClass().getResourceAsStream("en-sent.bin") ) {
                if (modelIn == null) {
                    throw new IOException("resource en-sent.bin not found in classpath");
                }
                SentenceModel sentenceModel = new SentenceModel(modelIn);
                sentenceDetector = new SentenceDetectorME(sentenceModel);
            }
        }

        // setup the max-ent tokenizer
        {
            logger.debug("VaderNLP: loading en-token.bin");
            try ( InputStream modelIn = getClass().getResourceAsStream("en-token.bin") ) {
                if ( modelIn == null ) {
                    throw new IOException("resource en-sent.bin not found in classpath");
                }
                TokenizerModel tokenizerModel = new TokenizerModel(modelIn);
                tokenizer = new TokenizerME(tokenizerModel);
            }
        }

        // setup the pos tagger
        {
            logger.debug("VaderNLP: loading en-pos-maxent.bin");
            try ( InputStream modelIn = getClass().getResourceAsStream("en-pos-maxent.bin") ) {
                if (modelIn == null) {
                    throw new IOException("resource en-sent.bin not found in classpath");
                }
                POSModel posModel = new POSModel(modelIn);
                posTagger = new POSTaggerME(posModel);
            }
        }


    }


}

