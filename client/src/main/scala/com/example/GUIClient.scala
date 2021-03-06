package com.example

import java.text.SimpleDateFormat
import java.util.Calendar

import akka.Done
import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.TextMessage
import scalafx.scene.control.TextArea

class GUIClient(host: String) {

  private val client = new Client(host)

  var socketRef: ActorRef = _

  def listRooms(): List[String] = client.listRooms()

  def disconnectFromRoom(): Unit = {
    socketRef ! Done
  }

  def connectToRoom(url: String, textArea: TextArea): Unit = {

    def guiPrint(msg: String): Unit = {
      val dateTimeFormatter = new SimpleDateFormat("hh:mm:ss a")
      val dateTime = dateTimeFormatter.format(Calendar.getInstance.getTime)
      textArea.text.update(textArea.text.get() + dateTime + " : " + msg + "\n")
      textArea.selectPositionCaret(textArea.getLength)
      textArea.deselect()
    }

    socketRef = client.connectToRoom(url, guiPrint)
  }

  def exit(): Unit = client.exit()

  def sendMessage(msg: String): Unit = {
    socketRef ! TextMessage(msg)
  }

}

object GUIClient {
  def apply(host: String): GUIClient = {
    new GUIClient(host)
  }
}