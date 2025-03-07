import chisel3._

import leros.Leros
import leros.DataMem
import leros.util.Assembler

class DtuSubsystem extends Module with DidacticSubsystem {
    
    val io = IO(new DidacticSubsystemIO(apbAddrWidth = 32, apbDataWidth = 32))
    io.irq := 0.B

    val syncReset = withReset(0.B)(RegNext(RegNext(reset.asBool)))

    withReset(syncReset) {

        val leros = Module(new Leros)
        leros.reset := syncReset || io.ssCtrl(2)
        leros.dmemIO.rdData := 0.U
        dontTouch(leros.dmemIO)

        val instrMem = Module(new InstructionMemory(32 * 4))
        val dmem = Module(new DataMem(5, debugMem = false))
        leros.dmemIO <> dmem.io
        dmem.io.wr := 0.B

        val gpo = RegInit(0.U(8.W))
        when(leros.dmemIO.wrAddr === 0x3FFFL.U && leros.dmemIO.wr) {
            gpo := leros.dmemIO.wrData(7, 0)
        } otherwise {
            dmem.io.wr := leros.dmemIO.wr
        }
        io.pmod(0).gpo := gpo(3, 0)
        io.pmod(1).gpo := gpo(7, 4)
        io.pmod.foreach(_.oe := 0.U)

        leros.imemIO <> instrMem.instrPort
        instrMem.apbPort <> io.apb

    }

    
}

object DtuSubsystem extends App {
    (new stage.ChiselStage).emitSystemVerilog(new DtuSubsystem, Array("--target-dir", "generated"))
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