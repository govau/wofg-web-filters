package com.funnelback.dta;

import java.util.*;
import org.junit.*;
import org.junit.Test;
import com.funnelback.filter.api.*;
import com.funnelback.filter.api.documents.*;
import com.funnelback.filter.api.filters.*;
import com.funnelback.filter.api.mock.*;
import com.google.common.collect.ListMultimap;
import static com.funnelback.filter.api.DocumentType.*;

// Jsoup imports
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

// provide JSON escape function
import org.json.simple.JSONValue;

/**
 * Extracts metadata from a document and dumps it to a log file suitable for loading into Elastic Search
 */
@groovy.util.logging.Log4j2
public class WriteJSON implements StringDocumentFilter {

    // Content auditor field names
    def caFields=["X-Funnelback-Document-Fixer-Modified","X-Funnelback-Title-For-Duplicate-Detection","X-Funnelback-Content-Generator-Edit","X-Funnelback-Rounded-Flesch-Kincaid-Readability-Grade","X-Funnelback-Total-Request-Time-MS","X-Funnelback-Total-Request-Time-Band-MS","X-Funnelback-Undesirable-Text","X-Funnelback-Raw-Flesch-Kincaid-Readability-Grade","X-Funnelback-Pristine-Title"]

    // Domain to portfolio mappings (populated in constructor)
    def portfolioMapping = [:]
    // GA 360 IDS
    def ga360 = []

    def today = new Date().format('yyyy-MM-dd')

    def timestamp = new Date().getTime()

    def jsonLogFile

    public WriteJSON(File searchHome, String collectionName) {
        // Open a log file to write the JSON
        jsonLogFile = new File(searchHome.getAbsolutePath()+"/data/"+collectionName+"/log/metadata_report-"+timestamp+".json").newWriter()
        println ("Writing output to "+searchHome.getAbsolutePath()+"/data/"+collectionName+"/log/metadata_report-"+timestamp+".json")

        // Read the portfolio mappings and load these into the filter
        def pmFile = new File(searchHome.getAbsolutePath()+"/conf/"+collectionName+"/portfolio.mappings.generated")

        pmFile.readLines().each() {
            def pm = it.split("\t")
            portfolioMapping[pm[0]]=pm[1]
        }

        // Read the GA360 IDs and load these into the filter
        def ga360File = new File(searchHome.getAbsolutePath()+"/conf/"+collectionName+"/dta-gsa360.csv")

        ga360File.readLines().each() {
            def ga360line = it.split(",")
            if ((ga360line.size()>1) && (ga360line[1].startsWith("UA-"))) {
                ga360+=[ga360line[1]]
            }
        }


    }


    public PreFilterCheck canFilter(NoContentDocument document, FilterContext context) {
        return PreFilterCheck.ATTEMPT_FILTER;
    }

