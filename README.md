# Data Contract Migration Rules

This project shows Confluent Data Contract Migration Rules in Java and C#.

## Prerequisites

General:

- Bash, ZSH, or WSL
- curl
- jq
- Docker
- Docker Compose

For the Java example:

- Java 21
- OpenJDK 21.0.2
- Gradle 8.7.

For the C# example:

- DotNet SDK 9.0

## Running the Examples

First navigate to the root of the project.

To etup the Confluent Platform environment as follows:

```shell
./setup.sh
```

You can reach control center at: `localhost:9021`

For the Java example run the following commands:

```shell
gradle --refresh-dependencies
gradle build
gradle run
```

To run the C# example run the following commands:

```shell
dotnet restore src/main/dotnet/
dotnet build src/main/dotnet/
dotnet run --project src/main/dotnet/
```

What you should be able to see in the promt is the migration rules in action, the producers vary, but the individual consumer versions are consistent in their format.

To clean up the environment run:

```shell
./teardown.sh
```

## Notes

Do not recreate code from the schemas, the generated code has been slightly modified to be able to run in the same project.

You can see the schemas and data contracts in the `schema folder` they are separate for readability to assembled in the `setup.sh` script when deployed. The migration rules rely on separate compatibility groups being active when deploying:

```shell
curl http://localhost:8081/config/user-topic-value \
  -X PUT -H "Content-Type: application/vnd.schemaregistry.v1+json" --data \
  '{ "compatibilityGroup": "major.version" }' | \
  jq

jq --arg data "$(jq -c . "schema/user_v1.avsc")" '.schema = $data' "schema/user_v1.json" | \
  curl http://localhost:8081/subjects/user-topic-value/versions \
  -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" --data @- | \
  jq

jq --arg data "$(jq -c . "schema/user_v2.avsc")" '.schema = $data' "schema/user_v2.json" | \
  curl http://localhost:8081/subjects/user-topic-value/versions \
  -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" --data @- | \
  jq
```

Notice `major.version`, which is an arbitrary chosen property name, and the way we deploy schemas to the different compatibiliy groups is by specifying it as follows in the POST payload:

```json
{
    "schema": "...",
    "schemaType": "...",
    "metadata": {
        "properties": {
            "major.version": 1
        }
    },
    "ruleSet": {
        "domainRules": [],
        "migrationRules": []
    }
}
```

Also, how we choose to differentiate values, in this case a number of `1` can by value and type of value.

Further details can be found in the official: [documentation](https://docs.confluent.io/platform/current/schema-registry/fundamentals/data-contracts.html)