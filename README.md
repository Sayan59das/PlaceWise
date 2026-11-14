# PlaceWise - Student Placement Eligibility Checker

Java 25 / Maven / plain JDBC console application (course project 25CSE0202, Chitkara University).

```text
.\mvnw.cmd clean verify                       # build + 507 tests + coverage gate   (Linux/macOS: ./mvnw clean verify)
java -jar target\placewise.jar --no-db          # terminal menu, without a database (the backup for a live demo)
java -jar target\placewise.jar --no-db --web    # browser version: open http://localhost:8080/ (Enter stops it)
```

Everything else - setup with MySQL, the menu, rules, architecture, the five-minute demo, test evidence, viva questions -
is in [`docs/`](docs/README.md).
