package com.example

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{TextMessage, _}
import akka.stream.scaladsl._
import akka.stream.{CompletionStrategy, OverflowStrategy}
import scalafx.scene.control.TextArea

import scala.concurrent.{Await, Future}

class GUIClient(host: String) {

  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher

  var socketRef: ActorRef = _

  def listRooms(): List[String] = {

    val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(host + "schat/list"))

    val printSink: Sink[Message, Future[Seq[Message]]] =
      Sink.seq[Message]

    val (upgradeResponse, rooms) =
      Source.single(TextMessage("list"))
        .viaMat(webSocketFlow)(Keep.right)
        .toMat(printSink)(Keep.both)
        .run()

    checkResponse(upgradeResponse)

    import scala.concurrent.duration._
    Await.result(rooms, 10.seconds).toList.map(msg => "room no " + msg.asTextMessage.getStrictText)
  }

  def connectToRoom(url: String, textArea: TextArea): Unit = {

    val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(host + url))

    def guiPrint(msg: String): Unit ={
      textArea.text.update(textArea.text.get() + msg + "\n")
    }

    val printSink: Sink[Message, Future[Done]] =
      Sink.foreach {
        case message: TextMessage.Strict => guiPrint(message.text)
        case _ => guiPrint("unknown message")
      }

    val userSource: Source[Message, ActorRef] =
      Source.actorRef(
        completionMatcher = {
          case Done =>
            CompletionStrategy.draining
        },
        failureMatcher = PartialFunction.empty,
        bufferSize = 10,
        OverflowStrategy.dropTail)


    val (sourceRef, upgradeResponse) =
      userSource
        .viaMat(webSocketFlow)(Keep.both)
        .toMat(printSink)(Keep.left)
        .run()

    checkResponse(upgradeResponse)

    socketRef = sourceRef
    //    chat(sourceRef)
  }

  def exit(): Unit = {
    system.terminate()
  }

  private def checkResponse(response: Future[WebSocketUpgradeResponse]): Unit = response.map { upgrade =>
    if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
      Done
    } else {
      throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
    }
  }

  def sendMessage(msg: String): Unit = {
    socketRef ! TextMessage(msg)
  }

}

object GUIClient {
  def apply(host: String): GUIClient = {
    new GUIClient(host)
  }
}