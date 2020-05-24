package com.example

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{TextMessage, _}
import akka.stream.scaladsl._
import akka.stream.{CompletionStrategy, OverflowStrategy}

import scala.concurrent.{Await, Future}
import scala.io.StdIn

class Client(host: String) {

  implicit val system: ActorSystem = ActorSystem()

  import system.dispatcher

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

  def connectToRoom(url: String): Unit = {

    val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(host + url))

    val printSink: Sink[Message, Future[Done]] =
      Sink.foreach {
        case message: TextMessage.Strict => println(message.text)
        case _ => println("unknown message")
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

    chat(sourceRef)
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

  private def chat(socketRef: ActorRef): Unit = {
    var running = true
    while (running) {
      StdIn.readLine() match {
        case "exit" => running = false
          socketRef ! Done
        case msg => socketRef ! TextMessage(msg)
      }
    }
  }

  def runCli(): Unit = {
    var running = true
    print("enter name: ")
    val name = StdIn.readLine()
    println(
      s"""Type 'list' to list available rooms
Type 'connect <room number>' to join
Type 'exit' while in room to leave it
Type 'exit' while not in room to stop""")
    while (running) {
      print(name + ">")
      StdIn.readLine() match {
        case "list" => println("available rooms:")
          this.listRooms().foreach(println)
        case "exit" => this.exit()
          println("Shutdown")
          running = false
        case connect: String => if (connect.contains("connect")) {
          val roomId = connect.replaceAll("connect", "").trim.toInt
          this.connectToRoom(s"schat/room/$roomId?name=$name")
        } else println("unknown command")
      }
    }
  }
}

object Client {
  def apply(host: String): Client = {
    new Client(host)
  }

  def main(args: Array[String]): Unit = {
    print("host (default localhost): ")
    val host = StdIn.readLine() match {
      case "" => "localhost"
      case h => h
    }
    print("port (default 8888): ")
    val port = StdIn.readLine() match {
      case "" => 8888
      case p => p.toInt
    }

    Client(s"ws://$host:$port/").runCli()
  }
}