package edu.oregonstate.mutation.statementHistory

import com.brindescu.gumtree.facade.CASTDiff

class StatementChangeDetectorTest extends GitTest with NodeChangeDetectorTest {

  it should "find a statement change in two consecutive commits" in {
    val first = add("A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("A.java", "public class A{\npublic void m(){\nint x=2;\n}\n}")
    val expected = Seq(ci(first.getName, "ADD"), ci(second.getName, "UPDATE"))

    val detector = nd(git)
    val commits = detector.findCommits("A.java", 3)
    commits should have size 2
    commits should equal(expected)
  }

  it should "find a statement in three consecutive commits" in {
    val first = add("A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("A.java", "public class A{\npublic void m(){\nint x=2;\n}\n}")
    val third = add("A.java", "public class A{\npublic void m(){\nint x=10;\n}\n}")
    val expected = Seq(ci(first.getName, "ADD"), ci(second.getName,"UPDATE"), ci(third.getName,"UPDATE"))
    val detector = nd(git)
    val commits = detector.findCommits("A.java", 3)
    commits should have size 3
    commits should equal(expected)
  }

  it should "find a statement if the line number changes" in {
    val first = add("A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("A.java", "public class A{\npublic void m(){\n//some comment\nint x=2;\n}\n}")
    val third = add("A.java", "public class A{\npublic void m(){\n//some comment\nint x=10;\n}\n}")
    val expected = Seq(ci(first.getName,"ADD"), ci(second.getName,"UPDATE"), ci(third.getName, "UPDATE"))

    val detector = nd(git)
    val commits = detector.findCommits("A.java", 4)
    commits should have size 3
    commits should equal(expected)
  }

  it should "not bother if statement was only moved" in {
    val first = add("A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("A.java", "public class A{\npublic void m(){\nSystem.out.println(\"\");\nint x=3;\n}\n}")
    val expected = Seq(ci(first.getName,"ADD"))

    val commits = nd(git).findCommits("A.java", 4)
    commits should have size expected.size
    commits should equal(expected)
  }

  it should "stop when a statement was added" in {
    val first = add("A.java", "public class A{\npublic void m(){}\n}")
    val second = add("A.java", "public class A{\npublic void m(){\nint x=3;}}")
    val expected = Seq(ci(second.getName,"ADD"))

    val commits = nd(git).findCommits("A.java", 3)
    commits should have size 1
    commits should equal(expected)
  }

  it should "find the commit with a partial path" in {
    val first = add("src/A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("src/A.java", "public class A{\npublic void m(){\nint x=20;}\n}")
    val expected = Seq(ci(first.getName,"ADD"), ci(second.getName, "UPDATE"))

    val commits = nd(git).findCommits("A.java", 3)
    commits should have size 2
    commits should equal(expected)
  }

  it should "find four changes" in {
    val first = add("src/A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("src/A.java", "public class A{\npublic void m(){\nint x=15;\n}\n}")
    val third = add("src/A.java", "public class A{\npublic void m(){\nint x=22;\n}\n}")
    val fourth = add("src/A.java", "public class A{\npublic void m(){\nint y=22;\n}\n}")
    val expected = Seq(ci(first.getName,"ADD"), ci(second.getName,"UPDATE"), ci(third.getName,"UPDATE"), ci(fourth.getName,"UPDATE"))

    val commits = nd(git).findCommits("A.java", 3)
    commits should have size 4
    commits should equal(expected)
  }

  it should "use given commit to find statement at line number" in {
    val first = add("src/A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("src/A.java", "public class A{\npublic void m(){\n\nint x=15;\n}\n}")
    val third = add("src/A.java", "public class A{\npublic void m(){\nint x=22;\n}\n}")
    val expected = Seq(ci(first.getName,"ADD"), ci(second.getName,"UPDATE"), ci(third.getName,"UPDATE"))

    val commits = nd(git).findCommits("A.java", 4, second.getName)
    commits should have size 3
    commits should equal(expected)
  }

  it should "detect a change in a moved statement" in {
    val first = add("src/A.java", "public class A{\npublic void m(){\nint x=33;\n}\n}")
    val second = add("src/A.java", "public class A{\npublic void m(){\nint y=293;\nint x=34;\n}\n}")
    val expected = Seq(ci(first.getName,"ADD"), ci(second.getName,"UPDATE"))

    val commits = nd(git).findCommits("A.java", 4)
    commits should have size 2
    commits should equal(expected)
  }

  it should "track a moved statement" in {
    val first = add("A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("A.java", "public class A{\npublic void m(){\nSystem.out.println(\"\");\nint x=3;\n}\n}")
    val third = add("A.java", "public class A{\npublic void m(){\nSystem.out.println(\"\");\nint x=4;\n}\n}")
    val expected = Seq(ci(first.getName, "ADD"), ci(third.getName, "UPDATE"))

    val commits = nd(git).findCommits("A.java", 4)
    commits should have size 2
    commits should equal (expected)
  }

  it should "track a statement inside a block" in {
    val first = add("A.java", "public class A{\npublic void m(){\nif(true){int x=3;}\n}\n}")
    val second = add("A.java", "public class A{\npublic void m(){\nif(true){\nint x=343;}\n}\n}")
    val expected = Seq(ci(first.getName, "ADD"), ci(second.getName, "UPDATE"))

    val commits = nd(git).findCommits("A.java",4)
    commits should have size expected.size
    commits should equal (expected)
  }

  it should "find before and after" in {
    val first = add("A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("A.java", "public class A{\npublic void m(){\nint x=32;\n}\n}")
    val third = add("A.java", "public class A{\npublic void m(){\nint x=4;\n}\n}")
    val expected=Seq(ci(first.getName, "ADD"), ci(second.getName, "UPDATE"), ci(third.getName, "UPDATE"))

    val commits = nd(git).findCommits("A.java", 3, second.getName)
    commits should have size expected.size
    commits should equal (expected)
  }

  it should "find only commits before" in {
    val first = add("A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("A.java", "public class A{\npublic void m(){\nint x=32;\n}\n}")
    val third = add("A.java", "public class A{\npublic void m(){\nint x=4;\n}\n}")
    val expected=Seq(ci(first.getName, "ADD"), ci(second.getName, "UPDATE"))

    val commits = nd(git).findCommits("A.java", 3, second.getName, Order.REVERSE)
    commits should have size expected.size
    commits should equal (expected)
  }

  it should "find only commits after" in {
    val first = add("A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("A.java", "public class A{\npublic void m(){\nint x=32;\n}\n}")
    val third = add("A.java", "public class A{\npublic void m(){\nint x=4;\n}\n}")
    val expected=Seq(ci(third.getName, "UPDATE"))

    val commits = nd(git).findCommits("A.java", 3, second.getName, Order.FORWARD)
    commits should have size expected.size
    commits should equal (expected)
  }

  it should "find commits after reference, where the file wasn't touched" in {
    val first = add("A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("B.java", "public class B{}")
    val third = add("A.java", "public class A{\npublic void m(){\nint x=32;\n}\n}")
    val fourth = add("A.java", "public class A{\npublic void m(){\nint x=4;\n}\n}")
    val expected = Seq(ci(third.getName, "UPDATE"), ci(fourth.getName, "UPDATE"))

    val commits = nd(git).findCommits("A.java", 3, second.getName, Order.FORWARD)
    commits should have size expected.size
    commits should equal (expected)
  }

  it should "find the commits before reference, when the file wasn't touched" in {
    val first = add("A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("B.java", "public class B{}")
    val third = add("A.java", "public class A{\npublic void m(){\nint x=32;\n}\n}")
    val fourth = add("A.java", "public class A{\npublic void m(){\nint x=4;\n}\n}")
    val expected = Seq(ci(first.getName, "ADD"))

    val commits = nd(git).findCommits("A.java", 3, second.getName, Order.REVERSE)
    commits should have size (expected.size)
    commits should equal (expected)
  }

  it should "not throw an NPE when the file wasn't modified in the reference commit" in {
    val first = add("A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("B.java", "public class B{}")
    git.rm().addFilepattern("A.java").call()
    val third = git.commit().setMessage("smth").call()

    val commits = nd(git).findCommits("A.java", 3, third.getName, Order.BOTH)
  }

  ignore should "track a change over a rename" in {
    val first = add("A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("B.java", "public class A{\npublic void m(){\nint x=33;\n}\n}")
    val expected = Seq(ci(first.getName, "ADD"), ci(second.getName, "UPDATE"))

    val commits = nd(git).findCommits("A.java", 3, second.getName, Order.BOTH)
    commits should have size (expected.size)
    commits should equal (expected)
  }

  it should "not track a curly brace" in {
    val first = add("A.java", "public class A{\npublic void m()\n{\nint x=3;\n}\n}")
    val second = add("A.java", "public class A{\npublic void m(){\nint x=33;\n}\n}")

    val commits = nd(git).findCommits("A.java", 3, first.getName, Order.FORWARD)
    commits should have size 0
  }

  it should "track across line moves" in {
    val first = add("A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("A.java", "public class A{\npublic void m(){\nint y=32;\nint x=3;\n}\n}")
    val third = add("A.java", "public class A{\npublic void m(){\nint y=32;\nint x=7;\n}\n}")
    val expected = Seq(ci(first.getName, "ADD"), ci(third.getName, "UPDATE"))

    val commits = nd(git).findCommits("A.java", 3, first.getName, Order.BOTH)
    commits should have size 2
    commits should equal (expected)
  }

  it should "track a change across C files" in {
    val first = add("test.c", "void main(){\nint x=3;\n}")
    val second = add("test.c", "void main(){\nint y=32;\nint x=3;\n}")
    val third = add("test.c", "void main(){\nint y=32;\nint x=7;\n}")
    val expected = Seq(ci(first.getName, "ADD"), ci(third.getName, "UPDATE"))
    val finder = StatementFinder
    finder.parser = CParser
    val nd = new NodeChangeDetector(git, finder, CASTDiff)
    val commits = nd.findCommits("test.c", 2, first.getName, Order.BOTH)
    commits should have size 2
    commits should equal (expected)
  }
}