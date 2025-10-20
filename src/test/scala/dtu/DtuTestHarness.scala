package dtu

import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils
import leros._
import io.UartPins
import io.GpioPins
import dtu.DtuSubsystem
import dtu.peripherals._
import apb.ApbPort
import dtu.DtuSubsystemConfig
import apb.ApbBfm
import dtu.DtuInterface

class DtuTestHarness(conf: DtuSubsystemConfig) extends Module {

  val io = IO(new Bundle {
    val dbg = new Debug(conf.lerosSize, conf.instructionMemoryAddrWidth)
    val apb = new ApbPort(conf.apbAddrWidth, conf.apbDataWidth)
    val lerosUart = new UartPins
    val ponteUart = new UartPins
    val gpio = new GpioPins(conf.gpioPins - 4)
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
  io.gpio.out := dtu.io.gpio.out(conf.gpioPins - 1, 4)
  io.gpio.oe := dtu.io.gpio.outputEnable(conf.gpioPins - 1, 4)
  dtu.io.gpio.in := Cat(io.gpio.in, io.lerosUart.rx, 0.B, io.ponteUart.rx, 0.B)

  io.lerosUart.tx := dtu.io.gpio.out(2)
  io.ponteUart.tx := dtu.io.gpio.out(0)

  dtu.io.irqEn := 0.B
  dtu.io.ssCtrl := 0.U
}

case class DtuTestHarnessBfm(dut: DtuTestHarness) extends DtuInterface {

  val apbBfm = new ApbBfm(dut.clock, dut.io.apb)

  def send(addr: Int, data: Seq[Int]): Unit = {
    assert(addr >= 0 && addr < 0x1000)
    data.zipWithIndex.foreach { case (d, i) =>
      apbBfm.write(addr + i * 4, BigInt(d) & 0xFFFFFFFFL)
    }
  }

  def read(addr: Int, words: Int): Seq[Int] = {
    assert(addr >= 0 && addr < 0x1000)
    (0 until words).map(i => apbBfm.read(addr + i * 4).unwrap().toInt)
  }

}
