package examples;

import java.util.List;

import com.example.UserV1;
import com.example.UserV2;

public class Example {

  public static void main(final String[] args) throws Exception {

    final UserV1 userV1 = UserV1
      .newBuilder()
      .setName("John Doe")
      .setAge(18)
      .build()
    ;

    final UserV2 userV2 = UserV2
      .newBuilder()
      .setName("John Doe")
      .setDateOfBirth(315532800)
      .build()
    ;

    KafkaClient.runProducer("user-topic", 1, List.of(userV1));
    KafkaClient.runProducer("user-topic", 2, List.of(userV2));

    KafkaClient.runConsumer("user-topic", "java-cg-v1", 1);
    KafkaClient.runConsumer("user-topic", "java-cg-v2", 2);
  }
}
