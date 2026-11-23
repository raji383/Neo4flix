# 1. Introduction to Neo4j

Neo4j is a **Native Graph Database Management System**.

It belongs to the NoSQL family and specializes in storing and querying highly connected data.

Unlike relational databases that organize data into tables, Neo4j uses nodes and relationships.

## Database comparison

| Database | Type | Data Model |
|---|---|---|
| MySQL | Relational | Tables |
| PostgreSQL | Relational | Tables |
| MongoDB | NoSQL | Documents |
| Redis | NoSQL | Key-Value |
| Neo4j | NoSQL | Graph |

## Why use Neo4j?

Neo4j is particularly useful when relationships are as important as the data itself.

Examples:

- Social networks.
- Recommendation engines.
- Fraud detection.
- Knowledge graphs.
- Dependency analysis.
- Network topology.

---

# 2. Neo4j Property Graph Model

Neo4j uses the Property Graph Model.

It consists of four main elements.

## 2.1 Nodes

Nodes represent entities.

Example:

```cypher
CREATE (u:User {
    name: "Reda",
    age: 25
});
```

Here:

- `u` is a variable.
- `User` is a label.
- `name` and `age` are properties.

## 2.2 Relationships

Relationships connect nodes.

```cypher
CREATE (a:User {name: "Reda"})
       -[:FOLLOWS]->
       (b:User {name: "Ali"});
```

Relationships have:

- A type.
- A direction.
- Optional properties.

Example:

```cypher
CREATE (a)-[:PURCHASED {
    date: "2026-01-01",
    amount: 100
}]->(b);
```

## 2.3 Labels

Labels categorize nodes.

A node can have multiple labels.

```cypher
CREATE (u:User:Admin {
    name: "Reda"
});
```

## 2.4 Properties

Properties are key-value pairs stored on nodes or relationships.

Supported values include:

- Strings.
- Numbers.
- Booleans.
- Dates and temporal values.
- Lists of supported property values.
- Spatial values.

---

# 3. Cypher Query Language

Cypher is Neo4j's declarative query language.

It describes the graph pattern you want to find or modify.

## CREATE

Creates nodes and relationships.

```cypher
CREATE (u:User {
    name: "Reda",
    age: 25
});
```

## MATCH

Finds graph patterns.

```cypher
MATCH (u:User)
RETURN u;
```

## WHERE

Filters results.

```cypher
MATCH (u:User)
WHERE u.age > 18
RETURN u;
```

## RETURN

Returns query results.

```cypher
MATCH (u:User)
RETURN u.name;
```

## MERGE

Matches an existing pattern or creates it if it does not exist.

```cypher
MERGE (u:User {
    email: "reda@example.com"
});
```

## SET

Updates properties or labels.

```cypher
MATCH (u:User {
    name: "Reda"
})
SET u.age = 26;
```

## DELETE

Deletes nodes or relationships.

```cypher
MATCH (u:User {
    name: "Reda"
})
DETACH DELETE u;
```

`DELETE` requires relationships to be removed first. `DETACH DELETE` also removes relationships connected to the matched node.

## OPTIONAL MATCH

Matches a pattern when it exists, otherwise returns null values for the optional part.

```cypher
MATCH (u:User)
OPTIONAL MATCH (u)-[:FOLLOWS]->(other)
RETURN u, other;
```

## WITH

Passes intermediate results between query parts.

```cypher
MATCH (u:User)
WITH u
WHERE u.age > 18
RETURN u;
```

## UNWIND

Transforms a list into rows.

```cypher
UNWIND ["Reda", "Ali", "Sara"] AS name
RETURN name;
```

## ORDER BY and LIMIT

```cypher
MATCH (u:User)
RETURN u
ORDER BY u.age DESC
LIMIT 5;
```

---

# 4. Neo4j Database Features

## ACID Transactions

Neo4j supports ACID transactions.

- **Atomicity:** All operations succeed or the transaction is rolled back.
- **Consistency:** Constraints and database rules remain valid.
- **Isolation:** Concurrent transactions are managed according to the configured isolation behavior.
- **Durability:** Committed changes are persisted.

## Schema Flexibility

Neo4j does not require every node to have exactly the same properties.

For example:

```cypher
CREATE (:User {
    name: "Reda",
    age: 25
});

CREATE (:User {
    name: "Ali",
    email: "ali@example.com"
});
```

Both nodes can share the `User` label while having different properties.

## Constraints

Constraints enforce data integrity.

Example:

```cypher
CREATE CONSTRAINT user_email_unique
FOR (u:User)
REQUIRE u.email IS UNIQUE;
```

## Indexes

Indexes help the database locate nodes or relationships efficiently.

They are not automatically used by every query; the planner chooses an appropriate access strategy.

