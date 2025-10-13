package wishbone

import misc.FormalHelper._

import chisel3._

import apb.ApbPort



class WishboneToApb(addrWidth: Int) extends Module {

  val io = IO(new Bundle {
    val wb = wishbone.WishbonePort.targetPort(addrWidth)
    val apb = ApbPort.masterPort(addrWidth, 32)
  })


  val accessPhase = RegInit(0.B)

  when(io.wb.cyc && io.wb.stb && !accessPhase) {
    accessPhase := 1.B
  }.elsewhen(io.apb.pready) {
    accessPhase := 0.B
  }

  // Default assignments
  io.apb.paddr := io.wb.adr
  io.apb.psel := io.wb.cyc && io.wb.stb
  io.apb.penable := accessPhase
  io.apb.pwrite := io.wb.we
  io.apb.pstrb := io.wb.sel
  io.apb.pwdata := io.wb.dat_i
  io.wb.dat_o := io.apb.prdata
  io.wb.ack := io.apb.pready

  formalProperties {
    // Properties
    io.apb.masterPortProperties("WishboneToApb.apb")
    io.wb.targetPortProperties("WishboneToApb.wb")
  }
  

}

object WishboneToApb {
  def apply(wb: WishbonePort): ApbPort = {
    val bridge = Module(new WishboneToApb(wb.addrWidth))
    bridge.io.wb <> wb
    bridge.io.apb
  }
}
