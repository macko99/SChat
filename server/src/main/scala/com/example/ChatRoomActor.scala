package com.example

import akka.actor.{Actor, ActorRef}

class ChatRoomActor(roomId: Int) extends Actor {

  var participants: Map[String, ActorRef] = Map.empty[String, ActorRef]

  override def receive: Receive = {
    case LogIn(name, actorRef) =>
      broadcast(ChatMsg("system", s"User $name joined room"))
      participants += name -> actorRef
      println(s"\n[log] User $name joined room $roomId ")

    case LogOut(name) =>
      println(s"\n[log] User $name left room $roomId ")
      participants -= name
      broadcast(ChatMsg("system", s"User $name left room"))

    case msg: ChatMsg =>
      broadcast(msg)

    case "list" =>

  }

  def broadcast(message: ChatMsg): Unit = participants.values.foreach(_ ! message)
}
