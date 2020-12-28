package com.massmutual.streaming.manager.util

import java.io.{BufferedReader, Reader}

import com.massmutual.streaming.model.connector_state.SPConnectState
import scalapb.json4s.JsonFormat

object SPConnectStateFactory {

  def fromReader(reader: BufferedReader): SPConnectState = {
    val content = Stream.continually(reader.readLine()).takeWhile(_ != null).map(_.concat("\n")).mkString

    reader.close()

    JsonFormat.fromJsonString[SPConnectState](content)

  }

}
