package didactic

import chisel3._

import apb.ApbTargetPort

class DidacticSubsystemIO(apbAddrWidth: Int, apbDataWidth: Int) extends Bundle {

  val apb = new ApbTargetPort(apbAddrWidth, apbDataWidth)

  val irq = Output(Bool())
  val irqEn = Input(Bool())

  val ssCtrl = Input(UInt(6.W))

  val pmod = Vec(2, new io.PmodPins)
}

abstract class DidacticSubsystem extends Module {

  val io: DidacticSubsystemIO

}
