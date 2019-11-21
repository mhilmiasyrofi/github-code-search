## Github Code Search
A java application to search an example of a java code

## Prerequisite

- JDK8
- Maven3
- GitHub OAuth Token

## Getting Started

First, you should set your OAuth token into an environment variable, like:
```
export GITHUB_AUTH_TOKEN_1=xxxxxxx
export GITHUB_AUTH_TOKEN_2=xxxxxxx
export GITHUB_AUTH_TOKEN_3=xxxxxxx
```
Visit this [link](https://github.com/settings/tokens) to create it. If you only have one token, please write the token in each env variable. It will works also :)


## How to Run

```
<go to project directory>

mvn clean compile assembly:single

java -cp target/github-code-search-1.0-SNAPSHOT-jar-with-dependencies.jar com.project.githubsearch.App
```
