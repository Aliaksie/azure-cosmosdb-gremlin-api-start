## Overview
Simple java app that show how to work with Azure Cosmos DB with Graph(Gremlin) API, which provides a graph data model with [Gremlin query/traversals](https://tinkerpop.apache.org/gremlin.html).

## Settings
* An active Azure account or, you can use the [Azure Cosmos DB Emulator](https://azure.microsoft.com/documentation/articles/documentdb-nosql-local-emulator) for this tutorial.
* JDK 11
* Maven

| Setting        | Suggested Value | Description |
| -------------- | --------------- | ----------- |
| hosts          | [***.gremlin.cosmosdb.azure.com] | This is the Gremlin URI value. |
| port           | 443 | Set the port to 443 |
| username       | `/dbs/<db>/colls/<coll>` | The resource of the form `/dbs/<db>/colls/<coll>` where `<db>` is your database name and `<coll>` is your collection name. |
| password       | primary_key | This is primary key for connection |
| connectionPool | `{enableSsl: true}` | Your connection pool setting for SSL. |
| serializer     | { className: org.apache.tinkerpop.gremlin.driver.ser.GraphSONMessageSerializerV1d0, config: { serializeResultToString: true }} | Set to this value. |

## Links
- [Azure Cosmos DB : Graph API](https://docs.microsoft.com/en-us/azure/cosmos-db/graph-introduction)
- [Gremlin Java SDK](http://tinkerpop.apache.org/docs/current/reference/#gremlin-java)
- [Gremlin Java Doc](http://tinkerpop.apache.org/javadocs/current/full/)