---

# 5. Neo4j Under the Hood

Understanding Neo4j internally requires separating two concepts:

1. **Logical Model:** How developers see the graph.
2. **Physical Storage Model:** How the database stores and accesses information.

The logical model contains:

- Nodes.
- Relationships.
- Labels.
- Properties.

The physical model contains storage structures, records or blocks, references, indexes, and cached pages.

## 5.1 Storage Engine

The Storage Engine is responsible for reading and writing database data.

It manages:

- Nodes.
- Relationships.
- Properties.
- Indexes.
- Storage files.
- Transactions.

## 5.2 Node Records

In record-based store formats, node records represent nodes and contain information needed to locate their labels, properties, and connected relationships.

## 5.3 Relationship Records

Relationship records represent connections.

They contain information such as:

- Start node.
- End node.
- Relationship type.
- Properties reference.
- Links used to navigate connected relationships.

## 5.4 Property Records

Properties store entity attributes.

For example:

```text
User:
    name = "Reda"
    age = 25
```

The physical representation depends on the selected store format and the size and type of the property values.

## 5.5 Store Formats

Neo4j supports different storage formats depending on version and edition.

Examples include:

- Standard.
- Aligned.
- High Limit.
- Block.

Traditional record-based formats organize graph data into records and references.

The newer Block format uses a different physical organization, with block-oriented files and storage for larger values.

Do not assume that every Neo4j installation uses the same record layout or file structure.

---

# 6. How Neo4j Stores Data

## 6.1 Native Relationship Storage

Neo4j is designed to store relationships as first-class graph structures.

This makes following connections a fundamental storage operation rather than reconstructing every relationship through a traditional table join.

## 6.2 Sparse Nodes

Sparse nodes have relatively few relationships.

Their relationship structures are generally simpler.

## 6.3 Dense Nodes

Dense nodes have many relationships.

Neo4j uses additional relationship organization to make navigation more efficient.

In traditional record-based formats, the default relationship grouping threshold is commonly 50 relationships and can be configured.

The exact behavior depends on the store format and database configuration.

## 6.4 Dynamic Storage

Large property values may need additional storage structures instead of fitting into the main record or block.

## 6.5 Inline Properties

Some values can be stored directly in the main storage representation when the selected format supports it and the values are suitable.

This can reduce additional storage lookups.

---

# 7. How Neo4j Retrieves Data

Suppose we execute:

```cypher
MATCH (u:User {
    name: "Reda"
})-[:FOLLOWS]->(friend:User)
RETURN friend;
```

Conceptually, Neo4j performs the following work:

1. Finds a starting `User` node.
2. Uses an appropriate index if the planner determines one is beneficial.
3. Reads the matching node.
4. Follows its outgoing `FOLLOWS` relationships.
5. Finds the connected users.
6. Retrieves the required properties.
7. Returns the resulting records.

The important idea is that relationships are directly represented in the graph storage model.

## Traversal

Traversal is the process of navigating from one node to connected nodes through relationships.

For example:

```cypher
MATCH (a:User)-[:FOLLOWS]->(b:User)
RETURN a, b;
```

This query describes a graph pattern involving two nodes connected by a directed relationship.

---

# 8. Query Execution Pipeline

When Neo4j receives a Cypher query, it goes through several stages.

```text
Client Application
       |
       v
   Cypher Query
       |
       v
     Parsing
       |
       v
  Query Planning
       |
       v
 Optimization
       |
       v
 Physical Execution Plan
       |
       v
   Query Runtime
       |
       v
  Storage Engine
       |
       v
   Query Results
       |
       v
      Client
```

## 8.1 Parsing

The query is analyzed for syntax and transformed into an internal representation.

## 8.2 Planning

Neo4j determines how to execute the query.

## 8.3 Optimization

The planner evaluates possible execution strategies and chooses a suitable plan based on available information and statistics.

## 8.4 Execution

The selected execution plan runs through operators that access indexes, retrieve nodes, traverse relationships, and process results.

## 8.5 Results

The resulting records are returned to the client, for example through the Bolt protocol.

---

# 9. Indexes and Query Optimization

Indexes reduce the work needed to locate graph entities.

## Common index types

- Token Lookup Index: Helps find nodes by labels and relationships by types.
- Range Index: Supports equality, range, and ordering operations on supported values.
- Text Index: Supports text-oriented searches.
- Point Index: Supports spatial queries.
- Full-text Index: Supports full-text search.
- Vector Index: Supports similarity search over vector embeddings.

## Example: Create a Range Index

```cypher
CREATE RANGE INDEX user_name_index
FOR (u:User)
ON (u.name);
```

## Example: Query Using the Indexed Property

