package didactic

import chisel3._

import apb.ApbTargetPort

trait DidacticConfig {
  val apbAddrWidth: Int
  val apbDataWidth: Int
  val frequency: Int
}

class DidacticSubsystemIO(conf: DidacticConfig) extends Bundle {

  val apb = new ApbTargetPort(conf.apbAddrWidth, conf.apbDataWidth)

  val irq = Output(Bool())
  val irqEn = Input(Bool())

  val ssCtrl = Input(UInt(6.W))

  val pmod = Vec(2, new io.PmodPins)
}

abstract class DidacticSubsystem extends Module {

  val io: DidacticSubsystemIO

}
