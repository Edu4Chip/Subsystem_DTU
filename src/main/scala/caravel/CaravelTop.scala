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

  val gpioPerLeros = 8

  val conf = DtuSubsystemConfig.default.copy(
    gpioPins = gpioPerLeros,
    frequency = 10_000_000,
    lerosBaudRate = baud,
    ponteBaudRate = baud,
    instructionMemorySize = 1 << 10,
    dataMemorySize = 1 << 10,
    romProgramPath = "leros-asm/selftest.s",
  )

  val io = IO(new Bundle {
    /** wishbone port */
    val wb = WishbonePort.targetPort(20)

    /** IO pads */
    val gpio = new GpioPins(4 * gpioPerLeros)
    
  })

  override def getWbPort: WishbonePort = io.wb

  // Caravel user project logic goes here

  

  val lerosCfram = MemoryFactory.using(mem.ChipFoundrySram) {
    Module(new LerosCaravel(conf, "ChipFoundrySram"))
  }
  lerosCfram.io.gpio.in := io.gpio.in(7, 0)

  val lerosSky130 = MemoryFactory.using(mem.Sky130Sram) {
    Module(new LerosCaravel(conf, "OpenRamSky130"))
  }
  lerosSky130.io.gpio.in := io.gpio.in(15, 8)

  val lerosDffRam = MemoryFactory.using(mem.DffRam) {
    Module(new LerosCaravel(conf, "DffRam"))
  }
  lerosDffRam.io.gpio.in := io.gpio.in(23, 16)

  val lerosRtlRam = MemoryFactory.using(mem.ChiselSyncMemory) {
    Module(new LerosCaravel(conf, "RtlSyncMemory"))
  }
  lerosRtlRam.io.gpio.in := io.gpio.in(31, 24)

  WishboneMux(io.wb)(
    lerosCfram.io.wb -> 0x00000000,
    lerosSky130.io.wb -> 0x00001000,
    lerosDffRam.io.wb -> 0x00002000,
    lerosRtlRam.io.wb -> 0x00003000,
  )

  io.gpio.out := Cat(lerosRtlRam.io.gpio.out, lerosDffRam.io.gpio.out, lerosSky130.io.gpio.out, lerosCfram.io.gpio.out)
  io.gpio.oe := Cat(lerosRtlRam.io.gpio.oe, lerosDffRam.io.gpio.oe, lerosSky130.io.gpio.oe, lerosCfram.io.gpio.oe)
  
  def printMemoryMap(): this.type = {
    io.wb.printMemoryMap()
    this
  }
}