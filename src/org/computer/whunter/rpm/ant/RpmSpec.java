/**
 * Copyright (c) 2012, Warwick Hunter. All rights reserved.
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
package org.computer.whunter.rpm.ant;

import java.io.FileNotFoundException;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Task;
import org.computer.whunter.rpm.parser.RpmSpecParser;

/**
 * An Ant task that parses an RPM Spec file and pushes the information from the spec file
 * into the ant properties.
 * 
 * @author Warwick Hunter (w.hunter@computer.org)
 * @date   2012-02-21
 */
public class RpmSpec extends Task {
    
    private String m_srcfile;
    private String m_env = "rpm";
    private String m_if;
    private String m_unless;
    
    @Override
    public void execute() throws BuildException {
        
        // If the if or unless conditions have been set evaluate them to decide if execution should proceed
        if (m_if != null || m_unless != null) {
            PropertyHelper propertyHelper = PropertyHelper.getPropertyHelper(getProject());
            if (m_if != null) {
                if (!propertyHelper.testIfCondition(m_if)) {
                    // If condition is false, do not execute this task
                    return;
                }
            }
            if (m_unless != null) {
                if (!propertyHelper.testUnlessCondition(m_unless)) {
                    // Unless condition is true, do not execute this task
                    return;
                }
            }
        }
        
        try {
            // Parse the RPM spec file and extract the interesting fields and macro definitions
            RpmSpecParser parser = RpmSpecParser.createParser(m_srcfile);
            Properties properties = parser.parse();
            
            // Push all the fields and macros into the project as properties
            Project project = getProject();
            StringBuilder prefix = new StringBuilder(m_env).append(".");
            for (Object key : properties.keySet()) {
                project.setProperty(prefix.toString() + key, properties.get(key).toString());
            }
        }
        catch (FileNotFoundException e) {
            throw new BuildException("RPM spec file not found", e);
        }
        super.execute();
    }

    public String getSrcfile() {
        return m_srcfile;
    }

    public void setSrcfile(String srcfile) {
        m_srcfile = srcfile;
    }

    public String getEnv() {
        return m_env;
    }

    public void setEnv(String env) {
        m_env = env;
    }

    public String getIf() {
        return m_if;
    }

    public void setIf(String if1) {
        m_if = if1;
    }

    public String getUnless() {
        return m_unless;
    }

    public void setUnless(String unless) {
        m_unless = unless;
    }
}
