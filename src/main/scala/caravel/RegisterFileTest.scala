package caravel

import chisel3._
import chisel3.util._

import wishbone.WishbonePort
import misc.FormalHelper._


class RegisterFileTest extends Module {

  /// size in bytes
  val size = 256

  val io = IO(new Bundle {
    val wb = WishbonePort.targetPort(log2Ceil(size))
  })

  formalProperties {
    io.wb.targetPortProperties("RegisterFileTest.wb")
  }

  val accessPhase = RegInit(0.B)

  when(accessPhase) {
    accessPhase := 0.B
  }.elsewhen(io.wb.cyc) {
    accessPhase := 1.B
  }

  io.wb.dat_o := 0xDEADBEEFL.U
  io.wb.ack := accessPhase

}
