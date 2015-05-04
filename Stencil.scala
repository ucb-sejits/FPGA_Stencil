package Stencil
import Chisel._
import param._
import ChiselFloat._



//Define a stallable, parameterizable shift register.
object EnableShiftRegister
{
  def apply[T <: Data](in: T, n: Int, en: Bool = Bool(true)): T =
  {
    if (n == 1) {
      RegEnable(in, en)
    } else if (n != 0) {
      RegEnable(apply(in, n-1, en), en)
    } else {
      in
    }
  }
}



//Define a connection between the datapath and window arithmatic.
class GridConnection extends Bundle {
  val valid = Bool()
  //TODO implement vec of vecs.
  val row1 = Vec.fill(window){ UInt() }
  val row2 = Vec.fill(window){ UInt() }
  val row3 = Vec.fill(window){ UInt() }
}



class WindowArithmatic extends Module {
  val io = new Bundle {
    val gc = new GridConnection().asInput
    val stream_out = UInt(OUTPUT, 32)
  }

  //TODO look at x and y counters to handle invalid cases
  when (io.gc.valid) {
    io.stream_out := (io.gc.row1(0) + io.gc.row1(1) + io.gc.row1(2) + io.gc.row2(0) + io.gc.row2(1) + io.gc.row2(2) + io.gc.row3(0) + io.gc.row3(1) + io.gc.row3(2)) 
  }
}



class Stencil extends Module {
  val DataWidth = 32
  
  val io = new Bundle { 
    val stream_in = UInt(INPUT, DataWidth)
    val valid = Bool(INPUT)
    val stream_out = UInt(OUTPUT, DataWidth)
  }


  //Initialize discrete registers and wires of delay line.
  val i1 = UInt()
  val i2 = Reg(UInt())
  val i3 = Reg(UInt())
  val i4 = UInt()
  val i5 = Reg(UInt())
  val i6 = Reg(UInt())
  val i7 = UInt()
  val i8 = Reg(UInt())
  val i9 = Reg(UInt())


  //Update the indexes that are registers.
  when (io.valid) {
    i2 := i1
    i3 := i2
    i5 := i4
    i6 := i5
    i8 := i7
    i9 := i8
  }

  //Update the indexes that are wires.
  i1 := io.stream_in
  i4 := EnableShiftRegister(i3, gridx - window - 1, io.valid)
  i7 := EnableShiftRegister(i6, gridx - window - 1, io.valid)



  //TODO Figure out why using connection results in null pointer connection

  /*
  //Initialize a connection between the delay line with the window arithmatic.
  val gc = new GridConnection().asOutput
  gc.valid := io.valid
  gc.row1(0) := i9
  gc.row1(1) := i8
  gc.row1(2) := i7
  gc.row2(0) := i6
  gc.row2(1) := i5
  gc.row2(2) := i4
  gc.row3(0) := i3
  gc.row3(1) := i2
  gc.row3(2) := i1

  //Initialize window arithmatic module
  val window = Module(new WindowArithmatic())
  window.io.gc := gc
  io.stream_out := window.io.stream_out
  */


  // Convolution with weights

  //Constants (eventually pull from memory instead of literals)
  val w1 = UInt("h40490fd0")
  val w2 = UInt("h40c90fe4")
  val w3 = UInt("h4116cbe6")
  val w4 = UInt("h41490ff9")
  val w5 = UInt("h417b53f8")
  val w6 = UInt("h4196cbfb")
  val w7 = UInt("h41afedc6")
  val w8 = UInt("h41c90fc5")
  val w9 = UInt("h41e231c4")
  //Sum extra outputs with zero to avoid timing problems for now.
  val zero = UInt("h00000000")

  //First layer of output registers
  val m11 = Reg(UInt())
  val m12 = Reg(UInt())
  val m13 = Reg(UInt())
  val m14 = Reg(UInt())
  val m15 = Reg(UInt())
  val m16 = Reg(UInt())
  val m17 = Reg(UInt())
  val m18 = Reg(UInt())
  val m19 = Reg(UInt())

  //Second layer of output registers
  val a21 = Reg(UInt())
  val a22 = Reg(UInt())
  val a23 = Reg(UInt())
  val a24 = Reg(UInt())
  val a25 = Reg(UInt())

  //Third layer of output registers
  val a31 = Reg(UInt())
  val a32 = Reg(UInt())
  val a33 = Reg(UInt())

  //Fourth layer of output registers
  val a41 = Reg(UInt())
  val a42 = Reg(UInt())

  //First layer of multiplications

