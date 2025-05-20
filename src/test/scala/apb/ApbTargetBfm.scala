package apb

import chisel3._
import chiseltest._

object ApbTargetBfm {
  sealed trait Op
  case class Read(addr: BigInt) extends Op
  case class Write(addr: BigInt, data: BigInt) extends Op
}

class ApbTargetBfm(apb: ApbPort, clk: Clock) {

  import ApbTargetBfm._

  def next(): Op = {
    while (!(apb.psel.peekBoolean() && apb.penable.peekBoolean())) clk.step()
    if (apb.pwrite.peekBoolean()) {
      Write(apb.paddr.peekInt(), apb.pwdata.peekInt())
    } else {
      Read(apb.paddr.peekInt())
    }
  }

  def respondRead(data: BigInt): Unit = {
    apb.pready.poke(true.B)
    apb.prdata.poke(data.U)
    clk.step()
    apb.pready.poke(false.B)
  }

  def respondWrite(): Unit = {
    apb.pready.poke(true.B)
    clk.step()
    apb.pready.poke(false.B)
  }

}
