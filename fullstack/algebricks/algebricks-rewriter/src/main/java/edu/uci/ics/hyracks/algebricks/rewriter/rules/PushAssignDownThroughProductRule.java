/*
 * Copyright 2009-2010 by The Regents of the University of California
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License from
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.uci.ics.hyracks.algebricks.rewriter.rules;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.mutable.Mutable;

import edu.uci.ics.hyracks.algebricks.common.exceptions.AlgebricksException;
import edu.uci.ics.hyracks.algebricks.core.algebra.base.ILogicalOperator;
import edu.uci.ics.hyracks.algebricks.core.algebra.base.IOptimizationContext;
import edu.uci.ics.hyracks.algebricks.core.algebra.base.LogicalOperatorTag;
import edu.uci.ics.hyracks.algebricks.core.algebra.base.LogicalVariable;
import edu.uci.ics.hyracks.algebricks.core.algebra.expressions.ConstantExpression;
import edu.uci.ics.hyracks.algebricks.core.algebra.operators.logical.AbstractBinaryJoinOperator;
import edu.uci.ics.hyracks.algebricks.core.algebra.operators.logical.AbstractLogicalOperator;
import edu.uci.ics.hyracks.algebricks.core.algebra.operators.logical.visitors.VariableUtilities;
import edu.uci.ics.hyracks.algebricks.core.rewriter.base.IAlgebraicRewriteRule;

public class PushAssignDownThroughProductRule implements IAlgebraicRewriteRule {

    @Override
    public boolean rewritePre(Mutable<ILogicalOperator> opRef, IOptimizationContext context) throws AlgebricksException {
        return false;
    }

    @Override
    public boolean rewritePost(Mutable<ILogicalOperator> opRef, IOptimizationContext context)
            throws AlgebricksException {
        AbstractLogicalOperator op1 = (AbstractLogicalOperator) opRef.getValue();
        if (op1.getOperatorTag() != LogicalOperatorTag.ASSIGN) {
            return false;
        }
        Mutable<ILogicalOperator> op2Ref = op1.getInputs().get(0);
        AbstractLogicalOperator op2 = (AbstractLogicalOperator) op2Ref.getValue();
        if (op2.getOperatorTag() != LogicalOperatorTag.INNERJOIN) {
            return false;
        }
        AbstractBinaryJoinOperator join = (AbstractBinaryJoinOperator) op2;
        if (join.getCondition().getValue() != ConstantExpression.TRUE) {
            return false;
        }

        List<LogicalVariable> used = new ArrayList<LogicalVariable>();
        VariableUtilities.getUsedVariables(op1, used);

        Mutable<ILogicalOperator> b0Ref = op2.getInputs().get(0);
        ILogicalOperator b0 = b0Ref.getValue();
        List<LogicalVariable> b0Scm = new ArrayList<LogicalVariable>();
        VariableUtilities.getLiveVariables(b0, b0Scm);
        if (b0Scm.containsAll(used)) {
            // push assign on left branch
            op2Ref.setValue(b0);
            b0Ref.setValue(op1);
            opRef.setValue(op2);
            return true;
        } else {
            Mutable<ILogicalOperator> b1Ref = op2.getInputs().get(1);
            ILogicalOperator b1 = b1Ref.getValue();
            List<LogicalVariable> b1Scm = new ArrayList<LogicalVariable>();
            VariableUtilities.getLiveVariables(b1, b1Scm);
            if (b1Scm.containsAll(used)) {
                // push assign on right branch
                op2Ref.setValue(b1);
                b1Ref.setValue(op1);
                opRef.setValue(op2);
                return true;
            } else {
                return false;
            }
        }
    }

}
