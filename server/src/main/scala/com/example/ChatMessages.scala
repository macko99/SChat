package com.example.chat

import akka.actor.ActorRef

sealed trait Messages

final case class LogIn(name: String, userActor: ActorRef) extends Messages

final case class LogOut(name: String) extends Messages

final case class ChatMsg(sender: String, message: String) extends Messages