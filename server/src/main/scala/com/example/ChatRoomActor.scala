package com.example

import akka.actor.{Actor, ActorRef}

class ChatRoomActor(roomId: Int) extends Actor {

  var participants: Map[String, ActorRef] = Map.empty[String, ActorRef]

  override def receive: Receive = {
    case LogIn(name, actorRef) =>
      participants += name -> actorRef
      broadcast(ChatMsg("system",s"User $name joined room"))
      println(s"User $name joined room $roomId ")

    case LogOut(name) =>
      println(s"User $name left room $roomId ")
      broadcast(ChatMsg("system",s"User $name left room"))
      participants -= name

    case msg: ChatMsg =>
      broadcast(msg)

    case "list" =>

  }

  def broadcast(message: ChatMsg): Unit = participants.values.foreach(_ ! message)

}
