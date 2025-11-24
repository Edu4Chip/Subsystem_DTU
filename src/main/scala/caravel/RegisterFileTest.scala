package caravel

import chisel3._
import chisel3.util._

import wishbone.WishbonePort
import misc.FormalHelper._


class RegisterFileTest extends Module {

  val io = IO(new Bundle {
    val wb = WishbonePort.targetPort(12)
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

  val rf = Module(new RegFileBlackbox)

  rf.io.clk := clock

  rf.io.w_data := io.wb.dat_i
  rf.io.w_addr := io.wb.adr
  rf.io.w_ena := io.wb.we

  rf.io.ra_addr := io.wb.adr
  val outa = rf.io.ra_data
  rf.io.rb_addr := io.wb.adr
  val outb = rf.io.rb_data
  io.wb.dat_o := outa
  // read the other port
  when (io.wb.adr(5)) {
    io.wb.dat_o := outb
  }

  io.wb.ack := accessPhase

}
/*
   rf_top i_rf(
      .w_data(w_data),
      .w_addr(w_addr),
      .w_ena(w_ena),
      .ra_addr(ra_addr),
      .rb_addr(rb_addr),
      .ra_data(ra_data),
      .rb_data(rb_data),
      .clk(clk)
   );
 */
class RegFileBlackbox() extends BlackBox {
  override def desiredName: String = "rf_top"
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val w_data = Input(UInt(32.W))
    val w_addr = Input(UInt(5.W))
    val w_ena = Input(Bool())
    val ra_addr = Input(UInt(5.W))
    val ra_data = Output(UInt(32.W))
    val rb_addr = Input(UInt(5.W))
    val rb_data = Output(UInt(32.W))
  })
}
