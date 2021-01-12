package com.massmutual.streaming.manager.util.base

import org.scalatest.BeforeAndAfterAll
import org.slf4j.Logger
import org.testcontainers.containers.{KafkaContainer, Network}
import org.testcontainers.utility.DockerImageName

abstract class KafkaBaseSpec extends BaseSpec with BeforeAndAfterAll {
  var kafka: KafkaContainer = _
  var schemaRegistry: ConfluentSchemaRegistry = _
  var controlCenter: ConfluentControlCenter = _
  val network: Network = Network.newNetwork()
  val confluentVersion = "5.5.1"

  val logger: Logger

  def initialize(): Unit = {
    logger.info {
      s"""
         |Components Endpoints:
         |Kafka: ${kafka.getNetworkAliases.get(0) + ":9092"}
         |Schema Registry: ${schemaRegistry.getSchemaRegistryUrl}
         |Control Center: ${controlCenter.getControlCenterUrl}
      """.stripMargin
    }
    //        Desktop.getDesktop.browse(new URI(controlCenter.getControlCenterUrl))
  }

  override def beforeAll(): Unit = {

    //kafka
    val kafkaImage = DockerImageName.parse(s"confluentinc/cp-enterprise-kafka:$confluentVersion")
    kafka = new KafkaContainer(
      kafkaImage.asCompatibleSubstituteFor("confluentinc/cp-kafka")
    )
    kafka.withNetwork(network)
    kafka.withEnv("KAFKA_METRIC_REPORTERS", "io.confluent.metrics.reporter.ConfluentMetricsReporter")
    kafka.withEnv("CONFLUENT_METRICS_REPORTER_BOOTSTRAP_SERVERS", kafka.getNetworkAliases.get(0) + ":9092")
    kafka.start()

    //schema registry
    schemaRegistry = ConfluentSchemaRegistry(confluentVersion, kafka, logger)
    schemaRegistry.start()

    //control center
    controlCenter = ConfluentControlCenter(confluentVersion, kafka, schemaRegistry, logger)
    controlCenter.start()

    initialize()
  }

  override def afterAll(): Unit = {
    controlCenter.stop()
    schemaRegistry.stop()
    kafka.stop()
  }

}
