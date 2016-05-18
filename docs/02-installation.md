# Installation

## Standalone version

The standalone version of ncWMS requires no installation. It can be run from the command-line with the command `java -jar ncWMS2-standalone.jar` . This will run the WMS server locally with no security for administration and configuration. It will be available at [http://localhost:8080/](http://localhost:8080/). All configuration data is stored in a directory named `.ncWMS2` in the home directory of the user running the software.

## Servlet Container

ncWMS is a Java servlet which runs on a servlet container such as Tomcat, JBoss, or Glassfish.  Tomcat is the recommended servlet container and is what ncWMS is written and tested on. Installation is servlet-container dependent, but there are no ncWMS-specific procedures for installation.

### Servlet-level Configuration

Once ncWMS is up-and-running, on first launch it will create a configuration file and logging directory. By default this is located in a directory named `.ncWMS2` in the home directory of the user running the servlet container. **Note that the user running the servlet container must have write access to their home directory. This is not always the case for system users such as `tomcat7` or `nobody`.**

To change the location of the server configuration, you need to redefine the context parameter `configDir`. To do this on Tomcat, you should create a file named `$CATALINA_BASE/conf/[enginename]/[hostname]/[webappname].xml`.  For example, with the default webapp name running on localhost this is `$CATALINA_BASE/conf/Catalina/localhost/ncWMS2.xml`. Inside this file, create an entry of the form:

```
<Parameter name="configDir" value="$HOME/.ncWMS2-testserver" override="false"/>
```

Note that `$HOME` represents the home directory of the user running **the servlet container** and is a special value - other environment variables cannot be used here. Since this setting is at the servlet container level, it will persist across redeploys of ncWMS2.


### Security configuration

Security for the administration of ncWMS is delegated to the servlet container (in standalone mode there is no security on any administration). You should define a security role with the name `ncWMS-admin`, and add users with that role. To do this on Tomcat, you could add the following to `tomcat-users.xml`:

```
<role rolename="ncWMS-admin" />
<user username="admin" password="ncWMS-password" roles="ncWMS-admin"/>
```

Access to the administration interface would then be granted to a user with the name `admin` and the password `ncWMS-password`