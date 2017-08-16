package com.gmail.webserg.hightload

import akka.actor.{Actor, ActorLogging, Props}
import com.gmail.webserg.hightload.UserDataReader.User
import com.gmail.webserg.hightload.UserLoaderActor.LoadUsers

class UserLoaderActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case LoadUsers => {
      val usersList: List[User] = UserDataReader.readData("C:\\git\\hightLoad\\webserver\\resources\\data\\data\\data\\users_1.json").users
      val usersMap = usersList.map(i => i.id -> i).toMap
      val userActor  = context.actorOf(Props(new UserActor(usersMap)), name = UserActor.name)
      context.actorSelection("/user/userLoader/user") ! 12
    }
    case user: Option[User] => {
//      log.debug("sdfsdfsdf" + user.get.email)
      print("fsdfsdfs")
    }
  }


}

object UserLoaderActor {
  val name = "userLoader"

  case object LoadUsers

  def props: Props = Props[UserLoaderActor]
}
