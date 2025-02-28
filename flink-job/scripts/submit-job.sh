#!/bin/bash

echo "Waiting for JobManager to be available..."
until $(curl --output /dev/null --silent --head --fail http://flink-jobmanager:8081); do
  printf '.'
  sleep 5
done
echo "JobManager is up and running!"

echo "Waiting for Kafka to be available..."
sleep 30
echo "Assuming Kafka is up by now"

echo "Waiting for StarRocks to be available..."
sleep 30
echo "Assuming StarRocks is up by now"

echo "Submitting Flink job..."
flink run -c com.example.streaming.StreamingDataPipeline /opt/flink/usrlib/streaming-data-pipeline-1.0-SNAPSHOT.jar

# Keep the container running to maintain logs
tail -f /dev/null