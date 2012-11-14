/**
 * Copyright (c) 2012, Warwick Hunter. All rights reserved.
 * Copyright 2012, Sean Flanigan. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 * 
 *  1. Redistributions of source code must retain the above copyright notice, this list 
 *     of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice, this 
 *     list of conditions and the following disclaimer in the documentation and/or other 
 *     materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES 
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT 
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT 
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR 
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package test.org.computer.whunter.rpm.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Properties;

import org.computer.whunter.rpm.parser.RpmSpecParser;
import org.junit.Test;

import com.google.common.collect.Multimap;

/**
 * Tests of the RPM spec file parser.
 * 
 * @author Warwick Hunter (w.hunter@computer.org)
 * @date   2012-02-22
 */
public class RpmSpecParserTest {

    @Test
    public void testSingleParsing() {
        try {
            RpmSpecParser parser = RpmSpecParser.createParser("tests/specs/p4bugzilla.spec");
            assertNotNull(parser);
            checkP4BugzillaResults(toProperties(parser.parse()));
        }
        catch (FileNotFoundException e) {
            fail(e.toString());
        }
    }

    private Properties toProperties(Multimap<String, String> multimap)
   {
      Properties props = new Properties();
      for (Map.Entry<String, String> entry : multimap.entries()) {
         props.setProperty(entry.getKey(), entry.getValue());
      }
      return props;
   }

   private void checkP4BugzillaResults(Properties properties) {
        assertNotNull(properties);
        assertTrue(properties.size() > 0);
        assertEquals("p4bugzilla", properties.getProperty("name"));
        assertEquals("noarch", properties.getProperty("buildarch"));
        assertEquals("Apache 2.0", properties.getProperty("license"));
        assertEquals("/usr", properties.getProperty("prefix"));
        assertEquals("%{_tmppath}/p4bugzilla", properties.getProperty("buildroot"));
        assertEquals("w.hunter@computer.org", properties.getProperty("packager"));
        assertEquals("1.1", properties.getProperty("version"));
        assertEquals("Perforce to Bugzilla Bridge", properties.getProperty("summary"));
        assertEquals("p4bugzilla-1.1-1", properties.getProperty("provides"));
        assertEquals("1", properties.getProperty("release"));
        assertEquals("no", properties.getProperty("autoreqprov"));
        assertEquals("Applications/Daemons", properties.getProperty("group"));
        assertEquals("p4bugzilla-1.1.tar.gz", properties.getProperty("source0"));
        assertEquals("jdk >= 1.5", properties.getProperty("requires"));
        assertEquals("https://sites.google.com/site/warwickhunter", properties.getProperty("url"));
        assertEquals("Warwick Hunter", properties.getProperty("who"));
        assertEquals("Warwick Hunter himself", properties.getProperty("vendor"));
    }

    /** Test that the internal state of the parser isn't corrupted by multiple parsing passes */
    @Test
    public void testDualParsing() {
        try {
            RpmSpecParser parser = RpmSpecParser.createParser("tests/specs/p4bugzilla.spec");
            assertNotNull(parser);
            for (int i = 0; i < 5; ++i) {
                checkP4BugzillaResults(toProperties(parser.parse()));
            }
        }
        catch (FileNotFoundException e) {
            fail(e.toString());
        }
    }

    @Test
    public void testExampleCode() {
        try {
            RpmSpecParser parser = RpmSpecParser.createParser("tests/specs/p4bugzilla.spec");
            Properties properties = toProperties(parser.parse());
            System.out.printf("RPM name: %s %n", properties.getProperty("name"));
            System.out.printf("RPM version: %s-%s %n", properties.getProperty("version"), properties.getProperty("release"));
        }
        catch (FileNotFoundException e) {
            // ...
        }
    }
}
