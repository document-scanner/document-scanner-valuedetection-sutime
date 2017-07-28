/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package richtercloud.document.scanner.valuedetection.sutime;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.time.*;
import edu.stanford.nlp.time.SUTime.Temporal;
import edu.stanford.nlp.util.CoreMap;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.document.scanner.valuedetectionservice.AbstractValueDetectionService;
import richtercloud.document.scanner.valuedetectionservice.ValueDetectionResult;
import richtercloud.document.scanner.valuedetectionservice.ValueDetectionService;
import richtercloud.document.scanner.valuedetectionservice.ValueDetectionServiceListener;
import richtercloud.document.scanner.valuedetectionservice.ValueDetectionServiceUpdateEvent;
import richtercloud.document.scanner.valuedetectionservice.annotations.ConfPanel;

/**
 * An {@link ValueDetectionService} which uses the <a href=""></a>.
 * @author richter
 */
@ConfPanel(confPanelClass = SUTimeValueDetectionServiceConfPanel.class)
public class SUTimeValueDetectionService extends AbstractValueDetectionService<Date> {
    private final static Logger LOGGER = LoggerFactory.getLogger(SUTimeValueDetectionService.class);
    public final static Set<String> SUPPORTED_LANGUAGES = new HashSet<>(Arrays.asList(ValueDetectionService.LANGUAGE_CHINESE,
            ValueDetectionService.LANGUAGE_ENGLISH,
            ValueDetectionService.LANGUAGE_FRENCH,
            ValueDetectionService.LANGUAGE_GERMAN,
            ValueDetectionService.LANGUAGE_SPANISH));
    /**
     * The {@code SUTime} pipeline used for date and time value discovery. Is
     * reusage according to class javadoc.
     */
    private final static AnnotationPipeline PIPELINE = new AnnotationPipeline();
    static {
        Properties props = new Properties();
        PIPELINE.addAnnotator(new TokenizerAnnotator(false));
        PIPELINE.addAnnotator(new WordsToSentencesAnnotator(false));
        PIPELINE.addAnnotator(new POSTaggerAnnotator(false));
        PIPELINE.addAnnotator(new TimeAnnotator("sutime", props));
    }
    protected final static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Parameterless constructor for Java SPI initialization.
     */
    /*
    internal implementation notes:
    - needs to be public in order to avoid instantiation exception
    */
    public SUTimeValueDetectionService() {
    }

    @Override
    protected LinkedHashSet<ValueDetectionResult<Date>> fetchResults0(String input,
            String languageIdentifier) {
        if(!supportsLanguage(languageIdentifier)) {
            throw new IllegalArgumentException(String.format(
                    "language '%s' isn't supported", languageIdentifier));
        }
        LinkedHashSet<ValueDetectionResult<Date>> retValue = new LinkedHashSet<>();
        Annotation annotation = new Annotation(input);
        String currentTime = SIMPLE_DATE_FORMAT.format( System.currentTimeMillis() );
        annotation.set(CoreAnnotations.DocDateAnnotation.class, currentTime);
        PIPELINE.annotate(annotation);
        List<CoreMap> timexAnnsAll = annotation.get(TimeAnnotations.TimexAnnotations.class);
        int progressCounter=0;
        for (CoreMap coreMap : timexAnnsAll) {
            String oCRSource = coreMap.toString();
            Date value;
            Temporal temporal = coreMap.get(TimeExpression.Annotation.class).getTemporal();
            if(temporal.getRange() == null || temporal.getRange().begin() == null) {
                LOGGER.debug(String.format("skipping temporal %s with null range or with range with null begin",
                        temporal));
                    //@TODO: investigate further and prevent causes for this if
                    //it makes sense
                continue;
            }
            try {
                value = SIMPLE_DATE_FORMAT.parse(temporal.getRange().begin().toString());
            } catch (ParseException | NumberFormatException ex) {
                //Something like `Caused by: java.text.ParseException: Unparseable date: "716-XX-XX"`
                //can happen @TODO: figure out
                LOGGER.error(String.format("an unexpected exception occured during: %s", ex.getMessage()));
                    //don't log stacktrace because it's confusing in output
                continue;
            }
            ValueDetectionResult<Date> result = new ValueDetectionResult<>(oCRSource,
                    value
            );
            LOGGER.trace(String.format("added result %s",
                    result));
            retValue.add(result);
            //it's not clear, what timexAnnsAll stand for and whether they even
            //remotely represent the progress, but they're the base for the main
            //loop of the method, so using them is better than nothing
            for(ValueDetectionServiceListener<Date> listener : getListeners()) {
                listener.onUpdate(new ValueDetectionServiceUpdateEvent<>(new LinkedList<>(retValue), timexAnnsAll.size(), progressCounter));
            }
            progressCounter++;
        }
        return retValue;
    }

    @Override
    public boolean supportsLanguage(String languageIdentifier) {
        return SUPPORTED_LANGUAGES.contains(languageIdentifier);
    }
}
