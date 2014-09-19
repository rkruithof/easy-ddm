/*
 * Copyright 1999-2005 The Apache Software Foundation. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed
 * to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing permissions and limitations under the License.
 */

package nl.knaw.dans.common.lang.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides utility methods for input and output streams.
 * 
 * @author Richard Wan
 */

// Contributed by ThoughtWorks Inc.
// copied from org.apache.log4j.lf5.util.StreamUtils;

public abstract class StreamUtil {
    // --------------------------------------------------------------------------
    // Constants:
    // --------------------------------------------------------------------------

    /**
     * Default value is 2048.
     */
    public static final int DEFAULT_BUFFER_SIZE = 2048;

    // --------------------------------------------------------------------------
    // Protected Variables:
    // --------------------------------------------------------------------------

    // --------------------------------------------------------------------------
    // Private Variables:
    // --------------------------------------------------------------------------

    // --------------------------------------------------------------------------
    // Constructors:
    // --------------------------------------------------------------------------

    // --------------------------------------------------------------------------
    // Public Methods:
    // --------------------------------------------------------------------------

    /**
     * Copies information from the input stream to the output stream using a default buffer size of 2048 bytes.
     * 
     * @throws java.io.IOException
     */
    public static void copy(InputStream input, OutputStream output) throws IOException {
        copy(input, output, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Copies information from the input stream to the output stream using the specified buffer size
     * 
     * @throws java.io.IOException
     */
    public static void copy(InputStream input, OutputStream output, int bufferSize) throws IOException {
        byte[] buf = new byte[bufferSize];
        int bytesRead = input.read(buf);
        while (bytesRead != -1) {
            output.write(buf, 0, bytesRead);
            bytesRead = input.read(buf);
        }
        output.flush();
    }

    /**
     * Copies information between specified streams and then closes both of the streams.
     * 
     * @throws java.io.IOException
     */
    public static void copyThenClose(InputStream input, OutputStream output) throws IOException {
        copy(input, output);
        input.close();
        output.close();
    }

    /**
     * @returns a byte[] containing the information contained in the specified InputStream.
     * @throws java.io.IOException
     */
    public static byte[] getBytes(InputStream input) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        copy(input, result);
        result.close();
        return result.toByteArray();
    }

    // --------------------------------------------------------------------------
    // Protected Methods:
    // --------------------------------------------------------------------------

    // --------------------------------------------------------------------------
    // Private Methods:
    // --------------------------------------------------------------------------

    // --------------------------------------------------------------------------
    // Nested Top-Level Classes or Interfaces
    // --------------------------------------------------------------------------

}
