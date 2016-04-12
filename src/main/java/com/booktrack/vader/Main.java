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

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by Rock de Vocht @ booktrack.com on 6 March 2016
 *
 * the main entry into the system - parse parameters
 * and run a statistical mood analysis over a large text (book)
 * output the result as a CSV so it can be graphed / analysed
 *
 */
public class Main {

    private static Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * main entry point and demo case for Vader
     * @param args the arguments - explained below in the code
     * @throws Exception anything goes wrong - except
     */
    public static void main(String[] args) throws Exception {

        // create Options object for command line parsing
        Options options = new Options();
        options.addOption("file", true, "input text-file (-file) to read and analyse using Vader");

        CommandLineParser cmdParser = new DefaultParser();
        CommandLine line = null;
        try {
            // parse the command line arguments
            line = cmdParser.parse( options, args );
        } catch( ParseException exp ) {
            // oops, something went wrong
            logger.error( "invalid command line: " + exp.getMessage() );
            System.exit(0);
        }

        // get the command line argument -file
        String inputFile = line.getOptionValue("file");
        if (inputFile == null ) {
            help(options);
            System.exit(0);
        }
        if ( !new File(inputFile).exists() ) {
            logger.error("file does not exist: " +inputFile);
            System.exit(0);
        }

        // example use of the classes
        // read the entire input file
        String fileText = new String(Files.readAllBytes(Paths.get(inputFile)));

        // setup Vader
        Vader vader = new Vader();
        vader.init(); // load vader

        // setup nlp processor
        VaderNLP vaderNLP = new VaderNLP();
        vaderNLP.init(); // load open-nlp

        // parse the text into a set of sentences
        List<List<Token>> sentenceList = vaderNLP.parse(fileText);

        // apply vader analysis to each sentence
        for ( List<Token> sentence : sentenceList ) {
            VScore vaderScore = vader.analyseSentence(sentence);
            logger.info("sentence:" + Token.tokenListToString(sentence) );
            logger.info("Vader score:" + vaderScore.toString());
        }

    }

    /**
     * display help for the command line
     * @param options the options file of the command line system
     */
    private static void help(Options options){
        logger.error(options.getOption("file").getDescription());
    }

}

