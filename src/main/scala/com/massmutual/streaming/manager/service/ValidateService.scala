package com.massmutual.streaming.manager.service

import java.io.BufferedReader

import com.massmutual.streaming.manager.ConnectorService.{config, gitHubSource}
import com.massmutual.streaming.manager.SPConnector
import com.massmutual.streaming.manager.util.SPConnectStateFactory
import com.twitter.finagle.Service
import com.twitter.finagle.http.{MediaType, Request, Response}
import com.twitter.util.Future
import io.netty.handler.codec.http.HttpResponseStatus

object ValidateService extends Service[Request, Response] {
  override def apply(request: Request): Future[Response] = {

    //create the response
    val response: Response = Response()
    response.setContentTypeJson()
    response.setContentType(MediaType.PlainText)

    gitHubSource.refresh() match {
      case Some(reader) =>

        val state = SPConnectStateFactory.fromReader(new BufferedReader(reader))

        reader.close()
        response.setContentString("Format Ok.")
        response.statusCode(HttpResponseStatus.OK.code())
      case _ =>
        response.statusCode(HttpResponseStatus.OK.code())
        response.setContentString("Empty State")
    }
    Future.value(response)
  }
}
