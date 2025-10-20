package caravel

import chisel3._
import wishbone._
import io._

trait CaravelUserProject {
  val io: CaravelUserProjectIO
}

class CaravelUserProjectIO extends Bundle {
  /** wishbone port */
  val wb = WishbonePort.targetPort(32)

  /** logic analyzer */
  val la = new Bundle {
    val in = Input(UInt(128.W))
    val out = Output(UInt(128.W))
    val oe = Input(UInt(128.W))
  }

  /** IO pads */
  val gpio = new GpioPins(16)

  /** IRQ */
  val user_irq = Output(UInt(3.W))
}