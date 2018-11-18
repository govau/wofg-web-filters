package com.funnelback.CAFilters

import com.funnelback.common.filter.jsoup.*
import java.util.regex.*
import groovy.json.JsonSlurper

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;

import com.funnelback.common.config.Keys;
import com.google.common.io.Files;

/**
 * Implements a set of generic content checking filters
 *
 * @author plevan@funnelback.com
 * @author jgriffithshawking@funnelback.com
 * @author lnowak@funnelback.com
 */
@groovy.util.logging.Log4j2
public class CheckContent implements IJSoupFilter {

    // Object holding all the content filtering rules
    def contentRules

    def wordLists = [:]
    def validationPatterns

    @Override
    public void setup(SetupContext context) {

        // Read the content filtering rules from check-content.cfg
        def rulesFile = new File(context.getSearchHome().getAbsolutePath()+"/conf/"+context.getCollectionName()+"/check-content.cfg")

        contentRules = new JsonSlurper().parseFile(rulesFile, 'UTF-8')
println "Loaded check-content rules: "+contentRules


        // Read through collection.cfg and load in any required word list files
        for (String key : context.getConfigKeysWithPrefix("filter.check-content.word-list.")) {
            String source = key

            /** An efficient data structure for finding matches of a set of strings within some input. */
            Trie wordListTrie = new Trie().caseInsensitive().onlyWholeWords();

            def wordList = source.replace("filter.check-content.word-list.","")
 
            try {
                for (String line : Files.readLines(new File(context.getConfigSetting(key)), StandardCharsets.UTF_8)) {
                    line = line.trim();
                    
                    if (line.startsWith("#")) {
                        continue; // Skip comment lines
                    }
                    wordListTrie.addKeyword(line.toLowerCase());
                }
            } catch (IOException e) {
                log.error("Exception trying to read word list from " + source, e);
            }

            wordLists[wordList] = wordListTrie            
        }

        def patternsFile = new File(context.getSearchHome().getAbsolutePath()+"/conf/"+context.getCollectionName()+"/check-content.validation-patterns.cfg")

        validationPatterns = new JsonSlurper().parseFile(patternsFile, 'UTF-8') 
println "Loaded validation patterns rules: "+validationPatterns
/*
        def validation_patterns = [
            "NUMBER_AS_TEXT":~/\b(one|two|three|four|five|six|seven|eight|nine|ten)\b/,
            "PHONE_NUMBER_AU":~/(\b1[38]00\s+(?:\D|\d\D|\d\d\D))|(\b1[38]00\d)/,
            "URL":~/^(http:\/\/|https:\/\/)?(www.)?([a-zA-Z0-9]+).[a-zA-Z0-9]*.[a-z]{3}.?([a-z]+)?$/,
            "EMAIL_ADDRESS":~/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$/,
            "GA_TRACKING_ID":~/UA-\d{4,9}-\d{1,4}/
        ]
*/

    }

