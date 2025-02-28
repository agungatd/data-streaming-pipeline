FROM maven:3.8.6-openjdk-11 AS builder

WORKDIR /app

# Copy POM and source files
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

FROM flink:1.20.1

WORKDIR /opt/flink

# Copy job JAR from build stage
COPY --from=builder /app/target/streaming-data-pipeline-1.0-SNAPSHOT.jar /opt/flink/usrlib/

# Copy any additional dependencies if needed
COPY scripts/submit-job.sh /opt/flink/

RUN chmod +x /opt/flink/submit-job.sh

CMD ["/opt/flink/submit-job.sh"]