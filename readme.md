## Github Code Search
A java application to search an example of a java code. Built using maven.

If you get an error on authorization please change the OAUTH_TOKEN using your own. Visit this [link](https://github.com/settings/tokens) to create it.

How to run
```
mvn clean compile assembly:single

java -cp target/github-code-search-1.0-SNAPSHOT-jar-with-dependencies.jar com.project.githubsearch.App
```