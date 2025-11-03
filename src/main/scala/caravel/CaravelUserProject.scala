package caravel

import chisel3._
import wishbone._
import io._
import dtu.DtuSubsystemConfig

trait CaravelUserProject {
  val io: CaravelUserProjectIO

  def printMemoryMap(): this.type = {
    println(s"--- Wishbone Memory Map ${"-" * 46}")
    val targets = io.wb.getTargets()
    targets.foreach { target =>
      println(
        target.toString()
      )
    }
    println(s"${"-" * (19 + 46)}")
    this
  }
}

class CaravelUserProjectIO(conf: DtuSubsystemConfig) extends Bundle {
  /** wishbone port */
  val wb = WishbonePort.targetPort(conf.apbAddrWidth)

  /** logic analyzer */
  val la = new Bundle {
    val in = Input(UInt(128.W))
    val out = Output(UInt(128.W))
    val oe = Input(UInt(128.W))
  }

  /** IO pads */
  val gpio = new GpioPins(conf.gpioPins)

  /** IRQ */
  val user_irq = Output(UInt(3.W))


  
}