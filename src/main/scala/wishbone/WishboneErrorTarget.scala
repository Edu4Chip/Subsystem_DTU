package wishbone

import chisel3._


class WishboneErrorTarget(addrWidth: Int) extends Module {

  val wbPort = IO(WishbonePort.targetPort(addrWidth))

  val active = RegInit(0.B) // tracks ongoing transaction
  when(active) {
    active := 0.B
  }.elsewhen(wbPort.cyc && wbPort.stb) {
    active := 1.B
  }

  wbPort.ack := active
  wbPort.dat_o := 0.U

}
