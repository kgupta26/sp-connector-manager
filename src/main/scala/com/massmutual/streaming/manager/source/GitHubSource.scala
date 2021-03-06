package com.massmutual.streaming.manager.source

import java.io.{Reader, StringReader}
import java.net.URL
import java.nio.charset.Charset
import java.util.Base64

import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.config.Config
import org.apache.http.client.methods.HttpUriRequest
import org.slf4j.LoggerFactory
import skinny.http.{HTTP, HTTPException, Request, Response}

import scala.util.Try

class GitHubSource extends Source {

  private val log = LoggerFactory.getLogger(classOf[GitHubSource])

  override val CONFIG_PREFIX: String = "github"
  final val USER_CONFIG = "user"
  final val REPO_CONFIG = "repo"
  final val FILEPATH_CONFIG = "filepath"
  final val BRANCH_CONFIG = "branch"
  final val HOSTNAME_CONFIG = "hostname"
  final val AUTH_BASIC_CONFIG = "auth.basic"
  final val AUTH_TOKEN_CONFIG = "auth.token"

  var lastModified: Option[String] = None
  val objectMapper = new ObjectMapper()
  var user: String = _
  var repo: String = _
  var filepath: String = _
  var branch: String = _
  var hostname: String = _
  var basicOpt: Option[String] = _
  var tokenOpt: Option[String] = _

  /**
   * internal config definition for the module
   */
  override def configure(config: Config): Unit = {
    user = config.getString(USER_CONFIG)
    repo = config.getString(REPO_CONFIG)
    filepath = config.getString(FILEPATH_CONFIG)
    branch = config.getString(BRANCH_CONFIG)
    hostname = config.getString(HOSTNAME_CONFIG)
    basicOpt = Try(config.getString(AUTH_BASIC_CONFIG)).toOption
    tokenOpt = Try(config.getString(AUTH_TOKEN_CONFIG)).toOption
  }

  override def refresh(): Option[Reader] = {
    val url = s"https://$hostname/repos/$user/$repo/contents/$filepath"

    val request: Request = new Request(url)

    // super important in order to properly fail in case a timeout happens for example
    request.enableThrowingIOException(true)

    // authentication if present
    basicOpt.foreach(basic => {
      val basicB64 = Base64.getEncoder.encodeToString(basic.getBytes("UTF-8"))
      request.header("Authorization", s"Basic $basicB64")
    })

    tokenOpt.foreach(token => {
      request.header("Authorization", s"Token $token")
    })

    request.header("Accept", "application/vnd.github.v3+json")

    // we use this header for the 304
    lastModified.foreach(header => request.header("If-Modified-Since", header))

    val response: Response = HTTP.get(request)

    response.status match {
      case 200 =>
        val b64encodedContent =
          objectMapper.readTree(response.textBody).get("content").asText()
        val data = new String(
          Base64.getDecoder
            .decode(b64encodedContent.replace("\n", "").replace("\r", "")),
          Charset.forName("UTF-8")
        )
        Some(new StringReader(data))
      case _ =>
        log.warn(response.asString)
        throw HTTPException(Some(response.asString), response)
    }
  }

  /**
   * Close all the necessary underlying objects or connections belonging to this instance
   */
  override def close(): Unit = {
    // HTTP
  }
}
