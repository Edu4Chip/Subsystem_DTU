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

  MemoryFactory.help()

  MemoryFactory.using(MemoryFactory.fromString(args.head)) {
    ChiselStage.emitSystemVerilogFile(
      new DtuSubsystem(
        DtuSubsystemConfig.default
          .copy(
            romProgramPath = args(1),
            instructionMemorySize = 1 << 10,
            dataMemorySize = 1 << 10,
            frequency = 100_000_000,
            lerosBaudRate = 115200,
            ponteBaudRate = 115200,
          )
      ).printMemoryMap(),
      args.drop(2),
      Array("--lowering-options=disallowLocalVariables,disallowPackedArrays")
    )
  }
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

object LerosAssemble extends App {
  val file = args.head

  val code = Assembler.assemble(file)
  val outFile = file.replace(".s", ".bin")

  val byteArray = code.flatMap { word =>
    Array(
      (word & 0xff).toByte,
      ((word >> 8) & 0xff).toByte
    )
  }.toArray

  val fos = new java.io.FileOutputStream(outFile)
  fos.write(byteArray)
  fos.close()
}

object ProgArray extends App {
  val name = "didactic_adder"

  val code = Assembler.assemble(s"leros-asm/$name.s")
  println(s"unsigned int $name[] = {")
  code.grouped(2).zipWithIndex.foreach {
    case (Array(a, b), i) =>
      println(f"  0x$b%04x$a%04x, // 0x${i * 4}%04x")
    case (Array(a), i) =>
      println(f"  0x$a%04x, // 0x${i * 4}%04x")
  }
  println("};")
}

object GdbLoader extends App {
  val code = Assembler.assemble("leros-asm/selftest.s")
  code.grouped(2).zipWithIndex.foreach {
    case (Array(a, b), i) =>
      val pointer = 0x01052000 + i * 4
      println(f"monitor mww 0x$pointer%08x 0x$b%04x$a%04x")
    case (Array(a), i) =>
      val pointer = 0x01052000 + i * 4
      println(f"monitor mww 0x$pointer%08x 0x$a%04x")
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
  uart.uartPins.rx := Mux(sysCtrl.ctrlPort.lerosUartLoopBack, uart.uartPins.tx, lerosRx)

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
    sysCtrl.apbPort -> 0xc00, // system control registers at 0xC00
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
  io.gpio.outputEnable := gpio.gpioPort.oe ## 0xa.U(4.W)
  gpio.gpioPort.in := io.gpio.in(conf.gpioPins - 1, 4)

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
    dataMemorySize = 1 << 8, // 512 Bytes but this is overwritten

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


object DtuSelfTest extends App {


    val com = new DtuSerialDriver("/dev/ttyUSB1", 9600)

    com.resetLerosEnable()

    com.uploadProgram("leros-asm/selftest.s")

    com.writeCrossCoreReg(0, 0)
    com.selectBootRam()
    com.enableUartLoopBack()

    println(com.readCrossCoreReg(0).toHexString)
    println(com.readCrossCoreReg(1).toHexString)

    com.resetLerosDisable()

    com.writeCrossCoreReg(0, 0xab)

    while (com.readCrossCoreReg(0) != 1) {
      print(".")
      Thread.sleep(100)
    }

    println(s"Result: ${com.readCrossCoreReg(1).toHexString}")

    com.close()

  }

object DtuSelectRom extends App {

  val com = new DtuSerialDriver("/dev/ttyUSB1", 9600)

  com.resetLerosEnable()
  com.selectBootRom()
  com.disableUartLoopBack()
  com.writeCrossCoreReg(0, 8_000_000 / 4)
  com.resetLerosDisable()

  com.close()

}