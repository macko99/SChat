package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{Directives, Route}

import scala.io.StdIn

class Server(host: String, port: Int) {

  implicit val actorSystem: ActorSystem = ActorSystem("akka-system")

  import Directives._

  private[this] val route = chatRoute ~ listRoute

  private[this] val binding = Http().bindAndHandle(route, host, port)

  import actorSystem.dispatcher

  def runCli(): Unit = {
    var running = true
    println(
      s"""Listening on http://$host:$port
Type 'list' to list available rooms
Type 'exit' to stop""")
    while (running) {
      print(">")
      StdIn.readLine() match {
        case "list" => ChatRoom.listRooms().foreach(room => println(room))
        case "exit" => binding.flatMap(_.unbind()).onComplete(_ => actorSystem.terminate())
          println("Shutdown")
          running = false
      }
    }
  }

  def chatRoute: Route = pathPrefix("schat" / "room" / IntNumber) { chatId =>
    parameter(Symbol("name")) { userName =>
      handleWebSocketMessages(ChatRoom.getRoom(chatId).webSocketRoomFlow(userName))
    }
  }

  def listRoute: Route = path("schat" / "list") {
    handleWebSocketMessages(ChatRoom.webSocketRoomList())
  }
}

object Server {
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
    Server(host, port).runCli()
  }

  def apply(host: String, port: Int): Server = {
    new Server(host, port)
  }
}