    @Override
    public FilterResult filterAsStringDocument(StringDocument document, FilterContext context) throws RuntimeException,
        FilterException {

        // Helper function to sanitise the metadata keys, replacing special chars with underscores
        def sanitizeKey = { it.replaceAll(~/[.: ]+/,"_").toLowerCase()}

        // Variable to hold the JSON output for this document.
        String jsonRecord = "{"

        def url = document.getURI().toASCIIString()
        def domain = document.getURI().getHost()

        // Calculate the TLD - ASSUMES no non AU country domains
        def tld
        if (domain.endsWith("csiro.au")) {
            tld = "csiro.au"
        }
        else if (domain.endsWith(".au")) {
        // keep X.X.au
            tld = domain
            tld=tld.replaceAll(/.+?\.(.+\.au$)/,'$1')
        }
        else {
        // keep X.X
            tld = domain
            tld=tld.replaceAll(/.+?\.(.+$)/,'$1')
        }

        // Insert the document's URL into the JSON        
        jsonRecord += "\"DOCURL\":\""+JSONValue.escape(url)+"\","
        jsonRecord += "\"HOST\":\""+JSONValue.escape(domain)+"\","
        jsonRecord += "\"TLD\":\""+tld+"\","
        jsonRecord += "\"PORTFOLIO\":\""+portfolioMapping[tld]+"\","

        // Jsoup process the document to extract the metadata
        String documentContent = document.getContentAsString()

        //if ((documentContent != null) && (documentContent != "")) {
        try {
            Document doc=Jsoup.parse(documentContent)

            // Map to collect together all the in-page metadata. A map is used as an intermediate step
            // to ensure that duplicate keys are combined into a list of values.
            def pageMetadata = [:].withDefault{key -> return []}

            try {
                // Process <meta> fields, combine identical keys into lists
                doc.select("meta").each() { metaField ->
                    if (metaField.hasAttr("name")) {
                        // Content auditor injected metadata
                        if (caFields.contains(metaField.attr("name"))) {
                            pageMetadata["ca_"+sanitizeKey(metaField.attr("name"))] += [metaField.attr("content")]
                        } 
                        // Standard in-page <meta> name/content tags (prefix with _m_)
                        else {
                            // Check for GA360 GA-IDs
                            if (metaField.attr("name") == "X-DTA-GA-ID") {
                                if (ga360.contains(metaField.attr("content"))) {
                                    pageMetadata["m_x-dta-ga360-id"].add(metaField.attr("content"))
                                }
                            }
                            pageMetadata["m_"+sanitizeKey(metaField.attr("name"))].add(metaField.attr("content"))
                        }
                    }
                    // <meta> property/content tags
                    else if (metaField.hasAttr("property")) {
                        pageMetadata["mp_"+sanitizeKey(metaField.attr("property"))].add(metaField.attr("content"))
                    }
                    // <meta> http-equiv/content tags
                    else if (metaField.hasAttr("http-equiv")) {
                        pageMetadata["mh_"+sanitizeKey(metaField.attr("http-equiv"))].add(metaField.attr("content"))
                    }
                }
            } catch (e) {
                println("Error scraping metadata from  '"+url+"' ["+e+"]")
            }

            // Create JSON for the in-page metadata. Each key will have an array of 1 or more values
            pageMetadata.sort().each {String k, List v ->
                String vals = ""

                Iterator vit = v.sort().iterator()
                while (vit.hasNext()) {
                    String val = (String) vit.next()

                    vals+="\""+JSONValue.escape(val)+"\""
                    if (vit.hasNext()) {
                        vals+=","
                    }
                }
                jsonRecord += "\""+JSONValue.escape(k)+"\":["+vals+"],"
            }        
        } catch(e) {
            println("Error parsing content from '"+url+"' ["+e+"]")
        }
        
        // Process the HTTP header metadata, as well as 'header style' metadata injected by the 
        // content and accessibility auditor filters
        ListMultimap<String, String> metadata = document.getCopyOfMetadata();

        Set<String> keySet = metadata.keySet().sort();
        Iterator keyIterator = keySet.iterator();
        while (keyIterator.hasNext()) {
            def valuestring ="";
            String key = (String) keyIterator.next();
            Collection<String> values = metadata.get(key).sort();

            Iterator valueIterator = values.iterator()
            while(valueIterator.hasNext()) {
                String value = (String) valueIterator.next();
                valuestring += "\""+JSONValue.escape(value)+"\""
                if (valueIterator.hasNext()) {
                    valuestring +=","
                }
            }

            // Process HTTP headers.

            // Accessibility auditor filter injects headers starting with X-Funnelback-AA-
            // prefix with _aa_
            if (key.startsWith("X-Funnelback-AA-")) {
                jsonRecord += "\"aa_"+JSONValue.escape(sanitizeKey(key))+"\": ["+valuestring+"], "
            }
            // General Funnelback-injected header metadata (prefix with _fb_)
            else if (key.startsWith("X-Funnelback-")) {
                jsonRecord += "\"fb_"+JSONValue.escape(sanitizeKey(key))+"\": ["+valuestring+"], "
            }
            // Other HTTP headers (prefix with _h_)
            else
            {
                jsonRecord += "\"h_"+JSONValue.escape(sanitizeKey(key))+"\": ["+valuestring+"], "
            }
        }


        // Add a field to indicate that these records all belong to the same report.
        jsonRecord += "\"REPORT_ID\":\""+context.getConfigValue("dta.report-id").orElse(today)+"\" "
        jsonRecord += "}"

        // Write the JSON record to the log file
        jsonLogFile << jsonRecord+'\n'
        jsonLogFile.flush()

        return FilterResult.of(document);
    }
}


