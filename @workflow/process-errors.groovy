/**
 * Processes the URL errors log file and produces a summary of errors encountered, and reports the largest file size encountered.
 * Also writes out all the items to a JSON format log. (url_errors.json)
 *
 * @author Peter Levan <plevan@funnelback.com>
 *
 * USAGE: groovy -cp '/opt/funnelback/lib/java/all/*' process-errors.groovy $SEARCH_HOME $COLLECTION_NAME $CURRENT_VIEW
 */
import com.funnelback.common.*;
import com.funnelback.common.config.*;
import com.funnelback.common.utils.*;
import java.net.URL;

// provide JSON escape function
import org.json.simple.JSONValue;

// Get our arguments, SEARCH_HOME first then the collection id
def searchHome = new File(args[0])
def searchHomeString = args[0]
def collection = args[1]
def view = args[2]

errors = urlErrorsLog = new File(searchHomeString+"/data/"+collection+"/"+view+"/log/url_errors.log").readLines()

// Create a configuration object to read collection.cfg
def config = new NoOptionsConfig(searchHome, collection)

// Define output file
//def errorReportFile = new File(searchHomeString+"/data/"+collection+"/"+view+"/log/url_errors.json").newWriter()
def errorReportFile = new File(searchHomeString+"/data/"+collection+"/log/error_report.json").newWriter()

// Map to track occurrences of various errors
def errorCounter = [:].withDefault { key -> key : 0}

def maxFileSize = 0

def i = 1

println ("Generating error summary")

