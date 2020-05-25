package com.example

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl._
import akka.stream.{CompletionStrategy, FlowShape, OverflowStrategy}
import akka.{Done, NotUsed}

class ChatRoom(roomId: Int, actorSystem: ActorSystem) {

  private val chatRoomActor = actorSystem.actorOf(Props(classOf[ChatRoomActor], roomId))

  def webSocketRoomFlow(user: String): Flow[Message, Message, _] =
    Flow.fromGraph(GraphDSL.create(Source.actorRef(
      completionMatcher = {
        case Done =>
          CompletionStrategy.draining
      },
      failureMatcher = PartialFunction.empty,
      bufferSize = 10,
      OverflowStrategy.dropTail)) {
      implicit builder =>
        import GraphDSL.Implicits._
        chatSource =>
          val messagesFromSocket = builder.add(
            Flow[Message].collect {
              case TextMessage.Strict(text) => ChatMsg(user, text)
            })

          val messagesToSocket = builder.add(
            Flow[Messages].map {
              case ChatMsg(author, text) => TextMessage(s"$author : $text")
              case HistoryMsg(history) => TextMessage(history)
            }
          )

          val messagesToActor = Sink.actorRef[Messages](chatRoomActor, LogOut(user), PartialFunction.empty)

          val incomingMessages = builder.add(Merge[Messages](2))

          val messagesFromActor = builder.materializedValue.map(actor => LogIn(user, actor))

          messagesFromSocket ~> incomingMessages.in(0)

          messagesFromActor ~> incomingMessages.in(1)

          incomingMessages ~> messagesToActor

          chatSource ~> messagesToSocket

          FlowShape(messagesFromSocket.in, messagesToSocket.out)
    })

  override def toString: String = s"$roomId"
}

object ChatRoom {
  private var chatRooms: Map[Int, ChatRoom] = Map.empty[Int, ChatRoom]

  def getRoom(number: Int)(implicit actorSystem: ActorSystem): ChatRoom = chatRooms.getOrElse(number, ChatRoom(number))

  def apply(roomId: Int)(implicit actorSystem: ActorSystem): ChatRoom = {
    val chatRoom = new ChatRoom(roomId, actorSystem)
    chatRooms += roomId -> chatRoom
    chatRoom
  }

  def webSocketRoomList(): Flow[Message, Message, NotUsed] =
    Flow.fromGraph(GraphDSL.create() {
      implicit builder =>
        val input = builder.add(Sink.ignore)
        val output = builder.add(Source(listRooms()).map(room => TextMessage(room.toString())))
        FlowShape(input.in, output.out)
    })

  def listRooms(): List[ChatRoom] = chatRooms.values.toList
}