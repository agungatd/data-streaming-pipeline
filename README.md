# Data Streaming Pipeline with Kafka, Flink and StarRocks

This project demonstrates a complete data streaming pipeline using Apache Kafka, Apache Flink, and StarRocks database with a Python-based data generator.

## Architecture

![Architecture Diagram](https://via.placeholder.com/800x400?text=Data+Streaming+Pipeline+Architecture)

The pipeline consists of the following components:

- **Data Generator**: Python service that generates synthetic event data
- **Apache Kafka**: Message broker for data streaming
- **Apache Flink**: Stream processing framework
- **StarRocks**: Analytical database for data storage and querying

## Prerequisites

- Docker and Docker Compose
- Git
- 8GB+ RAM available for Docker

## Project Structure

```
project/
├── docker-compose.yml
├── README.md
├── data-generator/
│   ├── Dockerfile
│   ├── requirements.txt
│   └── data_generator.py
└── flink-job/
    ├── Dockerfile
    ├── pom.xml
    ├── scripts/
    │   └── submit-job.sh
    └── src/
        └── main/
            └── java/
                └── com/
                    └── example/
                        └── streaming/
                            └── StreamingDataPipeline.java
```

## Getting Started

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/data-streaming-pipeline.git
   cd data-streaming-pipeline
   ```

2. Build and start the services:
   ```bash
   docker-compose up -d
   ```

3. Check the status of the services:
   ```bash
   docker-compose ps
   ```

### Monitoring

- **Kafka UI**: [http://localhost:8080](http://localhost:8080)
- **Flink Dashboard**: [http://localhost:8081](http://localhost:8081)
- **StarRocks Frontend**: [http://localhost:8030](http://localhost:8030)

### Data Flow

1. The Python data generator creates event data (clicks, purchases, etc.)
2. Events are published to Kafka topic `data-topic`
3. Flink consumes events from Kafka, processes them, and writes to StarRocks
4. StarRocks stores the data for analytical queries

## Accessing and Querying Data

### Connect to StarRocks

You can connect to StarRocks using any MySQL client:

```bash
mysql -h localhost -P 9030 -u root
```

### Sample Queries

```sql
-- Use the streaming database
USE streaming_data;

-- View all events
SELECT * FROM events LIMIT 10;

-- Count events by type
SELECT event_type, COUNT(*) as count
FROM events
GROUP BY event_type
ORDER BY count DESC;

-- Purchase analysis
SELECT 
    category,
    COUNT(*) as purchase_count,
    SUM(price * quantity) as total_revenue,
    AVG(price * quantity) as avg_order_value
FROM events
WHERE event_type = 'purchase'
GROUP BY category
ORDER BY total_revenue DESC;
```

## Custom Data Generation

The data generator creates synthetic events with the following properties:

- Event types: click, view, scroll, purchase, signup, login, logout
- Categories: electronics, clothing, food, books, sports, home, beauty
- User IDs: 1-1000
- Additional metadata: browser, device, OS, referrer

You can modify the generation parameters in `data-generator/data_generator.py`.

## Scaling the Pipeline

### Scaling Kafka

To add more Kafka brokers, modify the `docker-compose.yml` file:

```yaml
kafka-2:
  image: wurstmeister/kafka:2.13-2.8.1
  ports:
    - "9093:9093"
  environment:
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka-2:9093
    KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9093
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
  depends_on:
    - zookeeper
  networks:
    - streaming-network
```

### Scaling Flink

To add more TaskManagers:

```yaml
flink-taskmanager-2:
  image: flink:1.17
  depends_on:
    - flink-jobmanager
  command: taskmanager
  environment:
    - JOB_MANAGER_RPC_ADDRESS=flink-jobmanager
  networks:
    - streaming-network
```

### Scaling StarRocks

To add more StarRocks BE nodes:

```yaml
starrocks-be-2:
  image: starrocks/be:latest
  ports:
    - "8041:8040"
    - "9061:9060"
  environment:
    - FE_SERVERS=starrocks-fe:9020
    - PRIORITY_NETWORKS=172.0.0.0/8
  volumes:
    - starrocks-be-storage-2:/opt/starrocks/be/storage
  depends_on:
    - starrocks-fe
  networks:
    - streaming-network
```

## Troubleshooting

### Services Not Starting

Check container logs:
```bash
docker-compose logs [service-name]
```

### Kafka Connection Issues

Ensure Kafka is accessible from other containers:
```bash
docker-compose exec kafka kafka-topics.sh --list --bootstrap-server kafka:9092
```

### Flink Job Failures

Check Flink logs:
```bash
docker-compose logs flink-jobmanager
docker-compose logs flink-taskmanager
```

### StarRocks Connection Issues

Verify StarRocks is running:
```bash
docker-compose exec starrocks-fe curl -I http://localhost:8030
```

## Shutting Down

To stop all services:
```bash
docker-compose down
```

To stop and remove all data volumes:
```bash
docker-compose down -v
```

## Extending the Pipeline

### Adding Data Transformations

Modify the Flink job in `flink-job/src/main/java/com/example/streaming/StreamingDataPipeline.java` to add custom transformations.

### Creating New Data Sinks

You can add additional sinks to the Flink job to write data to other systems (e.g., Elasticsearch, PostgreSQL).

### Real-time Dashboards

Connect visualization tools like Grafana to StarRocks for real-time dashboards.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
