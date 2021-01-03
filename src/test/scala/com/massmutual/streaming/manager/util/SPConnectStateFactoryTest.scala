package com.massmutual.streaming.manager.util

import java.io.{BufferedReader, StringReader}

import com.massmutual.streaming.model.connector_state.SPConnectState
import com.massmutual.streaming.model.sp_connector_definition.SPConnectorDefinition
import scalapb.json4s.JsonFormat

class SPConnectStateFactoryTest extends org.scalatest.flatspec.AnyFlatSpec {

  "Simple connector definition" should "parse fine" in {
    val jsonString =
      """{
        |      "nameSpace": "com.massmutual.wikipedia",
        |      "connectorName": "wikipedia-irc",
        |      "ownerName": "EDAP",
        |      "ownerEmail": "EDAP@massmutual.com",
        |      "tCode": "T12345",
        |      "connectorType": "SOURCE",
        |      "connectorStatus" : "RUNNING",
        |      "paused" : false,
        |      "connectorConfig": {
        |        "connector.class": "com.github.cjmatta.kafka.connect.irc.IrcSourceConnector",
        |        "transforms": "WikiEditTransformation",
        |        "transforms.WikiEditTransformation.type": "com.github.cjmatta.kafka.connect.transform.wikiedit.WikiEditTransformation",
        |        "transforms.wikiEditTransformation.save.unparseable.messages": "true",
        |        "transforms.wikiEditTransformation.dead.letter.topic": "wikipedia.failed",
        |        "irc.channels": "#en.wikipedia,#fr.wikipedia,#es.wikipedia,#ru.wikipedia,#en.wiktionary,#de.wikipedia,#zh.wikipedia,#sd.wikipedia,#it.wikipedia,#mediawiki.wikipedia,#commons.wikimedia,#eu.wikipedia,#vo.wikipedia,#eo.wikipedia,#uk.wikipedia",
        |        "irc.server": "irc.wikimedia.org",
        |        "irc.server.port": "6667",
        |        "kafka.topic": "wikipedia.parsed",
        |        "producer.interceptor.classes": "io.confluent.monitoring.clients.interceptor.MonitoringProducerInterceptor",
        |        "value.converter": "io.confluent.connect.avro.AvroConverter",
        |        "value.converter.schema.registry.url": "https://schemaregistry:8085",
        |        "value.converter.schema.registry.ssl.truststore.location": "/etc/kafka/secrets/kafka.client.truststore.jks",
        |        "value.converter.schema.registry.ssl.truststore.password": "confluent",
        |        "value.converter.basic.auth.credentials.source": "USER_INFO",
        |        "value.converter.basic.auth.user.info": "connectorSA:connectorSA",
        |        "producer.override.sasl.jaas.config": "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required username=\"connectorSA\" password=\"connectorSA\" metadataServerUrls=\"http://kafka1:8091,http://kafka2:8092\";",
        |        "tasks.max": "1"
        |      }
        |    }""".stripMargin

    val stringReader = new StringReader(jsonString)
    val bufferedReader = new BufferedReader(stringReader)

//    SPConnectStateFactory.fromReader(bufferedReader)
    JsonFormat.fromJsonString[SPConnectorDefinition](jsonString)
  }

