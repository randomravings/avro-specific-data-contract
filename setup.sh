docker compose -f docker/docker-compose.yml -p cp-data-contracts up -d

echo "Waiting Broker UP..."
FOUND=''
while [[ $FOUND != "yes" ]]; do
  sleep 1
  FOUND=$(docker exec broker /bin/kafka-cluster cluster-id --bootstrap-server localhost:9092 &>/dev/null && echo 'yes')
done
echo "Broker ready!!"

echo "Creating topics..."
docker exec broker kafka-topics --bootstrap-server localhost:9092 --topic user-topic --create --partitions 1 --replication-factor 1 --if-not-exists

echo "Waiting Schema Registry UP..."
FOUND=''
while [[ $FOUND != "200" ]]; do
  sleep 1
  FOUND=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/)
done
echo "Schema Registry ready!!"

echo "Creating schemas..."

curl http://localhost:8081/config/user-topic-value \
  -X PUT -H "Content-Type: application/vnd.schemaregistry.v1+json" --data \
  '{ "compatibilityGroup": "major.version" }' | \
  jq

jq --arg data "$(jq -c . "schema/user_v1.avsc")" '.schema = $data' "schema/user_v1.json" | \
  curl http://localhost:8081/subjects/user-topic-value/versions \
  -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" --data @- | \
  jq

jq --arg data "$(jq -c . "schema/user_v2.avsc")" '.schema = $data' "schema/user_v2.json" | \
  curl http://localhost:8081/subjects/user-topic-value/versions \
  -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" --data @- | \
  jq

echo "Done!!"

# show result
docker ps -a