  /* Let x be the index of the FPMult32 module to be created
  val mul1x = Module(new FPMult32())
  mul1x.io.a := ix
  mul1x.io.b := wx
  m1x := mul1x.io.res
  */

  val mul11 = Module(new FPMult32())
  mul11.io.a := i1
  mul11.io.b := w1
  m11 := mul11.io.res

  val mul12 = Module(new FPMult32())
  mul12.io.a := i2
  mul12.io.b := w2
  m12 := mul12.io.res

  val mul13 = Module(new FPMult32())
  mul13.io.a := i3
  mul13.io.b := w3
  m13 := mul13.io.res

  val mul14 = Module(new FPMult32())
  mul14.io.a := i4
  mul14.io.b := w4
  m14 := mul14.io.res

  val mul15 = Module(new FPMult32())
  mul15.io.a := i5
  mul15.io.b := w5
  m15 := mul15.io.res

  val mul16 = Module(new FPMult32())
  mul16.io.a := i6
  mul16.io.b := w6
  m16 := mul16.io.res

  val mul17 = Module(new FPMult32())
  mul17.io.a := i7
  mul17.io.b := w7
  m17 := mul17.io.res

  val mul18 = Module(new FPMult32())
  mul18.io.a := i8
  mul18.io.b := w8
  m18 := mul18.io.res

  val mul19 = Module(new FPMult32())
  mul19.io.a := i9
  mul19.io.b := w9
  m19 := mul19.io.res

  //Second layer of additions

  /* Let x be the index of the FPAdd32 module to be created
  Let y = x*2 - 1
  Let z = x*2
  val add2x = Module(new FPAdd32())
  add2x.io.a := m1y
  add2x.io.b := m1z
  a2x := add2x.io.res
  */

  val add21 = Module(new FPAdd32())
  add21.io.a := m11
  add21.io.b := m12
  a21 := add21.io.res

  val add22 = Module(new FPAdd32())
  add22.io.a := m13
  add22.io.b := m14
  a22 := add22.io.res

  val add23 = Module(new FPAdd32())
  add23.io.a := m15
  add23.io.b := m16
  a23 := add23.io.res

  val add24 = Module(new FPAdd32())
  add24.io.a := m17
  add24.io.b := m18
  a24 := add24.io.res

  //Unnecessary add with zero to mantain proper timing
  val add25 = Module(new FPAdd32())
  add25.io.a := m19
  add25.io.b := zero
  a25 := add25.io.res

  //Third layer of additions

  /* Let x be the index of the FPAdd32 module to be created
  Let y = x*2 - 1
  Let z = x*2
  val add3x = Module(new FPAdd32())
  add3x.io.a := a2y
  add3x.io.b := a2z
  a3x := add3x.io.res
  */

  val add31 = Module(new FPAdd32())
  add31.io.a := a21
  add31.io.b := a22
  a31 := add31.io.res

  val add32 = Module(new FPAdd32())
  add32.io.a := a23
  add32.io.b := a24
  a32 := add32.io.res

  //Unnecessary add with zero to mantain proper timing
  val add33 = Module(new FPAdd32())
  add33.io.a := a25
  add33.io.b := zero
  a33 := add33.io.res

  //Fourth layer of additions

  /* Let x be the index of the FPAdd32 module to be created
  Let y = x*2 - 1
  Let z = x*2
  val add4x = Module(new FPAdd32())
  add4x.io.a := a3y
  add4x.io.b := a3z
  a4x := add4x.io.res
  */

  val add41 = Module(new FPAdd32())
  add41.io.a := a31
  add41.io.b := a32
  a41 := add41.io.res

  //Unnecessary add with zero to mantain proper timing
  val add42 = Module(new FPAdd32())
  add42.io.a := a33
  add42.io.b := zero
  a42 := add42.io.res

  //Fifth layer of additions

  /* Let x be the index of the FPAdd32 module to be created
  Let y = x*2 - 1
  Let z = x*2
  val add5x = Module(new FPAdd32())
  add5x.io.a := a4y
  add5x.io.b := a4z
  io.stream_out := add5x.io.res
  */

  val add51 = Module(new FPAdd32())
  add51.io.a := a41
  add51.io.b := a42
  io.stream_out := add51.io.res



}


class StencilTests(c: Stencil) extends Tester(c) {
  //val stream_in = 42
  //poke(c.io.stream_in, stream_in)
  //step(1)
  //expect(c.io.stream_out, 42)
}

object Stencil {
  def main(args: Array[String]): Unit = {
    val tutArgs = args.slice(1, args.length)
    chiselMainTest(tutArgs, () => Module(new Stencil())) {
      c => new StencilTests(c) }
  }
}