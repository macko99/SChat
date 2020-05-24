package com.example

import akka.Done
import akka.actor.{ActorRef, ActorSystem, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest, WebSocketUpgradeResponse}
import akka.stream.{CompletionStrategy, OverflowStrategy}
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.concurrent.{Await, Future, TimeoutException}

class Client(host: String) {
  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher

  def listRooms(): List[String] = {

    val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(host + "schat/list"))

    val (upgradeResponse, rooms) =
      Source.single(TextMessage("list"))
        .viaMat(webSocketFlow)(Keep.right)
        .toMat(Sink.seq[Message])(Keep.both)
        .run()

    checkResponse(upgradeResponse)

    import scala.concurrent.duration._
    try {
      Await.result(rooms, 10.seconds).toList.map(msg => "room no " + msg.asTextMessage.getStrictText)
    } catch {
      case _: TimeoutException => println("timeout")
        List()
    }
  }

  def connectToRoom(url: String, printFun: String => Unit ): ActorRef = {

    val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(host + url))

    val printSink: Sink[Message, Future[Done]] =
      Sink.foreach {
        case message: TextMessage.Strict => printFun(message.text)
        case _ => printFun("unknown message")
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

    sourceRef
  }

  def exit(): Future[Terminated] = system.terminate()

  private def checkResponse(response: Future[WebSocketUpgradeResponse]): Unit = response.map { upgrade =>
    if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
      Done
    } else {
      throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
    }
  }
}
