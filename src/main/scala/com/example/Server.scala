package com.example

class Server {
  case class Response(status: String, token: String)
  case class Request(sender: String, token: String, requestType: RequestType )

}
