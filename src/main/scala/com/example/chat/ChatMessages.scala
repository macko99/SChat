package com.example.chat

import akka.actor.ActorRef

sealed trait Messages

case class LogIn(name: String, userActor: ActorRef) extends Messages

case class LogOut(name: String) extends Messages

case class ChatMsg(sender: String, message: String) extends Messages