## Github Code Search
A java application to search an example of a java code

## Prerequisite

- JDK8
- Maven3
- GitHub OAuth Token

## Getting Started

First, you should set your OAuth token into an environment variable somewhere, like:
```
export GITHUB_OAUTH=xxxxxxx
```
Visit this [link](https://github.com/settings/tokens) to create it.


## How to Run

```
<go to project directory>

mvn clean compile assembly:single

java -cp target/github-code-search-1.0-SNAPSHOT-jar-with-dependencies.jar com.project.githubsearch.App
```