package examples;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.VoidDeserializer;
import org.apache.kafka.common.serialization.VoidSerializer;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;

public class KafkaClient {

  public static <T extends SpecificRecord> void runProducer(String topic, Integer version, List<T> values) throws IOException {
    final Properties props = loadConfig("config.properties");
    props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, VoidSerializer.class.getName());
    props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
    props.setProperty(KafkaAvroSerializerConfig.USE_LATEST_WITH_METADATA, String.format("major.version=%d", version));
    final KafkaProducer<Void, T> producer = new KafkaProducer<>(props);
    try {
      for (T t : values) {
        final ProducerRecord<Void, T> producerRecord = new ProducerRecord<>(topic, null, t);
        producer.send(producerRecord, (metadata, exception) -> {
          if (exception != null) {
            exception.printStackTrace();
  
          } else {
            System.out.printf("Producer Version: %d, Partition: %d, Offset: %d%n", version, metadata.partition(), metadata.offset());
            System.out.println(t);
          }
        }); 
      }
    }
    finally {
      producer.flush();
      producer.close();
    }
  }
  
  public static <T extends SpecificRecord> void runConsumer(String topic, String consumerGroup, Integer version) throws IOException {
    final Properties props = loadConfig("config.properties");
    props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, VoidDeserializer.class.getName());
    props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
    props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, String.format("%s-v%d", consumerGroup, version));
    props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.setProperty(KafkaAvroDeserializerConfig.USE_LATEST_WITH_METADATA, String.format("major.version=%d", version));
    final KafkaConsumer<Void, T> consumer = new KafkaConsumer<>(props);
    try (consumer) {
        consumer.subscribe(Collections.singletonList(topic));
        while (true) {
            ConsumerRecords<Void, T> records = consumer.poll(Duration.ofMillis(1000));
            if (records.isEmpty()) {
                break;
            }
            for (ConsumerRecord<Void, T> record : records) {
                System.out.printf("Consumer Version %d, Partition: %d, Offset: %d%n", version, record.partition(), record.offset());
                System.out.println(record.value());
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    finally {
      consumer.close();
    }
  }

  private static Properties loadConfig(final String configFile) throws IOException {
    if (!Files.exists(Paths.get(configFile))) {
      throw new IOException(configFile + " not found.");
    }
    final Properties cfg = new Properties();
    try (InputStream inputStream = new FileInputStream(configFile)) {
      cfg.load(inputStream);
    }
    return cfg;
  }
}