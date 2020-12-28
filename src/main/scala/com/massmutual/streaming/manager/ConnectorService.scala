package com.massmutual.streaming.manager

import java.io.File
import java.net.InetSocketAddress

import com.massmutual.streaming.manager.filter.{Authorize, HandleExceptions}
import com.massmutual.streaming.manager.service.{ApplyChange, GetAllNames, ValidateService}
import com.massmutual.streaming.manager.source.GitHubSource
import com.twitter.finagle.http._
import com.twitter.finagle.http.path._
import com.twitter.finagle.{Http, ListeningServer, Service}
import com.twitter.util.{Await, Future}
import com.typesafe.config.{Config, ConfigFactory}
import org.sourcelab.kafka.connect.apiclient.{Configuration, KafkaConnectClient}

object ConnectorService {

  val gitHubSource = new GitHubSource()

  val config: Config = ConfigFactory.load()

  val serverPort: InetSocketAddress = new InetSocketAddress(config.getInt("server.port"))

  val configuration: Configuration = {
    val host = config.getString("connect.host")
    val conf = new Configuration(host)
    val userName = config.getString("connect.username")
    val password = config.getString("connect.password")
    val truststoreFile = config.getString("connect.truststore")
    val truststorePass = config.getString("connect.truststorepass")

    conf
      .useBasicAuth(userName, password)
      .useTrustStore(new File(truststoreFile), truststorePass)
  }

  val client = new KafkaConnectClient(configuration)

  //pass the config (automatic reads from src/main/resources)
  gitHubSource.configure(config.getConfig("source.github"))

  def main(args: Array[String]) {

    val routeIt: Service[Request, Response] = request => {
      val (method, path) = (request.method, Path(request.path))
      (method, path) match {
        case Method.Get -> Root / "validate" => ValidateService(request)
        case Method.Get -> Root / "connectors" => GetAllNames(request)
        case Method.Post -> Root / "connectors" / "apply" => ApplyChange(request)
        case _ => Future.exception(new IllegalArgumentException("Unknown path"))
      }
    }

    // compose the Filters and Service together:
    val connectorManagerService: Service[Request, Response] =
      HandleExceptions andThen Authorize andThen routeIt

    val server: ListeningServer = Http.serve(serverPort, connectorManagerService)

    Await.ready(server)
  }

}
