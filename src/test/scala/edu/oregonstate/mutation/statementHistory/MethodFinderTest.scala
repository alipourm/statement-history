package edu.oregonstate.mutation.statementHistory

import org.eclipse.jdt.core.dom.MethodDeclaration
import org.scalatest.{FlatSpec, Matchers}

class MethodFinderTest extends FlatSpec with Matchers {


  it should "find the method" in {
    val content = "public class A{\npublic void m(){}}"
    val root = JavaParser.parse(content)
    val method = MethodFinder.findNode(2, root)
    method.getUnderlyingNode() shouldBe a [MethodDeclaration]
    method.getUnderlyingNode().asInstanceOf[MethodDeclaration].getName.getIdentifier should equal ("m")
  }

  it should "find a method spanning multiple lines" in {
    val content = "public class A{\npublic void m(){\nint x=22;\nint y=33;\n\n}}"
    val root = JavaParser.parse(content)
    MethodFinder.findNode(2, root).getUnderlyingNode().asInstanceOf[MethodDeclaration].getName.getIdentifier should equal ("m")
    MethodFinder.findNode(3, root).getUnderlyingNode().asInstanceOf[MethodDeclaration].getName.getIdentifier should equal ("m")
    MethodFinder.findNode(4, root).getUnderlyingNode().asInstanceOf[MethodDeclaration].getName.getIdentifier should equal ("m")
    MethodFinder.findNode(5, root).getUnderlyingNode().asInstanceOf[MethodDeclaration].getName.getIdentifier should equal ("m")
  }

  it should "fail gracefully if a method is not found" in {
    val content = "public class A{\n\npublic void m(){}}"
    var root = JavaParser.parse(content)
    MethodFinder.findNode(1, root) should be (null)
  }

}
