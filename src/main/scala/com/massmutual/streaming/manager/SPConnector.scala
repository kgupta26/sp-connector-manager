package com.massmutual.streaming.manager

import org.sourcelab.kafka.connect.apiclient.request.dto.{ConnectorDefinition, NewConnectorDefinition}

object SPConnector {

}

case class SPConnector(connector: ConnectorDefinition, ownedBy: String)
