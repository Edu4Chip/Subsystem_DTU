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
import misc.FormalHelper.formalProperties

object CaravelTop extends App {

  ChiselStage.emitSystemVerilogFile((new CaravelTop(115200)).printMemoryMap(), Array("-td", "generated/caravel"), Array("--lowering-options=disallowLocalVariables,disallowPackedArrays"))
  
}

object CaravelTopConfig {
  val gpioPerLeros = 6
  val numberOfLeros = 4
}

class CaravelTop(baud: Int) extends Module with HasWishbonePort {

  import CaravelTopConfig._

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
    val gpio = new GpioPins(numberOfLeros * gpioPerLeros)
    
  })

  formalProperties {
    io.wb.targetPortProperties("CaravelTop.wb")
  }

  override def getWbPort: WishbonePort = io.wb

  val lerosCfram = MemoryFactory.using(mem.ChipFoundrySram) {
    Module(new LerosCaravel(conf, "ChipFoundrySram"))
  }

  val lerosSky130 = MemoryFactory.using(mem.Sky130Sram) {
    Module(new LerosCaravel(conf, "OpenRamSky130"))
  }

  val lerosDffRam = MemoryFactory.using(mem.DffRam) {
    Module(new LerosCaravel(conf, "DffRam"))
  }

  val lerosRtlRam = MemoryFactory.using(mem.ChiselSyncMemory) {
    Module(new LerosCaravel(conf.copy(
      instructionMemorySize = 1 << 8,
      dataMemorySize = 1 << 7,
    ), "RtlSyncMemory"))
  }

  val lerosSystems = Seq(lerosCfram, lerosSky130, lerosDffRam, lerosRtlRam)
  require(numberOfLeros == lerosSystems.length, s"numberOfLeros ($numberOfLeros) must match the number of Leros subsystems instantiated (${lerosSystems.length})")

  // Connect GPIO inputs
  lerosSystems.zipWithIndex.foreach { case (leros, i) =>
    println(s"${leros.desiredName} has GPIO [${(i + 1) * gpioPerLeros - 1}:${i * gpioPerLeros}]")
    leros.io.gpio.in := RegNext(io.gpio.in((i + 1) * gpioPerLeros - 1, i * gpioPerLeros)) // input synchronization
  }

  // Connect outputs
  io.gpio.out := Cat(lerosSystems.map(_.io.gpio.out).reverse)
  io.gpio.oe := Cat(lerosSystems.map(_.io.gpio.oe).reverse)

  val registerFileTest = Module(new RegisterFileTest)

  WishboneMux(io.wb)(
    lerosCfram.io.wb -> 0x00000000,
    lerosSky130.io.wb -> 0x00001000,
    lerosDffRam.io.wb -> 0x00002000,
    lerosRtlRam.io.wb -> 0x00003000,
    registerFileTest.io.wb -> 0x00004000,
  )
  
  def printMemoryMap(): this.type = {
    io.wb.printMemoryMap()
    this
  }
}