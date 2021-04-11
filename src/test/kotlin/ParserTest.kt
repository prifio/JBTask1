import org.junit.Test
import kotlin.test.assertEquals

class ParserTest {
    @Test
    fun parseNumberTest() {
        val ten = testParseExpression("10")
        val minus = testParseExpression("-12345678")
        assertEquals(ConstExpr(10), ten)
        assertEquals(ConstExpr(-12345678), minus)
    }

    @Test
    fun parseIdentifierTest() {
        val expr = testParseExpression("abc", vars = setOf(IdentifierExpr("abc")))
        val id = parseIdentifier(MyReader("def", 0))
        assertEquals(IdentifierExpr("abc"), expr)
        assertEquals(IdentifierExpr("def"), id)
    }

    @Test
    fun parseIfTest() {
        val expr = testParseExpression("[1]?(2):(3)")
        assertEquals(IfExpr(ConstExpr(1), ConstExpr(2), ConstExpr(3)), expr)
    }

    @Test
    fun parseFunCallTest() {
        val expr = testParseExpression("foo(1,2)", funs = mapOf(IdentifierExpr("foo") to 2))
        assertEquals(
            FunCallExpr(
                IdentifierExpr("foo"),
                listOf(ConstExpr(1), ConstExpr(2))
            ),
            expr
        )
    }

    @Test
    fun evalTest() {
        assertEquals(4, doEval(ConstExpr(4)))
        assertEquals(4, doEval(OpExpr(0, 0, '+', ConstExpr(2), ConstExpr(2))))
        assertEquals(4, doEval(FunCallExpr(IdentifierExpr("foo"), listOf(ConstExpr(2), ConstExpr(2)))))
        assertEquals(2, doEval(IdentifierExpr("var2")))
        assertEquals(2, doEval(IfExpr(ConstExpr(1), ConstExpr(2), ConstExpr(3))))
        assertEquals(3, doEval(IfExpr(ConstExpr(0), ConstExpr(2), ConstExpr(3))))
    }

    @Test
    fun bigTest() {
        assertEquals(4, doParseEval("(2+2)"))
        assertEquals(4, doParseEval("(2+((3*4)/5))"))
        assertEquals(0, doParseEval("[((10+20)>(20+10))]?(1):(0)"))
    }

    @Test
    fun bigBigTest() {
        assertEquals(
            60, trueMain(
                listOf(
                    "f(x)={[(x>1)]?((f((x-1))+f((x-2)))):(x)}",
                    "g(x)={(f(x)+f((x/2)))}",
                    "g(10)"
                )
            )
        )
    }

    private fun doEval(e: IExpression): Int {
        return eval(
            e,
            mapOf(
                IdentifierExpr("var1") to 1,
                IdentifierExpr("var2") to 2,
                IdentifierExpr("var3") to 3
            ),
            mapOf(
                IdentifierExpr("foo") to FunDef(
                    IdentifierExpr("foo"),
                    listOf(IdentifierExpr("a"), IdentifierExpr("b")),
                    OpExpr(2, 0, '+', IdentifierExpr("a"), IdentifierExpr("b"))
                )
            )
        )
    }

    private fun testParseExpression(
        s: String,
        vars: Set<IdentifierExpr> = emptySet(),
        funs: Map<IdentifierExpr, Int> = emptyMap()
    ): IExpression {
        return parseExpression(MyReader(s, 0), vars, funs)
    }

    private fun doParseEval(s: String): Int {
        return doEval(testParseExpression(s, funs = mapOf(IdentifierExpr("foo") to 2)))
    }
}
