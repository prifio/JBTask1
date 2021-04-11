import java.lang.RuntimeException

class MyReader(private val s: String, val line: Int) {
    var pos = 0
        private set

    fun lookup(): Char {
        if (pos == s.length) {
            return '$'
        }
        return s[pos]
    }

    fun next(): Char {
        val a = lookup()
        ++pos
        return a
    }
}

class ParseException : RuntimeException {
    constructor() : super()
    constructor(m: String) : super(m)
}

class EvalException(val pos: Int, val line: Int, e: Exception) : RuntimeException(e)

data class NamesEnv(val vars: Set<IdentifierExpr>, val funs: Set<IdentifierExpr>)

interface IExpression

data class ConstExpr(val x: Int) : IExpression
data class IfExpr(val cond: IExpression, val onTrue: IExpression, val onFalse: IExpression) : IExpression
data class OpExpr(val pos: Int, val line: Int, val op: Char, val le: IExpression, val r: IExpression) : IExpression
data class IdentifierExpr(val s: String) : IExpression
data class FunCallExpr(val name: IdentifierExpr, val lst: List<IExpression>) : IExpression
data class FunDef(val name: IdentifierExpr, val lst: List<IdentifierExpr>, val body: IExpression)

val OPS = mapOf<Char, (Int, Int) -> Int>(
    '+' to { a, b -> a + b },
    '-' to { a, b -> a - b },
    '*' to { a, b -> a * b },
    '/' to { a, b -> a / b },
    '%' to { a, b -> a % b },
    '>' to { a, b -> if (a > b) 1 else 0 },
    '<' to { a, b -> if (a < b) 1 else 0 },
    '<' to { a, b -> if (a == b) 1 else 0 })

fun throwParseException(r: MyReader, m: String): Nothing {
    throw ParseException("$m at ${r.pos + 1}:${r.line + 1}")
}

fun assertChar(r: MyReader, c: Char) {
    if (r.next() != c) {
        throwParseException(r, "Expected char $c")
    }
}

fun parseIdentifier(r: MyReader): IdentifierExpr {
    val sb = StringBuilder()
    while (r.lookup().isLetter()) {
        sb.append(r.next())
    }
    if (sb.isEmpty()) {
        throwParseException(r, "Expected Identifier")
    }
    return IdentifierExpr(sb.toString())
}

fun parseExpression(r: MyReader, vars: Set<IdentifierExpr>, funs: Map<IdentifierExpr, Int>): IExpression {
    val c = r.lookup()
    if (c == '-' || c.isDigit()) {
        val k: Int
        if (c == '-') {
            k = -1
            r.next()
            if (!r.lookup().isDigit()) {
                throwParseException(r, "Expected number")
            }
        } else {
            k = 1
        }
        var ans = 0
        while (r.lookup().isDigit()) {
            ans = ans * 10 + (r.next() - '0')
        }
        return ConstExpr(ans * k)
    }
    if (c == '[') {
        r.next()
        val e1 = parseExpression(r, vars, funs)
        assertChar(r, ']')
        assertChar(r, '?')
        assertChar(r, '(')
        val e2 = parseExpression(r, vars, funs)
        assertChar(r, ')')
        assertChar(r, ':')
        assertChar(r, '(')
        val e3 = parseExpression(r, vars, funs)
        assertChar(r, ')')
        return IfExpr(e1, e2, e3)
    }
    if (c == '(') {
        r.next()
        val e1 = parseExpression(r, vars, funs)
        val opPos = r.pos
        val opLine = r.line
        val op = r.next()
        if (!OPS.contains(op)) {
            throwParseException(r, "Unexpected operator")
        }
        val e2 = parseExpression(r, vars, funs)
        assertChar(r, ')')
        return OpExpr(opPos, opLine, op, e1, e2)
    }
    if (c.isLetter()) {
        val id = parseIdentifier(r)
        if (r.lookup() == '(') {
            val cntArgs = funs[id] ?: throwParseException(r, "Unknown name of function: $id")
            r.next()
            val lst = mutableListOf<IExpression>()
            lst += parseExpression(r, vars, funs)
            for (i in 1 until cntArgs) {
                r.next()
                lst += parseExpression(r, vars, funs)
            }
            assertChar(r, ')')
            return FunCallExpr(id, lst)
        }
        if (!vars.contains(id)) {
            throwParseException(r, "Unknown name of identifier")
        }
        return id
    }
    if (c == '$') {
        throw ParseException("Unexpected end of line at line ${r.line + 1}")
    }
    throwParseException(r, "Unexpected character")
}

fun parseFun(r: MyReader, funs: MutableMap<IdentifierExpr, Int>): FunDef {
    val name = parseIdentifier(r)
    if (funs.contains(name)) {
        throwParseException(r, "Double definition of function at line")
    }
    assertChar(r, '(')
    val argsSet = mutableSetOf<IdentifierExpr>()
    val lst = mutableListOf<IdentifierExpr>()
    lst += parseIdentifier(r)
    argsSet += lst[0]
    while (r.lookup() == ',') {
        r.next()
        lst += parseIdentifier(r)
        if (!argsSet.add(lst.last())) {
            throwParseException(r, "Double arg declaration")
        }
    }
    assertChar(r, ')')
    assertChar(r, '=')
    assertChar(r, '{')
    funs[name] = lst.size
    val body = parseExpression(r, argsSet, funs)
    assertChar(r, '}')
    assertChar(r, '$')
    return FunDef(name, lst, body)
}

fun trueMain(lines: List<String>): Int {
    val funs = mutableMapOf<IdentifierExpr, FunDef>()
    val funsArgs = mutableMapOf<IdentifierExpr, Int>()

    for (i in 0 until (lines.size - 1)) {
        val f = parseFun(MyReader(lines[i], i), funsArgs)
        funs[f.name] = f
    }
    return eval(
        parseExpression(MyReader(lines.last(), lines.size - 1), emptySet(), funsArgs),
        emptyMap(), funs
    )
}

fun main() {
    val lines = mutableListOf<String>()
    while (true) {
        val line = readLine() ?: break
        if (line == "exit") {
            break
        }
        lines += line
    }
    try {
        print(trueMain(lines))
    } catch (e: ParseException) {
        println(e)
    } catch (e: EvalException) {
        println("Runtime error")
        println(e)
        println("At ${e.pos + 1}:${e.line + 1}")
    }
}
