package chisel3

import chisel3._
import chisel3.internal.Builder

import apb.ApbTargetPort

class DidacticSubsystemIO(apbAddrWidth: Int, apbDataWidth: Int) extends Bundle {

  val apb = new ApbTargetPort(apbAddrWidth, apbDataWidth)

  val irq = Output(Bool())
  val irqEn = Input(Bool())

  val ssCtrl = Input(UInt(6.W))

  val pmod = Vec(2, new didactic.PmodGpioPort)
}

abstract class DidacticSubsystem extends Module {

  val io: DidacticSubsystemIO

}
