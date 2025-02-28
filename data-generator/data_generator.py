import json
import random
import time
import datetime
import os
from kafka import KafkaProducer
import mysql.connector

# Kafka configuration
KAFKA_BOOTSTRAP_SERVERS = os.getenv('KAFKA_BOOTSTRAP_SERVERS', 'kafka:9092')
KAFKA_TOPIC = os.getenv('KAFKA_TOPIC', 'data-topic')

# StarRocks configuration
STARROCKS_HOST = os.getenv('STARROCKS_HOST', 'starrocks-fe')
STARROCKS_PORT = int(os.getenv('STARROCKS_PORT', '9030'))
STARROCKS_USER = os.getenv('STARROCKS_USER', 'root')
STARROCKS_PASSWORD = os.getenv('STARROCKS_PASSWORD', '')
STARROCKS_DB = os.getenv('STARROCKS_DB', 'streaming_data')
STARROCKS_TABLE = os.getenv('STARROCKS_TABLE', 'events')

# Event types and categories
EVENT_TYPES = ['click', 'view', 'scroll', 'purchase', 'signup', 'login', 'logout']
CATEGORIES = ['electronics', 'clothing', 'food', 'books', 'sports', 'home', 'beauty']
USER_IDS = list(range(1, 1001))  # 1000 users

def create_starrocks_table():
    """Create the database and table in StarRocks if they don't exist."""
    try:
        conn = mysql.connector.connect(
            host=STARROCKS_HOST,
            port=STARROCKS_PORT,
            user=STARROCKS_USER,
            password=STARROCKS_PASSWORD
        )
        cursor = conn.cursor()
        
        # Create database if not exists
        cursor.execute(f"CREATE DATABASE IF NOT EXISTS {STARROCKS_DB}")
        cursor.execute(f"USE {STARROCKS_DB}")
        
        # Create table if not exists
        cursor.execute(f"""
        CREATE TABLE IF NOT EXISTS {STARROCKS_TABLE} (
            event_id STRING,
            user_id INT,
            event_type STRING,
            category STRING,
            item_id INT,
            price DECIMAL(10, 2),
            quantity INT,
            timestamp DATETIME,
            event_data STRING
        ) ENGINE=OLAP
        DUPLICATE KEY(event_id)
        DISTRIBUTED BY HASH(event_id) BUCKETS 8
        PROPERTIES (
            "replication_num" = "1"
        );
        """)
        
        conn.close()
        print(f"StarRocks database and table initialized successfully")
    except Exception as e:
        print(f"Error initializing StarRocks: {e}")

def generate_event():
    """Generate a random event data point."""
    event_id = f"evt-{int(time.time() * 1000)}-{random.randint(1000, 9999)}"
    user_id = random.choice(USER_IDS)
    event_type = random.choice(EVENT_TYPES)
    category = random.choice(CATEGORIES)
    item_id = random.randint(1, 10000)
    price = round(random.uniform(1.99, 999.99), 2)
    quantity = random.randint(1, 10)
    timestamp = datetime.datetime.now().isoformat()
    
    event_data = {
        "event_id": event_id,
        "user_id": user_id,
        "event_type": event_type,
        "category": category,
        "item_id": item_id,
        "price": price,
        "quantity": quantity,
        "timestamp": timestamp,
        "event_data": json.dumps({
            "browser": random.choice(["Chrome", "Firefox", "Safari", "Edge"]),
            "device": random.choice(["desktop", "mobile", "tablet"]),
            "os": random.choice(["Windows", "MacOS", "iOS", "Android", "Linux"]),
            "referrer": random.choice(["direct", "google", "facebook", "twitter", "email"])
        })
    }
    return event_data

def main():
    print("Starting data generator...")
    
    # Create StarRocks table
    create_starrocks_table()
    
    # Set up Kafka producer
    producer = KafkaProducer(
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        value_serializer=lambda v: json.dumps(v).encode('utf-8')
    )
    
    # Generate and send events
    try:
        while True:
            event = generate_event()
            producer.send(KAFKA_TOPIC, event)
            producer.flush()
            
            # Print event info
            print(f"Generated event: {event['event_id']} - Type: {event['event_type']} - User: {event['user_id']}")
            
            # Random sleep to simulate realistic event generation
            time.sleep(random.uniform(0.1, 2.0))
    except KeyboardInterrupt:
        print("Data generation stopped.")
    finally:
        producer.close()

if __name__ == "__main__":
    # Add a small delay to ensure Kafka is ready
    time.sleep(30)
    main()