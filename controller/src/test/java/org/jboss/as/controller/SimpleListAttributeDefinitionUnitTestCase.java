/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.controller;

import static org.junit.Assert.fail;

import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ListValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.ValueExpression;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests of {@link SimpleListAttributeDefinition}.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class SimpleListAttributeDefinitionUnitTestCase {

    @Test
    public void testExpressions() throws OperationFailedException {

        AttributeDefinition ad = SimpleAttributeDefinitionBuilder.create("x", ModelType.INT).setAllowExpression(true).build();
        SimpleListAttributeDefinition ld = SimpleListAttributeDefinition.Builder.of("test", ad)
                .setAllowExpression(true)
                .setValidator(new IntRangeValidator(1, false, true))
                .build();

        ModelNode op = new ModelNode();
        op.get("test").add(2).add("${test:1}");

        ModelNode validated = ld.validateOperation(op);
        Assert.assertEquals(op.get("test").get(0), validated.get(0));
        Assert.assertEquals(new ModelNode().set(new ValueExpression(op.get("test").get(1).asString())), validated.get(1));

        ModelNode model = new ModelNode();
        ld.validateAndSet(op, model);
        Assert.assertEquals(op.get("test").get(0), model.get("test").get(0));
        Assert.assertEquals(new ModelNode().set(new ValueExpression(op.get("test").get(1).asString())), model.get("test").get(1));

        ad = SimpleAttributeDefinitionBuilder.create("x", ModelType.PROPERTY).setAllowExpression(true).build();
        ld = SimpleListAttributeDefinition.Builder.of("test", ad)
                .setAllowExpression(true)
                .setValidator(new ModelTypeValidator(ModelType.PROPERTY, false, true))
                .build();

        op = new ModelNode();
        op.get("test").add("foo", 2).add("bar", "${test:1}");

        try {
            ld.validateOperation(op);
            org.junit.Assert.fail("Did not reject " + op);
        } catch (IllegalStateException good) {
            //
        }

        try {
            ld.validateAndSet(op, new ModelNode());
            org.junit.Assert.fail("Did not reject " + op);
        } catch (IllegalStateException good) {
            //
        }

    }

    @Test
    public void testBuilderCopyPreservesListValidator() throws OperationFailedException {

        // List can contain at most 2 items. The value of the item can not null
        ParameterValidator pv = new ModelTypeValidator(ModelType.PROPERTY, false, true);
        ListValidator lv = new ListValidator(pv, false, 1, 2);

        AttributeDefinition ad = SimpleAttributeDefinitionBuilder.create("x", ModelType.PROPERTY).build();
        SimpleListAttributeDefinition original = SimpleListAttributeDefinition.Builder.of("test", ad)
                .setListValidator(lv)
                .build();
        SimpleListAttributeDefinition copy = new SimpleListAttributeDefinition.Builder(original).build();

        ModelNode validValue = new ModelNode();
        validValue.add("foo", "bar");

        // too many elements
        ModelNode invalidValue = new ModelNode();
        invalidValue.add("foo", "bar");
        invalidValue.add("foo2", "baz");
        invalidValue.add("foo3", "bat");

        // undefined is not a valid element value
        ModelNode invalidValue2 = new ModelNode();
        invalidValue2.add(new ModelNode());

        // the original and copy attribute definition must validate the same way
        validateOperation(original, validValue, true);
        validateOperation(copy, validValue, true);

        validateOperation(original, invalidValue, false);
        validateOperation(copy, invalidValue, false);

        validateOperation(original, invalidValue2, false);
        validateOperation(copy, invalidValue2, false);
    }

    private void validateOperation(AttributeDefinition attributeDefinition, ModelNode value, boolean expectSuccess) throws OperationFailedException {
        ModelNode operation = new ModelNode();
        operation.get("test").set(value);

        if (expectSuccess) {
            attributeDefinition.validateOperation(operation);
        } else {
            try {
                attributeDefinition.validateOperation(operation);
                fail("operation must fail with invalid value " + value);
            } catch (OperationFailedException e) {
            }
        }
    }

}
