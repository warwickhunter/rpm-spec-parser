/**
 * Copyright Warwick Hunter 2012. All rights reserved.
 */
package org.computer.whunter.rpm.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;

/**
 * This is a parser of an RPM Spec file. It extracts a number of properties from an RPM spec file
 * and presents them as properties.
 * 
 * @author Warwick Hunter (w.hunter@computer.org)
 * @date   2012-02-22
 */
public class RpmSpecParser {
    
    private static final String[]             FIELDS = { "name", "version", "release", "buildrequires", "requires",
                                                         "summary", "license", "vendor", "packager", "provides",
                                                         "url", "source[0-9]+", "group", "buildRoot", "buildArch",
                                                         "autoreqprov", "prefix", };
    private static final Map<Pattern, String> FIELD_PATTERNS;
    private static final Map<Pattern, String> MACRO_MATCHER_PATTERNS;
    private static final Map<String, Pattern> MACRO_REPLACE_PATTERNS;
    
    // Take the list of strings and turn them into case insensitive pattern matchers
    static {
        Map<Pattern, String> fieldRegexs = Maps.newHashMap();
        Map<Pattern, String> macroMatchRegexs = Maps.newHashMap();
        Map<String, Pattern> macroReplaceRegexs = Maps.newHashMap();
        for (String field : FIELDS) {
            StringBuilder fieldRegex = new StringBuilder("^");
            StringBuilder macroMatchRegex = new StringBuilder(".*%\\{");
            StringBuilder macroReplaceRegex = new StringBuilder("%\\{");
            fieldRegex.append("(");
            for (int i = 0; i < field.length(); ++i) {
                char ch = field.charAt(i);
                if (Character.isLetter(ch)) {
                    String regex = String.format("[%c%c]", Character.toLowerCase(ch), Character.toUpperCase(ch));
                    fieldRegex.append(regex);
                    macroMatchRegex.append(regex);
                    macroReplaceRegex.append(regex);
                } else {
                    fieldRegex.append(ch);
                    macroMatchRegex.append(ch);
                    macroReplaceRegex.append(ch);
                }
            }
            fieldRegex.append(":)(.*)");
            macroMatchRegex.append("\\}.*");
            macroReplaceRegex.append("\\}");
            fieldRegexs.put(Pattern.compile(fieldRegex.toString()), field);
            macroMatchRegexs.put(Pattern.compile(macroMatchRegex.toString()), field);
            macroReplaceRegexs.put(macroMatchRegex.toString(), Pattern.compile(macroReplaceRegex.toString()));
        }
        FIELD_PATTERNS = Collections.unmodifiableMap(fieldRegexs);
        MACRO_MATCHER_PATTERNS = Collections.unmodifiableMap(macroMatchRegexs);
        MACRO_REPLACE_PATTERNS = Collections.unmodifiableMap(macroReplaceRegexs);
    }
    
    /** The path of the spec file to parse */
    private final String m_specFilePath;
    
    /**
     * Create a parser that will parse an RPM spec file.
     *  
     * @param specFilePath the patch of the spec file to parse.
     * @return a parser ready to parse the file.
     */
    public static RpmSpecParser createParser(String specFilePath) {
        return new RpmSpecParser(specFilePath);
    }
    
    /** Private constructor */
    private RpmSpecParser(String specFilePath) {
        m_specFilePath = specFilePath;
    }

    /**
     * Parse the RPM spec file. Each of the supported fields is placed into the {@link Properties} returned.
     * @return the {@link Properties} of the spec file. 
     * @throws FileNotFoundException if the path of the spec file could not be opened for reading.
     */
    public Properties parse() throws FileNotFoundException {
        Properties properties = new Properties();
        Scanner scanner = new Scanner(new File(m_specFilePath));
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            for (Map.Entry<Pattern, String> entry : FIELD_PATTERNS.entrySet()) {
                Matcher matcher = entry.getKey().matcher(line);
                if (matcher.matches() && matcher.groupCount() > 1) {
                    properties.setProperty(matcher.group(1).replaceAll(":","").toLowerCase(), matcher.group(2).trim());
                }
            }
        }
        expandMacros(properties);
        return properties;
    }
    
    /** 
     * The values of fields can themselves contain the values of other directives. Search through the 
     * properties and replace these values if they are present.
     * 
     * @param properties the properties to modify by expanding any directive values
     */
    private void expandMacros(Properties properties) {
        Properties newProperties = new Properties();
        for (Entry<Object, Object> property : properties.entrySet()) {
            String newValue = property.getValue().toString();
            for (Map.Entry<Pattern, String> macro : MACRO_MATCHER_PATTERNS.entrySet()) {
                Matcher matcher = macro.getKey().matcher(property.getValue().toString());
                if (matcher.matches()) {
                    Pattern replacePattern = MACRO_REPLACE_PATTERNS.get(macro.getKey().toString());
                    newValue = newValue.replaceAll(replacePattern.toString(), properties.getProperty(macro.getValue()));
                }
            }
            newProperties.setProperty(property.getKey().toString(), newValue);
        }
        properties.clear();
        properties.putAll(newProperties);
    }
}
