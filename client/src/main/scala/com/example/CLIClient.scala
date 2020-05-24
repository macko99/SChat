package com.example

import akka.Done
import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.TextMessage

import scala.io.StdIn

class CLIClient(host: String) {

  private val client = new Client(host)

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
          client.listRooms().foreach(println)
        case "exit" => client.exit()
          println("Shutdown")
          running = false
        case connect: String => if (connect.contains("connect")) {
          val roomId = connect.replaceAll("connect", "").trim.toInt
          chat(client.connectToRoom(s"schat/room/$roomId?name=$name", println))
        } else println("unknown command")
      }
    }
  }
}

object CLIClient {
  def apply(host: String): CLIClient = {
    new CLIClient(host)
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

    CLIClient(s"ws://$host:$port/").runCli()
  }
}