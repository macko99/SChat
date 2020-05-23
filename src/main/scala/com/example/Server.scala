package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import com.example.chat.ChatRooms

import scala.io.StdIn

object Server extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("akka-system")

  val config = actorSystem.settings.config
  val interface = config.getString("app.interface")

  val port = config.getInt("app.port")

  import Directives._

  val route = ChatRoutes.chatRoute ~
    ChatRoutes.listRoute

  val binding = Http().bindAndHandle(route, interface, port)

  command()

  import actorSystem.dispatcher

  binding.flatMap(_.unbind()).onComplete(_ => actorSystem.terminate())
  println("Server is down...")

  private def list(): Unit = {
    ChatRooms.listRooms().foreach(room => println(room))
  }

  private def command(): Unit = {
    var running = true
    println(s"""
Listening on http://$interface:$port
Type 'list' to list available rooms
Type 'exit' to stop
  """)
    while (running) {
      StdIn.readLine() match {
        case "list" => list()
        case "exit" => running = false
      }
    }
  }
}