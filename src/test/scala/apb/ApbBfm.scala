package apb
import chisel3._

import chiseltest._


import misc.TestingHelper.ClockExtension

object ApbBfm {
  sealed trait AccessResult[+T] {

    def unwrap(): T = this match {
      case AccessSuccess(d) => d
      case TargetError      => org.scalatest.Assertions.fail("Target error")
    }

    def expectError(): Unit = this match {
      case AccessSuccess(_) => org.scalatest.Assertions.fail("Expected error")
      case TargetError      =>
    }

  }
  case class AccessSuccess[T](data: T) extends AccessResult[T]
  case object TargetError extends AccessResult[Nothing]
}

class ApbBfm(clock: Clock, io: ApbTargetPort) {

  import ApbBfm._

  def write(
      addr: BigInt,
      wdata: BigInt,
      mask: Int = 0xf
  ): AccessResult[Unit] = {
    // setup phase
    io.paddr.poke(addr.U)
    io.pwrite.poke(1.B)
    io.psel.poke(1.B)
    io.penable.poke(0.B)
    io.pwdata.poke(wdata.U)
    io.pstrb.poke(mask.U)
    clock.step()

    // access phase
    io.penable.poke(1.B)
    clock.step()
    clock.stepUntil(io.pready.peekBoolean)
    if (io.pslverr.peekBoolean()) return TargetError

    // finish
    io.paddr.poke(0.U)
    io.pwrite.poke(0.B)
    io.psel.poke(0.B)
    io.penable.poke(0.B)
    io.pwdata.poke(0.U)
    clock.step()

    AccessSuccess(())
  }

  def read(addr: BigInt): AccessResult[BigInt] = {
    // setup phase
    io.paddr.poke(addr.U)
    io.pwrite.poke(0.B)
    io.psel.poke(1.B)
    io.penable.poke(0.B)
    io.pwdata.poke(0.U)
    clock.step()

    // access phase
    io.penable.poke(1.B)
    clock.stepUntil(io.pready.peekBoolean)
    if (io.pslverr.peekBoolean()) return TargetError
    val rdData = io.prdata.peekInt()
    clock.step()

    // finish + read out read data
    io.paddr.poke(0.U)
    io.pwrite.poke(0.B)
    io.psel.poke(0.B)
    io.penable.poke(0.B)
    io.pwdata.poke(0.U)
    

    AccessSuccess(rdData)
  }

}
