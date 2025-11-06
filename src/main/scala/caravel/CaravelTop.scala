package caravel

import mem.MemoryFactory
import dtu.DtuSubsystemConfig
import io.GpioPins
import circt.stage.ChiselStage

import chisel3._
import chisel3.util._
import wishbone.WishboneMux
import wishbone.HasWishbonePort
import wishbone.WishbonePort

object CaravelTop extends App {

  ChiselStage.emitSystemVerilogFile((new CaravelTop(115200)).printMemoryMap(), Array("-td", "generated/caravel"), Array("--lowering-options=disallowLocalVariables,disallowPackedArrays"))
  
}

class CaravelTop(baud: Int) extends Module with HasWishbonePort {

  val conf = DtuSubsystemConfig.default.copy(
    gpioPins = 12,
    frequency = 10_000_000,
    lerosBaudRate = baud,
    ponteBaudRate = baud,
    instructionMemorySize = 1 << 10,
    dataMemorySize = 1 << 10,
    romProgramPath = "leros-asm/selftest.s",
  )

  val io = IO(new Bundle {
    /** wishbone port */
    val wb = WishbonePort.targetPort(32)

    /** logic analyzer debug outputs */
    val la_out = Output(UInt(128.W))

    /** IO pads */
    val gpio = new GpioPins(24)
    
  })

  override def getWbPort: WishbonePort = io.wb

  // Caravel user project logic goes here

  

  val lerosCfram = MemoryFactory.using(mem.ChipFoundrySram) {
    Module(new LerosCaravel(conf, "ChipFoundrySram"))
  }
  lerosCfram.io.gpio.in := io.gpio.in(11, 0)

  val lerosSky130 = MemoryFactory.using(mem.Sky130Sram) {
    Module(new LerosCaravel(conf, "OpenRamSky130"))
  }
  lerosSky130.io.gpio.in := io.gpio.in(23, 12)


  WishboneMux(io.wb)(
    lerosCfram.io.wb -> 0x00000000,
    lerosSky130.io.wb -> 0x00001000,
  )

  io.gpio.out := Cat(lerosSky130.io.gpio.out, lerosCfram.io.gpio.out)
  io.gpio.oe := Cat(lerosSky130.io.gpio.oe, lerosCfram.io.gpio.oe)
  val expandedPcCfram = Wire(UInt(32.W))
  expandedPcCfram := lerosCfram.io.dbg.pc
  val expandedPcSky130 = Wire(UInt(32.W))
  expandedPcSky130 := lerosSky130.io.dbg.pc
  io.la_out := Cat(lerosSky130.io.dbg.acc, expandedPcSky130, lerosCfram.io.dbg.acc, expandedPcCfram)

  def printMemoryMap(): this.type = {
    io.wb.printMemoryMap()
    this
  }
}