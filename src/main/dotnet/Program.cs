using com.example;
using Examples;
            
var userV1 = new UserV1
{
    Name = "John Doe",
    Age = 25,
};

var userV2 = new UserV2
{
    Name = "John Doe",
    DateOfBirth =  315532800
};

await KafkaClient.RunProducer("user-topic", 1, userV1);
await KafkaClient.RunProducer("user-topic", 2, userV2);
KafkaClient.RunConsumer<UserV1>("user-topic", "user-dotnet-v1", 1);
KafkaClient.RunConsumer<UserV2>("user-topic", "user-dotnet-v2", 2);