package ru.solverit.core

import akka.actor.{ActorLogging, Actor, ActorRef}
import ru.solverit.domain.Player
import ru.solverit.net.packet.Packet.{LoginResp, PacketMSG}
import ru.solverit.tcpfront.Send

// -----
case class Authenticate(session: ActorRef, comm: PacketMSG)
case class AuthenticatedFailed(session: ActorRef, comm: PacketMSG)

class AuthService extends Actor with ActorLogging {

  // -----
  val storageService = context.actorSelection("akka://server/user/storage")
  val taskService = context.actorSelection("akka://server/user/task")
  val gameService = context.actorSelection("akka://server/user/game")

  // ----- actor -----
  override def preStart() {
    log.info("Starting auth service")
  }

  override def receive = {
    case task: Authenticate  => handleAuth(task)
    case task: SomePlayer => handleAuthenticated(task)
    case task: AuthenticatedFailed => handleFailed(task)

    case _ => log.info("unknown message")
  }

  override def postStop() {
    // clean up resources
    log.info("Stoping auth service")
  }


  // ----- handles -----
  def handleAuth(task: Authenticate) = {
    storageService ! GetPlayerByName(task.session, task.comm)
  }

  def handleAuthenticated(task: SomePlayer) = {
    val login: LoginResp.Builder = LoginResp.newBuilder()
    login.setId(task.player.id)
    task.session ! Send(Cmd.AuthResp, login.build().toByteArray)
    gameService ! task
  }

  def handleFailed(task: AuthenticatedFailed) = {
    task.session ! Send(Cmd.AuthErr, Array[Byte]())
  }
}
