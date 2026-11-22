# 01 — Java 21 for TypeScript developers

## The mental model
| TypeScript / Node | Java | Notes |
|---|---|---|
| `package.json` | `pom.xml` (Maven) | dependencies + build |
| `npm install` / `npm run build` | `mvn package` | produces `target/*.jar` |
| `node dist/main.js` | `java -jar app.jar` | |
| ES module / file | `package com.x.y;` + one public class per file | folder path **must** match the package |
| `import { X } from './x'` | `import com.walkthrough.api.task.Task;` | imports by fully qualified name |
| `interface User { name: string }` | `record User(String name) {}` | immutable data class |
| `class` | `class` | but fields are typed and access is enforced at compile time |
| `string`, `number`, `boolean` | `String`, `int`/`long`/`double`, `boolean` | numbers have distinct types |
| `number \| null` | `Long` (wrapper, may be null) vs `long` (primitive, never null) | |
| `T[]` / `Array<T>` | `List<T>` | `List.of(...)` is immutable |
| `Record<K, V>` / `Map` | `Map<K, V>` | |
| `arr.map(f).filter(g)` | `list.stream().map(f).filter(g).toList()` | streams = lazy pipelines |
| `x => x * 2` | `x -> x * 2` | lambda |
| `obj.method.bind(obj)` | `Obj::method` | method reference |
| `async/await`, Promise | blocking calls + (virtual) threads | Java 21 virtual threads make blocking cheap |
| `try/catch` | `try/catch` | plus *checked* exceptions the compiler forces you to handle |
| decorators `@Component()` | annotations `@Service` | metadata read by the framework |

## Java 21 features used in this repo
### Records — `backend/.../summary/dto/SummaryResponse.java`
```java
public record CountItem(String key, String label, long count) {}
// gives you: constructor, item.key(), equals/hashCode/toString - all generated.
```

### Switch expressions (exhaustive) — `backend/.../task/TaskStatus.java`
```java
return switch (this) {
    case TODO -> "To Do";
    case IN_PROGRESS -> "In Progress";
    case DONE -> "Done";
};  // add a new enum constant and forget it here -> compile error
```

### Text blocks — `ProjectRepository.java`
Multi-line strings with `"""` (like JS template literals, without interpolation). Used for JPQL queries.

### Sequenced collections — `SummaryService.priorityBreakdown()`
`list.reversed()`, `list.getFirst()`, `list.getLast()` are new in Java 21.

### Virtual threads — `application.yml` → `spring.threads.virtual.enabled: true`
Node handles concurrency with one event loop and non-blocking I/O. Classic Java used one *OS thread*
per request (expensive). Virtual threads are tiny JVM-managed threads: you write simple blocking code
(`repository.count()`), and the JVM parks the thread while waiting on the DB, much like `await`.

## Things that surprise JS developers
- **`==` on objects compares references.** Use `a.equals(b)` for Strings and objects.
- **No `undefined`.** Only `null`. `Optional<T>` is the idiomatic "maybe" return type.
- **Compile step catches a lot**: wrong types, missing switch cases, unhandled checked exceptions.
- **Getters/setters**: Java convention is `getName()`/`setName()` (records use `name()`).
- **Everything is in a class.** There are no free functions; use `static` methods.
