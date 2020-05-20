package com.example

class Client {
  case class Login(sender: String, password: String)
  case class Message(sender: String, message: String)

}
