/**
 * Copyright Warwick Hunter 2012. All rights reserved.
 */
package org.computer.whunter.rpm.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final String MACRO_DEFINITION_PATTERN = "^%define\\s.*";

    private final Map<Pattern, String> m_fieldPatterns;
    private final Map<Pattern, String> m_fieldReferenceMatcherPatterns;
    private final Map<String, Pattern> m_fieldReferenceReplacePatterns;
    private final Map<Pattern, String> m_macroReferenceMatcherPatterns = new HashMap<Pattern, String>();
    private final Map<String, Pattern> m_macroReferenceReplacePatterns = new HashMap<String, Pattern>();
    
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
        Map<Pattern, String> fieldRegexs = new HashMap<Pattern, String>();
        Map<Pattern, String> macroMatchRegexs = new HashMap<Pattern, String>();
        Map<String, Pattern> macroReplaceRegexs = new HashMap<String, Pattern>();
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
            if (line.startsWith("#")) {
                // Discard comments
                continue;
            }
            
            // Examine the line to see if it's a field 
            for (Map.Entry<Pattern, String> entry : m_fieldPatterns.entrySet()) {
                Matcher matcher = entry.getKey().matcher(line);
                if (matcher.matches() && matcher.groupCount() > 1) {
                    properties.setProperty(matcher.group(1).replaceAll(":","").toLowerCase(), matcher.group(2).trim());
                }
            }
            // Examine the line to see if it's a macro definition 
            if (line.matches(MACRO_DEFINITION_PATTERN)) {
                String[] words = line.split("\\s");
                if (words != null && words.length > 2) {
                    StringBuilder value = new StringBuilder();
                    for (int i = 2; i < words.length; ++i) {
                        if (i != 2) {
                            value.append(" ");
                        }
                        value.append(words[i]);
                    }
                    properties.setProperty(words[1], value.toString().trim());
                    // Add a matcher pattern for it so that any references to it can be expanded
                    StringBuilder macroMatchRegex = new StringBuilder(".*%\\{");
                    StringBuilder macroReplaceRegex = new StringBuilder("%\\{");
                    for (int i = 0; i < words[1].length(); ++i) {
                        char ch = words[1].charAt(i);
                        if (Character.isLetter(ch)) {
                            String regex = String.format("[%c%c]", Character.toLowerCase(ch), Character.toUpperCase(ch));
                            macroMatchRegex.append(regex);
                            macroReplaceRegex.append(regex);
                        } else {
                            macroMatchRegex.append(ch);
                            macroReplaceRegex.append(ch);
                        }
                    }
                    macroMatchRegex.append("\\}.*");
                    macroReplaceRegex.append("\\}");
                    m_macroReferenceMatcherPatterns.put(Pattern.compile(macroMatchRegex.toString()), words[1]);
                    m_macroReferenceReplacePatterns.put(macroMatchRegex.toString(), Pattern.compile(macroReplaceRegex.toString()));
                }
            }
        }
        expandReferences(properties);
        return properties;
    }
    
    /** 
     * The values of fields and macros can themselves contain the values of other directives. Search through the 
     * properties and replace these values if they are present.
     * 
     * @param properties the properties to modify by expanding any values
     */
    private void expandReferences(Properties properties) {

        Map<Pattern, String> matcherPatterns = new HashMap<Pattern, String>();
        matcherPatterns.putAll(m_fieldReferenceMatcherPatterns);
        matcherPatterns.putAll(m_macroReferenceMatcherPatterns);

        Map<String, Pattern> replacePatterns = new HashMap<String, Pattern>();
        replacePatterns.putAll(m_fieldReferenceReplacePatterns);
        replacePatterns.putAll(m_macroReferenceReplacePatterns);

        Properties newProperties = new Properties();
        for (Entry<Object, Object> property : properties.entrySet()) {
            String newValue = expandReferences(property.getValue().toString(), properties, matcherPatterns, replacePatterns);
            newProperties.setProperty(property.getKey().toString(), newValue);
        }
        properties.clear();
        properties.putAll(newProperties);
    }

    /** 
     * The values of fields and macros can themselves contain the values of other directives. Search through the 
     * property value and replace these values if they are present.
     *
     * @param propertyValue the value to search for any replacements
     * @param properties the properties to use to expand any values
     * @param matcherPatterns patterns to find references to fields or macros
     * @param replacePatterns patters to replace references to fields or macros with the values
     */
    private String expandReferences(String propertyValue, Properties properties, 
                                    Map<Pattern, String> matcherPatterns, 
                                    Map<String, Pattern> replacePatterns) {

        String newValue = propertyValue;

        for (Map.Entry<Pattern, String> macro : matcherPatterns.entrySet()) {
            Matcher matcher = macro.getKey().matcher(propertyValue);
            if (matcher.matches()) {
                Pattern replacePattern = replacePatterns.get(macro.getKey().toString());
                newValue = newValue.replaceAll(replacePattern.toString(), properties.getProperty(macro.getValue()));
            }
        }
        return newValue;
    }
}
