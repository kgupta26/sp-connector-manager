package com.massmutual.streaming.manager.filter

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.http.{Request, Response, Status, Version}
import com.twitter.util.Future
import io.netty.handler.codec.http.HttpResponseStatus

/**
 * A simple Filter that catches exceptions and converts them to appropriate
 * HTTP responses.
 */
object HandleExceptions extends SimpleFilter[Request, Response] {
  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    // `handle` asynchronously handles exceptions.
    service(request) handle {
      case error =>
        error.printStackTrace()
        val statusCode: Status = error match {
          case _: IllegalArgumentException =>
            Status(HttpResponseStatus.FORBIDDEN.code())
          case _ =>
            Status(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
        }

        val errorResponse = Response(Version.Http11, statusCode)

        errorResponse.setContentString(error.getMessage)

        errorResponse
    }
  }
}
