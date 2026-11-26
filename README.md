# Student Registration Application (DevOps Lab)

A minimal Java/Maven web application demonstrating a complete DevOps workflow with **HTML**, **Maven**, **GitHub**, **Jenkins**, **GitHub Actions**, and **Docker**.

## Project Structure

- `index.html` — Simple Student Registration HTML form (`Student Name`, `Roll Number`, `Email`, `Course`, `Submit`)
- `src/main/java/com/example/registration/StudentRegistrationServer.java` — Minimal JDK `HttpServer` handling `GET /` and `POST /register` in memory
- `src/test/java/com/example/registration/StudentRegistrationServerTest.java` — JUnit 5 unit and HTTP integration tests
- `pom.xml` — Maven build configuration producing `target/student-registration-1.0.0.jar`
- `Jenkinsfile` — Declarative Jenkins CI pipeline for Maven test & package
- `.github/workflows/maven.yml` — GitHub Actions CI workflow running `mvn -B clean package` on push
- `Dockerfile` — Containerizes the Java application on port `8080`

## Run Locally with Maven

```bash
mvn clean package
java -jar target/student-registration-1.0.0.jar 8081
```

Open `http://localhost:8081/` in a browser.

## Run with Docker

```bash
docker build -t student-registration:latest .
docker run -d --name student-registration-app -p 8081:8080 student-registration:latest
```

Open `http://localhost:8081/` in a browser.
