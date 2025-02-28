package com.example.streaming;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;

import org.json.JSONObject;

public class StreamingDataPipeline {

    public static void main(String[] args) throws Exception {
        // Set up the execution environment
        Configuration config = new Configuration();
        config.setString(RestOptions.ADDRESS, "flink-jobmanager");
        config.setInteger(RestOptions.PORT, 8081);

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(config);

        // Configure Kafka source
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("kafka:9092")
                .setTopics("data-topic")
                .setGroupId("flink-consumer-group")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // Create data stream from Kafka
        DataStream<String> stream = env.fromSource(source, org.apache.flink.api.common.eventtime.WatermarkStrategy.noWatermarks(), "Kafka Source");

        // Transform the JSON data
        DataStream<Event> eventStream = stream.map(new MapFunction<String, Event>() {
            @Override
            public Event map(String value) throws Exception {
                JSONObject json = new JSONObject(value);
                
                Event event = new Event();
                event.setEventId(json.getString("event_id"));
                event.setUserId(json.getInt("user_id"));
                event.setEventType(json.getString("event_type"));
                event.setCategory(json.getString("category"));
                event.setItemId(json.getInt("item_id"));
                event.setPrice(json.getDouble("price"));
                event.setQuantity(json.getInt("quantity"));
                event.setTimestamp(json.getString("timestamp"));
                event.setEventData(json.getString("event_data"));
                
                return event;
            }
        });

        // Add some basic processing/analytics
        // For example, we can filter for purchase events
        DataStream<Event> purchaseEvents = eventStream
                .filter(event -> "purchase".equals(event.getEventType()));

        // Print filtered events to stdout for debugging
        purchaseEvents.print("Purchase Events: ");

        // Create a JDBC sink to StarRocks
        SinkFunction<Event> starRocksSink = JdbcSink.sink(
                "INSERT INTO streaming_data.events (event_id, user_id, event_type, category, item_id, price, quantity, timestamp, event_data) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                (statement, event) -> {
                    statement.setString(1, event.getEventId());
                    statement.setInt(2, event.getUserId());
                    statement.setString(3, event.getEventType());
                    statement.setString(4, event.getCategory());
                    statement.setInt(5, event.getItemId());
                    statement.setDouble(6, event.getPrice());
                    statement.setInt(7, event.getQuantity());
                    statement.setString(8, event.getTimestamp());
                    statement.setString(9, event.getEventData());
                },
                JdbcExecutionOptions.builder()
                        .withBatchSize(1000)
                        .withBatchIntervalMs(200)
                        .withMaxRetries(5)
                        .build(),
                new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                        .withUrl("jdbc:mysql://starrocks-fe:9030/streaming_data")
                        .withDriverName("com.mysql.cj.jdbc.Driver")
                        .withUsername("root")
                        .withPassword("")
                        .build()
        );

        // Add the StarRocks sink
        eventStream.addSink(starRocksSink);

        // Execute the streaming pipeline
        env.execute("Streaming Data Pipeline");
    }

    // Event POJO class
    public static class Event {
        private String eventId;
        private int userId;
        private String eventType;
        private String category;
        private int itemId;
        private double price;
        private int quantity;
        private String timestamp;
        private String eventData;

        // Getters and setters
        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }

        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }

        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public int getItemId() { return itemId; }
        public void setItemId(int itemId) { this.itemId = itemId; }

        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public String getEventData() { return eventData; }
        public void setEventData(String eventData) { this.eventData = eventData; }
    }
}