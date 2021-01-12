package com.massmutual.streaming.manager.util.base

import io.confluent.kafka.schemaregistry.client.{CachedSchemaRegistryClient, SchemaRegistryClient}

abstract class SchemaRegistryBaseSpec extends KafkaBaseSpec {
  private val IDENTITY_MAP_CAPACITY = 5
  var schemaRegistryClient: SchemaRegistryClient = _

  override def initialize(): Unit = {
    super.initialize()
    schemaRegistryClient = new CachedSchemaRegistryClient(schemaRegistry.getSchemaRegistryUrl, IDENTITY_MAP_CAPACITY)
  }
}
