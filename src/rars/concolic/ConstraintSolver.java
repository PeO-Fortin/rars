package rars.concolic;

import com.microsoft.z3.*;

import java.util.*;

class ConstraintSolver {
    Context ctx;
    BitVecSort bvs;
    Map<String, Expr<BitVecSort>> varExprs;
    Expr<BitVecSort> zero;
    public Map<String, Integer> solve(Collection<SymbolicValue> constraints) {
        ctx = new Context();
        Collection<String> vars = collectVariables(constraints);
        bvs = ctx.mkBitVecSort(32);
        varExprs = new HashMap<>();
        for (String var : vars) {
            varExprs.put(var, ctx.mkConst(var, bvs));
        }

        zero = ctx.mkNumeral(0, bvs);
        Solver solver = ctx.mkSolver();
        for (SymbolicValue constraint : constraints) {
            solver.add(translateBool(constraint));
        }
        if (solver.check() != Status.SATISFIABLE) {
            return null; // unsat
        }
        Model model = solver.getModel();
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, Expr<BitVecSort>> entry : varExprs.entrySet()) {
            Expr<BitVecSort> v = model.evaluate(entry.getValue(), false);
            result.put(entry.getKey(), ((BitVecNum) v).getBigInteger().intValue());
        }
        return result;
    }

    Expr<BoolSort> translateBool(SymbolicValue constraint) {
        if (constraint instanceof SymbolicOperation) {
            SymbolicOperation op = (SymbolicOperation) constraint;
            switch (op.operator) {
            case Not:
                return ctx.mkNot(translateBool(op.operands[0]));
            case Lt:
                Expr<BitVecSort> leftLt = translateInt(op.operands[0]);
                Expr<BitVecSort> rightLt = translateInt(op.operands[1]);
                return ctx.mkBVSLT(leftLt, rightLt);
            case Neq:
                Expr<BitVecSort> leftNeq = translateInt(op.operands[0]);
                Expr<BitVecSort> rightNeq = translateInt(op.operands[1]);
                return ctx.mkNot(ctx.mkEq(leftNeq, rightNeq));
            }
        }
        // Otherwise, we convert the int to a bool through a non-zero check
        return ctx.mkNot(ctx.mkEq(translateInt(constraint), zero));
    }

    Expr<BitVecSort> translateInt(SymbolicValue v) {
        if (v instanceof SymbolicVariable) {
            SymbolicVariable var = (SymbolicVariable) v;
            Expr<BitVecSort> varExpr = varExprs.get(var.name);
            if (varExpr == null) throw new RuntimeException("var '" + var + "' is not bound to an expr!");
            return varExpr;
        } else if (v instanceof SymbolicOperation) {
            SymbolicOperation op = (SymbolicOperation) v;
            switch (op.operator) {
            case Add:
                return ctx.mkBVAdd(translateInt(op.operands[0]), translateInt(op.operands[1]));
            case Sub:
                return ctx.mkBVSub(translateInt(op.operands[0]), translateInt(op.operands[1]));
            case Mul:
                return ctx.mkBVMul(translateInt(op.operands[0]), translateInt(op.operands[1]));
            case Bxor:
                return ctx.mkBVXOR(translateInt(op.operands[0]), translateInt(op.operands[1]));
            case Band:
                return ctx.mkBVAND(translateInt(op.operands[0]), translateInt(op.operands[1]));
            case Bor:
                return ctx.mkBVOR(translateInt(op.operands[0]), translateInt(op.operands[1]));
            case Bshl:
                return ctx.mkBVSHL(translateInt(op.operands[0]), translateInt(op.operands[1]));
            default:
                throw new RuntimeException("type mismatch when generating constraint");
            }
        } else {
            SymbolicInteger value = (SymbolicInteger) v;
            return ctx.mkNumeral(value.value, bvs);
        }
    }

    Set<String> collectVariables(Collection<SymbolicValue> constraints) {
        Set<String> vars = new HashSet<>();
        for (SymbolicValue v : constraints) {
            vars.addAll(v.variables());
        }
        return vars;
    }

}
