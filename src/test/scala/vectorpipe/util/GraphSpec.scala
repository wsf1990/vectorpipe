package vectorpipe.util

import org.scalatest._
import spire.std.any._

// --- //

class GraphSpec extends FunSpec with Matchers {
  describe("Graph Construction") {
    it("Empty graph") {
      val g = Graph.fromEdges(Seq())

      g.size shouldBe 0
    }

    it("Singleton Graph") {
      val g = Graph.fromEdges(Seq((0, "hi", Seq())))

      g.size shouldBe 1
      g.get(0) shouldBe Some("hi")
    }

    it("Connected Graph") {
      val g = Graph.fromEdges(Seq(
        (1, 'a', Seq(2,4)),
        (2, 'b', Seq(3)),
        (3, 'c', Seq(6, 7)),
        (4, 'd', Seq(5)),
        (5, 'e', Seq(7)),
        (6, 'f', Seq()),
        (7, 'g', Seq())
      ))

      g.size shouldBe 7
      g.get(7) shouldBe Some('g')
    }

    it("Disconnected Graph") {
      val g = Graph.fromEdges(Seq(
        (1, 'a', Seq(2,4)),
        (2, 'b', Seq(3)),
        (3, 'c', Seq(6, 7)),
        (4, 'd', Seq(5)),
        (5, 'e', Seq(7)),
        (6, 'f', Seq()),
        (7, 'g', Seq()),
        (8, 'h', Seq(9, 10)),
        (9, 'i', Seq(10)),
        (10, 'j', Seq())
      ))

      g.size shouldBe(10)
      g.get(1) shouldBe Some('a')
      g.get(7) shouldBe Some('g')
      g.get(10) shouldBe Some('j')
    }
  }
}
