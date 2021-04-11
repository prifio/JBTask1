import java.lang.RuntimeException

fun eval(e: IExpression, vars: Map<IdentifierExpr, Int>, funs: Map<IdentifierExpr, FunDef>): Int {
    if (e is ConstExpr) {
        return e.x
    }
    if (e is IdentifierExpr) {
        return vars[e]!!
    }
    if (e is OpExpr) {
        val le = eval(e.le, vars, funs)
        val r = eval(e.r, vars, funs)
        try {
            return OPS[e.op]!!(le, r)
        } catch (exception: RuntimeException) {
            throw EvalException(e.pos, e.line, exception)
        }
    }
    if (e is FunCallExpr) {
        val f = funs[e.name]!!
        val newVarsMap = mutableMapOf<IdentifierExpr, Int>()
        for (i in e.lst.indices) {
            newVarsMap[f.lst[i]] = eval(e.lst[i], vars, funs)
        }
        return eval(f.body, newVarsMap, funs)
    }
    if (e is IfExpr) {
        val cond = eval(e.cond, vars, funs)
        return eval(if (cond != 0) e.onTrue else e.onFalse, vars, funs)
    }
    throw RuntimeException("Unreachable part of code")
}
