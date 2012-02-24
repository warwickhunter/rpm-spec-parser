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
 * and presents them as properties. Some properties can refer to the values of other properties with
 * a syntax of %{fieldName}. The references are expanded in the properties where possible.
 * 
 * @author Warwick Hunter (w.hunter@computer.org)
 * @date   2012-02-22
 */
public class RpmSpecParser {
    
    private static final String[] FIELDS = { "name", "version", "release", "buildrequires", "requires",
                                             "summary", "license", "vendor", "packager", "provides",
                                             "url", "source[0-9]+", "group", "buildRoot", "buildArch",
                                             "autoreqprov", "prefix", };

    private final Map<Pattern, String> m_fieldPatterns;
    private final Map<Pattern, String> m_fieldReferenceMatcherPatterns;
    private final Map<String, Pattern> m_fieldReferenceReplacePatterns;
    
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

        // Take the list of strings and turn them into case insensitive pattern matchers
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
        m_fieldPatterns = Collections.unmodifiableMap(fieldRegexs);
        m_fieldReferenceMatcherPatterns = Collections.unmodifiableMap(macroMatchRegexs);
        m_fieldReferenceReplacePatterns = Collections.unmodifiableMap(macroReplaceRegexs);
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
            for (Map.Entry<Pattern, String> entry : m_fieldPatterns.entrySet()) {
                Matcher matcher = entry.getKey().matcher(line);
                if (matcher.matches() && matcher.groupCount() > 1) {
                    properties.setProperty(matcher.group(1).replaceAll(":","").toLowerCase(), matcher.group(2).trim());
                }
            }
        }
        expandFieldReferences(properties);
        return properties;
    }
    
    /** 
     * The values of fields can themselves contain the values of other directives. Search through the 
     * properties and replace these values if they are present.
     * 
     * @param properties the properties to modify by expanding any values
     */
    private void expandFieldReferences(Properties properties) {
        Properties newProperties = new Properties();
        for (Entry<Object, Object> property : properties.entrySet()) {
            String newValue = expandFieldReferences(property.getValue().toString(), properties);
            newProperties.setProperty(property.getKey().toString(), newValue);
        }
        properties.clear();
        properties.putAll(newProperties);
    }

    /** 
     * The values of fields can themselves contain the values of other directives. Search through the 
     * property value and replace these values if they are present.
     *
     * @param propertyValue the value to search for any replacements
     * @param properties the properties to use to expand any values
     */
    private String expandFieldReferences(String propertyValue, Properties properties) {
        String newValue = propertyValue;
        for (Map.Entry<Pattern, String> macro : m_fieldReferenceMatcherPatterns.entrySet()) {
            Matcher matcher = macro.getKey().matcher(propertyValue);
            if (matcher.matches()) {
                Pattern replacePattern = m_fieldReferenceReplacePatterns.get(macro.getKey().toString());
                newValue = newValue.replaceAll(replacePattern.toString(), properties.getProperty(macro.getValue()));
            }
        }
        return newValue;
    }
}
