package com.funnelback.CAFilters

@Grapes([
  @Grab(group='com.cybozu.labs', module='langdetect', version='1.1-20120112'),
  @Grab(group='net.arnx', module='jsonic', version='1.3.10'),
  @Grab(group='org.projectlombok', module='lombok', version='1.18.2')
])

import com.funnelback.common.filter.jsoup.*
import com.funnelback.common.html.HTMLUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.apache.log4j.Logger;
import java.util.ArrayList;
import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.Language;
import com.cybozu.labs.langdetect.LangDetectException;

/**
 * Detects the language of a document
 *
 * @author bpottier@funnelback.com
 * @author plevan@funnelback.com
 */

@groovy.util.logging.Log4j2
public class DetectLang implements IJSoupFilter {

    private double probabilityTreshold = 0.5;

    @Override
    public void setup(SetupContext context) {

        if (context.getConfigSetting("filter.langdetect.threshold") != null) {
            probabilityTreshold = Double.parseDouble(context.getConfigSetting("filter.langdetect.threshold").orElse("0.5"));
        }
        log.info("Probability threshold is set to " + probabilityTreshold);

        String profilePath = context.getSearchHome().getAbsolutePath() + "/share/langdetect/profiles/"
        
        if (context.getConfigSetting("filter.langdetect.profilepath") != null) {
            profilePath = context.getConfigSetting("filter.langdetect.profilepath")
        }
        log.info("Loading profiles from " + profilePath);

        try { 
            DetectorFactory.loadProfile(profilePath);
        } 
        catch(LangDetectException e) {
            log.error("Error in language detection setup: " + e.getMessage());
        }
    }   

    @Override
    void processDocument(FilterContext context) {
        def doc = context.getDocument()
        def url = doc.baseUri()

        doc.outputSettings(new Document.OutputSettings().prettyPrint(false)); //makes html() preserve linebreaks and spacing
        doc.select("br").append("\\n");
        doc.select("p").prepend("\\n");
        doc.select("div").prepend("\\n");
        String text = doc.select("body").html().replaceAll("\\\\n", "\n");
        text = Jsoup.clean(text, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false));
        // Now we have a string with linebreaks preserved, including <br> and <p>
        try { 
            ArrayList<Language> languages = detectLangs(text);

            for (Language l : languages) {

                if (l.prob >= probabilityTreshold) {
                    context.additionalMetadata.put("X-Funnelback-Detected-Language", l.lang);
                    //result = HTMLUtils.insertMetadata(result, "FB:LanguageDetected", l.lang);       
                } else {
                    log.debug("Detected language " + l.lang + " but probability was too low (" + l.prob + ") ["+url+"]");
                }
            }
        } 
        catch(LangDetectException e) {
            log.error("Error in language detection: " + e.getMessage() + " ["+url+"]");
        }
    }

    private ArrayList<Language> detectLangs(String text) throws LangDetectException {
        Detector detector = DetectorFactory.create();
        detector.setMaxTextLength(text.length() + 1); // up to text + 1 length allowed so we get all of it
        detector.append(text);

        return detector.getProbabilities();
    }
}


