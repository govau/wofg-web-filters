package com.funnelback.CAFilters

@Grapes([
  @Grab(group='com.twitter', module='twitter-text', version='1.14.7')
])

import com.funnelback.common.filter.jsoup.*
import java.util.List;
import com.twitter.Extractor;

/**
 * Detects Twitter #hashtags and @usertags and adds these to custom metadata fields.
 *
 * @author plevan@funnelback.com
 */

@groovy.util.logging.Log4j2
public class SocialTags implements IJSoupFilter {


    public static final String FBUSERTAGS = "X-Funnelback-Twitter-User-Tags";
    public static final String FBHASHTAGS = "X-Funnelback-Twitter-Hash-Tags";

    @Override
    public void processDocument(FilterContext context) {

        // get the document object
        def doc = context.getDocument();
        def url = doc.baseUri()

        // Make sure this filter doesn't run on Tika converted documents
        def parser = []
        def filter = true

        parser = doc.select("meta[name=X-Parsed-By]")

        //if any X-Parsed by tags indicate Tika then set a flag to skip filtering this for social tags 
        parser.each() { 
            if (it.attr("content").startsWith("org.apache.tika")) {
                filter = false
            }
        }


        if ((doc != null ) && (doc.body() != null) && filter){

            // extract the body text
            def content = doc.body().text();

            List usertags;
            List hashtags;

            Extractor extractor = new Extractor();

            usertags = extractor.extractMentionedScreennames(content);
            hashtags = extractor.extractHashtags(content);
           
            if (usertags.size() > 0) { 
                context.additionalMetadata.put(FBUSERTAGS+"-count", usertags.size().toString());
            }
            if (hashtags.size() >0) {
                context.additionalMetadata.put(FBHASHTAGS+"-count", hashtags.size().toString());
            }
            usertags.unique().each {
                context.additionalMetadata.put(FBUSERTAGS, it);
            }

            hashtags.unique().each {
                context.additionalMetadata.put(FBHASHTAGS, it);
            }


        }
        else if (!filter) {
            log.info ("Skipping social tag extraction: ["+url+"]")
        }
        else {
            log.error ("Failed extracting text: ["+url+"]")
        }

    }
}
