# Use a specific version of Tomcat as base image
FROM tomcat:8.0.20-jre8

# Expose port 8080 to access the application
EXPOSE 8080

# Copy the WAR file from the target directory of your Maven project to the Tomcat webapps directory
COPY target/bitsvalley-micro-0.0.1-SNAPSHOT.jar /usr/local/tomcat/webapps/
