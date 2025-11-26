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
  val numberOfWbGpio = 6
  val helloMorseGpio = 1
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
    apbAddrWidth = 16
  )

  require((numberOfLeros * gpioPerLeros) + numberOfWbGpio + helloMorseGpio <= 37, s"Total number of GPIO pins exceeds available pins in Caravel (max 37)")

  val io = IO(new Bundle {
    /** wishbone port */
    val wb = WishbonePort.targetPort(20)

    /** IO pads */
    val gpio = new GpioPins((numberOfLeros * gpioPerLeros) + numberOfWbGpio + helloMorseGpio)
    
  })

  formalProperties {
    io.wb.targetPortProperties("CaravelTop.wb")
  }

  override def getWbPort: WishbonePort = io.wb

  val lerosCfram = MemoryFactory.using(mem.ChipFoundrySram) {
    Module(new LerosCaravel(conf.copy(
      instructionMemorySize = 1 << 12,
      dataMemorySize = 1 << 12,
    ), "ChipFoundrySram"))
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

  val wishboneGpio = Module(new WishboneGpio(numberOfWbGpio))

  val morse = Module(new HelloMorse(conf.frequency))
  val morseGpio = Wire(new GpioPins(1))
  morseGpio.out := morse.io.led
  morseGpio.oe := 0.B



  val gpios = lerosSystems.map(l => (l.desiredName, gpioPerLeros, l.io.gpio)) :+ ("wishboneGpio", numberOfWbGpio, wishboneGpio.io.gpio) :+ ("helloMorse", helloMorseGpio, morseGpio)


  gpios.foldLeft(0) { case (offset, (name, gpioCount, gpio)) =>
    println(s"$name connected to GPIO [${offset + gpioCount - 1}:$offset]")
    gpio.in := RegNext(io.gpio.in((offset + gpioCount) - 1, offset)) // input synchronization
    offset + gpioCount
  }

  // Connect outputs
  io.gpio.out := Cat(gpios.map(_._3.out).reverse)
  io.gpio.oe := Cat(gpios.map(_._3.oe).reverse)

  val registerFileTest = Module(new RegisterFileTest)
  

  WishboneMux(io.wb)(
    lerosCfram.io.wb -> 0x00000000, 
    lerosSky130.io.wb -> 0x00010000,
    lerosDffRam.io.wb -> 0x00020000,
    lerosRtlRam.io.wb -> 0x00030000,
    registerFileTest.io.wb -> 0x00040000,
    wishboneGpio.io.wb -> 0x00050000,
  )
  
  def printMemoryMap(): this.type = {
    io.wb.printMemoryMap()
    this
  }
}