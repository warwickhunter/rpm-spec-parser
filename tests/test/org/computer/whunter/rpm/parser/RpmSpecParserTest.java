/**
 * Copyright Warwick Hunter 2012. All rights reserved.
 */
package test.org.computer.whunter.rpm.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.util.Properties;

import org.computer.whunter.rpm.parser.RpmSpecParser;
import org.junit.Test;

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
            checkP4BugzillaResults(parser.parse());
        }
        catch (FileNotFoundException e) {
            fail(e.toString());
        }
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
                checkP4BugzillaResults(parser.parse());
            }
        }
        catch (FileNotFoundException e) {
            fail(e.toString());
        }
    }
}
