package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import com.example.chat.ChatRooms

object ChatRoutes {

  def chatRoute(implicit actorSystem: ActorSystem, materializer: Materializer): Route = pathPrefix("schat/room" /
    IntNumber) { chatId =>
    parameter(Symbol("name")) { userName =>
      handleWebSocketMessages(ChatRooms.getRoom(chatId).websocketUserFlow(userName))
    }
  }

  def listRoute: Route = path("schat/list") {
    complete(ChatRooms.listRooms().toString())
  }
}
