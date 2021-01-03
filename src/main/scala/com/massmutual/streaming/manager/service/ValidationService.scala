package com.massmutual.streaming.manager.service

import java.io.{BufferedReader, Reader}

import com.massmutual.streaming.manager.ConnectorService.gitHubSource
import com.massmutual.streaming.manager.util.SPConnectStateFactory
import com.massmutual.streaming.model.connector_state.SPConnectState
import com.massmutual.streaming.model.sp_connector_definition.SPConnectorDefinition
import com.twitter.finagle.http.{MediaType, Request, Response}
import com.twitter.util.Future
import io.netty.handler.codec.http.HttpResponseStatus

import scala.util.{Failure, Success, Try}

object ValidationService {

  def connNameDupValidation(state: SPConnectState): Set[(String, Int)] = {
    val actual: Seq[String] = state.connectors.map(x => x.nameSpace + "." + x.connectorName)
    val set: Set[String] = actual.toSet

    val counts = set map { s =>
      s -> actual.count(_ == s)
    }

    counts filter (_._2 > 1)
  }

  def missingOwnerNameValidation(state: SPConnectState): Seq[SPConnectorDefinition] = {
    state.connectors.filter(_.ownerName.isEmpty)
  }

  def missingOwnerEmailValidation(state: SPConnectState): Seq[SPConnectorDefinition] = {
    state.connectors.filter(_.ownerEmail.isEmpty)
  }

  def validateStateFile(state: SPConnectState): Future[Response] = {
    val duplicateConnNames = connNameDupValidation(state)
    val missingOwnerName = missingOwnerNameValidation(state)
    val missingOwnerEmail = missingOwnerEmailValidation(state)

    if (duplicateConnNames.nonEmpty) {
      Future.exception(ValidationServiceException(s"Multiple connectors with the same namespace and name. ${duplicateConnNames.mkString(",")}"))
    } else if (missingOwnerName.nonEmpty) {
      Future.exception(ValidationServiceException(s"Missing owner names for some connectors. ${missingOwnerName.map(_.connectorName).mkString(",")}"))
    } else if (missingOwnerEmail.nonEmpty) {
      Future.exception(ValidationServiceException(s"Missing owner emails for some connectors. ${missingOwnerEmail.map(_.connectorName).mkString(",")}"))
    } else {
      val response: Response = Response()
      response.setContentTypeJson()
      response.statusCode(HttpResponseStatus.OK.code())
      Future.value(response)
    }
  }

  def apply(request: Request): Future[Response] = {

    gitHubSource.refresh() match {
      case Some(reader) =>

        val parsingValidation: Reader => Try[SPConnectState] = (r: Reader) => {
          Try(SPConnectStateFactory.fromReader(new BufferedReader(r)))
        }

        parsingValidation(reader) match {
          case Success(state) =>
            //close the reader to avoid leak
            reader.close()
            validateStateFile(state)

          case Failure(ex) =>
            Future.exception(ValidationServiceException(ex.getMessage))
        }

      case _ =>
        Future.exception(ValidationServiceException("No state found"))
    }
  }

  case class ValidationServiceException(message: String) extends Throwable {
    override def getMessage: String = message
  }

}
