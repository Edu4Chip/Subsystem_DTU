package didactic

import chisel3._

import apb.ApbPort
import io.GpioPins

trait DidacticConfig {
  val apbAddrWidth: Int
  val apbDataWidth: Int
  val gpioPins: Int
  val ssCtrlPins: Int
  val frequency: Int
}

class DidacticSubsystemIO(conf: DidacticConfig) extends Bundle {

  val apb = ApbPort.targetPort(conf.apbAddrWidth, conf.apbDataWidth)

  val irq = Output(Bool())
  val irqEn = Input(Bool())

  val ssCtrl = Input(UInt(conf.ssCtrlPins.W))

  val gpio = new GpioPins(conf.gpioPins)
}

abstract class DidacticSubsystem extends Module {

  val io: DidacticSubsystemIO

  def printMemoryMap(): this.type = {
    println(s"--- Apb Memory Map ${"-" * 49}")
    val targets = io.apb.getTargets()
    targets.foreach { target =>
      println(
        target.toString()
      )
    }
    println(s"${"-" * (19 + 49)}")
    this
  }

}
