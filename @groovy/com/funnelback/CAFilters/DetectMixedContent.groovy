package com.funnelback.CAFilters

import com.funnelback.common.filter.jsoup.*

/**
 * Detects mixed content with pages (specifically detects https pages that contain http links).
 *
 * All contained img[src|srcset], iframe[src], script[src], link[href][rel="stylesheet"], object[data], form[action], embed[src], video[src], audio[src], source[src|srcset], and params[name="movie"][value] elements are checked for being Mixed Content or not
* All contained a[href] elements linking to the same or a deeper level are successively processed for Mixed Content
 */

@groovy.util.logging.Log4j2
public class DetectMixedContent implements IJSoupFilter {

   @Override
   void processDocument(FilterContext context) {
    def doc = context.getDocument()
    def url = doc.baseUri()

    try {
    
        if (url.startsWith("https://")) {

            def mixedContent = false
            def stylesheets = doc.select("link[rel=stylesheet]")     
            def inpageStyleBlocks = doc.select("style") 
            def params = doc.select("params[name=movie]") 

            if ((!doc.select("img[src^=http://]").isEmpty())
                 || (!doc.select("img[srcset^=http://]").isEmpty())
                 || (!doc.select("iframe[src^=http://]").isEmpty())
                 || (!doc.select("script[src^=http://]").isEmpty())
                 || (!doc.select("object[data^=http://]").isEmpty())
                 || (!doc.select("form[action^=http://]").isEmpty())
                 || (!doc.select("embed[src^=http://]").isEmpty())
                 || (!doc.select("video[src^=http://]").isEmpty())
                 || (!doc.select("audio[src^=http://]").isEmpty())
                 || (!doc.select("source[src^=http://]").isEmpty())
                 || (!doc.select("source[srcset^=http://]").isEmpty())) {
                context.additionalMetadata.put("X-Funnelback-Contains-Mixed-Content","true")
                log.info("Identified "+url+" as containing mixed content (various element src/srcset check).")
            }
            // Check imported stylesheets
            else if (stylesheets.find { it -> it.attr("href").startsWith("http://") }) {
                context.additionalMetadata.put("X-Funnelback-Contains-Mixed-Content","true")
                log.info("Identified "+url+" as containing mixed content (link styles check).")
            }
            else if (inpageStyleBlocks.find { it -> it.html().matches("@import\\s+([\"']?http:|url\\(['\"]?http:)") } ) {
                context.additionalMetadata.put("X-Funnelback-Contains-Mixed-Content","true")
                log.info("Identified "+url+" as containing mixed content (in-page styles blocks check).")
            }
            // Check move type params elements
            else if (params.find { it -> it.attr("value").startsWith("https://") }) {
                context.additionalMetadata.put("X-Funnelback-Contains-Mixed-Content","true")
                log.info("Identified "+url+" as containing mixed content (params check).")
            }
        }
    } catch (e) {
      log.error("Error identifying mixed content from '{}': {}", url, e)
    }
  }
}
