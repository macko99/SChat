package com.example

import akka.actor.{Actor, ActorRef}

class ChatRoomActor(roomId: Int) extends Actor {

  var participants: Map[String, ActorRef] = Map.empty[String, ActorRef]
  var roomHistory: List[String] = List[String]()

  override def receive: Receive = {
    case LogIn(name, actorRef) =>
      logMsg(s"User $name joined room")
      if (roomHistory.nonEmpty) actorRef ! HistoryMsg(roomHistory.fold("\n")(_ + _))
      participants += name -> actorRef

    case LogOut(name) =>
      participants -= name
      logMsg(s"User $name left room")

    case msg@ChatMsg(sender, text) =>
      broadcast(msg)
      if (roomHistory.size == 1000) roomHistory = roomHistory.tail
      roomHistory = roomHistory ++ List(s"$sender > $text\n")
  }

  def logMsg(msg: String): Unit = {
    println(s"\n[log] $msg $roomId")
    broadcast(ChatMsg("system", msg))
    roomHistory = roomHistory ++ List(s"system > $msg\n")
  }

  def broadcast(message: ChatMsg): Unit = participants.values.foreach(_ ! message)
}
