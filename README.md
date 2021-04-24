# Tango REST API Test Suite

A Test suite for Tango REST API

# How to integrate this tests suite into a java project #

`{PROJECT_ROOT}/pom.xml`
```xml
<dependency>
  <groupId>de.hereon.tango</groupId>
  <artifactId>rest-test-suite</artifactId>
  <version>{LATEST}</version>
  <classifier>tests</classifier>
  <type>test-jar</type>
  <scope>test</scope>
</dependency>
```

`{PROJECT_ROOT}/pom.xml` or `{M2_HOME}/settings.xml`
```xml
<repository>
    <id>github-hzg</id>
    <url>https://maven.pkg.github.com/hzg-wpi/*</url>
</repository>
``` 

Add corresponding server to settings.xml

```xml
 <server>
    <id>github-hzg</id>
    <username>GITHUB_USER</username>
    <password>GITHUB_TOKEN</password>
</server>
```

# How to run the tests suite #

```BASH
$> git clone https://github.com/tango-controls/rest-api.git 
destination directory: rest-api
requesting all changes
adding changesets
adding manifests
adding file changes
added 1 changesets with 2 changes to 2 files
updating to branch default
2 files updated, 0 files merged, 0 files removed, 0 files unresolved
$> cd rest-api
$> mvn clean test \
    -Dtango.rest.url=http://localhost:8080/tango/rest \
    -Dtango.host=tango-cs \
    -Dtango.port=10000 \
    -Dtango.rest.auth.method=basic \
    -Dtango.rest.user={user} -Dtango.rest.password={password}

[INFO] Scanning for projects…
[…]
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.518 sec

Results :

Tests run: 13, Failures: 0, Errors: 0, Skipped: 0

[INFO] ————————————————————————
[INFO] BUILD SUCCESS
[INFO] ————————————————————————
[INFO] Total time: 3.289 s
[INFO] Finished at: 2015-12-17T18:40:41+01:00
[INFO] Final Memory: 14M/490M
[INFO] ————————————————————————
$> ^_^
```

