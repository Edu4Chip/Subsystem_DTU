package ponte

import chisel3._

import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import scala.util.Random

import ponte._
import apb.ApbTargetBfm
import apb.ApbTargetBfm._

class PonteDecoderTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "PonteDecoder"

  it should "decode a frame" in {
    test(new PonteDecoder).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      val ponte = new PonteDecoderBfm(dut)
      val apb = new ApbTargetBfm(dut.io.apb, dut.clock)

      Random.setSeed(0x34df3)
      val n = 256
      val testVec = Seq.fill(n)(BigInt(32, Random))
      val accesses = Random.shuffle(Seq.range(0, n))

      println(s"Test vector: ${testVec.mkString(", ")}")
      println(s"Accesses: ${accesses.mkString(", ")}")

      fork {
        for (_ <- 0 until (2 * n)) {
          apb.next() match {
            case Read(addr) =>
              println(s"Reading from $addr = ${testVec(addr.toInt)}")
              apb.respondRead(testVec(addr.toInt))
            case Write(addr, data) =>
              println(s"Writing $data to $addr")
              assert(data == testVec(addr.toInt))
              apb.respondWrite()
            case _ =>
              fail("Invalid operation")
          }
        }
      }

      for (addr <- accesses) {
        ponte.write(addr, Seq(testVec(addr).toInt))
      }

      for (addr <- accesses) {
        val read = BigInt(ponte.read(addr, 1).head) & 0xffffffffL
        assert(read == testVec(addr))
      }
    }
  }

}
