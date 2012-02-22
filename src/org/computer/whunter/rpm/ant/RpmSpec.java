/**
 * Copyright Warwick Hunter 2012. All rights reserved.
 */
package org.computer.whunter.rpm.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

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
    
    @Override
    public void execute() throws BuildException {
        StringBuilder prefix = new StringBuilder(m_env).append(".");
        Project project = getProject();
        log("project " + project.getProperty("ant.project.name"));
        log("parsing " + m_srcfile);
        project.setProperty(prefix.toString() + "wasa", "was here");
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

}
