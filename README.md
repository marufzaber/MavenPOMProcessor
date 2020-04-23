# Maven-pom-processor
This API parses a `pom.xml` file and updates the version of a given plugin

To run this maven project
```
mvn -q compile exec:java -Dexec.args="<project_directory> <plug in name> <version>"

For example, 
mvn -q compile exec:java -Dexec.args="common-math/ maven-surefire-plugin 2.19.1"

```

