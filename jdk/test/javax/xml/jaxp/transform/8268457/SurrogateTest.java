/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8268457
 * @library /test/lib
 * @run main/othervm transform.SurrogateTest
 * @summary XML Transformer outputs Unicode supplementary character incorrectly to HTML
 */
public class SurrogateTest {

    final static String TEST_SRC = System.getProperty("test.src", ".");

    public void toHTMLTest() throws Exception {
        String out = "SurrogateTest1out.html";
        String expected = TEST_SRC + File.separator + "SurrogateTest1.html";
        String xml = TEST_SRC + File.separator + "SurrogateTest1.xml";
        String xsl = TEST_SRC + File.separator + "SurrogateTest1.xsl";

        try (FileInputStream tFis = new FileInputStream(xsl);
            InputStream fis = new FileInputStream(xml);
            FileOutputStream fos = new FileOutputStream(out)) {

            Source tSrc = new StreamSource(tFis);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer(tSrc);
            t.setOutputProperty("method", "html");

            Source src = new StreamSource(fis);
            Result res = new StreamResult(fos);
            t.transform(src, res);
        }
        Assert.assertTrue(compareWithGold(expected, out));
    }

    public void handlerTest() throws Exception {
        File xmlFile = new File(TEST_SRC, "SurrogateTest2.xml");
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser sp = spf.newSAXParser();
        TestHandler th = new TestHandler();
        sp.parse(xmlFile, th);
        Assert.assertTrue(compareLinesWithGold(TEST_SRC + File.separator + "SurrogateTest2.txt", th.lines));
    }

    private static class TestHandler extends DefaultHandler {
        private List<String> lines = new ArrayList<>();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            lines.add( localName + "@attr:" + attributes.getValue("attr"));
        }
    }

    public static void main(String[] args) throws Exception {
        SurrogateTest test = new SurrogateTest();
        test.toHTMLTest();
        test.handlerTest();
    }

    /**
     * Compare contents of golden file with test output file line by line.
     * return true if they're identical.
     * @param goldfile Golden output file name
     * @param outputfile Test output file name
     * @return true if two files are identical.
     *         false if two files are not identical.
     * @throws IOException if an I/O error occurs reading from the file or a
     *         malformed or unmappable byte sequence is read.
     */
    public static boolean compareWithGold(String goldfile, String outputfile)
            throws IOException {
        return compareWithGold(goldfile, outputfile, StandardCharsets.UTF_8);
    }

    /**
     * Compare contents of golden file with test output file line by line.
     * return true if they're identical.
     * @param goldfile Golden output file name.
     * @param outputfile Test output file name.
     * @param cs the charset to use for decoding.
     * @return true if two files are identical.
     *         false if two files are not identical.
     * @throws IOException if an I/O error occurs reading from the file or a
     *         malformed or unmappable byte sequence is read.
     */
    public static boolean compareWithGold(String goldfile, String outputfile,
             Charset cs) throws IOException {
        boolean isSame = Files.readAllLines(Paths.get(goldfile)).
                equals(Files.readAllLines(Paths.get(outputfile), cs));
        if (!isSame) {
            System.err.println("Golden file " + goldfile + " :");
            Files.readAllLines(Paths.get(goldfile)).forEach(System.err::println);
            System.err.println("Output file " + outputfile + " :");
            Files.readAllLines(Paths.get(outputfile), cs).forEach(System.err::println);
        }
        return isSame;
    }

    /**
     * Compare contents of golden file with test output list line by line.
     * return true if they're identical.
     * @param goldfile Golden output file name.
     * @param lines test output list.
     * @return true if file's content is identical to given list.
     *         false if file's content is not identical to given list.
     * @throws IOException if an I/O error occurs reading from the file or a
     *         malformed or unmappable byte sequence is read
     */
    public static boolean compareLinesWithGold(String goldfile, List<String> lines)
            throws IOException {
        return Files.readAllLines(Paths.get(goldfile)).equals(lines);
    }
}
