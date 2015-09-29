package edu.oregonstate.mutation.statementHistory

class StatementChangeDetectorTest extends GitTest {

  private def ci(a: String, b:String): CommitInfo = new CommitInfo(a, b)
  private def nd(repo: String): StatementChangeDetector = nd(repo, "HEAD")
  private def nd(repo: String, sha: String): StatementChangeDetector = new StatementChangeDetector(repo, sha)

  it should "find a statement change in two consecutive commits" in {
    val first = add("A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("A.java", "public class A{\npublic void m(){\nint x=2;\n}\n}")
    val expected = Seq(ci(first.getName, "ADD"), ci(second.getName, "UPDATE"))

    val detector = nd(repo.getAbsolutePath)
    val commits = detector.findCommits("A.java", 3)
    commits should have size 2
    commits should equal(expected)
  }

  it should "find a statement in three consecutive commits" in {
    val first = add("A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("A.java", "public class A{\npublic void m(){\nint x=2;\n}\n}")
    val third = add("A.java", "public class A{\npublic void m(){\nint x=10;\n}\n}")
    val expected = Seq(ci(first.getName, "ADD"), ci(second.getName,"UPDATE"), ci(third.getName,"UPDATE"))
    val detector = nd(repo.getAbsolutePath)
    val commits = detector.findCommits("A.java", 3)
    commits should have size 3
    commits should equal(expected)
  }

  it should "find a statement if the line number changes" in {
    val first = add("A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("A.java", "public class A{\npublic void m(){\n//some comment\nint x=2;\n}\n}")
    val third = add("A.java", "public class A{\npublic void m(){\n//some comment\nint x=10;\n}\n}")
    val expected = Seq(ci(first.getName,"ADD"), ci(second.getName,"UPDATE"), ci(third.getName, "UPDATE"))

    val detector = nd(repo getAbsolutePath)
    val commits = detector.findCommits("A.java", 4)
    commits should have size 3
    commits should equal(expected)
  }

  it should "not bother if statement was only moved" in {
    val first = add("A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("A.java", "public class A{\npublic void m(){\nSystem.out.println(\"\");\nint x=3;\n}\n}")
    val expected = Seq(ci(first.getName,"ADD"))

    val commits = nd(repo getAbsolutePath).findCommits("A.java", 4)
    commits should have size expected.size
    commits should equal(expected)
  }

  ignore should "know if something was deleted" in {
    val first = add("A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("A.java", "public class A{\npublic void m(){}\n}")
    add("A.java", "public class A{\npublic void m(){}\npublic void n(){}}")
    val expected = Seq(ci(first.getName,"ADD"), ci(second.getName,"DELETE"))

    val commits = nd(repo getAbsolutePath).findCommits("A.java", 3)
    commits should have size 2
    commits should equal(expected)
  }

  it should "find the commit with a partial path" in {
    val first = add("src/A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("src/A.java", "public class A{\npublic void m(){\nint x=20;}\n}")
    val expected = Seq(ci(first.getName,"ADD"), ci(second.getName, "UPDATE"))

    val commits = nd(repo getAbsolutePath).findCommits("A.java", 3)
    commits should have size 2
    commits should equal(expected)
  }

  it should "find four changes" in {
    val first = add("src/A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("src/A.java", "public class A{\npublic void m(){\nint x=15;\n}\n}")
    val third = add("src/A.java", "public class A{\npublic void m(){\nint x=22;\n}\n}")
    val fourth = add("src/A.java", "public class A{\npublic void m(){\nint y=22;\n}\n}")
    val expected = Seq(ci(first.getName,"ADD"), ci(second.getName,"UPDATE"), ci(third.getName,"UPDATE"), ci(fourth.getName,"UPDATE"))

    val commits = nd(repo getAbsolutePath).findCommits("A.java", 3)
    commits should have size 4
    commits should equal(expected)
  }

  it should "start at given commit" in {
    val first = add("src/A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("src/A.java", "public class A{\npublic void m(){\nint x=15;\n}\n}")
    val third = add("src/A.java", "public class A{\npublic void m(){\nint x=22;\n}\n}")
    val expected = Seq(ci(first.getName,"ADD"), ci(second.getName,"UPDATE"))

    val commits = nd(repo.getAbsolutePath, second.getName).findCommits("A.java", 3)
    commits should have size 2
    commits should equal(expected)
  }

  it should "use given commit to find statement at line number" in {
    val first = add("src/A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("src/A.java", "public class A{\npublic void m(){\n\nint x=15;\n}\n}")
    val third = add("src/A.java", "public class A{\npublic void m(){\nint x=22;\n}\n}")
    val expected = Seq(ci(first.getName,"ADD"), ci(second.getName,"UPDATE"))

    val commits = nd(repo.getAbsolutePath, second.getName).findCommits("A.java", 4)
    commits should have size 2
    commits should equal(expected)
  }

  it should "detect a change in a moved statement" in {
    val first = add("src/A.java", "public class A{\npublic void m(){\nString x=\"\";\n}\n}")
    val second = add("src/A.java", "public class A{\npublic void m(){\nint y=293;\nString x=\"abcd\";\n}\n}")
    val expected = Seq(ci(first.getName,"ADD"), ci(second.getName,"UPDATE"))

    val commits = nd(repo.getAbsolutePath).findCommits("A.java", 4)
    commits should have size 2
    commits should equal(expected)
  }

  it should "track a moved statement" in {
    val first = add("A.java", "public class A{\npublic void m(){\nint x=3;\n}\n}")
    val second = add("A.java", "public class A{\npublic void m(){\nSystem.out.println(\"\");\nint x=3;\n}\n}")
    val third = add("A.java", "public class A{\npublic void m(){\nSystem.out.println(\"\");\nint x=4;\n}\n}")
    val expected = Seq(ci(first.getName, "ADD"), ci(third.getName, "UPDATE"))

    val commits = nd(repo.getAbsolutePath).findCommits("A.java", 4)
    commits should have size 2
    commits should equal (expected)
  }
}