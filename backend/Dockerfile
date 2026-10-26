FROM eclipse-temurin:21-jdk

WORKDIR app
COPY target/vmp-project-0.0.1-SNAPSHOT.jar app.jar

# The jar contains application.yml. Profiles come from SPRING_PROFILES_ACTIVE (e.g. "seed" for the demo, Q39).
CMD ["java", "-jar", "app.jar"]