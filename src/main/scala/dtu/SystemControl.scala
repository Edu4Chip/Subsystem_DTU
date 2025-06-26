package dtu

import chisel3._
import chisel3.util._
import apb._

class SystemControl extends Module {

  val apbPort = IO(ApbPort.targetPort(2, 32))
  val ctrlPort = IO(new Bundle {
    val lerosReset = Output(Bool())
    val lerosBootFromRam = Output(Bool())
    val lerosUartLoopBack = Output(Bool())
  })

  val lerosResetReg = RegInit(0.B)
  val lerosBootFromRamReg = RegInit(0.B)
  val lerosUartLoopBackReg = RegInit(0.B)
  ctrlPort.lerosReset := lerosResetReg
  ctrlPort.lerosBootFromRam := lerosBootFromRamReg
  ctrlPort.lerosUartLoopBack := lerosUartLoopBackReg

  apbPort.prdata := Cat(
    lerosUartLoopBackReg,
    lerosBootFromRamReg,
    lerosResetReg
  )
  apbPort.pready := 0.B
  apbPort.pslverr := 0.B

  when(apbPort.psel && apbPort.penable) {

    apbPort.pready := 1.B

    when(apbPort.pwrite) {
      lerosResetReg := apbPort.pwdata(0)
      lerosBootFromRamReg := apbPort.pwdata(1)
      lerosUartLoopBackReg := apbPort.pwdata(2)
    }

  }

}
