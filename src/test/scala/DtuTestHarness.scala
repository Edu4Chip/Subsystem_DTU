

import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils
import leros._
import io.UartPins
import io.PmodPins
import dtu.DtuSubsystem
import dtu.peripherals._
import apb.ApbTargetPort
import dtu.DtuSubsystemConfig
import apb.ApbBfm

class DtuTestHarness(conf: DtuSubsystemConfig) extends Module {

  val io = IO(new Bundle {
    val dbg = new Debug(conf.lerosSize, conf.instructionMemoryAddrWidth)
    val apb = new ApbTargetPort(conf.apbAddrWidth, conf.apbDataWidth)
    val uart = new UartPins
    val bootSel = Input(Bool())
    val resetLeros = Input(Bool())
    val pmod1 = new PmodPins
  })

  val dtu = Module(new DtuSubsystem(conf))

  // Boring Utils for debugging
  io.dbg.accu := DontCare
  io.dbg.pc := DontCare
  io.dbg.instr := DontCare
  io.dbg.exit := DontCare
  BoringUtils.bore(dtu.leros.accu, Seq(io.dbg.accu))
  BoringUtils.bore(dtu.leros.pcReg, Seq(io.dbg.pc))
  BoringUtils.bore(dtu.leros.instr, Seq(io.dbg.instr))
  BoringUtils.bore(dtu.leros.exit, Seq(io.dbg.exit))

  io.apb <> dtu.io.apb
  io.pmod1 <> dtu.io.pmod(1)
  dtu.io.pmod(0).gpi := Cat(io.uart.rx, io.bootSel)
  io.uart.tx := dtu.io.pmod(0).gpo(2)

  dtu.io.irqEn := false.B
  dtu.io.ssCtrl := Cat(io.resetLeros, io.bootSel)

}
