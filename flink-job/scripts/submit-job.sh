#!/bin/bash

echo "Waiting for JobManager to be available..."
# while ! curl --output /dev/null --silent --head --fail http://flink-jobmanager:8081; do
#     echo "JobManager not available yet. Retrying in 5 seconds..."
#     sleep 5
# done
sleep 10
echo "JobManager is up and running!"

echo "Waiting for Kafka to be available..."
sleep 10
echo "Assuming Kafka is up by now"

echo "Waiting for StarRocks to be available..."
sleep 10
echo "Assuming StarRocks is up by now"

echo "Submitting Flink job..."
flink run -c com.example.streaming.StreamingDataPipeline /opt/flink/usrlib/streaming-data-pipeline-1.0-SNAPSHOT.jar

# Keep the container running to maintain logs
tail -f /dev/null