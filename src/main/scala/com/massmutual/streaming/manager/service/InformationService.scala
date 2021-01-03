package com.massmutual.streaming.manager.service

import com.massmutual.streaming.manager.ConnectorService.client
import com.massmutual.streaming.model.sp_connector_definition.SPConnectorDefinition
import com.twitter._
import com.twitter.finagle.Service
import com.twitter.finagle.http.path.{->, /, Path, Root}
import com.twitter.finagle.http.{MediaType, Method, Request, Response}
import com.twitter.util.Future
import org.json4s.JValue
import org.json4s.JsonAST.{JArray, JSet, JString}
import org.json4s.jackson.JsonMethods._

import scala.collection.JavaConverters._
import scala.language.postfixOps

object InformationService {

  val prefix: Path = Root / "connectors"

  def listNames: List[String] = client.getConnectors.asScala.toList

  def listEverything: List[String] = client.getConnectors.asScala.toList

  def listRunning: Set[String] = client.getConnectorsWithExpandedStatus.getAllStatuses.asScala filter { x =>
    x.getConnector.asScala("state").equalsIgnoreCase("running")
  } map {
    _.getName
  } toSet

  def listPaused: Set[String] = client.getConnectorsWithExpandedStatus.getAllStatuses.asScala filter { x =>
    x.getConnector.asScala("state").equalsIgnoreCase("paused")
  } map {
    _.getName
  } toSet

  def listFailed: Set[String] = client.getConnectorsWithExpandedStatus.getAllStatuses.asScala filter { x =>
    x.getConnector.asScala("state").equalsIgnoreCase("failed")
  } map {
    _.getName
  } toSet

  private def createOkResponseWithJsonBody(json: JValue): Future[Response] = {
    val response = Response()
    response.setContentTypeJson()

    val jsonString = compact(render(json))

    response.setContentString(jsonString)
    Future.value(response)
  }

  def apply(request: Request): Future[Response] = {

    def createResponder(set: Set[String]): () => Future[Response] = () => {
      val json: JValue = JSet(set.map(JString))
      createOkResponseWithJsonBody(json)
    }

    val invalidPathResponder = () => {
      Future.exception(throw InformationServiceException("Please supply one of the following values: ['running', 'paused', 'failed']"))
    }

    val response: () => Future[Response] = Path(request.path.toLowerCase) match {
      case `prefix` / ("running" | "") => createResponder(listRunning)
      case `prefix` / "paused" => createResponder(listPaused)
      case `prefix` / "failed" => createResponder(listFailed)
      case _ => invalidPathResponder
    }

    response()
  }

  case class InformationServiceException(message: String) extends Throwable {
    override def getMessage: String = message
  }

}