   @Override
   void processDocument(FilterContext context) {
        def doc = context.getDocument()
        def url = doc.baseUri()

        def wordsCount = { it.tokenize().size() }

        /*
         * Check functions
         */ 

        def check_funcs = [
            LENGTH_EQ_CHARS: { str, len -> str.length() == len },
            LENGTH_GT_CHARS: { str, len -> str.length() > len },
            LENGTH_LT_CHARS: { str, len -> str.length() < len },
            LENGTH_EQ_WORDS: { str, len -> wordsCount(str) == len },
            LENGTH_GT_WORDS: { str, len -> wordsCount(str) > len },
            LENGTH_LT_WORDS: { str, len -> wordsCount(str) < len },
            ENDS_WITH: { str, substr -> str.endsWith(substr) },
            STARTS_WITH: { str, substr -> str.startsWith(substr) },
            NOT_ENDS_WITH: { str, substr -> !str.endsWith(substr) },
            NOT_STARTS_WITH: { str, substr -> !str.startsWith(substr) },
            EQUALS: { s1, s2 -> s1 == s2},
            NOT_EQUALS: { s1, s2 -> s1 != s2 },
            CONTAINS: { str, substr -> str.contains(substr) },
            NOT_CONTAINS: { str, substr -> !str.contains(substr) },
            MATCHES: { str, pattern -> str =~ pattern },
            NOT_MATCHES: { str, pattern -> !(str =~ pattern) },
            FULLY_MATCHES: { str, pattern -> str ==~ pattern },
            NOT_FULLY_MATCHES: { str, pattern -> !(str ==~ pattern) }
        ]


        /* 
         * Test for the existence of an element
         * 
         * Params:
         *   selector: the jsoup selector to check the existence of
         *   metaField: Adds the metadata field with a value of true if the check was found, and <metaField>-count with a count of how many times the match was found
         *   extractField: acceptable values:
         *                 - CONTENT: use element content as the text for comparison and extraction
         *                 - ATTRIBUTE:<attribute name>: use <attribute name> as the text for comparison and extraction
         *   extractValue: Indicates if the value of the extractField should be extracted.
         *   extractMode: TEXT or HTML
         *   document: Jsoup object representing the document
         */

        def elementExists = { String selector, String metaField, String extractField, Boolean extractValue, String extractMode, document ->

            def els = document.select(selector)
            def extractAttr

            if ( extractField.startsWith("ATTRIBUTE") ) {
                extractAttr = extractField.replace("ATTRIBUTE:","")
            }


            if (els.size() > 0) {
                context.additionalMetadata.put(metaField, "true")
                context.additionalMetadata.put(metaField+"-count", els.size().toString())
                if ( extractValue ) {
                    els.each {
                        def text =""
                        if ( extractField.startsWith("ATTRIBUTE") ) {
                            // compare to the attribute's value
                            text = it.attr(extractAttr)
                        }
                        else {
                            // compare against the element value
                            if ( extractMode == "HTML" ) {
                                text = it.html()
                            }
                            else {
                                text = it.text()
                            }
                        }

                        context.additionalMetadata.put(metaField+"-value",text)
                    }
                }
            } 
        }

        /*
         * Test the length of an element's content
         *
         * Params:
         *   selector: the jsoup selector to check the content length of
         *   metaField: Adds the metadata field with a value of true if the check was found, and <metaField>-count with 
         *              a count of how many times the match was found
         *   comparator: compare function to use. Acceptable values are (where N is set using the length parameter):
         *               - LENGTH_EQ_CHARS: length of the selected value is equal to N characters
         *               - LENGTH_GT_CHARS: length of the selected value is greater than N characters
         *               - LENGTH_LT_CHARS: length of the selected value is less than N characters
         *               - LENGTH_EQ_WORDS: length of the selected value is equal to N words
         *               - LENGTH_GT_WORDS: length of the selected value is greated than N words
         *               - LENGTH_LT_WORDS: length of the selected value is less than N words
         *   compareField: acceptable values: 
         *                 - CONTENT: use element content as the text for comparison and extraction 
         *                 - ATTRIBUTE:<attribute name>: use <attribute name> as the text for comparison and extraction
         *   length: length value used for the comparison
         *   extractValue: Indicates if the value of the compareField should be extracted.
         *   extractMode: TEXT or HTML
         *   document: Jsoup object representing the document
         */
        def checkLength = { String selector, String comparator, String compareField, Integer length, String metaField, Boolean extractValue, String extractMode, document ->

            def els = document.select(selector)
            def checkFoundCount = 0
            def compareAttr

            if ( compareField.startsWith("ATTRIBUTE") ) {
                compareAttr = compareField.replace("ATTRIBUTE:","")
            }

            if (comparator in check_funcs) {
                els.each {

                    def text =""
                    def html = ""
                    if ( compareField.startsWith("ATTRIBUTE") ) {
                        // compare to the attribute's value
                        text = it.attr(compareAttr)
                    }
                    else {
                        // compare against the element value

                        text = it.text()
                        html = it.html()
                    }

                    if (check_funcs[comparator](text, length.toInteger())) {
                        checkFoundCount++
                        if ( extractValue ) {
                            els.each {
                                if ( extractMode == "HTML" ) {
                                    context.additionalMetadata.put(metaField+"-value",html)
                                }
                                else {
                                    context.additionalMetadata.put(metaField+"-value",text)
                                }
                                if (comparator.endsWith("_CHARS")) {
                                    context.additionalMetadata.put(metaField+"-size",text.length().toString())
                                }
                                else if (comparator.endsWith("_WORDS")) {
                                    context.additionalMetadata.put(metaField+"-size",wordsCount(text).toString())
                                }
                            }
                        }
                    }
                }
            }
            if (checkFoundCount > 0) {
                context.additionalMetadata.put(metaField, "true")
                context.additionalMetadata.put(metaField+"-count", checkFoundCount.toString())
            }
        }

        /*
         * Tests an element's content
         *
         * Params:
         *   selector: the jsoup selector to check the content of
         *   metaField: Adds the metadata field with a value of true if the check was found, and <metaField>-count with a 
         *              count of how many times the match was found
         *   comparator: compare function to use. Acceptable values are (where <comparison text> is set using the compareText parameter):
         *               - ENDS_WITH: compareField value ends with <comparison text>
         *               - STARTS_WITH: compareField value starts with <comparison text>
         *               - NOT_ENDS_WITH: compareField value does not end with <comparison text>
         *               - NOT_STARTS_WITH: compareField value does not start with <comparison text>
         *               - EQUALS: compareField value equals <comparison text>
         *               - NOT_EQUALS: compareField value does not equal <comparison text>
         *               - CONTAINS: compareField value contains <comparison text>
         *               - NOT_CONTAINS: compareField value does not contain <comparison text>
         *               - MATCHES: compareField value includes text that matches the regular expression defined in <comparison text>
         *               - NOT_MATCHES: compareField value does not include text that matches the regular expression defined in <comparison text>
         *               - FULLY_MATCHES: compareField value fully matches the regular expression defined in <comparison text>
         *               - NOT_FULLY_MATCHES: compareField value does not fully match the regular expression defined in <comparison text>
         *   compareField: acceptable values:
         *                 - CONTENT: use element content as the text for comparison and extraction
         *                 - ATTRIBUTE:<attribute name>: use <attribute name> as the text for comparison and extraction
         *   compareText: value used for the comparison
         *   extractValue: Indicates if the value of the compareField should be extracted.
         *   extractMode: TEXT or HTML
         *   document: Jsoup object representing the document
         */

        def checkContent = { String selector, String comparator, String compareField, String compareText, String metaField, Boolean extractValue, String extractMode,  document ->

            def els = document.select(selector)
            def checkFoundCount = 0
            def compareAttr

            if ( compareField.startsWith("ATTRIBUTE") ) {
                compareAttr = compareField.replace("ATTRIBUTE:","")      
            }

            if (comparator in check_funcs) {
                els.each {
                    def text =""
                    if ( compareField.startsWith("ATTRIBUTE") ) {
                        // compare to the attribute's value
                        text = it.attr(compareAttr)
                    }
                    else {
                        // compare against the element value
                        if ( extractMode == "HTML" ) {
                            text = it.html()
                        }
                        else {
                            text = it.text()
                        }
                    }
                    if (check_funcs[comparator](text, compareText)) {
                        checkFoundCount++
                        if ( extractValue ) {
                            context.additionalMetadata.put(metaField+"-value",text)
                        }
                    }
                }
            } 
            if (checkFoundCount > 0) {
                context.additionalMetadata.put(metaField, "true")
                context.additionalMetadata.put(metaField+"-count", checkFoundCount.toString())
            }

        }


        /*
         * Check an element's content to see if it includes any words from a specified words list
         *
         * Params:
         *   selector: the jsoup selector to check the content of
         *   metaField: Adds the metadata field containing a list of the found words, and <metaField>-count with a
         *              count of how many times the match was found
         *   wordList: The word list to check against (word list must be loaded via the filter.check-content.word-list-X 
         *             collection.cfg parameter)
         *   document: Jsoup object representing the document
         *
         * Code is based on the undesirable text core filter code, extended to support multiple word lists.
         */

        def wordListCompare = { String selector, String wordList, String metaField, document ->

            def els = document.select(selector)
            def checkFoundCount = 0

            els.each {
                // Compare each matched selector against the wordlist
                for (Emit emit : wordLists[wordList].parseText(context.getDocument().text())) {

                    context.getAdditionalMetadata().put(metaField, emit.getKeyword());
                    checkFoundCount++
                }
            }    
            if (checkFoundCount > 0) {
                context.additionalMetadata.put(metaField+"-count", checkFoundCount.toString())
            }

        }

        /*
         * Validate an element's content using a library pattern
         *
         * Params:
         *   selector: the jsoup selector to check the content of
         *   metaField: Adds the metadata field with a value of true if the check was found, and <metaField>-count with a 
         *              count of how many times the match was found
         *   comparator: compare function to use. Acceptable values are (where <comparison text> is set using the compareText parameter):
         *               - MATCHES: compareField value includes text that matches the regular expression defined in <comparison text>
         *               - NOT_MATCHES: compareField value does not include text that matches the regular expression defined in <comparison text>
         *               - FULLY_MATCHES: compareField value fully matches the regular expression defined in <comparison text>
         *               - NOT_FULLY_MATCHES: compareField value does not fully match the regular expression defined in <comparison text>
         *   compareField: acceptable values:
         *                 - CONTENT: use element content as the text for comparison and extraction
         *                 - ATTRIBUTE:<attribute name>: use <attribute name> as the text for comparison and extraction
         *   matchPattern: Regex matcher as defined in the validation patterns
         *   document: Jsoup object representing the document
         */

        // NOTE: VALIDATION IS PERFORMED BY THE CheckContent function


        /*
         * Run through each rule and call the relevant check function
         */

        try {

            // FOR each rule
            contentRules.each {
                if (it.check == "ELEMENT_EXISTENCE") {
                    // Set content extraction as the default
                    def ef = "CONTENT"
                    if ( it.extractField != null ) {
                        ef = it.extractField
                    }
                    elementExists(it.selector,it.metaField,ef,it.extractValue,it.extractMode,doc)
                }
                else if (it.check == "ELEMENT_LENGTH") {
                    // Set content extraction as the default
                    def ef = "CONTENT"
                    if ( it.compareField != null ) {
                        ef = it.compareField
                    }
                    checkLength(it.selector,it.comparator,ef,it.length,it.metaField,it.extractValue,it.extractMode,doc)
                }
                else if (it.check == "ELEMENT_CONTENT") {
                    // Set content extraction as the default
                    def ef = "CONTENT"
                    if ( it.compareField != null ) {
                        ef = it.compareField
                    }
                    checkContent(it.selector,it.comparator,ef,it.compareText,it.metaField,it.extractValue,it.extractMode,doc)
                }
                else if (it.check == "ELEMENT_VALIDATE") {
                    // Set content extraction as the default
                    def ef = "CONTENT"
                    if ( it.compareField != null ) {
                        ef = it.compareField
                    }
                    // Special case of check content that applies pre-defined validation patterns
                    checkContent(it.selector,it.comparator,ef,validationPatterns[it.matchPattern],it.metaField,it.extractValue,it.extractMode,doc)
                }
                else if (it.check == "WORD_LIST_COMPARE") {
                    wordListCompare(it.selector,it.wordList,it.metaField,doc)
                }
            }

        } catch (e) {
            log.error("Error checking '{}'", url, e)
        }
    }
}            

