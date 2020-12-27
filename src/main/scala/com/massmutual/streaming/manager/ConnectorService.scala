package com.massmutual.streaming.manager

import java.io.{BufferedReader, InputStreamReader}
import java.net.InetSocketAddress
import java.util.UUID

import com.massmutual.streaming.manager.source.GitHubSource
import com.twitter.finagle.http.path.{->, /, Integer, Path, Root}
import com.twitter.finagle.http.{MediaType, Method, Request, Response, Status, Version}
import com.twitter.finagle.stack.Endpoint
import com.twitter.finagle.{Http, ListeningServer, Service, SimpleFilter, http}
import com.twitter.util.{Await, Future}
import com.typesafe.config.ConfigFactory
import io.netty.handler.codec.http.HttpResponseStatus
import org.sourcelab.kafka.connect.apiclient.request.dto.NewConnectorDefinition
import org.sourcelab.kafka.connect.apiclient.{Configuration, KafkaConnectClient}

object ConnectorService {

  //  val configuration =
  //    new Configuration("http://hostname.for.kafka-connect.service.com:8083")
  //
  //  val client = new KafkaConnectClient(configuration)
  //
  //  //
  //  //  val connectorList = client.getConnectors;
  //  //
  //  //  //  val connectorDefinition = client.addConnector(NewConnectorDefinition.newBuilder()
  //  //  //    .withName("MyNewConnector")
  //  //  //    .withConfig("connector.class", "org.apache.kafka.connect.tools.VerifiableSourceConnector")
  //  //  //    .withConfig("tasks.max", 3)
  //  //  //    .withConfig("topics", "test-topic")
  //  //  //    .build()
  //  //  //  )
  //  //
  //  //  val service: Service[Request, Response] = (req: http.Request) => Future.value(
  //  //    http.Response(req.version, http.Status.Ok)
  //  //  )
  //  //
  //  //  Endpoint
  //  //
  //  //  val server: ListeningServer = Http.serve(":8080", service)
  //  //
  //  //  Await.ready(server)
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
  //  object GetAllConnectorService extends Service[Request, Response] {
  //    override def apply(request: Request): Future[Response] = {
  //
  //      //      val ss = client.getConnectorsWithAllExpandedMetadata
  //
  //      val ss = client.getConnector("foo")
  //
  //      //      client.getConnectorsWithAllExpandedMetadata.
  //
  //      val response = Response()
  //      request.setContentTypeJson()
  //      response.setContentType(MediaType.PlainText)
  //      response.setContentString("show service world\n")
  //      Future.value(response)
  //    }
  //  }
  //
  //  def main(args: Array[String]): Unit = {
  //    println("Hello World!")
  //  }

  /**
   * A simple Filter that catches exceptions and converts them to appropriate
   * HTTP responses.
   */
  class HandleExceptions extends SimpleFilter[Request, Response] {
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

  /**
   * A simple Filter that checks that the request is valid by inspecting the
   * "Authorization" header.
   */
  class Authorize extends SimpleFilter[Request, Response] {
    override def apply(request: Request, continue: Service[Request, Response]): Future[Response] = {
      val secret = request.headerMap("Authorization")
      if (secret == "open sesame") {
        continue(request)
      } else {
        Future.exception(new IllegalArgumentException("You don't know the secret"))
      }
    }
  }

  object ShowService extends Service[Request, Response] {
    override def apply(request: Request): Future[Response] = {
      val response = Response()
      request.setContentTypeJson()
      response.setContentType(MediaType.PlainText)
      response.setContentString("show service world\n")
      Future.value(response)
    }
  }

  object SubmitService extends Service[Request, Response] {
    override def apply(request: Request): Future[Response] = {
      val response = Response()
      request.setContentTypeJson()
      response.setContentType(MediaType.PlainText)
      val body = request.contentString
      //.. kick off your worker job ..
      val jobId = UUID.randomUUID().toString
      val paramX = request.params("x")
      response.setContentString(s"I have submitted your job with $jobId. You gave me this $body. X = $paramX\n")
      Future.value(response)
    }
  }

  case class SearchService(i: Int) extends Service[Request, Response] {
    override def apply(request: Request): Future[Response] = {
      // If you throw an exception here, the filter will catch it.
      // throw new RuntimeException("s")
      Future {
        val response = Response()
        response.setContentString("search service world")
        response
      }
    }
  }

  val gitHubSource = new GitHubSource()

  //pass the config (automatic reads from src/main/resources)
  gitHubSource.configure(ConfigFactory.load().getConfig("source.github"))

  object ValidateService extends Service[Request, Response] {
    override def apply(request: Request): Future[Response] = {
      val response: Response = Response()
      response.setContentTypeJson()
      response.setContentType(MediaType.PlainText)

      gitHubSource.refresh() match {
        case Some(reader) =>
          val sReader = new BufferedReader(reader)

          val content =
            Stream.continually(sReader.readLine()).takeWhile(_ != null).map(_.concat("\n")).mkString

          sReader.close()

          reader.close()

          response.setContentString(content)

        case _ =>
          response.setContentString("Empty State")

      }
      Future.value(response)
    }
  }

  def main(args: Array[String]) {
    val handleExceptions = new HandleExceptions

    val auth = new Authorize

    val routingService: Service[Request, Response] = request => {
      val (method, path) = (request.method, Path(request.path))
      (method, path) match {
        case Method.Get -> Root / "validate" => ValidateService(request)
        case Method.Get -> Root / "api1" / Integer(userId) => ShowService(request)
        case Method.Post -> Root / "submitJob" => SubmitService(request)
        case Method.Post -> Root / "api2" / Integer(statusId) => SearchService(statusId)(request)
        case _ => Future.exception(new IllegalArgumentException("Unknown path"))
      }
    }

    // compose the Filters and Service together:
    val debuggingService: Service[Request, Response] =
      handleExceptions andThen auth andThen routingService

    val serverPort = new InetSocketAddress(8080)

    val server: ListeningServer = Http.serve(serverPort, debuggingService)

    Await.ready(server)

  }

}
