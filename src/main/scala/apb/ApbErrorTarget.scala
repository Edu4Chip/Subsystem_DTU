package apb

import chisel3._

class ApbErrorTarget(addrWidth: Int, dataWidth: Int) extends Module {

  val apbPort = IO(ApbPort.targetPort(addrWidth, dataWidth))

  val active = RegInit(0.B) // tracks ongoing transaction
  when(active) {
    active := 0.B
  }.elsewhen(apbPort.psel) {
    active := 1.B
  }

  apbPort.pready := active
  apbPort.pslverr := active
  apbPort.prdata := 0.U

}
