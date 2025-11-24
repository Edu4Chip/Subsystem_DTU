package caravel

import circt.stage.ChiselStage

import chisel3._
import chisel3.util.Cat

import leros.Leros
import leros.DataMem
import leros.InstrMem
import leros.util.Assembler

import io._
import apb._
import didactic._
import ponte.Ponte
import dtu.peripherals.RegBlock
import chisel3.util.log2Ceil
import mem.MemoryFactory
import dtu._
import wishbone._
import chisel3.util.experimental.BoringUtils
import misc.FormalHelper.formalProperties

object LerosCaravel extends App {

  MemoryFactory.help()

  MemoryFactory.using(MemoryFactory.fromString(args.head)) {
    ChiselStage.emitSystemVerilogFile(
      new LerosCaravel(
        DtuSubsystemConfig.default
          .copy(
            romProgramPath = args(1),
            instructionMemorySize = args(2).toInt,
            dataMemorySize =  args(3).toInt,
            frequency = 10_000_000,
            lerosBaudRate = 115200,
            ponteBaudRate = 115200,
            gpioPins = CaravelTopConfig.gpioPerLeros,
            apbAddrWidth = 16
          ),
        args.head
      ).printMemoryMap(),
      args.drop(4),
      Array("--lowering-options=disallowLocalVariables,disallowPackedArrays")
    )
  }

}

class LerosCaravel(conf: DtuSubsystemConfig, memoryType: String) extends Module with HasWishbonePort {

  override def desiredName: String = s"LerosCaravel_${memoryType}"

  val io = IO(new Bundle {
    /** wishbone port */
    val wb = WishbonePort.targetPort(conf.apbAddrWidth)

    /** IO pads */
    val gpio = new GpioPins(conf.gpioPins)
  
  })
  override def getWbPort: WishbonePort = io.wb

  formalProperties {
    io.wb.targetPortProperties("LerosCaravel.wb")
  }

  val lerosRx = io.gpio.in(3)
  val ponteRx = io.gpio.in(1)

  val sysCtrl = Module(new SystemControl)
  val ponte = Module(new Ponte(conf.frequency, conf.ponteBaudRate))
  ponte.io.uart.rx := ponteRx

  val leros = Module(new Leros(conf.lerosSize, conf.instructionMemoryAddrWidth))
  leros.reset := RegNext(sysCtrl.ctrlPort.lerosReset || reset.asBool)

  val instrMem = Module(new InstructionMemory(conf.instructionMemorySize))
  val rom = Module(
    new InstrMem(conf.instructionMemoryAddrWidth, conf.romProgramPath)
  )
  val regBlock = Module(new peripherals.RegBlock(conf.crossCoreRegisters))
  val gpio = Module(new peripherals.Gpio(conf.gpioPins - 4))
  val dmem = Module(new DataMemory(conf.dataMemorySize))
  val uart = Module(new peripherals.Uart(conf.frequency, conf.lerosBaudRate))
  uart.uartPins.rx := Mux(sysCtrl.ctrlPort.lerosUartLoopBack, uart.uartPins.tx, lerosRx)

  leros.imemIO <> instrMem.instrPort
  leros.imemIO <> rom.io
  leros.imemIO.instr := Mux( // choose boot source
    sysCtrl.ctrlPort.lerosBootFromRam,
    instrMem.instrPort.instr,
    rom.io.instr
  )

  // Apb interconnect visible to Ibex and Ponte (Uart bridge)
  ApbMux(ApbArbiter(ponte.io.apb, WishboneToApb(io.wb)))(
    instrMem.apbPort -> 0x0000, // instruction memory at 0x0000
    regBlock.apbPort -> 0x1000, // cross-core registers at 0x1000
    sysCtrl.apbPort -> 0x2000, // system control registers at 0x2000
  )

  // Interconnect visible to Leros
  DataMemMux(leros.dmemIO)( // 16 bit address space
    dmem.dmemPort -> 0x0000, // data memory at 0x0000
    regBlock.dmemPort -> 0x8000, // cross-core registers at 0x8000
    gpio.dmemPort -> 0x8100, // GPIO at 0x8100
    uart.dmemPort -> 0x8110, // UART at 0x8110
  )

  io.gpio.out := gpio.gpioPort.out ## Cat(
    0.B,
    uart.uartPins.tx,
    0.B,
    ponte.io.uart.tx,
  )
  io.gpio.oe := gpio.gpioPort.oe ## 0xa.U(4.W)
  gpio.gpioPort.in := io.gpio.in(conf.gpioPins - 1, 4)

  def printMemoryMap(): this.type = {
    io.wb.printMemoryMap()
    this
  }

}
