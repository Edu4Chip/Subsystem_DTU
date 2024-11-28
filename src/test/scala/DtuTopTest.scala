import chisel3._
import chisel3.util.experimental.BoringUtils

import leros._

class DtuTopTest(prog:String) extends Module {
  val dtuTop = Module(new DtuTop(prog = prog, resetSyncFact = () => Module(new ResetSyncTest())))
  // val programmer = Module(new Programmer(dtuTop.lerosClockFreq, dtuTop.lerosUartBaudrate, prog))
  val io = IO(new Debug(dtuTop.lerosSize, dtuTop.lerosMemAddrWidth))

  // Boring Utils for debugging
  io.accu := DontCare
  io.pc := DontCare
  io.instr := DontCare
  io.exit := DontCare
  BoringUtils.bore(dtuTop.leros.accu, Seq(io.accu))
  BoringUtils.bore(dtuTop.leros.pcReg, Seq(io.pc))
  BoringUtils.bore(dtuTop.leros.instr, Seq(io.instr))
  BoringUtils.bore(dtuTop.leros.exit, Seq(io.exit))
  

  dtuTop.io.apb.paddr := 0.U
  dtuTop.io.apb.pwrite := false.B
  dtuTop.io.apb.psel := false.B
  dtuTop.io.apb.penable := false.B
  dtuTop.io.apb.pwdata := 0.U

  // dtuTop.io.pmod0.gpi := programmer.io.txd ## 0.U
  dtuTop.io.pmod0.gpi := 0.U
  dtuTop.io.pmod1.gpi := 0.U
  dtuTop.io.irqEn1 := false.B
  dtuTop.io.ssCtrl1 := 0.U
}

