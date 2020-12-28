package com.massmutual.streaming.manager.service

import java.io.BufferedReader

import com.massmutual.streaming.manager.ConnectorService.{client, gitHubSource}
import com.massmutual.streaming.model.sp_connector_definition.SPConnectorDefinition
import com.twitter.finagle.Service
import com.twitter.finagle.http.{MediaType, Request, Response}
import com.twitter.util.Future
import io.netty.handler.codec.http.HttpResponseStatus
import org.sourcelab.kafka.connect.apiclient.request.dto.NewConnectorDefinition
import scalapb.json4s.JsonFormat
import scala.collection.JavaConverters._

import scala.language.postfixOps

object ApplyChange extends Service[Request, Response] {

  private def toNewConnectDefinition(spConnectorDefinition: SPConnectorDefinition): NewConnectorDefinition = {
    new NewConnectorDefinition(spConnectorDefinition.connectorName, spConnectorDefinition.connectorConfig.asJava)
  }

  private def findNewConnector(spConnectorDefinition: SPConnectorDefinition): NewConnectorDefinition = {
    new NewConnectorDefinition(spConnectorDefinition.connectorName, spConnectorDefinition.connectorConfig.asJava)
  }

  override def apply(request: Request): Future[Response] = {

    //create the response
    val response: Response = Response()
    response.setContentTypeJson()
    response.setContentType(MediaType.PlainText)

    val runningList = ListAll.listNames toSet

    gitHubSource.refresh() match {
      case Some(reader) =>
        val sReader = new BufferedReader(reader)

        val content =
          Stream.continually(sReader.readLine()).takeWhile(_ != null).map(_.concat("\n")).mkString

        val githubConnectors = JsonFormat.fromJsonString[SPConnectorDefinition](content)

        sReader.close()
        reader.close()
        response.setContentString(githubConnectors.toProtoString)
        response.statusCode(HttpResponseStatus.OK.code())
      case _ =>
        response.statusCode(HttpResponseStatus.OK.code())
        response.setContentString("Empty State")
    }
    Future.value(response)
  }
}