errors.each { String error ->

    def jsonLog = ""

/*
Command used to find new error messages - run this command if you find unknown errors reported and then add checks as required (and also add to this command):
egrep -v "(400 Bad Request|401 Unauthorized|403 Forbidden|403 FORBIDDEN|404 Not Found|405 Method Not Allowed|500 Internal Server Error|500 Service unavailable|503 Service Unavailable|Contains robots NOFOLLOW tag|Can't scan root page|Can't get text signature|404 NOT FOUND|503 Service Temporarily Unavailable|Crawl URLs: java.lang.RuntimeException: java.lang.IllegalArgumentException: Metadata keys must not contain white space invalid key is:|500 500|404 |Crawl URLs: java.lang.RuntimeException: java.net.URISyntaxException: Illegal character in query|Exceeds max_download_size:|500 Layout not found:|400 Value for one of the query parameters specified in the request URI is invalid.|Crawl URLs: java.lang.NullPointerException|204 No Content|401 Authorization Required|Crawl URLs: java.lang.StackOverflowError|Link Extraction: java.net.MalformedURLException: no protocol:|500 |Link Extraction: java.net.MalformedURLException: For input string:|Crawl URLs: java.lang.IllegalArgumentException: unexpected url:|410 Gone|415 UNSUPPORTED MEDIA TYPE|302 Found|524 |Crawl URLs: java.lang.RuntimeException: java.net.URISyntaxException: Illegal character in path|303 See Other|504 GATEWAY_TIMEOUT|403 Access Denied/Forbidden|502 Proxy Error|301 Moved Permanently|504 Gateway Time-out|502 Bad Gateway|400 400|Link Extraction: java.net.MalformedURLException: Invalid host:|409 Conflict|400 BAD REQUEST|504 Gateway Time-out|400 Bad request|400 BAD_REQUEST|Crawl URLs: java.lang.RuntimeException: java.lang.ClassCastException: org.apache.pdfbox.cos.COSInteger cannot be cast to org.apache.pdfbox.cos.COSObject|Connection establishment timed out|unknown error\]|Read timed out|Parser timed out|Connection reset|Premature EOF encountered|SSLHandshakeException|\[490 |\[503 |\[302 |Net Error: www|\[410 |No route to host)|Crawl URLs: java.lang.RuntimeException: java.lang.NullPointerException|Crawl URLs: java.lang.OutOfMemoryError: Java heap space|Crawl URLs: java.lang.RuntimeException: java.lang.IllegalArgumentException: String must not be empty|Net Error: Unsupported protocol|Connection has been shutdown: javax.net.ssl.SSLException: Received fatal alert: close_notify|Reached store limit|Crawl URLs: java.lang.RuntimeException: groovy.lang.MissingPropertyException: No such property: Thumbnails for class: filter.ResultThumb" ./url_errors.log | less
*/
    def m
    // HTTP errors detected
    // Error messages in format [<3 digit HTTP status code> <HTTP status message>]
    if ((m = error =~ /E\s(.+?)\s\[(\d{3})\s(.*?)\]/)) {

        // Increment the relevant HTTP error counter
        errorCounter["e_http-"+m[0][2].toString()]++

        // Add the URL to a the JSON log
        jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"HTTP status code error\", \"e_type_pretty\":\"HTTP "+JSONValue.escape(m[0][2])+"\", \"e_type\":\"HTTP error\", \"e_http-status-code\":\""+JSONValue.escape(m[0][2])+"\", \"e_msg\":\""+JSONValue.escape(m[0][3])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
    
    }
    else if ((m = error =~ /E\s(.+?)\s\[(Crawl URLs: .+?)\]/)) {
        // Java errors

         switch (m[0][2]) {

            case { it.startsWith("Crawl URLs: java.lang.IllegalArgumentException: unexpected url") }:
                errorCounter["cr_unexpected-url"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Web crawler error\", \"e_type_pretty\":\"Unexpected URL\", \"e_type\":\"cr_unexpected-url\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break

            case { it.startsWith("Crawl URLs: java.lang.NullPointerException") || it.startsWith("Crawl URLs: java.lang.RuntimeException: java.lang.NullPointerException") }:
                errorCounter["cr_null-pointer-exception"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Web crawler error\", \"e_type_pretty\":\"NullPointerException\", \"e_type\":\"cr_null-pointer-exception\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break

            case { it.startsWith("Crawl URLs: java.lang.RuntimeException: java.lang.ClassCastException: org.apache.pdfbox.cos.COSInteger cannot be cast to org.apache.pdfbox.cos.COSObject") }:
                errorCounter["cr_class-cast-exception"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Web crawler error\", \"e_type_pretty\":\"PDF box class cast exception\", \"e_type\":\"cr_class-cast-exception\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break

            case { it.startsWith("Crawl URLs: java.lang.RuntimeException: java.lang.IllegalArgumentException: Metadata keys must not contain white space invalid key is:") }:
                errorCounter["cr_metadata-key-containing-white-space"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Web crawler error\", \"e_type_pretty\":\"Metadata key containing white space\", \"e_type\":\"cr_metadata-key-containing-white-space\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break

            case { it.startsWith("Crawl URLs: java.lang.RuntimeException: java.lang.IllegalArgumentException: String must not be empty") }:
                errorCounter["cr_metadata-string-must-not-be-empty"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Web crawler error\", \"e_type_pretty\":\"String must not be empty\", \"e_type\":\"cr_metadata-string-must-not-be-empty\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break

            case { it.startsWith("Crawl URLs: java.lang.RuntimeException: java.net.URISyntaxException: Illegal character in path") }:
                errorCounter["cr_url-syntax-illegal-character-in-path"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Web crawler error\", \"e_type_pretty\":\"Illegal character in path\", \"e_type\":\"cr_url-syntax-illegal-character-in-path\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break
                
            case { it.startsWith("Crawl URLs: java.lang.RuntimeException: java.net.URISyntaxException: Illegal character in query") }:
                errorCounter["cr_url-syntax-illegal-character-in-query"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Web crawler error\", \"e_type_pretty\":\"Illegal character in query string\", \"e_type\":\"cr_url-syntax-illegal-character-in-query\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break
        
            case { it.startsWith("Crawl URLs: java.lang.RuntimeException: groovy.lang.MissingPropertyException: No such property") }:
                errorCounter["cr_url-missing-property"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Web crawler error\", \"e_type_pretty\":\"No such property\", \"e_type\":\"cr_url-missing-property\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break

            case { it.startsWith("Crawl URLs: java.lang.OutOfMemoryError: Java heap space") }:
                errorCounter["cr_java-heap-space"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Web crawler error\", \"e_type_pretty\":\"Out of memory\", \"e_type\":\"cr_java-heap-space\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break
                
            case { it.startsWith("Crawl URLs: java.lang.OutOfMemoryError: GC overhead limit exceeded") }:
                errorCounter["cr_java-gc-overhead"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Web crawler error\", \"e_type_pretty\":\"GC overhead limit exceeded\", \"e_type\":\"cr_java-gc-overhead\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break

            case { it.startsWith("Crawl URLs: java.lang.StackOverflowError") }:
                errorCounter["cr_stack-overflow"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Web crawler error\", \"e_type_pretty\":\"StackOverflowError\", \"e_type\":\"cr_metadata-key-containing-white-space\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break

            default:
                errorCounter["cr_unknown"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Web crawler error\", \"e_type_pretty\":\"Unknown\", \"e_type\":\"cr_unknown\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break
        }
    }
    else if ((m = error =~ /E\s(.+?)\s\[(Link Extraction: .+?)\]/)) {
        // Link extraction errors

        switch (m[0][2]) {

            case { it.startsWith("Link Extraction: java.net.MalformedURLException: For input string:") }:
                errorCounter["le_malformed-input-string"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Link extraction error\", \"e_type_pretty\":\"Malformed input string\", \"e_type\":\"le_malformed-input-string\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break

            case { it.startsWith("Link Extraction: java.net.MalformedURLException: Invalid host:") }:
                errorCounter["le_invalid-host"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Link extraction error\", \"e_type_pretty\":\"Inavlid host\", \"e_type\":\"le_invalid-host\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break

            case { it.startsWith("Link Extraction: java.net.MalformedURLException: no protocol:") }:
                errorCounter["le_no-protocol"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Link extraction error\", \"e_type_pretty\":\"No protocol\", \"e_type\":\"le_no-protocol\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break               

            default:
                errorCounter["le_unknown"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Link extraction error\", \"e_type_pretty\":\"Unknown\", \"e_type\":\"le_unknown\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break
        }       
    }
    else if ( (m = error =~ /E\s(.+?)\s\[Net Error:\s(.+?)\]/) ) {
        // Net errors

        switch (m[0][2]) {

            case "Connection establishment timed out":
                errorCounter["ne_connection-timeout"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Network error\", \"e_type_pretty\":\"Connection timeout\", \"e_type\":\"ne_connection-timeout\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break

            case "Read timed out":
                errorCounter["ne_read-timeout"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Network error\", \"e_type_pretty\":\"Read timeout\", \"e_type\":\"ne_read-timeout\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break

            case "Connection reset":
                errorCounter["ne_connection-reset"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Network error\", \"e_type_pretty\":\"Connection reset\", \"e_type\":\"ne_connection-reset\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break   

            case "No route to host":
                errorCounter["ne_no-route-to-host"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Network error\", \"e_type_pretty\":\"No route to host\", \"e_type\":\"ne_no-route-to-host\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break   

            case { it.startsWith("Unsupported protocol") }:
                errorCounter["ne_unsupported-protocol"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Network error\", \"e_type_pretty\":\"Unsupported protocol\", \"e_type\":\"ne_unsupported-protocol\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break            

            case "Premature EOF encountered":
                errorCounter["ne_premature-eof"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Network error\", \"e_type_pretty\":\"Encountered premature EOF\", \"e_type\":\"ne_premature-eof\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break   

            case { it.endsWith("unknown error") }:
                errorCounter["ne_unknown"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Network error\", \"e_type_pretty\":\"Unspecified network error\", \"e_type\":\"ne_unknown\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break

            case "Connection has been shutdown: javax.net.ssl.SSLHandshakeException: Received fatal alert: handshake_failure":
                errorCounter["ne_ssl-handshake"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Network error\", \"e_type_pretty\":\"SSL handshake failure\", \"e_type\":\"ne_ssl-handshake\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break

            case "Connection has been shutdown: javax.net.ssl.SSLException: Received fatal alert: close_notify":
                errorCounter["ne_ssl-close-notify"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Network error\", \"e_type_pretty\":\"SSL close notify\", \"e_type\":\"ne_ssl-close-notify\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break

            default:
                errorCounter["ne_other"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"Network error\", \"e_type_pretty\":\"Unknown\", \"e_type\":\"ne_other\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break
        }       
    }   
    else if ( (m = error =~ /E\s(.+?)\s\[Exceeds max_download_size: (\d+)\]/) ) {
        // Max file size

        errorCounter["e_file-size"]++
        
        // extract the size
        def size = m[0][2].toInteger()
        
        // Update the max file size encountered
        maxFileSize = Math.max(maxFileSize,size)
        jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"General error\", \"e_type_pretty\":\"Rejected due to configured file size limit\", \"e_type\":\"e_file-size\", \"e_msg\":\"Exceeds maximum file size\", \"file-size\":\""+JSONValue.escape(size.toString())+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
    }   
    else if ( (m = error =~ /E\s(.+?)\s\[(.+?)\]/) ) {

        // General errors

        switch (m[0][2]) {

            case "Contains robots NOFOLLOW tag":
                errorCounter["e_robots-nofollow"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"General error\", \"e_type_pretty\":\"Skipped due to robots no-follow tag\", \"e_type\":\"e_robots-nofollow\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break

            case "Can't scan root page":
                errorCounter["e_root-page"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"General error\", \"e_type_pretty\":\"Can't scan root page\", \"e_type\":\"e_root-page\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break

            case "Can't get text signature":
                errorCounter["e_text-signature"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"General error\", \"e_type_pretty\":\"Can't get text signature\", \"e_type\":\"e_text-signature\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break

            case "Parser timed out":
                errorCounter["e_parser-timeout"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"General error\", \"e_type_pretty\":\"Parser timed out\", \"e_type\":\"e_parser-timeout\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break

            case "Reached store limit":
                errorCounter["e_store-limit"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"General error\", \"e_type_pretty\":\"Reached store limit\", \"e_type\":\"e_store-limit\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break

            default:
                errorCounter["e_unknown"]++
                jsonLog += "{\"DOCURL\":\""+JSONValue.escape(m[0][1])+"\", \"e_group\":\"General error\", \"e_type_pretty\":\"Unknown\", \"e_type\":\"e_unknown\", \"e_msg\":\""+JSONValue.escape(m[0][2])+"\", \"REPORT_ID\":\""+config.value("dta.report-id")+"\"}\n"
                break               
        }
    }
    errorReportFile<< jsonLog
    i++
}

println (" - Processed "+i+" errors\n")

errorReportFile.close()

println "Error summary"
println "-------------"

errorCounter.sort().each { k, v ->

        if ( k.startsWith("ne_") ) {
            println "Network error ["+k.substring(3)+"]: "+v
        }
        else if ( k.startsWith("cr_") ) {
            println "Crawler error ["+k.substring(3)+"]: "+v
        }
        else if ( k.startsWith("le_") ) {
            println "Link extraction error ["+k.substring(3)+"]: "+v
        }
        else if ( k.startsWith("e_http-") ) { 
            println "HTTP error ["+k.substring(7)+"]: "+v
        }
        else if ( k.startsWith("e_") ) {
            println "General error ["+k.substring(2)+"]: "+v
        }   
}


println "\nOther information"
println "-----------------"

// Calculate max file size in MB
def mfs = maxFileSize / 1024 / 1024

if (mfs>0) {
    println "\nLargest file size encountered: "+mfs.toInteger().toString()+"MB."
    println " - The crawler.max_download_size setting can be used to set the accepted limit."
}


