package com.funnelback.CAFilters

import com.funnelback.common.filter.jsoup.*

/**
 * Detects RSS feeds and injects some additional identifying metadata.
 */

@groovy.util.logging.Log4j2
public class DetectRSS implements IJSoupFilter {

   @Override
   void processDocument(FilterContext context) {
    def doc = context.getDocument()
    def url = doc.baseUri()

    try {
    

        if (!doc.select("rss[version]").isEmpty()) {

            def version = doc.select("rss[version]").attr("version")
            // RSS 0.91, 0.92 and 2.0 feeds can be identified by the presence of an <rss version="X.X"> element
            context.additionalMetadata.put("X-Funnelback-Feed-Type","RSS")
            context.additionalMetadata.put("X-Funnelback-Feed-Version","RSS "+version)
            log.info("Identified "+url+" as an RSS "+version+" feed.")
        }
        else if (!doc.select("rdf|RDF[xmlns=http://purl.org/rss/1.0/]").isEmpty()) {
            // RSS 1.0 feeds can be identified by the presence of an <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns="http://purl.org/rss/1.0/"> element
            context.additionalMetadata.put("X-Funnelback-Feed-Type","RSS")
            context.additionalMetadata.put("X-Funnelback-Feed-Version","RSS 1.0")
            log.info("Identified "+url+" as an RSS 1.0 feed.")
        }
        else if (!doc.select("rdf|RDF[xmlns=http://channel.netscape.com/rdf/simple/0.9/]").isEmpty()) {
            // RSS 0.9 feeds can be identified by the presence of an <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns="http://channel.netscape.com/rdf/simple/0.9/"> element
            context.additionalMetadata.put("X-Funnelback-Feed-Type","RSS")
            context.additionalMetadata.put("X-Funnelback-Feed-Version","RSS 0.9")
            log.info("Identified "+url+" as an RSS 0.9 feed.")
        }
        else if (!doc.select("feed[xmlns=http://www.w3.org/2005/Atom]").isEmpty()) {
            // ATOM feeds can be identified by the presence of an <feed xmlns="http://www.w3.org/2005/Atom"> element
            context.additionalMetadata.put("X-Funnelback-Feed-Type","ATOM")
            context.additionalMetadata.put("X-Funnelback-Feed-Version","ATOM 1.0")
            log.info("Identified "+url+" as an ATOM feed.")
        }
    } catch (e) {
      log.error("Error identifying RSS feeds from '{}': {}", url, e)
    }
  }
}
