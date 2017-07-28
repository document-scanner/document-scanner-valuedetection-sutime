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

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Random;
import org.apache.commons.lang3.RandomStringUtils;
import static org.junit.Assert.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.document.scanner.valuedetectionservice.ValueDetectionResult;

/**
 *
 * @author richter
 */
public class SUTimeValueDetectionServiceTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(SUTimeValueDetectionServiceTest.class);
    private final static Random RANDOM;
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
    @Test
    public void testFetchResults0() {
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
                expResult.add(new ValueDetectionResult<>(date.toString(), date));
                if(!addExtra) {
                    inputBuilder.append(date.toString());
                    inputBuilder.append(RandomStringUtils.random(1, " \t\n\\x0B\f\r"));
                }else {
                    int insertPosition = RANDOM.nextInt(wordLength);
                    inputBuilder.append(word.substring(0, insertPosition));
                    inputBuilder.append(date.toString());
                    inputBuilder.append(RandomStringUtils.random(1, //count
                            true, //letters
                            false //numbers
                    ));
                        //add avoid adding number right after the year of the
                        //date since that does make the recognition impossible
                    if(insertPosition < wordLength) {
                        inputBuilder.append(word.substring(insertPosition+1,wordLength));
                    }
                    inputBuilder.append(RandomStringUtils.random(1, " \t\n\\x0B\f\r"));
                }
            }
        }
        assert !expResult.isEmpty();
        String input = inputBuilder.toString();
        LOGGER.trace(String.format("input: %s", input));
        SUTimeValueDetectionService instance = new SUTimeValueDetectionService();
        LinkedHashSet<ValueDetectionResult<Date>> result = instance.fetchResults0(input);
        //The detection service might randomly recognize more dates which is not
        //a problem
        assertTrue(result.size() >= expResult.size());
            //@TODO: all results have the current year as year value which
            //shouldn't be the case, but now works fine
    }
}
