import com.funnelback.common.*;
import com.funnelback.common.config.*;
import com.funnelback.common.utils.*;
import java.net.URL;

// CSV parser imports
import org.apache.commons.csv.CSVParser
import static org.apache.commons.csv.CSVFormat.*

// Get our arguments, SEARCH_HOME first then the collection id
def searchHome = new File(args[0])
def searchHomeString = args[0]
def collection = args[1]

// Create a configuration object to read collection.cfg
def config = new NoOptionsConfig(searchHome, collection)

// Read configuration or fall back to default values
def format = config.value("csv.format", "csv")
def csvHeader = config.valueAsBoolean("csv.header", true)
def csvCustomHeader = config.value("csv.header.custom")
def csvEncoding = config.value("csv.encoding", "UTF-8")
def csvDebug = config.valueAsBoolean("csv.debug", false)

// Flag to indicate if AGOR should be downloaded
def downloadAgor = config.valueAsBoolean("agor.download",true)

// Define output files
def agorFile
if (downloadAgor) {
    agorFile = new File(searchHomeString+"/conf/"+collection+"/agor.csv").newOutputStream()
}
def startUrlsFile = new File(searchHomeString+"/conf/"+collection+"/collection.cfg.start.urls.generated").newWriter()
def portfolioMappingsFile = new File(searchHomeString+"/conf/"+collection+"/portfolio.mappings.generated").newWriter()
def siteProfilesFile = new File(searchHomeString+"/conf/"+collection+"/site_profiles.cfg.generated").newWriter()
def configLines = new File(searchHomeString+"/conf/"+collection+"/collection.cfg").readLines()
def collectionCfg = new File(searchHomeString+"/conf/"+collection+"/collection.cfg.generated").newWriter()
def staticPortfolioMappings = new File(searchHomeString+"/conf/"+collection+"/supplementalDomainMappings.csv").getText(csvEncoding)
def configMap = [:]

// Read collection.cfg into a Map
configLines.each {
    if (!it.startsWith('#')) {
        if((m = it =~ /(.+?)=(.*)$/)) {
            configMap[m[0][1]] = m[0][2]
        }
    }
}

// define the available fileformat based on supported types.
// See https://commons.apache.org/proper/commons-csv/archives/1.0/apidocs/org/apache/commons/csv/CSVFormat.html

def csvFormats = ["csv": DEFAULT, "xls": EXCEL, "rfc4180": RFC4180, "tsv": TDF, "mysql": MYSQL]

def domainPortfolioMap = [:]
def seedList = [:]
def nonGovAuList = [:]

// Process the supplemental mappings.  We do this before doing AGOR so that any portfolio mappings in AGOR trump the values in the supplemental file.

println "Processing supplemental CSV mappings"

staticPortfolioMappings.replaceAll("[\n\r]+","\n")
def supplementalCsv = null
    
if (csvHeader) {
    // use the header row to define fields
   supplementalCsv = CSVParser.parse(staticPortfolioMappings, csvFormats[format].withHeader())
} 
else {
    if (csvCustomHeader != null) {
        // use field definitions
        supplementalCsv = CSVParser.parse(staticPortfolioMappings, csvFormats[format].withHeader(csvCustomHeader.split(",")))
    }
    else {
        supplementalCsv = CSVParser.parse(staticPortfolioMappings, csvFormats[format])
    }
}

for (record in supplementalCsv.iterator()) {
    domainPortfolioMap[record.get("Domainname.gov.au")]=record.get("Portfolio")
    seedList["http://"+record.get("Domainname.gov.au")]=0
}

// Process the AGOR CSV

