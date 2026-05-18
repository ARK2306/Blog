# Spring Boot Blog Backend — Learning Document

This document explains everything built in this project from the ground up.
It is written as a learning reference — every concept, annotation, method, and
pattern is explained with the "what" and the "why."

---

## Table of Contents

1. [How Spring Boot Works — The Big Idea](#1-how-spring-boot-works--the-big-idea)
2. [Project Structure](#2-project-structure)
3. [Build Config — pom.xml](#3-build-config--pomxml)
4. [Configuration — application.properties](#4-configuration--applicationproperties)
5. [Domain — Entities](#5-domain--entities)
6. [Repositories](#6-repositories)
7. [DTOs and Mappers](#7-dtos-and-mappers)
8. [Services](#8-services)
9. [Controllers](#9-controllers)
10. [Error Handling](#10-error-handling)
11. [Security — Deep Dive](#11-security--deep-dive)
12. [Auth and JWT — Deep Dive](#12-auth-and-jwt--deep-dive)
13. [How It All Wires Together](#13-how-it-all-wires-together)
14. [Spring Boot App Building Roadmap](#14-spring-boot-app-building-roadmap)

---

## 1. How Spring Boot Works — The Big Idea

### The problem Spring solves

In plain Java, to use a service inside a controller you would do:

```java
// Plain Java — you manage everything yourself
CategoryRepository repo = new CategoryRepository(dataSource);
CategoryService service = new CategoryServiceImpl(repo);
CategoryController controller = new CategoryController(service);
```

You create every object, wire every dependency, manage every lifecycle.
In a real app with dozens of classes this becomes unmanageable.

### Inversion of Control (IoC)

Spring flips this. Instead of you creating and wiring objects, you *declare*
what you need and Spring creates and wires everything at startup.

```java
// Spring way — you declare, Spring wires
@RestController
public class CategoryController {
    private final CategoryService categoryService; // Spring injects this
}
```

This is called **Inversion of Control** — you give up control of object
creation to the framework. The objects Spring manages are called **Beans**.

### Dependency Injection (DI)

When Spring creates `CategoryController` it sees it needs a `CategoryService`.
It looks in its registry of beans, finds `CategoryServiceImpl` (which is
annotated `@Service`), creates it (injecting *its* dependencies first), and
passes it into the controller. This chain is **Dependency Injection**.

### How Spring finds your classes

`@SpringBootApplication` on `BlogApplication` includes `@ComponentScan`.
This tells Spring to scan the entire `com.ark.blog` package at startup and
register anything annotated with:

| Annotation | Meaning |
|---|---|
| `@Component` | Generic bean |
| `@Service` | Business logic bean |
| `@Repository` | Data access bean |
| `@Controller` / `@RestController` | HTTP handler bean |
| `@Configuration` + `@Bean` | Manually defined beans |

Spring reads all these annotations, builds a dependency graph, and wires
everything before the app starts accepting requests.

### Auto-configuration

Spring Boot reads your `pom.xml` dependencies and automatically configures
sensible defaults. For example:
- You add `spring-boot-starter-data-jpa` → Spring configures Hibernate,
  creates a datasource, sets up transaction management automatically.
- You add `spring-boot-starter-security` → Spring adds a security filter
  chain with basic defaults automatically.

You override these defaults in `application.properties` or with your own
`@Configuration` classes.

---

## 2. Project Structure

```
src/main/java/com/ark/blog/
│
├── BlogApplication.java              ← entry point
│
├── config/
│   └── SecurityConfig.java           ← security filter chain config
│
├── controllers/
│   ├── AuthController.java           ← POST /api/v1/auth (login)
│   ├── CategoryController.java       ← CRUD for categories
│   └── ErrorController.java         ← global error handler
│
├── domain/
│   ├── PostStatus.java               ← enum: DRAFT / PUBLISHED
│   ├── entities/
│   │   ├── User.java                 ← users table
│   │   ├── Post.java                 ← posts table
│   │   ├── Category.java             ← categories table
│   │   └── Tag.java                  ← tags table
│   └── dtos/
│       ├── AuthResponse.java         ← { token, expiresIn }
│       ├── LoginRequest.java         ← { email, password }
│       ├── CategoryDto.java          ← category API response shape
│       ├── CreateCategoryRequest.java← category creation input
│       └── ApiErrorResponse.java     ← standard error shape
│
├── mappers/
│   └── CategoryMapper.java           ← entity ↔ DTO conversion
│
├── repositories/
│   ├── UserRepository.java
│   ├── CategoryRepository.java
│   ├── PostRepository.java
│   └── TagRepository.java
│
├── security/
│   ├── AuthenticationService.java    ← interface: authenticate + generateToken
│   ├── BlogUserDetails.java          ← wraps User for Spring Security
│   ├── BlogUserDetailsService.java   ← loads user from DB for Spring Security
│   └── JwtAuthenticationFilter.java  ← reads JWT on every request
│
└── services/
    ├── CategoryService.java          ← interface
    └── impl/
        ├── AuthenticationServiceImpl.java  ← JWT logic
        └── CategoryServiceImpl.java        ← category business logic
```

Every HTTP request flows through these layers in order:

```
Request → Security Filter → Controller → Service → Repository → Database
                                ↕
                         DTOs (input/output shapes)
```

---

## 3. Build Config — pom.xml

Maven's `pom.xml` is the project's dependency and build configuration.

### Key dependencies

**`spring-boot-starter-web`**
Bundles an embedded Tomcat server and Spring MVC. Your app runs without
deploying to an external server — `java -jar app.jar` starts everything.

**`spring-boot-starter-data-jpa`**
Brings in Hibernate (the ORM) and Spring Data JPA. Hibernate translates your
Java entity classes into SQL tables and queries so you rarely write raw SQL.

**`spring-boot-starter-security`**
Adds the Spring Security filter chain. The moment this is on the classpath,
every endpoint is locked down by default — you then configure what's public.

**`spring-boot-starter-validation`**
Enables `@NotBlank`, `@Size`, `@Pattern`, etc. on DTO fields. When used with
`@Valid` on a controller parameter, Spring automatically validates input and
returns 400 errors if validation fails.

**`jjwt-api`, `jjwt-impl`, `jjwt-jackson`**
The JJWT library. Used to create (sign) and parse (verify) JWT tokens.
Three artifacts because the API, implementation, and JSON serialization
are separated — you need all three.

**`mapstruct`**
Generates entity ↔ DTO conversion code at compile time. You write an
interface, MapStruct writes the implementation. No reflection at runtime,
so it's fast and type-safe.

**`lombok`**
Generates boilerplate (getters, setters, constructors, builders, loggers)
at compile time via annotations. `@Getter`, `@Setter`, `@Builder`,
`@RequiredArgsConstructor`, `@Slf4j`, etc. The class file ends up with all
the methods but you never write them.

**`postgresql`** (runtime) + **`h2`** (test)
PostgreSQL is the production database driver. H2 is an in-memory database
used in tests so they don't need a real Postgres instance running.

---

## 4. Configuration — application.properties

### Production (`src/main/resources/application.properties`)

```properties
spring.application.name=blog
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=Aryanksql5!
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

`ddl-auto=update` — Hibernate reads your entity classes and automatically
updates the database schema to match. It adds new columns and creates new
tables but never drops anything. Good for development, not for production
(use migrations like Flyway instead in production).

`show-sql=true` — prints every SQL query Hibernate runs to the console.
Invaluable for debugging and understanding what Hibernate is actually doing.

### Test (`src/test/resources/application.properties`)

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
```

`create-drop` — creates the schema fresh when tests start, drops it
when they finish. Each test run starts with a clean database.

---

## 5. Domain — Entities

Entities are Java classes that map to database tables. JPA (via Hibernate)
reads these classes and manages the translation between objects and rows.

### Key JPA annotations

**`@Entity`** — marks the class as a database table.

**`@Table(name = "users")`** — specifies the table name. Without it,
Hibernate uses the class name.

**`@Id`** — marks the primary key field.

**`@GeneratedValue(strategy = GenerationType.UUID)`** — tells the database
to auto-generate a UUID when a new row is inserted. You never set `id` yourself.

**`@Column(nullable = false, unique = true)`** — maps the field to a column
with constraints. These constraints are enforced at both the database and
Hibernate level.

**`@Enumerated(EnumType.STRING)`** — stores the enum as its string name
("DRAFT", "PUBLISHED") instead of its ordinal number (0, 1). Always use
`STRING` — if you use `ORDINAL` and reorder the enum values, your data corrupts.

### Lombok annotations on entities

**`@Getter` / `@Setter`** — generates `getX()` / `setX()` for every field.

**`@NoArgsConstructor`** — generates a no-argument constructor. JPA requires
this to instantiate entities when loading from the database.

**`@AllArgsConstructor`** — generates a constructor with every field as a
parameter. Used alongside `@Builder`.

**`@Builder`** — generates a fluent builder pattern so you can construct
objects like:
```java
User user = User.builder()
    .email("test@test.com")
    .name("Aryan")
    .build();
```

### Lifecycle hooks

**`@PrePersist`** — runs before an entity is first saved (INSERT).
Used to auto-set `createdAt`.

**`@PreUpdate`** — runs before an entity is updated (UPDATE).
Used to auto-set `updatedAt`.

```java
@PrePersist
protected void onCreate() {
    this.createdAt = LocalDateTime.now();
}

@PreUpdate
protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
}
```

You never call these methods yourself. Hibernate calls them automatically
at the right moment.

### Relationships

**`@OneToMany` + `@ManyToOne`** — one user has many posts.

```java
// In User.java
@OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Post> posts = new ArrayList<>();

// In Post.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "author_id", nullable = false)
private User author;
```

`mappedBy = "author"` — tells JPA that `Post.author` is the owning side
(the side that has the foreign key column in the database). The `User` side
doesn't create any column — it just navigates the relationship.

`cascade = CascadeType.ALL` — any operation on User (save, delete) cascades
to its posts. Delete a user → their posts are deleted too.

`orphanRemoval = true` — if a post is removed from `user.posts`, it's also
deleted from the database. Without this, removing from the list would just
break the relationship but leave a dangling row.

`fetch = FetchType.LAZY` — don't load posts when you load a user. Only
fetch them when `user.getPosts()` is actually called. This is almost always
what you want — loading every post every time you need a user would be
extremely wasteful.

`@JoinColumn(name = "author_id")` — the foreign key column in the `posts`
table that stores the user's ID.

**`@ManyToMany`** — posts and tags: one post can have many tags, one tag
can be on many posts.

```java
// In Post.java (owning side)
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
    name = "post_tags",
    joinColumns = @JoinColumn(name = "post_id"),
    inverseJoinColumns = @JoinColumn(name = "tag_id")
)
private Set<Tag> tags = new HashSet<>();

// In Tag.java (inverse side)
@ManyToMany(mappedBy = "tags")
private Set<Post> posts = new HashSet<>();
```

`@JoinTable` — defines the join table (`post_tags`) that holds the pairs.
Only one side defines the join table (the "owning" side). The other side
uses `mappedBy` to point back to it.

`HashSet` instead of `List` for tags — sets automatically prevent duplicates
and are more appropriate for many-to-many where order doesn't matter.

### equals() and hashCode()

Every entity implements these based on its fields (not the default Java
identity). This matters for collections — when you do `posts.contains(post)`,
Java uses `equals()` to check. If you don't override it, two `Post` objects
with the same data are considered different objects.

---

## 6. Repositories

Repositories are the data access layer. Spring Data JPA generates the
implementation from your interface at startup — you write zero SQL for
standard operations.

### JpaRepository

```java
public interface UserRepository extends JpaRepository<User, UUID> { }
```

Extending `JpaRepository<User, UUID>` gives you these for free:
- `save(entity)` — INSERT or UPDATE
- `findById(id)` — SELECT by primary key, returns `Optional<User>`
- `findAll()` — SELECT all rows
- `deleteById(id)` — DELETE by primary key
- `existsById(id)` — SELECT COUNT > 0
- `count()` — SELECT COUNT(*)
- And more

The two generic types are: the entity class (`User`) and the primary key
type (`UUID`).

### Derived queries

Spring Data reads method names and generates SQL:

```java
Optional<User> findByEmail(String email);
// → SELECT * FROM users WHERE email = ?

boolean existsByNameIgnoreCase(String name);
// → SELECT COUNT(*) > 0 FROM categories WHERE LOWER(name) = LOWER(?)
```

Spring parses the method name character by character:
- `find` / `get` / `read` → SELECT
- `exists` → SELECT COUNT > 0
- `delete` → DELETE
- `By` → WHERE
- Field names after `By` → column conditions
- `IgnoreCase` → LOWER() comparison
- `And` / `Or` → logical operators

If the method name doesn't match any entity field, the app **fails at
startup** — which is good because you catch mistakes immediately.

### Custom JPQL queries

When derived queries aren't enough, use `@Query` with JPQL (Java Persistence
Query Language — like SQL but using entity/field names instead of table/column names):

```java
@Query("SELECT c FROM Category c LEFT JOIN FETCH c.post")
List<Category> findAllWithPostCount();
```

`LEFT JOIN FETCH` — loads the category AND its posts in a single query.
Without `FETCH`, accessing `category.getPost()` after the query would trigger
a separate SQL query per category (the N+1 problem). With `FETCH`, it's one
query total.

---

## 7. DTOs and Mappers

### Why DTOs exist

You don't expose entities directly over the API. Entities have:
- Lazy-loaded relationships (serializing them can trigger infinite loops)
- Fields you don't want to expose (like passwords)
- Internal state that doesn't belong in an API contract

DTOs (Data Transfer Objects) are clean, controlled shapes for input and output.

### The three DTO types in this project

**Request DTOs** — what the API accepts as input:
```java
public class CreateCategoryRequest {
    @NotBlank
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9 ]+$")
    private String name;
}
```
Validation annotations here control what's accepted. When the controller
uses `@Valid`, Spring checks these before your code runs.

**Response DTOs** — what the API returns:
```java
public class CategoryDto {
    private UUID id;
    private String name;
    private long postCount; // computed, not stored
}
```
`postCount` is not a column — it's calculated from the posts list.
The entity has a list of posts; the DTO has a count. Mapper handles that.

**Auth DTOs** — login input/output:
```java
public class LoginRequest { String email; String password; }
public class AuthResponse { String token; long expiresIn; }
```

### MapStruct

MapStruct is a compile-time code generator. You write an interface:

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    @Mapping(target = "postCount", source = "post", qualifiedByName = "calculatePostCount")
    CategoryDto toDto(Category category);

    Category toEntity(CreateCategoryRequest request);

    @Named("calculatePostCount")
    default long calculatePostCount(List<Post> posts) {
        if (posts == null) return 0;
        return posts.stream()
            .filter(post -> PostStatus.PUBLISHED.equals(post.getStatus()))
            .count();
    }
}
```

At compile time, MapStruct generates a class `CategoryMapperImpl` that
copies fields by name. For fields that match by name, no annotation needed.
For mismatches or computed fields, you use `@Mapping`.

**`componentModel = "spring"`** — makes MapStruct register the generated
implementation as a Spring bean so you can inject it with `@Autowired` /
constructor injection.

**`unmappedTargetPolicy = ReportingPolicy.IGNORE`** — don't warn if
destination has fields that the source doesn't. Useful because DTOs often
have fewer fields than entities.

**`@Mapping(target = "postCount", source = "post", qualifiedByName = "calculatePostCount")`**
— the `postCount` field on `CategoryDto` should be populated by calling
the method named `"calculatePostCount"` with the `post` field from `Category`.

**`@Named("calculatePostCount")`** — marks this method as a named custom
mapping method that `@Mapping` can reference.

**`default`** on an interface method — Java 8+ allows default implementations
in interfaces. MapStruct uses this to let you write custom logic inside the
mapper interface itself without needing a separate implementation class.

The `.stream().filter().count()` pattern:
```java
posts.stream()                                     // create a stream from the list
    .filter(post -> PostStatus.PUBLISHED.equals(post.getStatus())) // keep only PUBLISHED
    .count();                                       // count what's left
```

---

## 8. Services

Services hold business logic. They sit between controllers (HTTP concerns)
and repositories (database concerns).

### Interface + Implementation pattern

```java
// CategoryService.java — the contract
public interface CategoryService {
    List<Category> listCategories();
    Category createCategory(Category category);
    void deleteCategory(UUID id);
}

// CategoryServiceImpl.java — the implementation
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService { ... }
```

Why split them? Controllers depend on the interface, not the implementation.
This means:
- You can swap implementations without touching controllers
- You can mock the interface in tests cleanly
- The interface documents what the service does without the how

**`@Service`** — marks this as a Spring bean. Spring finds it during
component scan and registers it. When a controller needs a `CategoryService`,
Spring provides this implementation.

**`@RequiredArgsConstructor`** (Lombok) — generates a constructor for all
`final` fields. Spring uses this constructor to inject dependencies:

```java
private final CategoryRepository categoryRepository;
// Spring sees: "CategoryServiceImpl needs a CategoryRepository bean"
// Spring creates CategoryRepository, injects it here
```

**`@Transactional`** on `createCategory` — wraps the method in a database
transaction. If anything throws an exception, all DB changes in that method
are rolled back. Essential for operations that must be atomic.

### Business logic examples

**Duplicate check:**
```java
if (categoryRepository.existsByNameIgnoreCase(category.getName())) {
    throw new IllegalArgumentException("Category with name '" + category.getName() + "' already exists");
}
```
The service enforces business rules before delegating to the repository.
`IllegalArgumentException` is caught by `ErrorController` and returned as 400.

**Delete guard:**
```java
Optional<Category> category = categoryRepository.findById(id);
if (category.isPresent()) {
    if (!category.get().getPost().isEmpty()) {
        throw new IllegalArgumentException("Category cannot be deleted because it has posts");
    }
}
categoryRepository.deleteById(id);
```
`Optional<T>` — a container that either holds a value or is empty. Safer
than returning null. `.isPresent()` checks if a value exists, `.get()` retrieves it.
If the category doesn't exist, `deleteById` is still called — it's a no-op
for non-existent IDs.

---

## 9. Controllers

Controllers are the HTTP layer. They receive requests, call services,
and return responses.

### Key annotations

**`@RestController`** — combines `@Controller` and `@ResponseBody`. Every
method return value is serialized to JSON automatically.

**`@RequestMapping(path = "/api/v1/categories")`** — base path for all
endpoints in this controller.

**`@GetMapping`** / **`@PostMapping`** / **`@DeleteMapping`** — map HTTP
methods to handler methods.

**`@PathVariable UUID id`** — extracts `{id}` from the URL path and
converts it to UUID.

**`@RequestBody`** — deserializes the JSON request body into the parameter type.

**`@Valid`** — triggers validation on the parameter. Spring checks all
`@NotBlank`, `@Size`, etc. annotations on the DTO. If validation fails,
Spring throws a `MethodArgumentNotValidException` before your method runs.

### ResponseEntity

```java
return ResponseEntity.ok(categories);              // 200 OK with body
return new ResponseEntity<>(dto, HttpStatus.CREATED); // 201 Created with body
return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204 No Content, no body
```

`ResponseEntity<T>` gives you explicit control over status code, headers,
and body. The generic type `T` is the body type.

### The stream map pattern in listCategories

```java
categoryService.listCategories()
    .stream()
    .map(categoryMapper::toDto)
    .toList();
```

`stream()` — converts the List into a Stream for functional operations.
`.map(categoryMapper::toDto)` — transforms each `Category` into `CategoryDto`.
`categoryMapper::toDto` is a method reference — shorthand for
`category -> categoryMapper.toDto(category)`.
`.toList()` — collects results back into a List.

---

## 10. Error Handling

### The problem without centralized error handling

Without it, every controller method would need try/catch blocks:

```java
// Without @ControllerAdvice — messy
public ResponseEntity<?> createCategory(...) {
    try {
        ...
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()...
    } catch (Exception e) {
        return ResponseEntity.internalServerError()...
    }
}
```

Every endpoint would repeat the same error mapping logic.

### @ControllerAdvice

```java
@ControllerAdvice
public class ErrorController { ... }
```

This class intercepts any exception thrown by any controller (or anything
called by a controller). One class handles errors for the entire application.

### @ExceptionHandler

```java
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
    ...
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
}
```

Spring picks the **most specific** matching handler. So an
`IllegalArgumentException` hits this handler, not the generic `Exception` one.

### The exception → HTTP status mapping

| Exception | HTTP Status | Meaning |
|---|---|---|
| `Exception` (generic) | 500 Internal Server Error | Unexpected, unhandled error |
| `IllegalArgumentException` | 400 Bad Request | Caller provided bad input |
| `IllegalStateException` | 409 Conflict | Resource conflict (e.g., in use) |
| `BadCredentialsException` | 401 Unauthorized | Wrong email or password |

### Using Java's built-in exceptions as signals

Instead of creating custom exception classes, the services throw
`IllegalArgumentException` and `IllegalStateException`. These map to
400 and 409 in the error controller. It's a lightweight approach — as the
app grows, you'd typically replace these with custom exceptions like
`ResourceNotFoundException`, `DuplicateResourceException`, etc.

### ApiErrorResponse

```java
public class ApiErrorResponse {
    private int status;
    private String message;
    private List<FieldError> errors;
}
```

Every error the API returns looks the same. The frontend always knows the
shape. `errors` (the list) is populated when Spring's `@Valid` catches
multiple field validation failures.

### @Slf4j

```java
@Slf4j
public class ErrorController { ... }
```

Lombok generates a `log` field of type `Logger`. You then call:
```java
log.info("something happened");
log.warn("something suspicious");
log.error("something broke", exception);
```

The `log.error("Caught exception", e)` in the generic handler prints the
full stack trace to your logs — critical for debugging production issues.

---

## 11. Security — Deep Dive

### How Spring Security works — the Filter Chain

Every HTTP request passes through a **chain of filters** before reaching
any controller. Spring Security inserts its filters into this chain. The
filters run in order:

```
HTTP Request
    ↓
[CORS Filter]
[Security Filter Chain]
  → Session check
  → JWT Authentication Filter  ← our custom filter
  → Authorization check
    ↓ (if all pass)
[DispatcherServlet]
    ↓
Controller
```

If any filter rejects the request (e.g., invalid token, missing
permissions), the request never reaches the controller. The filter
returns the error response directly.

### SecurityConfig.java

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
```

`@Configuration` — this class defines beans manually via `@Bean` methods.

`@EnableWebSecurity` — activates Spring Security and allows you to
customize the filter chain.

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
```

This method builds the filter chain. `HttpSecurity` is a builder.
You configure it, call `.build()`, and return it as a bean. Spring
registers it as the application's security configuration.

**CSRF disabled:**
```java
.csrf(csrf -> csrf.disable())
```
CSRF (Cross-Site Request Forgery) attacks work by tricking a browser into
sending a request using the victim's *session cookie*. Since this API is
stateless (no cookies, no sessions), CSRF attacks don't apply. Disabling
it prevents it from interfering with API clients.

**Stateless sessions:**
```java
.sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```
Tells Spring: never create HTTP sessions. Every request must authenticate
itself independently (via a JWT in the Authorization header). This is the
correct model for REST APIs.

**Authorization rules:**
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.GET, "/api/v1/posts/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/v1/categories").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/v1/tags").permitAll()
    .anyRequest().authenticated()
)
```
Rules are checked in order, first match wins.
- `permitAll()` — anyone can access, no token needed.
- `.anyRequest().authenticated()` — catch-all: everything else requires
  a valid authenticated identity.

Public read access makes sense for a blog — visitors can read posts and
browse categories without an account. Creating or deleting content requires
being logged in.

**PasswordEncoder:**
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
}
```
This bean defines how passwords are hashed. The **delegating** encoder
stores a prefix with each hash: `{bcrypt}$2a$10$...`. This lets you use
different algorithms for different users and migrate over time. The default
is **bcrypt**, which is intentionally slow (making brute-force attacks
expensive).

You **never** store plain text passwords. When a user registers, their
password is passed through this encoder before saving. When they log in,
the same encoder hashes the provided password and compares it to the stored
hash. You never "decrypt" a password — you only compare hashes.

**AuthenticationManager:**
```java
@Bean
public AuthenticationManager authenticationManager(
        AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
}
```
`AuthenticationManager` is the component that verifies credentials. It
uses your `UserDetailsService` to load the user and your `PasswordEncoder`
to compare passwords. You expose it as a bean so `AuthenticationServiceImpl`
can inject and call it.

### BlogUserDetails.java

Spring Security has no knowledge of your `User` entity. It works with its
own type: `UserDetails`. `BlogUserDetails` is the bridge.

```java
public class BlogUserDetails implements UserDetails {
    private final User user;
```

Think of it as a "view" of your User from Spring Security's perspective.

**`getUsername()`** — returns `user.getEmail()`. The "username" concept
in Spring Security maps to email in this app. This is just a getter — the
user is already loaded, no DB query happens here.

**`getPassword()`** — returns the stored bcrypt hash. Spring Security
uses this internally when comparing the login password.

**`getAuthorities()`** — returns what roles this user has.
```java
return List.of(new SimpleGrantedAuthority("ROLE_USER"));
```
`GrantedAuthority` is Spring Security's type for a permission or role.
`SimpleGrantedAuthority` is just a string wrapper. Every authenticated
user gets `ROLE_USER`. This could later drive role-based access control
(e.g., `ROLE_ADMIN` can delete anything).

**`isAccountNonExpired()`, `isAccountNonLocked()`, `isEnabled()`** — all
return `true` for now. These hooks exist for account lifecycle management.
When you add features like email verification, banning users, or expiring
old accounts, these methods return the appropriate state from your entity.

**`getId()`** — not from `UserDetails`, added by you. The JWT filter uses
this to stamp the user's ID onto the request so controllers know which
user made the request.

### BlogUserDetailsService.java

```java
@Service
public class BlogUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return new BlogUserDetails(user);
    }
}
```

Spring Security calls `loadUserByUsername` during authentication. It's
the bridge between Spring Security's world (UserDetails) and your app's
world (User entity + UserRepository).

`@Service` is required — without it, Spring doesn't register this as a
bean and the `AuthenticationManager` can't find it.

`.orElseThrow()` — `findByEmail` returns `Optional<User>`. If no user
with that email exists, instead of returning null (which would cause
NullPointerExceptions later), it immediately throws `UsernameNotFoundException`.
Spring Security catches this and turns it into a failed authentication.

---

## 12. Auth and JWT — Deep Dive

### Why JWT

HTTP is stateless — the server remembers nothing between requests.
After logging in, how does the server know who you are on the next request?

**Sessions (old approach):** Server stores session data in memory, gives
client a session ID cookie. Every request, server looks up the session.
Doesn't scale well (session lives on one server), bad for mobile/API clients.

**JWT (current approach):** Server creates a cryptographically signed token
and gives it to the client. Client sends the token with every request. Server
verifies the *signature* — no database lookup needed, no shared state.

### What a JWT looks like

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGVtYWlsLmNvbSIsImlhdCI6MTYwMH0.abc123sig
      ↑                              ↑                                  ↑
   Header                         Payload                          Signature
 (algorithm)              (email, issued at, expiry)          (HMAC of above two)
```

Three parts, Base64 encoded, separated by dots.

The payload is **not encrypted** — anyone can decode it. But the signature
is an HMAC (Hash-based Message Authentication Code) created with your
server's secret key. If anyone modifies the header or payload, the signature
won't match and you reject the token.

### AuthenticationServiceImpl.java

**`authenticate(email, password)`**
```java
authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(email, password)
);
return userDetailsService.loadUserByUsername(email);
```

Step 1: `authenticationManager.authenticate()` does all the work:
- Calls `BlogUserDetailsService.loadUserByUsername(email)` to fetch the user
- Hashes the provided `password`
- Compares the hash to `user.getPassword()` (the stored bcrypt hash)
- If they match → success
- If not → throws `BadCredentialsException`

`UsernamePasswordAuthenticationToken` is a holder object — it carries
the email and password to the authentication manager.

Step 2: After successful authentication, call `loadUserByUsername` again
to get the `UserDetails` to return. Yes, it hits the DB twice (minor
redundancy, acceptable at this scale).

**`generateToken(userDetails)`**
```java
Jwts.builder()
    .setClaims(claims)
    .setSubject(userDetails.getUsername())    // stores email
    .setIssuedAt(new Date(...))               // when created
    .setExpiration(new Date(... + 86400000L)) // expires in 24 hours
    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
    .compact();                               // serialize to the string
```

JJWT's builder pattern. Each method adds a piece to the JWT payload.
`signWith` takes your secret key and uses HMAC-SHA256 to create the
signature that prevents tampering. `.compact()` serializes into the
three-part dot-separated string.

`86400000L` = 86,400,000 milliseconds = 86,400 seconds = 24 hours.

**`validateToken(token)`**
```java
String username = extractUsername(token);
return userDetailsService.loadUserByUsername(username);
```

Extracts the email from the token, then loads the full `UserDetails`
from the database. The JWT filter calls this on every authenticated request.

**`extractUsername(token)`**
```java
Claims claims = Jwts.parserBuilder()
    .setSigningKey(getSigningKey())
    .build()
    .parseClaimsJws(token)
    .getBody();
return claims.getSubject();
```

`parseClaimsJws` does two critical things simultaneously:
1. **Verifies the signature** — if the token was tampered with, this throws
   `SignatureException` and authentication fails.
2. **Checks expiry** — if `now > expiration`, throws `ExpiredJwtException`.

If both checks pass, `.getBody()` returns the payload as a `Claims` object,
and `.getSubject()` retrieves the email stored in `setSubject`.

**`getSigningKey()`**
```java
byte[] keyBytes = secretkey.getBytes();
return Keys.hmacShaKeyFor(keyBytes);
```

Converts your secret string (from `application.properties`) into a
cryptographic `Key` object. The same key must be used for both signing
(during token generation) and verification (during token validation).
If they don't match, every token is rejected.

**`@Value("${jwt.secretkey}")`** — Spring injects the value of the
`jwt.secretkey` property from `application.properties` into this field
at startup.

### JwtAuthenticationFilter.java

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {
```

`OncePerRequestFilter` — a Spring base class guaranteeing the filter
runs exactly once per request. You override `doFilterInternal`.

**`extractToken(request)`**
```java
String bearerToken = request.getHeader("Authorization");
if (bearerToken != null && bearerToken.startsWith("Bearer")) {
    return bearerToken.substring(7);
}
return null;
```

Reads the `Authorization` header. The standard format is
`Authorization: Bearer <token>`. `substring(7)` strips the 7-character
"Bearer " prefix to get the raw token. Returns `null` for unauthenticated
requests — that's fine, public endpoints don't need a token.

**The authentication logic:**
```java
UserDetails userDetails = authenticationService.validateToken(token);

UsernamePasswordAuthenticationToken authentication =
    new UsernamePasswordAuthenticationToken(
        userDetails,                    // principal (who)
        null,                           // credentials (password — not needed after auth)
        userDetails.getAuthorities()    // what they're allowed to do
    );

SecurityContextHolder.getContext().setAuthentication(authentication);
```

`SecurityContextHolder` is Spring Security's request-scoped storage for
"who is making this request." Setting authentication here tells Spring
Security "this request is authenticated." Every subsequent security check
(like `.authenticated()` in `SecurityConfig`) reads from this holder.

Without setting this, even with a valid token, Spring Security would still
treat the request as unauthenticated.

**`filterChain.doFilter(request, response)`**
Passes the request to the next filter in the chain. **This line is critical.**
Without it, the request stops at this filter — nothing after it runs, the
controller is never reached, and the client gets no response.

**The try/catch:**
```java
} catch (Exception e) {
    log.warn("Received invalid auth token");
}
filterChain.doFilter(request, response); // outside the try — always runs
```

If token validation fails (tampered, expired, malformed), the exception
is swallowed and the request continues without setting authentication.
Spring Security then sees an unauthenticated request and returns 401 if
the endpoint requires auth. The `filterChain.doFilter` is outside the
try block so it always executes whether or not there was a token error.

### The complete auth flow

```
1. REGISTER (to be built)
   POST /api/v1/auth/register { email, password, name }
   → hash password with bcrypt
   → save User to database

2. LOGIN
   POST /api/v1/auth { email, password }
   → AuthController.login()
   → AuthenticationServiceImpl.authenticate(email, password)
       → AuthenticationManager verifies credentials
       → throws BadCredentialsException if wrong → 401
   → AuthenticationServiceImpl.generateToken(userDetails)
       → builds signed JWT with email + 24h expiry
   → return { token: "eyJ...", expiresIn: 86400 }

3. AUTHENTICATED REQUEST
   GET /api/v1/posts (with Authorization: Bearer eyJ...)
   → JwtAuthenticationFilter.doFilterInternal()
       → extractToken() reads "eyJ..." from header
       → authenticationService.validateToken("eyJ...")
           → parseClaimsJws verifies signature + expiry
           → extract email from payload
           → load UserDetails from DB
       → set authentication in SecurityContextHolder
   → SecurityConfig sees .authenticated() requirement → passes
   → PostController.listPosts() runs normally

4. INVALID TOKEN
   GET /api/v1/posts (with bad/expired token)
   → JwtAuthenticationFilter catches exception
   → SecurityContextHolder has no authentication set
   → SecurityConfig sees .authenticated() requirement → fails
   → Spring Security returns 401 Unauthorized
```

---

## 13. How It All Wires Together

This is Spring's "magic" made explicit. At startup:

1. `@SpringBootApplication` triggers component scan on `com.ark.blog`
2. Spring finds every `@Service`, `@Repository`, `@Controller`, `@Configuration`
3. Spring builds a dependency graph:
   - `CategoryController` needs `CategoryService` and `CategoryMapper`
   - `CategoryServiceImpl` needs `CategoryRepository`
   - `AuthController` needs `AuthenticationService`
   - `AuthenticationServiceImpl` needs `AuthenticationManager` and `UserDetailsService`
   - `AuthenticationManager` (from `SecurityConfig @Bean`) needs `UserDetailsService` and `PasswordEncoder`
   - `BlogUserDetailsService` needs `UserRepository`
   - `JwtAuthenticationFilter` needs `AuthenticationService`
4. Spring resolves the graph bottom-up: creates repositories first
   (no dependencies), then services, then controllers
5. Spring registers the `SecurityFilterChain` which includes your JWT filter
6. App starts, Tomcat listens on port 8080

You never call `new` on any of these classes. Spring does it all.

---

## 14. Spring Boot App Building Roadmap

There's a standard order. The reason for the order is **dependency** —
each layer depends on the layer below it.

```
Step 1 — DOMAIN (Entities + Enums)
  Define your data model first. Everything else depends on it.
  → User, Post, Category, Tag, PostStatus
  Why first: repositories, services, DTOs all reference these

Step 2 — REPOSITORIES
  Once entities exist, define how to query them.
  → UserRepository, CategoryRepository, PostRepository, TagRepository
  Why here: services depend on repositories

Step 3 — DTOs + MAPPERS
  Define API shapes and how to convert to/from entities.
  → CreateCategoryRequest, CategoryDto, CategoryMapper
  Why here: controllers and services use these

Step 4 — SERVICES
  Business logic. Depends on repositories and entities.
  → CategoryService interface + CategoryServiceImpl
  Why here: controllers depend on services

Step 5 — CONTROLLERS
  HTTP layer. Depends on services and DTOs.
  → CategoryController
  Why here: top of the dependency chain

Step 6 — ERROR HANDLING
  Wraps all controllers.
  → ErrorController with @ControllerAdvice
  Why here: can be added any time, but before you test endpoints

Step 7 — SECURITY
  Wraps the entire app from the outside.
  → SecurityConfig, BlogUserDetails, BlogUserDetailsService
  → JwtAuthenticationFilter
  → AuthenticationServiceImpl
  → AuthController
  Why last: easier to build and test business logic without
  auth first. Once core works, layer security on top.
```

### Why security last?

If you add security on day one, every test call needs a valid JWT.
You spend time fighting auth problems before your business logic
even works. Build without security first — verify your endpoints
work in Postman freely — then add the security layer.

### The mental model for building a new feature

Every new feature (e.g., posts) follows the same pattern:

```
1. Entity (Post.java) → defines the DB table
2. Repository (PostRepository.java) → defines queries
3. DTOs (CreatePostRequest, PostDto) → defines API shapes
4. Mapper (PostMapper.java) → converts between them
5. Service (PostService + PostServiceImpl) → business rules
6. Controller (PostController) → HTTP endpoints
```

This is the repeating unit. Once you've done it for categories, doing
it for posts, tags, and users is the same structure every time.
