package com.funnelback.dta

import com.funnelback.common.filter.jsoup.*

/**
 * Scrapes tags, author and thumbnails from the content and injects these as custom metadata
 */

@groovy.util.logging.Log4j2
public class SplitMetadata implements IJSoupFilter {

   @Override
   void processDocument(FilterContext context) {
    def doc = context.getDocument()
    def url = doc.baseUri()

    try {

      // Split the AGLS function metadata fields on ; and , and inject these as a separate fields. 

      doc.select("meta[name=agls.function]").each { metaval ->
        def content = metaval.attr("content").split(/(;|,)/)

        content.each { val ->
          if (val.trim() != '') {
            context.additionalMetadata.put("dta.agls.function", val.trim().toLowerCase())
            context.additionalMetadata.put("dta.function", val.trim().toLowerCase())
            log.info("Added metadata value dta.agls.function: '{}' for '{}'", val.trim().toLowerCase(), url)
          }
        }
      }


      doc.select("meta[name=aglsterms.function]").each { metaval ->
        def content = metaval.attr("content").split(/(;|,)/)

        content.each { val ->
          if (val.trim() != '') {
            context.additionalMetadata.put("dta.aglsterms.function", val.trim().toLowerCase())
            context.additionalMetadata.put("dta.function", val.trim().toLowerCase())
            log.info("Added metadata value dta.aglsterms.function: '{}' for '{}'", val.trim().toLowerCase(), url)
          }
        }
      }

    } catch (e) {
      log.error("Error scraping metadata from '{}'", url, e)
    }
  }
}
