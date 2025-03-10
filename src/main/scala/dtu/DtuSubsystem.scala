package dtu

import chisel3._
import chisel3.util.Cat

import leros.Leros
import leros.DataMem
import leros.InstrMem
import leros.util.Assembler

import apb._
import didactic._
import peripherals.RegBlock

class DtuSubsystem(prog: String) extends Module with DidacticSubsystem {
    
    val io = IO(new DidacticSubsystemIO(apbAddrWidth = 12, apbDataWidth = 32))
    io.irq := 0.B

    val bootSelect = io.pmod(0).gpi(0) || io.ssCtrl(3)

    val syncReset = withReset(0.B)(RegNext(RegNext(reset.asBool)))

    withReset(syncReset) {

        val leros = Module(new Leros)
        leros.reset := syncReset || io.ssCtrl(2)

        val instrMem = Module(new InstructionMemory(32 * 4))
        val rom = Module(new InstrMem(8, prog))
        val regBlock = Module(new peripherals.RegBlock)
        val gpio = Module(new peripherals.Gpio)
        val uart = Module(new peripherals.Uart(4, 20, 5))
        val dmem = Module(new DataMemory(5 * 4))

        leros.imemIO <> instrMem.instrPort
        leros.imemIO <> rom.io
        leros.imemIO.instr  := Mux(bootSelect, instrMem.instrPort.instr, rom.io.instr)

        ApbMux(io.apb)(
            instrMem.apbPort -> (0x0000 until 0x0FF0),
            regBlock.apbPort -> (0x0FF0 until 0x1000),
        )

        DataMemMux(leros.dmemIO)(
            dmem.dmemPort     -> (0x00 until 0x15),
            regBlock.dmemPort -> (0x20 until 0x24),
            gpio.dmemPort     -> (0x30 until 0x34),
            uart.dmemPort     -> (0x40 until 0x44)
        )

        io.pmod(1) <> gpio.pmodPort
        io.pmod(0).gpo := uart.uartPins.tx ## 0.U(2.W)
        io.pmod(0).oe := Cat(0.B, 0.B, 1.B, 1.B)
        uart.uartPins.rx := io.pmod(1).gpi(2)
    }

    
}

object DtuSubsystem extends App {
    (new stage.ChiselStage).emitSystemVerilog(new DtuSubsystem("leros/asm/didactic.s"), Array("--target-dir", "generated"))
}

object Code extends App {
    val code = Assembler.assemble("leros/asm/didactic.s")
    code.grouped(2).zipWithIndex.foreach { 
        case (Array(a,b), i) => 
            val pointer = 0x01052000 + i * 4
            println(f"*(volatile unsigned int*)(0x$pointer%08x) = 0x$b%04x$a%04x;")
        case (Array(a), i) => 
            val pointer = 0x01052000 + i * 4
            println(f"*(volatile unsigned int*)(0x$pointer%08x) = 0x$a%04x;")
    }

    println()

    code.foreach { instr =>
        println(f"0x$instr%04x")
    }
}