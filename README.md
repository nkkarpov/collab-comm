# Experiments

## Compile:
Run in the folder with pom.xml
```
mvn package
```
## Run
Copy `target/consoleApp-1.0-SNAPSHOT-jar-with-dependencies.jar` in the same folder with `movie.txt` and `movienon.txt`. Run the experiments with command

```
java -jar consoleApp-1.0-SNAPSHOT-jar-with-dependencies.jar <IID> <T> <AGENT> <M>
```
set IID to iid or noniid
set T to the running time
set AGENT to the number of agents
set M to the number of best arms

For example,

```java -jar consoleApp-1.0-SNAPSHOT-jar-with-dependencies.jar iid 30000 10 1```

