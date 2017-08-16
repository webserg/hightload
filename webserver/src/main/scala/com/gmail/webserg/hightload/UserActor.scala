package com.gmail.webserg.hightload

import akka.actor.{Actor, ActorLogging, Props}
import com.gmail.webserg.hightload.UserDataReader.User

class UserActor(val users: Map[Int, User]) extends Actor with ActorLogging{

  override def receive: Receive = {
    case id: Int =>
      sender ! Option(users.get(id))
  }
}

object UserActor {
  val name: String = "user"

  def props: Props = Props[UserActor]
}
