package com.gmail.webserg.hightload

import akka.actor.{Actor, ActorLogging, Props}
import com.gmail.webserg.hightload.QueryRouter.{UserPostQuery, UserPostQueryParameter}
import com.gmail.webserg.hightload.UserDataReader.User

class UserQueryActor extends Actor with ActorLogging {


  override def receive: Receive = {

    case q: UserPostQuery =>
      val oldUser = q.oldUser
      val nfirst_name = q.param.first_name.getOrElse(oldUser.first_name)
      val nlast_name = q.param.last_name.getOrElse(oldUser.last_name)
      val nbirth_date = q.param.birth_date.getOrElse(oldUser.birth_date)
      val ngender = q.param.gender.getOrElse(oldUser.gender)
      val nemail = q.param.email.getOrElse(oldUser.email)
      val newUser = User(oldUser.id, nfirst_name, nlast_name, nbirth_date, ngender, nemail)
      Database.updateUser(newUser)
      context.actorSelection("/user/" + QueryRouter.name) ! newUser

    case q: UserPostQueryParameter =>
      val nid = q.id.get
      val nfirst_name = q.first_name.get
      val nlast_name = q.last_name.get
      val nbirth_date = q.birth_date.get
      val ngender = q.gender.get
      val nemail = q.email.get
      val newUser = User(nid, nfirst_name, nlast_name, nbirth_date, ngender, nemail)
      Database.addUser(newUser)
      context.actorSelection("/user/" + QueryRouter.name) ! newUser
  }


}

object UserQueryActor {
  val name: String = "user"

  def props: Props = Props[UserQueryActor]

  def validateNewPostUserQuery(q: UserPostQueryParameter) = {
    q.id.isDefined && q.first_name.isDefined && q.last_name.isDefined && q.birth_date.isDefined && q.email.isDefined && q.gender.isDefined
  }
}

