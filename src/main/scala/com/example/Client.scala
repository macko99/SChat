package com.example

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{TextMessage, _}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl._

import scala.concurrent.Future
import scala.io.StdIn

class Client(host: String) {

  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher

  def listRooms(): Unit = {

    val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(host + "schat/list"))

    val printSink: Sink[Message, Future[Done]] =
      Sink.foreach {
        case message: TextMessage.Strict => println(message.text)
      }

    Source.single(TextMessage("list"))
        .viaMat(webSocketFlow)(Keep.both)
      .toMat(printSink)(Keep.left)
      .run()
  }

  def connectToRoom(url: String): Unit = {

    val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(host + url))

    val printSink: Sink[Message, Future[Done]] =
      Sink.foreach {
         case message: TextMessage.Strict => println(message.text)
      }

    val userSource: Source[Message, ActorRef] =
      Source.actorRef(10,
        OverflowStrategy.dropTail)


    val (sourceRef, upgradeResponse)=
      userSource
        .viaMat(webSocketFlow)(Keep.both)
        .toMat(printSink)(Keep.left)
        .run()

    upgradeResponse.map { upgrade =>
      if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
        Done
      } else {
        throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
      }
    }

    chat(sourceRef)
  }

  def exit(): Unit ={
    system.terminate()
  }

  private def chat(socketRef: ActorRef): Unit = {
    var running = true
    while (running) {
      StdIn.readLine() match {
        case "exit" =>
          println("Logout")
          running = false
        case msg => socketRef ! TextMessage(msg)
      }
    }
  }
}

object Client {
  def apply(host: String): Client = {
    new Client(host)
  }

  def main(args: Array[String]): Unit = {

    val client = Client("ws://localhost:8888/")

    println("enter name:")
    val name = StdIn.readLine()

    client.listRooms()

    println("enter room number")
    val roomId = StdIn.readInt()
    client.connectToRoom(s"schat/room/$roomId?name=$name")

    client.exit()
  }
}
