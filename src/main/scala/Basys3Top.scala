import chisel3._
import chisel3.util._

import io.UartPins
import io.PmodPins
import ponte.Ponte
import apb.ApbMux
import apb.ApbTargetPort
import dtu.DtuSubsystem
import didactic.DidacticSubsystem

class DidacticControl extends Module {

  val io = IO(new Bundle {
    val apb = new ApbTargetPort(4, 32)
    val ssCtrl = Output(UInt(6.W))
    val pmod = Vec(2, Flipped(new PmodPins))
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
    ctrl.io.ssCtrl(3,0),
    ctrl.io.pmod(0).gpi,
    sub.io.pmod(1).gpo,
    sub.io.pmod(0).gpo
  )

}

object Basys3Top extends App {
  (new stage.ChiselStage).emitSystemVerilog(
    new Basys3Top(new DtuSubsystem("leros/asm/didactic_rt.s")),
    Array("--target-dir", args.headOption.getOrElse("generated"))
  )
}

import com.fazecast.jSerialComm._
import leros.util.Assembler

object Basys3Communication extends App {

  val port = new ponte.PonteSerialPort("/dev/ttyUSB1", 921600)

  def enableLerosReset() = {
    val status = port.read(0x00)
    port.send(0x00, status | 0x02)
  }

  def disableLerosReset() = {
    val status = port.read(0x00)
    port.send(0x00, status & ~0x02)
  }

  def bootFromRam() = {
    val status = port.read(0x00)
    port.send(0x00, status | 0x01)
  }

  def program(path: String) = {
    val blop = Assembler.assemble(path)
    val words = blop.grouped(2).map {
      case Array(a,b) => (b << 8) | a
      case Array(a) => a
    }.toSeq
    println(s"Programming $path")
    words.foreach(println)
    port.send(0x2000, words)
  }



  for (i <- 0 until 16) {
    port.send(0x00, i)
    Thread.sleep(50)
  }

  for (i <- 0 until 16) {
    port.send(0x0C, i)
    Thread.sleep(50)
  }

  for (i <- 0 until 16) {
    port.send(0x00, Seq.fill(4)(i))
    Thread.sleep(50)
  }

  port.send(0x00, Seq.fill(4)(0))


  println("resetting leros")
  enableLerosReset()
  Thread.sleep(500)

  println("selecting ram boot")
  port.send(0x0C, 1)
  bootFromRam()
  Thread.sleep(500)

  println("programming")
  program("leros/asm/didactic.s")
  Thread.sleep(500)

  println("disabling reset")
  disableLerosReset()

  port.close()

}