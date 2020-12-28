package com.massmutual.streaming.manager.service

import com.massmutual.streaming.manager.ConnectorService.client
import com.twitter.finagle.Service
import com.twitter.finagle.http.{MediaType, Request, Response}
import com.twitter.util.Future
import scala.collection.JavaConverters._

object ListAll extends Service[Request, Response] {

  def listNames: List[String] = client.getConnectors.asScala.toList

  override def apply(request: Request): Future[Response] = {
    val connectors = listNames
    val response = Response()
    request.setContentTypeJson()
    response.setContentType(MediaType.JsonUtf8)
    response.setContentString(connectors.toString())
    Future.value(response)
  }
}
