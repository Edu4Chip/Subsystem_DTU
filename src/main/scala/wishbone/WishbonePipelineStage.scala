package wishbone

import chisel3._
import misc.FormalHelper._

class WishbonePipelineStage(addrWidth: Int) extends Module {

  val io = IO(new Bundle {
    val in = WishbonePort.targetPort(addrWidth)
    val out = WishbonePort.masterPort(addrWidth)
  })

  formalProperties {
    io.in.targetPortProperties("WishbonePipelineStage.in")
    io.out.masterPortProperties("WishbonePipelineStage.out")
  }

  
val accessReg = RegInit(0.B)

  when(io.in.cyc && !accessReg && !io.in.ack) {
    accessReg := 1.B
  }.elsewhen(io.out.ack) {
    accessReg := 0.B
  }


  io.out.cyc := accessReg
  io.out.stb := accessReg
  io.out.we := RegNext(io.in.we, 0.B)
  io.out.sel := RegNext(io.in.sel, 0.U)
  io.out.adr := RegNext(io.in.adr, 0.U)
  io.out.dat_i := RegNext(io.in.dat_i, 0.U)

  io.in.dat_o := RegNext(io.out.dat_o, 0.U)
  io.in.ack := RegNext(io.out.ack, 0.B)

}

object WishbonePipelineStage {
  def apply(
    in: WishbonePort,
  ): WishbonePort = {
    val stage = Module(new WishbonePipelineStage(in.addrWidth))
    stage.io.in <> in
    stage.io.out
  }
}