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

import java.util.List;

/**
 * Created by Rock de Vocht @ booktrack.com on 14/03/16.
 *
 * a token, usually a single word, with POS (part of speech) tag
 *
 */
public class Token {

    private String value; // the value of the token
    private String posTag; // the part of speech tag (POS) of this item of text
    private double wordScore; // the vader individual word score

    public Token() {
    }

    // pretty print
    public String toString() {
        return value + ":" + posTag;
    }

    /**
     * conver a list of tokens to a readable string / very crude
     * for demo purposes
     * @param tokenList the list to convert
     * @return a string for the list
     */
    public static String tokenListToString( List<Token> tokenList ) {
        if ( tokenList != null ) {
            StringBuilder sb = new StringBuilder();
            for ( Token token : tokenList ) {
                sb.append( token.getValue() ).append(" ");
            }
            return sb.toString();
        }
        return "";
    }

    public Token(String value, String posTag) {
        this.value = value;
        this.posTag = posTag;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getPosTag() {
        return posTag;
    }

    public void setPosTag(String posTag) {
        this.posTag = posTag;
    }


    public double getWordScore() {
        return wordScore;
    }

    public void setWordScore(double wordScore) {
        this.wordScore = wordScore;
    }

}



