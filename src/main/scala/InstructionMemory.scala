import chisel3._
import chisel3.util.log2Ceil


import leros.InstrMemIO
import io.ApbTargetPort

import Helper.WordToByte
import Helper.UIntRangeCheck
import Helper.BytesToWord





class InstructionMemory(noBytes: Int) extends Module {
    require(noBytes % 4 == 0, "Number of bytes must be a multiple of 4")

    val addrWidth = log2Ceil(noBytes)

    val instrPort = IO(new InstrMemIO(addrWidth))
    val apbPort = IO(new ApbTargetPort(32, 32))

    val mem = MemoryFactory.create(noBytes)

    val readyReg = RegInit(0.B)
    val errorReg = RegInit(0.B)

    readyReg := 0.B
    errorReg := 0.B

    apbPort.pready  := readyReg
    apbPort.pslverr := errorReg
    apbPort.prdata  := DontCare

    when(apbPort.psel && apbPort.penable) { // transaction for us

        readyReg := 1.B

        instrPort.instr := 0.U

        val localAddr = apbPort.paddr(addrWidth - 1, 2)

        when (localAddr.inRange(0.U, noBytes.U) && apbPort.pwrite) {
            mem.write(
                localAddr, 
                apbPort.pwdata, 
                apbPort.pstrb
            )
        } otherwise {
            errorReg := 1.B
        }

    } otherwise {
        val word = mem.read(instrPort.addr(addrWidth - 1, 1))
        val upr = word(31, 16)
        val lwr = word(15,  0)
        instrPort.instr := Mux(RegNext(instrPort.addr(0)), upr, lwr)
    }
}
