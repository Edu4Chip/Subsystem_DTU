package dtu

import circt.stage.ChiselStage

import chisel3._
import chisel3.util.Cat

import leros.Leros
import leros.DataMem
import leros.InstrMem
import leros.util.Assembler

import apb._
import didactic._
import ponte.Ponte
import peripherals.RegBlock
import chisel3.util.log2Ceil
import mem.MemoryFactory

object DtuSubsystem extends App {

  MemoryFactory.use(args.head match {
    case "sky130Sram"   => mem.Sky130Sram.create
    case "chiselSram"   => mem.ChiselSyncMemory.create
    case "didacticSram" => mem.DidacticSpSram.create
    case "registerRam"  => mem.RegMemory.create
    case _              => throw new Exception("Unknown memory type")
  })

  ChiselStage.emitSystemVerilogFile(
    new DtuSubsystem(
      DtuSubsystemConfig.default
        .copy(
          romProgramPath = args(1),
          instructionMemorySize = 1 << 10,
          dataMemorySize = 1 << 10
        )
    ),
    args.drop(2),
    Array("--lowering-options=disallowLocalVariables,disallowPackedArrays")
  )
}

object IbexCode extends App {
  val code = Assembler.assemble("leros-asm/didactic.s")
  code.grouped(2).zipWithIndex.foreach {
    case (Array(a, b), i) =>
      val pointer = 0x01052000 + i * 4
      println(f"*(volatile unsigned int*)(0x$pointer%08x) = 0x$b%04x$a%04x;")
    case (Array(a), i) =>
      val pointer = 0x01052000 + i * 4
      println(f"*(volatile unsigned int*)(0x$pointer%08x) = 0x$a%04x;")
  }
}

class DtuSubsystem(conf: DtuSubsystemConfig) extends DidacticSubsystem {

  val io = IO(new DidacticSubsystemIO(conf))
  io.irq := 0.B

  val lerosRx = io.gpio.in(3)
  val ponteRx = io.gpio.in(1)

  val sysCtrl = Module(new SystemControl)
  val ponte = Module(new Ponte(conf.frequency, conf.ponteBaudRate))
  ponte.io.uart.rx := ponteRx

  val leros = Module(new Leros(conf.lerosSize, conf.instructionMemoryAddrWidth))
  leros.reset := reset.asBool || sysCtrl.ctrlPort.lerosReset

  val instrMem = Module(new InstructionMemory(conf.instructionMemorySize))
  val rom = Module(
    new InstrMem(conf.instructionMemoryAddrWidth, conf.romProgramPath)
  )
  val regBlock = Module(new peripherals.RegBlock(conf.crossCoreRegisters))
  val gpio = Module(new peripherals.Gpio(conf.gpioPins - 4))
  val dmem = Module(new DataMemory(conf.dataMemorySize))
  val uart = Module(new peripherals.Uart(conf.frequency, conf.lerosBaudRate))
  uart.uartPins.rx := lerosRx

  leros.imemIO <> instrMem.instrPort
  leros.imemIO <> rom.io
  leros.imemIO.instr := Mux( // choose boot source
    sysCtrl.ctrlPort.lerosBootFromRam,
    instrMem.instrPort.instr,
    rom.io.instr
  )

  // Apb interconnect visible to Ibex and Ponte (Uart bridge)
  ApbMux(ApbArbiter(ponte.io.apb, io.apb))( // 12 bit address space
    instrMem.apbPort -> 0x000, // instruction memory at 0x000
    regBlock.apbPort -> 0x800, // cross-core registers at 0x800
    sysCtrl.apbPort -> 0xc00 // system control registers at 0xC00
  )

  // Interconnect visible to Leros
  DataMemMux(leros.dmemIO)( // 16 bit address space
    dmem.dmemPort -> 0x0000, // data memory at 0x0000
    regBlock.dmemPort -> 0x8000, // cross-core registers at 0x8000
    gpio.dmemPort -> 0x8100, // GPIO at 0x8100
    uart.dmemPort -> 0x8110 // UART at 0x8110
  )

  io.gpio.out := gpio.gpioPort.out ## Cat(
    0.B,
    uart.uartPins.tx,
    0.B,
    ponte.io.uart.tx
  )
  io.gpio.outputEnable := gpio.gpioPort.outputEnable ## 0xa.U(4.W)
  gpio.gpioPort.in := io.gpio.in(conf.gpioPins - 1, 4)

  printMemoryMap()
}

case class DtuSubsystemConfig(
    romProgramPath: String,
    instructionMemorySize: Int,
    dataMemorySize: Int,
    lerosSize: Int,
    crossCoreRegisters: Int,
    frequency: Int,
    lerosBaudRate: Int,
    ponteBaudRate: Int,
    apbAddrWidth: Int,
    apbDataWidth: Int,
    ssCtrlPins: Int,
    gpioPins: Int
) extends DidacticConfig {
  val instructionMemoryAddrWidth = log2Ceil(instructionMemorySize)
}
object DtuSubsystemConfig {
  def default = DtuSubsystemConfig(
    // Path to the assembly file
    romProgramPath = "leros-asm/didactic_rt.s",

    // Size of the programmable instruction memory in bytes
    instructionMemorySize = 1 << 11, // 2kB

    // Size of the data memory in bytes
    dataMemorySize = 1 << 8, // 256 Bytes

    // Width of the Leros datapath
    lerosSize = 32, // 32-bit accumulator

    // Number of cross-core registers
    // for communication between Leros and the APB
    crossCoreRegisters = 4,

    // Frequency of the system clock
    frequency = 100000000, // 1MHz

    // Baud rate of the Leros UART
    lerosBaudRate = 115200,

    // Baud rate of the Ponte UART for the UART-to-APB bridge
    ponteBaudRate = 921600,

    // Address and data width of the APB interface
    apbAddrWidth = 12,
    apbDataWidth = 32,

    // Number of control pins for the system coming
    // from the staff area
    ssCtrlPins = 8,

    // Number of GPIO pins
    gpioPins = 16
  )
}