```cypher
MATCH (u:User {
    name: "Reda"
})
RETURN u;
```

The query planner may use the index if it considers that strategy appropriate.

## Important Operators

| Operator | Purpose |
|---|---|
| NodeByLabelScan | Scans nodes with a specific label |
| NodeIndexSeek | Looks up nodes through an index |
| Expand(All) | Traverses relationships |
| Filter | Filters rows |
| Projection | Computes or selects output values |
| ProduceResults | Produces final query results |

The exact operators depend on the query, data, indexes, statistics, and runtime.

---

# 10. Performance Analysis

Neo4j provides two important commands for inspecting query execution.

## EXPLAIN

```cypher
EXPLAIN
MATCH (u:User {
    name: "Reda"
})
RETURN u;
```

- Builds an execution plan.
- Does not execute the query.
- Useful for inspecting the planned strategy.

## PROFILE

```cypher
PROFILE
MATCH (u:User {
    name: "Reda"
})
RETURN u;
```

- Executes the query.
- Displays actual execution information.
- Helps analyze performance and identify expensive operators.

Be careful: `PROFILE` executes the query. If the query writes data, those writes can occur.

## Important Metrics

### DB Hits

Represents logical storage operations performed by query operators.

It is not the same as the number of physical disk reads.

### Page Cache Hits

Indicates data access served from the database page cache.

### Page Cache Misses

Indicates that the requested data was not found in the page cache and needed to be loaded from storage.

### Execution Time

Measures the time required to execute the query.

### Rows

Indicates how many records flow through an execution operator.

## Execution Plan Reading

Execution plans are generally read from the bottom toward the top.

Leaf operators start retrieving data, and higher operators process the resulting rows until the final results are produced.

---

# 11. Neo4j vs Relational Databases

| Concept | Relational Database | Neo4j |
|---|---|---|
| Data structure | Tables | Nodes and Relationships |
| Connections | Foreign Keys | Native Relationships |
| Query language | SQL | Cypher |
| Relationship traversal | JOIN operations | Graph Traversal |
| Schema | Usually predefined | Flexible |
| Indexes | B-Tree, Hash, etc. | Range, Text, Point, Vector, etc. |
| Transactions | ACID | ACID |
| Main strength | Structured relational data | Highly connected data |

## Important clarification

Relational databases can also handle relationships and graph-like queries.

Neo4j's advantage is its native graph model and relationship-oriented traversal.

It does not mean that Neo4j is always faster than PostgreSQL or MySQL. Performance depends on the data model, query patterns, indexes, workload, and execution strategy.

---

# Final Summary

The complete learning path can be represented as:

```text
GRAPH THEORY
    |
    |-- Nodes & Edges
    |-- Directed / Undirected
    |-- Weighted / Unweighted
    |-- Cyclic / Acyclic
    |-- Sparse / Dense
    |
    v
GRAPH REPRESENTATION
    |
    |-- Adjacency List
    |-- Adjacency Matrix
    |-- Edge List
    |
    v
GRAPH ALGORITHMS
    |
    |-- BFS
    |-- DFS
    |-- Shortest Path
    |-- Cycle Detection
    |-- Reachability
    |
    v
NEO4J
    |
    |-- Property Graph Model
    |-- Nodes
    |-- Relationships
    |-- Labels
    |-- Properties
    |
    v
CYPHER
    |
    |-- CREATE
    |-- MATCH
    |-- MERGE
    |-- WHERE
    |-- RETURN
    |-- DELETE
    |
    v
INTERNAL ARCHITECTURE
    |
    |-- Storage Engine
    |-- Node Records
    |-- Relationship Records
    |-- Properties
    |-- Sparse / Dense Nodes
    |-- Store Formats
    |
    v
QUERY EXECUTION
    |
    |-- Parsing
    |-- Planning
    |-- Optimization
    |-- Index Lookup
    |-- Traversal
    |-- Results
    |
    v
PERFORMANCE
    |
    |-- Indexes
    |-- Page Cache
    |-- Transaction Logs
    |-- Checkpoints
    |-- EXPLAIN
    |-- PROFILE
    |-- DB Hits
```

## Recommended Official Resources

- [Neo4j Documentation](https://neo4j.com/docs/)
- [Cypher Manual](https://neo4j.com/docs/cypher-manual/current/)
- [Neo4j Graph Data Science](https://neo4j.com/docs/graph-data-science/current/)
- [Neo4j GraphAcademy](https://graphacademy.neo4j.com/)
- [Neo4j Store Formats](https://neo4j.com/docs/operations-manual/current/database-internals/store-formats/)

---

**Learning Objective:** Understand not only how to write Cypher queries, but also how graph data is represented, stored, traversed, retrieved, and optimized internally by Neo4j.