  "Simple a set of connector definitions" should "parse fine" in {
    val jsonString =
      """{
        |  "connectors": [
        |    {
        |      "nameSpace": "com.massmutual.wikipedia",
        |      "connectorName": "wikipedia-irc",
        |      "ownerName": "EDAP",
        |      "ownerEmail": "EDAP@massmutual.com",
        |      "tCode": "T12345",
        |      "connectorType": "SOURCE",
        |      "connectorStatus" : "RUNNING",
        |      "paused" : false,
        |      "connectorConfig": {
        |        "connector.class": "com.github.cjmatta.kafka.connect.irc.IrcSourceConnector",
        |        "transforms": "WikiEditTransformation",
        |        "transforms.WikiEditTransformation.type": "com.github.cjmatta.kafka.connect.transform.wikiedit.WikiEditTransformation",
        |        "transforms.wikiEditTransformation.save.unparseable.messages": "true",
        |        "transforms.wikiEditTransformation.dead.letter.topic": "wikipedia.failed",
        |        "irc.channels": "#en.wikipedia,#fr.wikipedia,#es.wikipedia,#ru.wikipedia,#en.wiktionary,#de.wikipedia,#zh.wikipedia,#sd.wikipedia,#it.wikipedia,#mediawiki.wikipedia,#commons.wikimedia,#eu.wikipedia,#vo.wikipedia,#eo.wikipedia,#uk.wikipedia",
        |        "irc.server": "irc.wikimedia.org",
        |        "irc.server.port": "6667",
        |        "kafka.topic": "wikipedia.parsed",
        |        "producer.interceptor.classes": "io.confluent.monitoring.clients.interceptor.MonitoringProducerInterceptor",
        |        "value.converter": "io.confluent.connect.avro.AvroConverter",
        |        "value.converter.schema.registry.url": "https://schemaregistry:8085",
        |        "value.converter.schema.registry.ssl.truststore.location": "/etc/kafka/secrets/kafka.client.truststore.jks",
        |        "value.converter.schema.registry.ssl.truststore.password": "confluent",
        |        "value.converter.basic.auth.credentials.source": "USER_INFO",
        |        "value.converter.basic.auth.user.info": "connectorSA:connectorSA",
        |        "producer.override.sasl.jaas.config": "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required username=\"connectorSA\" password=\"connectorSA\" metadataServerUrls=\"http://kafka1:8091,http://kafka2:8092\";",
        |        "tasks.max": "1"
        |      }
        |    },
        |    {
        |      "nameSpace": "com.massmutual.wikipedia",
        |      "connectorName": "replicate-topic",
        |      "ownerName": "EDAP",
        |      "ownerEmail": "EDAP@massmutual.com",
        |      "tCode": "T12345",
        |      "connectorType": "SOURCE",
        |      "connectorStatus" : "RUNNING",
        |      "paused" : false,
        |      "connectorConfig": {
        |        "connector.class": "io.confluent.connect.replicator.ReplicatorSourceConnector",
        |        "topic.whitelist": "wikipedia.parsed",
        |        "topic.rename.format": "\\${topic}.replica",
        |        "key.converter": "io.confluent.connect.replicator.util.ByteArrayConverter",
        |        "value.converter": "io.confluent.connect.replicator.util.ByteArrayConverter",
        |        "dest.kafka.bootstrap.servers": "kafka1:10091",
        |        "dest.kafka.security.protocol": "SASL_SSL",
        |        "dest.kafka.ssl.key.password": "confluent",
        |        "dest.kafka.ssl.truststore.location": "/etc/kafka/secrets/kafka.client.truststore.jks",
        |        "dest.kafka.ssl.truststore.password": "confluent",
        |        "dest.kafka.ssl.keystore.location": "/etc/kafka/secrets/kafka.client.keystore.jks",
        |        "dest.kafka.ssl.keystore.password": "confluent",
        |        "dest.kafka.sasl.login.callback.handler.class": "io.confluent.kafka.clients.plugins.auth.token.TokenUserLoginCallbackHandler",
        |        "dest.kafka.sasl.jaas.config": "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required username=\"connectorSA\" password=\"connectorSA\" metadataServerUrls=\"http://kafka1:8091,http://kafka2:8092\";",
        |        "dest.kafka.sasl.mechanism": "OAUTHBEARER",
        |        "confluent.topic.replication.factor": "1",
        |        "src.kafka.bootstrap.servers": "kafka1:10091",
        |        "src.kafka.security.protocol": "SASL_SSL",
        |        "src.kafka.ssl.key.password": "confluent",
        |        "src.kafka.ssl.truststore.location": "/etc/kafka/secrets/kafka.client.truststore.jks",
        |        "src.kafka.ssl.truststore.password": "confluent",
        |        "src.kafka.ssl.keystore.location": "/etc/kafka/secrets/kafka.client.keystore.jks",
        |        "src.kafka.ssl.keystore.password": "confluent",
        |        "src.kafka.sasl.login.callback.handler.class": "io.confluent.kafka.clients.plugins.auth.token.TokenUserLoginCallbackHandler",
        |        "src.kafka.sasl.jaas.config": "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required username=\"connectorSA\" password=\"connectorSA\" metadataServerUrls=\"http://kafka1:8091,http://kafka2:8092\";",
        |        "src.kafka.sasl.mechanism": "OAUTHBEARER",
        |        "src.consumer.interceptor.classes": "io.confluent.monitoring.clients.interceptor.MonitoringConsumerInterceptor",
        |        "src.consumer.confluent.monitoring.interceptor.security.protocol": "SASL_SSL",
        |        "src.consumer.confluent.monitoring.interceptor.bootstrap.servers": "kafka1:10091",
        |        "src.consumer.confluent.monitoring.interceptor.ssl.key.password": "confluent",
        |        "src.consumer.confluent.monitoring.interceptor.ssl.truststore.location": "/etc/kafka/secrets/kafka.client.truststore.jks",
        |        "src.consumer.confluent.monitoring.interceptor.ssl.truststore.password": "confluent",
        |        "src.consumer.confluent.monitoring.interceptor.ssl.keystore.location": "/etc/kafka/secrets/kafka.client.keystore.jks",
        |        "src.consumer.confluent.monitoring.interceptor.ssl.keystore.password": "confluent",
        |        "src.consumer.confluent.monitoring.interceptor.sasl.login.callback.handler.class": "io.confluent.kafka.clients.plugins.auth.token.TokenUserLoginCallbackHandler",
        |        "src.consumer.confluent.monitoring.interceptor.sasl.jaas.config": "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required username=\"connectorSA\" password=\"connectorSA\" metadataServerUrls=\"http://kafka1:8091,http://kafka2:8092\";",
        |        "src.consumer.confluent.monitoring.interceptor.sasl.mechanism": "OAUTHBEARER",
        |        "src.consumer.group.id": "connect-replicator",
        |        "src.kafka.timestamps.producer.interceptor.classes": "io.confluent.monitoring.clients.interceptor.MonitoringProducerInterceptor",
        |        "src.kafka.timestamps.producer.confluent.monitoring.interceptor.security.protocol": "SASL_SSL",
        |        "src.kafka.timestamps.producer.confluent.monitoring.interceptor.bootstrap.servers": "kafka1:10091",
        |        "src.kafka.timestamps.producer.confluent.monitoring.interceptor.ssl.key.password": "confluent",
        |        "src.kafka.timestamps.producer.confluent.monitoring.interceptor.ssl.truststore.location": "/etc/kafka/secrets/kafka.client.truststore.jks",
        |        "src.kafka.timestamps.producer.confluent.monitoring.interceptor.ssl.truststore.password": "confluent",
        |        "src.kafka.timestamps.producer.confluent.monitoring.interceptor.ssl.keystore.location": "/etc/kafka/secrets/kafka.client.keystore.jks",
        |        "src.kafka.timestamps.producer.confluent.monitoring.interceptor.ssl.keystore.password": "confluent",
        |        "src.kafka.timestamps.producer.confluent.monitoring.interceptor.sasl.login.callback.handler.class": "io.confluent.kafka.clients.plugins.auth.token.TokenUserLoginCallbackHandler",
        |        "src.kafka.timestamps.producer.confluent.monitoring.interceptor.sasl.jaas.config": "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required username=\"connectorSA\" password=\"connectorSA\" metadataServerUrls=\"http://kafka1:8091,http://kafka2:8092\";",
        |        "src.kafka.timestamps.producer.confluent.monitoring.interceptor.sasl.mechanism": "OAUTHBEARER",
        |        "offset.timestamps.commit": "false",
        |        "producer.override.sasl.jaas.config": "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required username=\"connectorSA\" password=\"connectorSA\" metadataServerUrls=\"http://kafka1:8091,http://kafka2:8092\";",
        |        "consumer.override.sasl.jaas.config": "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required username=\"connectorSA\" password=\"connectorSA\" metadataServerUrls=\"http://kafka1:8091,http://kafka2:8092\";",
        |        "tasks.max": "1",
        |        "provenance.header.enable": "false"
        |      }
        |    },
        |    {
        |      "nameSpace": "com.massmutual.wikipedia",
        |      "connectorName": "elasticsearch-ksqldb",
        |      "ownerName": "EDAP",
        |      "ownerEmail": "EDAP@massmutual.com",
        |      "tCode": "T12345",
        |      "connectorType": "SINK",
        |      "connectorStatus" : "RUNNING",
        |      "paused" : false,
        |      "connectorConfig": {
        |        "connector.class": "io.confluent.connect.elasticsearch.ElasticsearchSinkConnector",
        |        "consumer.interceptor.classes": "io.confluent.monitoring.clients.interceptor.MonitoringConsumerInterceptor",
        |        "topics": "WIKIPEDIABOT",
        |        "topic.index.map": "WIKIPEDIABOT:wikipediabot",
        |        "connection.url": "http://elasticsearch:9200",
        |        "type.name": "wikichange",
        |        "key.ignore": "true",
        |        "key.converter.schema.registry.url": "https://schemaregistry:8085",
        |        "value.converter": "io.confluent.connect.avro.AvroConverter",
        |        "value.converter.schema.registry.url": "https://schemaregistry:8085",
        |        "value.converter.schema.registry.ssl.truststore.location": "/etc/kafka/secrets/kafka.client.truststore.jks",
        |        "value.converter.schema.registry.ssl.truststore.password": "confluent",
        |        "value.converter.basic.auth.credentials.source": "USER_INFO",
        |        "value.converter.basic.auth.user.info": "connectorSA:connectorSA",
        |        "consumer.override.sasl.jaas.config": "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required username=\"connectorSA\" password=\"connectorSA\" metadataServerUrls=\"http://kafka1:8091,http://kafka2:8092\";",
        |        "schema.ignore": "true"
        |      }
        |    }
        |  ]
        |}""".stripMargin

    val stringReader = new StringReader(jsonString)
    val bufferedReader = new BufferedReader(stringReader)

    val state = SPConnectStateFactory.fromReader(bufferedReader)


    assert(state.connectors.length == 3)
  }
}