def sources = config.value("dta.agor.sourceurl").split(",")
sources.each { source ->
    println "Gathering CSV from ${source}"

    if (downloadAgor) {
        // Save a copy of the AGOR data
        agorFile << new URL(source).openStream()
        agorFile.close()
    }
    else {
        println "Using pre-downloaded agor.csv"
    }

    def csvText = new File(searchHomeString+"/conf/"+collection+"/agor.csv").getText(csvEncoding)

    // Remove blank lines
    csvText = csvText.replaceAll("[\n\r]+","\n")
    def csv = null
    if (csvHeader) {
        // use the header row to define fields
        csv = CSVParser.parse(csvText, csvFormats[format].withHeader())
    } else {
        if (csvCustomHeader != null) {
            // use field definitions
            csv = CSVParser.parse(csvText, csvFormats[format].withHeader(csvCustomHeader.split(",")))
        }
        else {
            csv = CSVParser.parse(csvText, csvFormats[format])
        }
    }


    for (record in csv.iterator()) {
        def fields = record.toMap()

        // If the record has a URL
        if (record.get("Website Address") != "") {

            def urltext = record.get("Website Address")
println "Processing "+urltext

            if (!urltext.startsWith("http")) {
                urltext = "http://"+urltext
            }
            def url = new URL(urltext)
            def domain = url.getHost()
            def protocol = url.getProtocol()
            def port = url.getPort()
            def tld
            // If the URL is state gov or empty
            if ((domain.endsWith(".qld.gov.au"))
             || (domain.endsWith(".nsw.gov.au"))
             || (domain.endsWith(".act.gov.au"))
             || (domain.endsWith(".vic.gov.au"))
             || (domain.endsWith(".tas.gov.au"))
             || (domain.endsWith(".sa.gov.au"))
             || (domain.endsWith(".wa.gov.au"))
             || (domain.endsWith(".nt.gov.au"))
             || (domain.equals(""))) {
                println "Skipping: "+record.get("Website Address")+" (STATE GOVERNMENT SITE)"
            }
            else {
               // Add the URL to a seed list
                seedList[urltext]=0

                // Calculate the TLD - ASSUMES no non AU country domains
                if (domain.endsWith("csiro.au")) {
                    tld = "csiro.au"
                }
                else if (domain.endsWith(".au")) {
                // keep X.X.au
                    tld = domain
                    tld=tld.replaceAll(/.*?\.?([^.]+\.[^.]+?\.au$)/,'$1')
                }
                else {
                // keep X.X
                    tld = domain
                    tld=tld.replaceAll(/.*?\.?([^.]+\.[^.]+$)/,'$1')
                }

                // Get the domain and portfolio and write to a map
                domainPortfolioMap[tld]=record.get("Portfolio")

                if (!domain.endsWith(".gov.au")) {
                    // Add a site profiles entry to limit the domain to 10000 docs
                    nonGovAuList[domain]=0
                }
            }
        }
    }

    println "Writing SEED LIST: (collection.cfg.start.urls)"
    // Write the seed list
    seedList.keySet().each {
        startUrlsFile << it+'\n'
    }

    println "Writing DOMAIN - PORTFOLIO MAP: (JSON for ES)"
    // write the domain-portfolio mappings file
    domainPortfolioMap.each { k, v ->
        portfolioMappingsFile << k+"\t"+v+"\n"
    }

    println "Writing NON GOV-AU LIST: (site_profiles.cfg)"
    // write the site profiles config
    def includePatterns =""
    nonGovAuList.keySet().each {
        def pat = it+",1,250,SimpleRevisitPolicy,,,10000"
        siteProfilesFile << pat+'\n'
        includePatterns += ",http://"+it+",https://"+it
    }

    println "Writing INCLUDE_PATTERNS: (collection.cfg)"
    // Insert include patterns into collection.cfg.  Final include patterns concatenate include_patterns.static with the list from AGOR
    if (configMap["include_patterns.static"]) {
        configMap["include_patterns"] = configMap["include_patterns.static"]+includePatterns
    }
    else {
        configMap["include_patterns"] = ".gov.au"+includePatterns
    }

    println("INCLUDE PATTERNS: "+configMap["include_patterns"])

    configMap.each { k, v -> 
        collectionCfg << k+"="+v+"\n"
    }
    
    // Close files
    startUrlsFile.close()
    siteProfilesFile.close()
    portfolioMappingsFile.close()
    collectionCfg.close()
}
