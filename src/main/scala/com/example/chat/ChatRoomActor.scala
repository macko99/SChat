package com.example.chat

import akka.actor.{Actor, ActorRef}

class ChatRoomActor(roomId: Int) extends Actor {

  var participants: Map[String, ActorRef] = Map.empty[String, ActorRef]

  override def receive: Receive = {
    case LogIn(name, actorRef) =>
      participants += name -> actorRef
      broadcast(ChatMsg("system",s"User $name joined channel..."))
      println(s"User $name joined channel[$roomId]")

    case LogOut(name) =>
      println(s"User $name left channel[$roomId]")
      broadcast(ChatMsg("system",s"User $name left channel[$roomId]"))
      participants -= name

    case msg: ChatMsg =>
      broadcast(msg)
  }

  def broadcast(message: ChatMsg): Unit = participants.values.foreach(_ ! message)

}
