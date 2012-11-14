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
package org.computer.whunter.rpm.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

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
            fieldRegex.append("):(.*)");
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
    public Multimap<String, String> parse() throws FileNotFoundException {
        Multimap<String, String> properties = ArrayListMultimap.create();
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
                    // TODO handle multiple values
                    properties.put(matcher.group(1).toLowerCase(), matcher.group(2).trim());
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
                    assert !properties.containsKey(words[1]);
//                    properties.removeAll(words[1]);
                    properties.put(words[1], value.toString().trim());
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
    private void expandReferences(Multimap<String, String> properties) {

        Map<Pattern, String> matcherPatterns = new HashMap<Pattern, String>();
        matcherPatterns.putAll(m_fieldReferenceMatcherPatterns);
        matcherPatterns.putAll(m_macroReferenceMatcherPatterns);

        Map<String, Pattern> replacePatterns = new HashMap<String, Pattern>();
        replacePatterns.putAll(m_fieldReferenceReplacePatterns);
        replacePatterns.putAll(m_macroReferenceReplacePatterns);

        Multimap<String, String> newProperties = ArrayListMultimap.create();
        for (Entry<String, String> property : properties.entries()) {
            String newValue = expandReferences(property.getValue().toString(), properties, matcherPatterns, replacePatterns);
            newProperties.put(property.getKey().toString(), newValue);
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
    private String expandReferences(String propertyValue, Multimap<String, String> properties, 
                                    Map<Pattern, String> matcherPatterns, 
                                    Map<String, Pattern> replacePatterns) {

        String newValue = propertyValue;

        for (Map.Entry<Pattern, String> macro : matcherPatterns.entrySet()) {
            Matcher matcher = macro.getKey().matcher(propertyValue);
            if (matcher.matches()) {
                Pattern replacePattern = replacePatterns.get(macro.getKey().toString());
                newValue = newValue.replaceAll(replacePattern.toString(), getProperty(properties, macro.getValue()));
            }
        }
        return newValue;
    }
    
    String getProperty(Multimap<String, String> properties, String key) {
       Collection<String> collection = properties.get(key);
       if (collection.isEmpty())
          return null;
       if (collection.size() != 1)
          throw new RuntimeException("multiple values for key "+key);
       return collection.iterator().next();
    }
}
