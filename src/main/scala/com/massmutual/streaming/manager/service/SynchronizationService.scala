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
import scala.util.{Failure, Success, Try}

object SynchronizationService {

  implicit val formats: AnyRef with Formats = Serialization.formats(NoTypeHints)

  def apply(request: Request): Future[Response] = {

    //create the response
    val response: Response = Response()
    response.setContentTypeJson()

    gitHubSource.refresh() match {
      case Some(reader) =>

        val sourceState = SPConnectStateFactory.fromReader(new BufferedReader(reader))

        reader.close()

        //synchronize
        Try(Operations.synchronize(sourceState, client)) match {
          //if a failure just, just return
          case Failure(ex) => return Future.exception(ex)
          case Success(result) =>

            val jsonString = write(result)

            response.setContentString(jsonString)

            response.setContentType(MediaType.JsonUtf8)

            response.statusCode(HttpResponseStatus.CREATED.code())

        }
      case _ =>
        response.setContentType(MediaType.PlainTextUtf8)
        response.statusCode(HttpResponseStatus.UNPROCESSABLE_ENTITY.code())
        response.setContentString("~Empty State~")
    }

    Future.value(response)
  }
}


