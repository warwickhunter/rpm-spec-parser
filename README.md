The rpm-spec-parser is a Java API and Ant task that allows you to parse an 
RPM spec file and extract items that are defined inside it. 

It's useful for building java code that is to be shipped inside an RPM. 
The RPM can be used as the single place where the version of the software 
is defined and the ant scripts can query the spec file when building to get 
the version number and other interesting fields.

Here is an example of how you might use the ant task.

    <project name="rpm-spec-parser-test" default="test">

        <taskdef 
            name="rpmspec" 
            classname="org.computer.whunter.rpm.ant.RpmSpec" 
            classpath="dist/rpm-spec-parser-0.2.jar" />

        <target name="init" description="get the info from the rpm spec file">
            <rpmspec srcfile="tests/specs/p4bugzilla.spec" env="r" />
            <property name="foo" value="${r.source0}"/>
        </target>

        <target name="test" depends="init" description="do something">
            <echo>rpm.name=${r.name}</echo>
            <echo>rpm.version=${r.version}</echo>
            <echo>rpm.release=${r.release}</echo>
            <echo>rpm.source0=${r.source0}</echo>
            <echo>rpm.who=${r.who}</echo>
            <echo>foo=${foo}</echo>
        </target>

    </project>

Here is an example of how to use the Java API.

        try {
            RpmSpecParser parser = RpmSpecParser.createParser("tests/specs/p4bugzilla.spec");
            Properties properties = parser.parse();
            System.out.printf("RPM name: %s %n", properties.getProperty("name"));
            System.out.printf("RPM version: %s-%s %n", properties.getProperty("version"), properties.getProperty("release"));
        }
        catch (FileNotFoundException e) {
            // ...
        }

Warwick Hunter 2012-02-25