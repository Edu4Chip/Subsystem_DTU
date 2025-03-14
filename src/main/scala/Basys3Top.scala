import chisel3._
import chisel3.util._

import io.UartPins
import ponte.Ponte
import apb.ApbMux
import apb.ApbTargetPort
import dtu.DtuSubsystem

class DidacticControl extends Module {

  val io = IO(new Bundle {
    val apb = new ApbTargetPort(4, 32)
    val ssCtrl = Output(UInt(6.W))
    val pmod = Vec(2, Flipped(new didactic.PmodGpioPort))
  })

  val ssCtrlReg = RegInit(0.U(6.W))
  val gpiReg = RegInit(0.U(8.W))

  io.pmod(0).gpi := gpiReg(3, 0)
  io.pmod(1).gpi := gpiReg(7, 4)
  io.ssCtrl := ssCtrlReg

  io.apb.pready := 0.B
  io.apb.pslverr := 0.B
  io.apb.prdata := MuxLookup(
    RegNext(io.apb.paddr(3, 2)),
    DontCare,
    Seq(
      0.U -> ssCtrlReg,
      1.U -> io.pmod(1).oe ## io.pmod(0).oe,
      2.U -> io.pmod(1).gpo ## io.pmod(0).gpo,
      3.U -> gpiReg
    )
  )

  when(io.apb.psel && io.apb.penable) {
    io.apb.pready := 1.B

    when(io.apb.pwrite) {
      switch(io.apb.paddr(3, 2)) {
        is(0.U) {
          ssCtrlReg := io.apb.pwdata
        }
        is(3.U) {
          gpiReg := io.apb.pwdata
        }
      }
    }
  }

}

class Basys3Top(subsystem: => DidacticSubsystem) extends Module {

  val io = IO(new Bundle {
    val uart = new UartPins
    val led = Output(UInt(16.W))
  })

  val sub = Module(subsystem)
  sub.io.irqEn := 0.B

  val ponte = Module(new Ponte(100000000, 921600))
  io.uart <> ponte.io.uart

  val ctrl = Module(new DidacticControl)
  ctrl.io.pmod <> sub.io.pmod
  sub.io.ssCtrl := ctrl.io.ssCtrl

  ApbMux(ponte.io.apb)(
    ctrl.io.apb -> 0x0000,
    sub.io.apb -> 0x2000
  )

  io.led := Cat(
    ponte.io.apb.pready,
    0.U(7.W),
    sub.io.pmod(1).gpo,
    sub.io.pmod(0).gpo
  )

}

object Basys3Top extends App {
  (new stage.ChiselStage).emitSystemVerilog(
    new Basys3Top(new DtuSubsystem("leros/asm/didactic.s")),
    Array("--target-dir", args.headOption.getOrElse("generated"))
  )
}
