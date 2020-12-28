//package com.massmutual.streaming.manager
//
//import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
//import java.util
//import java.util.UUID
//
//import com.twitter.finagle.http.{ParamMap, _}
//import com.twitter.finagle.http.path._
//import com.twitter.finagle.http.service.RoutingService
//import com.twitter.finagle.{Http, ListeningServer, Service, SimpleFilter}
//import com.twitter.util.Await
////import org.jboss.netty.handler.codec.http._
////import org.jboss.netty.handler.codec.http.HttpResponseStatus._
////import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
////import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
////import org.jboss.netty.util.CharsetUtil.UTF_8
//import java.net.InetSocketAddress
//
//import com.twitter.util.Future
//import io.netty.handler.codec.http.HttpResponseStatus
//
///**
// * This example demonstrates a sophisticated HTTP server that handles exceptions
// * and performs authorization via a shared secret. The exception handling and
// * authorization code are written as Filters, thus isolating these aspects from
// * the main service (here called "Respond") for better code organization.
// */
//object HttpServer {
//
//  /**
//   * A simple Filter that catches exceptions and converts them to appropriate
//   * HTTP responses.
//   */
//  class HandleExceptions extends SimpleFilter[Request, Response] {
//    override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
//      // `handle` asynchronously handles exceptions.
//      service(request) handle {
//        case error =>
//          error.printStackTrace()
//
//          val statusCode: Status = error match {
//            case _: IllegalArgumentException =>
//              Status(HttpResponseStatus.FORBIDDEN.code())
//            case _ =>
//              Status(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
//          }
//
//          val errorResponse = Response(Version.Http11, statusCode)
//
//          errorResponse.setContentString(error.getMessage)
//
//          errorResponse
//      }
//    }
//  }
//
//  /**
//   * A simple Filter that checks that the request is valid by inspecting the
//   * "Authorization" header.
//   */
//  class Authorize extends SimpleFilter[Request, Response] {
//    override def apply(request: Request, continue: Service[Request, Response]): Future[Response] = {
//      val secret = request.headerMap("Authorization")
//      if (secret == "open sesame") {
//        continue(request)
//      } else {
//        Future.exception(new IllegalArgumentException("You don't know the secret"))
//      }
//    }
//  }
//
//  object ShowService extends Service[Request, Response] {
//    override def apply(request: Request): Future[Response] = {
//      val response = Response()
//      request.setContentTypeJson()
//      response.setContentType(MediaType.PlainText)
//      response.setContentString("show service world\n")
//      Future.value(response)
//    }
//  }
//
//  object SubmitService extends Service[Request, Response] {
//    override def apply(request: Request): Future[Response] = {
//      val response = Response()
//      request.setContentTypeJson()
//      response.setContentType(MediaType.PlainText)
//      val body = request.contentString
//      //.. kick off your worker job ..
//      val jobId = UUID.randomUUID().toString
//      val paramX = request.params("x")
//      response.setContentString(s"I have submitted your job with $jobId. You gave me this $body. X = $paramX\n")
//      Future.value(response)
//    }
//  }
//
//  case class SearchService(i: Int) extends Service[Request, Response] {
//    override def apply(request: Request): Future[Response] = {
//      // If you throw an exception here, the filter will catch it.
//      // throw new RuntimeException("s")
//      Future {
//        val response = Response()
//        response.setContentString("search service world")
//        response
//      }
//    }
//  }
//
//  def main(args: Array[String]) {
//    val handleExceptions = new HandleExceptions
//    val auth = new Authorize
//
//    val routingService: Service[Request, Response] = request => {
//      val (method, path) = (request.method, Path(request.path))
//      (method, path) match {
//        case Method.Get -> Root / "api1" / Integer(userId) => ShowService(request)
//        case Method.Post -> Root / "submitJob" => SubmitService(request)
//        case Method.Post -> Root / "api2" / Integer(statusId) => SearchService(statusId)(request)
//        case _ => Future.exception(new IllegalArgumentException("Unknown path"))
//      }
//    }
//
//    // compose the Filters and Service together:
//    val debuggingService: Service[Request, Response] =
//      handleExceptions andThen auth andThen routingService
//
//    val serverPort = new InetSocketAddress(8080)
//
//    val server: ListeningServer = Http.serve(serverPort, debuggingService)
//
//    Await.ready(server)
//
//  }
//}