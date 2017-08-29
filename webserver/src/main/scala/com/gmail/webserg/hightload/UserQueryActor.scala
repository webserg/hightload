package com.gmail.webserg.hightload

import akka.actor.{Actor, ActorLogging, Props}
import com.gmail.webserg.hightload.QueryRouter.{UserPostQuery, UserPostQueryParameter}
import com.gmail.webserg.hightload.UserDataReader.User

class UserQueryActor(var users: Map[Int, User]) extends Actor with ActorLogging {


  override def receive: Receive = {
    case id: Int =>
      sender ! users.get(id)

    case q: UserPostQuery =>
      val user = users.get(q.id)
      if (user.isDefined) {
        val oldUser = user.get
        val nfirst_name = q.param.first_name.getOrElse(oldUser.first_name)
        val nlast_name = q.param.last_name.getOrElse(oldUser.last_name)
        val nbirth_date = q.param.birth_date.getOrElse(oldUser.birth_date)
        val ngender = q.param.gender.getOrElse(oldUser.gender)
        val nemail = q.param.email.getOrElse(oldUser.email)
        val newUser = User(oldUser.id, nfirst_name, nlast_name, nbirth_date, ngender, nemail)
        users = users + (q.id -> newUser)
        sender() ! Some(User)

      } else sender() ! None

    case q: UserPostQueryParameter =>
      if (validateNewPostUserQuery(q)) {
        val user = users.get(q.id.get)
        if (user.isEmpty) {
          val nid = q.id.get
          val nfirst_name = q.first_name.get
          val nlast_name = q.last_name.get
          val nbirth_date = q.birth_date.get
          val ngender = q.gender.get
          val nemail = q.email.get
          val newUser = User(nid, nfirst_name, nlast_name, nbirth_date, ngender, nemail)
          users = users + (nid -> newUser)
          sender() ! Some(User)

        } else sender() ! None

      }
  }

  private def validateNewPostUserQuery(q: UserPostQueryParameter) = {
    q.id.isDefined && q.first_name.isDefined && q.last_name.isDefined && q.birth_date.isDefined && q.email.isDefined && q.gender.isDefined
  }
}

object UserQueryActor {
  val name: String = "user"

  def props: Props = Props[UserQueryActor]
}

