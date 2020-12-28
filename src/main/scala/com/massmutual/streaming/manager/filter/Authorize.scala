package com.massmutual.streaming.manager.filter

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future

/**
 * A simple Filter that checks that the request is valid by inspecting the
 * "Authorization" header.
 */
object Authorize extends SimpleFilter[Request, Response] {
  override def apply(request: Request, continue: Service[Request, Response]): Future[Response] = {
    val secret = request.headerMap("Authorization")
    if (secret == "open sesame") {
      continue(request)
    } else {
      Future.exception(new IllegalArgumentException("You don't know the secret"))
    }
  }
}

