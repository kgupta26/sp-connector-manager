package com.massmutual.streaming.manager.service

import java.io.BufferedReader

import com.massmutual.streaming.manager.ConnectorService.{client, gitHubSource}
import com.massmutual.streaming.manager.Operations
import com.massmutual.streaming.manager.util.SPConnectStateFactory
import com.massmutual.streaming.model.connector_state.SPConnectState
import com.massmutual.streaming.model.sp_connector_definition.SPConnectorDefinition
import com.twitter.finagle.Service
import com.twitter.finagle.http.{MediaType, Request, Response}
import com.twitter.util.Future
import io.netty.handler.codec.http.HttpResponseStatus
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.write
import org.sourcelab.kafka.connect.apiclient.request.dto.NewConnectorDefinition

import scala.collection.JavaConverters._
import scala.language.postfixOps

object SynchronizationService {

  implicit val formats: AnyRef with Formats = Serialization.formats(NoTypeHints)

  private def toNewConnectDefinition(spConnectorDefinition: SPConnectorDefinition): NewConnectorDefinition = {
    new NewConnectorDefinition(spConnectorDefinition.connectorName, spConnectorDefinition.connectorConfig.asJava)
  }

  private def findNewConnector(current: Set[String], state: SPConnectState): Seq[NewConnectorDefinition] = {
    state.connectors.filter(c => !(current contains c.connectorName)) map toNewConnectDefinition
  }

  def apply(request: Request): Future[Response] = {

    //create the response
    val response: Response = Response()
    response.setContentTypeJson()
    response.setContentType(MediaType.JsonUtf8)

    gitHubSource.refresh() match {
      case Some(reader) =>

        val sourceState = SPConnectStateFactory.fromReader(new BufferedReader(reader))

        reader.close()

        //synchronize
        val result: Operations.SyncResult = Operations.synchronize(sourceState, client)

        val jsonString = write(result)

        response.setContentString(pretty(render(jsonString)))

        response.statusCode(HttpResponseStatus.OK.code())
      case _ =>
        response.statusCode(HttpResponseStatus.UNPROCESSABLE_ENTITY.code())
        response.setContentString("Empty State")
    }
    Future.value(response)
  }
}


