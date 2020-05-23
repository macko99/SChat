package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.Materializer
import com.example.chat.ChatRoom

import scala.io.StdIn

object Server extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("akka-system")

  val config = actorSystem.settings.config
  val interface = config.getString("app.interface")

  val port = config.getInt("app.port")

  import Directives._

  val route = chatRoute ~ listRoute

  val binding = Http().bindAndHandle(route, interface, port)

  import actorSystem.dispatcher

  runCli()

  private def runCli(): Unit = {
    var running = true
    println(s"""
Listening on http://$interface:$port
Type 'list' to list available rooms
Type 'exit' to stop
  """)
    while(running) {
      StdIn.readLine() match {
        case "list" => ChatRoom.listRooms().foreach(room => println(room))
        case "exit" => binding.flatMap(_.unbind()).onComplete(_ => actorSystem.terminate())
          println("Shutdown")
          running = false
      }
    }
  }

  def chatRoute(implicit actorSystem: ActorSystem, materializer: Materializer): Route = pathPrefix("schat"/"room" /
    IntNumber) { chatId =>
    parameter(Symbol("name")) { userName =>
      handleWebSocketMessages(ChatRoom.getRoom(chatId).websocketUserFlow(userName))
    }
  }

  def listRoute: Route = path("schat"/"list") {
    complete( ChatRoom.listRooms().toString())
  }
}
