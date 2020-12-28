package com.massmutual.streaming.manager

import java.lang

import com.massmutual.streaming.model.connector_state.SPConnectState
import com.massmutual.streaming.model.sp_connector_definition.SPConnectorDefinition
import org.sourcelab.kafka.connect.apiclient.KafkaConnectClient
import org.sourcelab.kafka.connect.apiclient.request.dto.{ConnectorDefinition, NewConnectorDefinition}

import scala.collection.JavaConverters._
import scala.language.postfixOps

object Operations {

  private def getRunningNames(client: KafkaConnectClient) = {
    client.getConnectors.asScala toSet
  }

  private def toNewConnectDefinition(spConnectorDefinition: SPConnectorDefinition): NewConnectorDefinition = {
    new NewConnectorDefinition(spConnectorDefinition.connectorName, spConnectorDefinition.connectorConfig.asJava)
  }

  def applyNewConnectors(state: SPConnectState, client: KafkaConnectClient): Seq[ConnectorDefinition] = {

    val currentRunning = getRunningNames(client)

    val asPerState = state.connectors.map(_.connectorName) toSet

    val newConns = asPerState -- currentRunning

    val applied = state.connectors filter { x =>
      (newConns contains x.connectorName) && !x.paused
    } map { nc =>
      client.addConnector(toNewConnectDefinition(nc))
    }

    applied
  }

  def applyRemoveConnectors(state: SPConnectState, client: KafkaConnectClient): Set[(String, lang.Boolean)] = {

    val currentRunning = getRunningNames(client)

    val asPerState = state.connectors.map(_.connectorName) toSet

    val removedConns = currentRunning -- asPerState

    val applied = removedConns map { r =>
      r -> client.deleteConnector(r)
    }

    applied
  }

  def applyPauseConnectors(state: SPConnectState, client: KafkaConnectClient): Set[(String, lang.Boolean)] = {

    val currentRunning = client.getConnectorsWithExpandedStatus.getAllStatuses.asScala filter { x =>
      x.getConnector.asScala("state").equalsIgnoreCase("running")
    } map {
      _.getName
    } toSet

    val asPerStatePaused = state.connectors filter (_.paused) map (_.connectorName) toSet

    val pausedConns = asPerStatePaused & currentRunning

    val applied = pausedConns map { pc =>
      pc -> client.pauseConnector(pc)
    }

    applied
  }

  def applyResumeConnectors(state: SPConnectState, client: KafkaConnectClient): Set[(String, lang.Boolean)] = {

    val currentPaused = client.getConnectorsWithExpandedStatus.getAllStatuses.asScala filter { x =>
      x.getConnector.asScala("state").equalsIgnoreCase("paused")
    } map {
      _.getName
    } toSet

    val asPerStateResumed = state.connectors filter (!_.paused) map (_.connectorName) toSet

    val resumeConns = asPerStateResumed & currentPaused

    val applied = resumeConns map { rc =>
      rc -> client.resumeConnector(rc)
    }

    applied
  }

  def applyUpdateConnectors(state: SPConnectState, client: KafkaConnectClient): Set[ConnectorDefinition] = {
    val currentRunning = client.getConnectorsWithExpandedInfo.getAllDefinitions.asScala.map { x =>
      x.getName -> x.getConfig.asScala
    } toMap

    val asPerState = state.connectors map { x =>
      x.connectorName -> x.connectorConfig
    } toMap

    val commonConnectors = currentRunning.keySet & asPerState.keySet

    val updatedConns = commonConnectors map { conn =>

      val expectedConfigs = asPerState(conn)

      val actualConfigs = currentRunning(conn)

      val commonKeys = expectedConfigs.keySet & currentRunning.keySet

      val configsToUpdate = commonKeys.foldLeft(Map[String, String]()) { (acc, k) =>

        val expectedValue = expectedConfigs(k)
        val actualValue = actualConfigs(k)

        if (expectedValue != actualValue) {
          acc + (k -> expectedValue)
        } else acc
      }

      conn -> configsToUpdate
    }

    val applied = updatedConns map { uc =>
      client.updateConnectorConfig(uc._1, uc._2.asJava)
    }

    applied
  }

  def synchronize(state: SPConnectState, client: KafkaConnectClient): SyncResult = {
    SyncResult(
      applyNewConnectors(state, client).map(_.getName).toList,
      applyRemoveConnectors(state, client).map(_._1).toList,
      applyPauseConnectors(state, client).map(_._1).toList,
      applyResumeConnectors(state, client).map(_._1).toList,
      applyUpdateConnectors(state, client).map(_.getName).toList
    )
  }

  case class SyncResult(added: List[String], removed: List[String],
                        paused: List[String], resumed: List[String], updated: List[String])

}
