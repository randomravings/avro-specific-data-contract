using Confluent.Kafka;
using Confluent.SchemaRegistry.Serdes;
using Confluent.SchemaRegistry;
using Confluent.Kafka.SyncOverAsync;
using Avro.Specific;
using Newtonsoft.Json;

namespace Examples
{
    public static class KafkaClient
    {
        public static async Task RunProducer<T>(string topic, int version, params T[] value) where T : ISpecificRecord
        {
            Confluent.SchemaRegistry.Rules.JsonataExecutor.Register();
            Confluent.SchemaRegistry.Rules.CelExecutor.Register();

            var schemaRegistryClient = new CachedSchemaRegistryClient(new SchemaRegistryConfig()
            {
                Url = "http://localhost:8081",
            });

            var avroSerializer = new AvroSerializer<T>(schemaRegistryClient, new()
            {
                UseLatestWithMetadata = new Dictionary<string, string>{ {"major.version", $"{version}" } },
            });

            var producerConfig = new ProducerConfig
            {
                BootstrapServers = "localhost:9092",
                AllowAutoCreateTopics = false,
            };
            using var producer = new ProducerBuilder<Null, T>(producerConfig)
                .SetKeySerializer(Serializers.Null)
                .SetValueSerializer(avroSerializer.AsSyncOverAsync())
                .Build()
            ;

            try
            {
                foreach(var v in value)
                {
                    var result = await producer.ProduceAsync(topic, new Message<Null, T> { Value = v });
                    Console.WriteLine($"Producer Version: {version}, Partition: {result.Partition}, Offset: {result.Offset}");
                    Console.WriteLine(JsonConvert.SerializeObject(result.Message.Value));
                }
            }
            catch(Exception e)
            {
                Console.WriteLine($"An error occurred: {e}");
            }
            finally
            {
                producer.Flush();
            }
        }
        public static void RunConsumer<T>(string topic, string consumerGroup, int version) where T : ISpecificRecord
        {
            Confluent.SchemaRegistry.Rules.JsonataExecutor.Register();
            Confluent.SchemaRegistry.Rules.CelExecutor.Register();

            var schemaRegistryClient = new CachedSchemaRegistryClient(new SchemaRegistryConfig()
            {
                Url = "http://localhost:8081",
            });

            var avroDeserializer = new AvroDeserializer<T>(schemaRegistryClient, new()
            {
                UseLatestWithMetadata = new Dictionary<string, string>{ {"major.version", $"{version}" } },
            });

            var consumerConfig = new ConsumerConfig
            {
                BootstrapServers = "localhost:9092",
                GroupId = consumerGroup,
                AutoOffsetReset = AutoOffsetReset.Earliest,
            };
            using var consumer = new ConsumerBuilder<Ignore, T>(consumerConfig)
                .SetKeyDeserializer(Deserializers.Ignore)
                .SetValueDeserializer(avroDeserializer.AsSyncOverAsync())
                .Build()
            ;

            try
            {
                consumer.Subscribe(topic);
                while (true) {
                    var result = consumer.Consume(1000);
                    if(result == null)
                        break;
                    Console.WriteLine($"Consumer Version: {version}, Partition: {result.Partition}, Offset: {result.Offset}");
                    Console.WriteLine(JsonConvert.SerializeObject(result.Message.Value));
                }
            }
            catch(Exception e)
            {
                Console.WriteLine($"An error occurred: {e}");
            }
            finally
            {
                consumer.Close();
            }
        }
    }
}
