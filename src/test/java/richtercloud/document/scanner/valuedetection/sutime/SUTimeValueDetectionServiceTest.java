/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package richtercloud.document.scanner.valuedetection.sutime;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomStringUtils;
import static org.junit.Assert.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.document.scanner.valuedetectionservice.ValueDetectionResult;
import richtercloud.document.scanner.valuedetectionservice.ValueDetectionService;

/**
 *
 * @author richter
 */
public class SUTimeValueDetectionServiceTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(SUTimeValueDetectionServiceTest.class);
    private final static Random RANDOM;
    private final static String WHITESPACE_CHARS = " \t\n\\x0B\f\r";
    static {
        long randomSeed = System.currentTimeMillis();
        LOGGER.debug(String.format("random seed: %d", randomSeed));
        RANDOM = new Random(randomSeed);
    }
    private final int wordLengthMin = 1;
    private final int wordLengthMax = 100;
    private final int wordCountMax = 10000;
    /**
     * The inverse of the probability of adding a date into the test input.
     */
    private final int dateProbability = 100;
    private final int maxYear = 9999;

    /**
     * Test of fetchResults0 method, of class SUTimeValueDetectionService by
     * creating inputs of random length (in range between
     */
    /*
    internal implementation notes:
    - parallelizing test in different threads doesn't work because either
    SUTimeValueDetectionService.fetchResult0 creates a pipeline setup for each
    request (initialization using an ExcecutorService takes up to 5 minutes
    under 100% CPU load with hardly noticable progress) or every instance has
    one pipeline property which is initialized in the constructor (CPU load
    drops to ~0% after successfully processing some tests and the test
    apparently deadlocks)
    */
    @Test
    public void testFetchResults0() throws IOException, ClassNotFoundException, InterruptedException, Exception {
        SUTimeValueDetectionService instance = new SUTimeValueDetectionService();
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<Exception>> testCallableFutures = new LinkedList<>();
        for(Locale availableLocale : Locale.getAvailableLocales()) {
            Callable<Exception> testCallable = () -> {
                try {
                    String languageIdentifier = ValueDetectionService.retrieveLanguageIdentifier(availableLocale);
                    if(!instance.supportsLanguage(languageIdentifier)) {
                        LOGGER.debug(String.format("skipping test for locale '%s' with language identifier '%s'",
                                availableLocale,
                                languageIdentifier));
                        return null;
                    }
                    StringBuilder inputBuilder = new StringBuilder(wordCountMax*wordLengthMax);
                    int wordCount = wordCountMax+RANDOM.nextInt(1000)*(RANDOM.nextBoolean() ? 1 : -1);
                    LOGGER.trace(String.format("wordCount: %d", wordCount));
                    LinkedHashSet<ValueDetectionResult<Date>> expResult = new LinkedHashSet<>();
                    for(int i=0; i< wordCount; i++) {
                        int wordLength = wordLengthMin+RANDOM.nextInt(wordLengthMax-wordLengthMin);
                        String word = RandomStringUtils.random(wordLength);
                        int dateProbabilityValue = RANDOM.nextInt();
                        if(dateProbabilityValue % dateProbability == 0) {
                            boolean addExtra = RANDOM.nextBoolean();
                                //whether to add the date into a word or as a separate word
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTime(new Date(RANDOM.nextLong()));
                            calendar.set(Calendar.YEAR, RANDOM.nextInt(maxYear));
                            Date date = calendar.getTime();
                            String dateFormatted;
                            try {
                                dateFormatted = SUTimeValueDetectionService.SIMPLE_DATE_FORMAT.format(date);
                            }catch(ArrayIndexOutOfBoundsException ex) {
                                //need to figure what why and when this happens
                                LOGGER.error(String.format("unexpected exception during formatting of date '%s' occured",
                                        date.toString()),
                                        ex);
                                throw ex;
                            }
                            expResult.add(new ValueDetectionResult<>(dateFormatted, date));
                            if(!addExtra) {
                                inputBuilder.append(dateFormatted);
                                inputBuilder.append(RandomStringUtils.random(1,
                                        WHITESPACE_CHARS));
                            }else {
                                int insertPosition = RANDOM.nextInt(wordLength);
                                inputBuilder.append(word.substring(0, insertPosition));
                                inputBuilder.append(dateFormatted);
                                inputBuilder.append(RandomStringUtils.random(1, //count
                                        true, //letters
                                        false //numbers
                                ));
                                    //add avoid adding number right after the year of the
                                    //date since that does make the recognition impossible
                                if(insertPosition < wordLength) {
                                    inputBuilder.append(word.substring(insertPosition+1,wordLength));
                                }
                                inputBuilder.append(RandomStringUtils.random(1,
                                        WHITESPACE_CHARS));
                            }
                        }
                    }
                    assert !expResult.isEmpty();
                    String input = inputBuilder.toString();
                    LOGGER.trace(String.format("input: %s", input));
                    LinkedHashSet<ValueDetectionResult<Date>> result = instance.fetchResults0(input,
                            languageIdentifier
                    );
                    LOGGER.debug(String.format("recognized %d dates",
                            result.size()));
                    //The detection service might randomly recognize more dates which is not
                    //a problem
                    assertFalse(result.isEmpty());
                    return null;
                }catch(Exception ex) {
                    LOGGER.error(String.format("exception in test thread for locale %s",
                                    availableLocale),
                            ex);
                        //it's difficult to get the right stacktrace at another
                        //location
                    return ex;
                }
            };
            Future<Exception> testRunnableFuture = executorService.submit(testCallable);
            testCallableFutures.add(testRunnableFuture);
            LOGGER.debug(String.format("started test thread for locale %s",
                    availableLocale));
        }
        LOGGER.debug(String.format("waiting for %d test threads to be executed",
                testCallableFutures.size()));
        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        for(Future<Exception> testCallableFuture : testCallableFutures) {
            Exception callableFutureException = testCallableFuture.get();
            if(callableFutureException != null) {
                throw callableFutureException;
            }
        }
    }
}
