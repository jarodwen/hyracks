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
package edu.uci.ics.hyracks.algebricks.core.algebra.expressions;

import java.util.List;

import org.apache.commons.lang3.mutable.Mutable;

import edu.uci.ics.hyracks.algebricks.common.exceptions.AlgebricksException;
import edu.uci.ics.hyracks.algebricks.core.algebra.base.ILogicalExpression;
import edu.uci.ics.hyracks.algebricks.core.algebra.functions.IFunctionInfo;
import edu.uci.ics.hyracks.algebricks.core.algebra.visitors.ILogicalExpressionVisitor;

public class UnnestingFunctionCallExpression extends AbstractFunctionCallExpression {

    private boolean returnsUniqueValues;

    public UnnestingFunctionCallExpression(IFunctionInfo finfo) {
        super(FunctionKind.UNNEST, finfo);
    }

    public UnnestingFunctionCallExpression(IFunctionInfo finfo, List<Mutable<ILogicalExpression>> arguments) {
        super(FunctionKind.UNNEST, finfo, arguments);
    }

    public UnnestingFunctionCallExpression(IFunctionInfo finfo, Mutable<ILogicalExpression>... expressions) {
        super(FunctionKind.UNNEST, finfo, expressions);
    }

    @Override
    public UnnestingFunctionCallExpression cloneExpression() {
        cloneAnnotations();
        List<Mutable<ILogicalExpression>> clonedArgs = cloneArguments();
        UnnestingFunctionCallExpression ufce = new UnnestingFunctionCallExpression(finfo, clonedArgs);
        ufce.setReturnsUniqueValues(returnsUniqueValues);
        ufce.setOpaqueParameters(this.getOpaqueParameters());
        return ufce;
    }

    @Override
    public <R, T> R accept(ILogicalExpressionVisitor<R, T> visitor, T arg) throws AlgebricksException {
        return visitor.visitUnnestingFunctionCallExpression(this, arg);
    }

    public void setReturnsUniqueValues(boolean returnsUniqueValues) {
        this.returnsUniqueValues = returnsUniqueValues;
    }

    public boolean returnsUniqueValues() {
        return returnsUniqueValues;
    }

}
