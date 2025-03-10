import chisel3._

import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import dtu.InstructionMemory

class InstructionMemoryTest extends AnyFlatSpec with ChiselScalatestTester {
    behavior of "InstructionMemory"

    def expectInstr(dut: InstructionMemory, addr: BigInt, instr: BigInt): Unit = {
        dut.instrPort.addr.poke((addr / 2).U)
        dut.clock.step()
        dut.instrPort.instr.expect(instr.U)
    }

    it should "accept Apb write transactions" in {
        test(new InstructionMemory(32 * 4)) { dut =>

            val bfm = new ApbBfm(dut.clock, dut.apbPort)

            bfm.write(0x4, 0xdeadbeefL).unwrap()

            expectInstr(dut, addr = 0x4, instr = 0xbeef)
            expectInstr(dut, addr = 0x6, instr = 0xdead)

        }
    }

    it should "raise an error for Apb read transactions" in {
        test(new InstructionMemory(32 * 4)) { dut =>

            val bfm = new ApbBfm(dut.clock, dut.apbPort)

            bfm.read(0x4).expectError()

        }
    }

    it should "correctly store and retrieve data" in {
        test(new InstructionMemory(32 * 4)) { dut =>

            val bfm = new ApbBfm(dut.clock, dut.apbPort)

            for (i <- 0 until 128 by 4) {
                val word = Seq.tabulate(4)(j => (0xFFL - (i + j)) << (j * 8)).reduce(_ | _)
                bfm.write(i, word).unwrap()
            }

            for (i <- 0 until 128 by 2) {
                val halfword = Seq.tabulate(2)(j => (0xFFL - (i + j)) << (j * 8)).reduce(_ | _)
                expectInstr(dut, i, halfword)
            }

        }
    }

    it should "correctly write masked apb transactions" in {
        test(new InstructionMemory(32 * 4)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

            val bfm = new ApbBfm(dut.clock, dut.apbPort)

            bfm.write(0x00, 0xdeadbeefL, 0x1).unwrap()
            bfm.write(0x04, 0xdeadbeefL, 0x2).unwrap()
            bfm.write(0x08, 0xdeadbeefL, 0x4).unwrap()
            bfm.write(0x0C, 0xdeadbeefL, 0x8).unwrap()
            bfm.write(0x10, 0xdeadbeefL, 0x3).unwrap()
            bfm.write(0x14, 0xdeadbeefL, 0xC).unwrap()

            expectInstr(dut, addr = 0x0, instr = 0x00ef)
            expectInstr(dut, addr = 0x2, instr = 0x0000)

            expectInstr(dut, addr = 0x4, instr = 0xbe00)
            expectInstr(dut, addr = 0x6, instr = 0x0000)

            expectInstr(dut, addr = 0x8, instr = 0x0000)
            expectInstr(dut, addr = 0xa, instr = 0x00ad)

            expectInstr(dut, addr = 0xc, instr = 0x0000)
            expectInstr(dut, addr = 0xe, instr = 0xde00)

        }
    }

}
