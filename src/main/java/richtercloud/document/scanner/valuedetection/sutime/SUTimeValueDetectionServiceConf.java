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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;
import richtercloud.document.scanner.valuedetectionservice.ValueDetectionServiceConf;
import richtercloud.document.scanner.valuedetectionservice.ValueDetectionServiceConfValidationException;

/**
 *
 * @author richter
 */
public class SUTimeValueDetectionServiceConf implements ValueDetectionServiceConf {
    private static final long serialVersionUID = 1L;
    private List<String> modelJARPathes;

    public SUTimeValueDetectionServiceConf() {
        this(new LinkedList<>());
    }

    public SUTimeValueDetectionServiceConf(List<String> modelJARPathes) {
        this.modelJARPathes = modelJARPathes;
    }

    public List<String> getModelJARPathes() {
        return modelJARPathes;
    }

    public void setModelJARPathes(List<String> modelJARPathes) {
        this.modelJARPathes = modelJARPathes;
    }

    @Override
    public void validate() throws ValueDetectionServiceConfValidationException {
        if(modelJARPathes.isEmpty()) {
            throw new ValueDetectionServiceConfValidationException("list of "
                    + "model JAR pathes is empty");
        }
        //@TODO: figure out validation
        for(String modelJARPath : modelJARPathes) {
            try {
                ClassLoader testClassLoader = new URLClassLoader(new URL[] {new File(modelJARPath).toURI().toURL()},
                        Thread.currentThread().getContextClassLoader());
            } catch (MalformedURLException ex) {
                throw new ValueDetectionServiceConfValidationException(ex);
            }
        }
    }
}
