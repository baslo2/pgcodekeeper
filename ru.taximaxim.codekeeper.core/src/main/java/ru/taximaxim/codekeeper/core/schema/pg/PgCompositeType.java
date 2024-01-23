/*******************************************************************************
 * Copyright 2017-2024 TAXTELECOM, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package ru.taximaxim.codekeeper.core.schema.pg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import ru.taximaxim.codekeeper.core.PgDiffUtils;
import ru.taximaxim.codekeeper.core.hashers.Hasher;
import ru.taximaxim.codekeeper.core.schema.AbstractColumn;
import ru.taximaxim.codekeeper.core.schema.AbstractType;
import ru.taximaxim.codekeeper.core.schema.PgStatement;
import ru.taximaxim.codekeeper.core.schema.StatementUtils;

public final class PgCompositeType extends AbstractType{

    private static final String COLLATE = " COLLATE ";

    private final List<AbstractColumn> attrs = new ArrayList<>();

    public PgCompositeType(String name) {
        super(name);
    }

    @Override
    protected void appendDef(StringBuilder sb) {
        sb.append(" AS (");
        for (AbstractColumn attr : attrs) {
            sb.append("\n\t").append(PgDiffUtils.getQuotedName(attr.getName()))
            .append(' ').append(attr.getType());

            if (attr.getCollation() != null) {
                sb.append(COLLATE).append(attr.getCollation());
            }
            sb.append(',');
        }
        if (!attrs.isEmpty()) {
            sb.setLength(sb.length() - 1);
        }
        sb.append("\n);");
    }

    @Override
    public void appendComments(StringBuilder sb) {
        super.appendComments(sb);

        for (final AbstractColumn column : attrs) {
            column.appendComments(sb);
        }
    }

    @Override
    protected boolean compareUnalterable(AbstractType newType) {
        return !StatementUtils.isColumnsOrderChanged(((PgCompositeType) newType).getAttrs(), attrs);
    }

    @Override
    protected void compareType(AbstractType newType, StringBuilder sb,
            AtomicBoolean isNeedDepcies) {
        PgCompositeType newCompositeType = (PgCompositeType) newType;
        StringBuilder attrSb = new StringBuilder();
        for (AbstractColumn attr : newCompositeType.getAttrs()) {
            AbstractColumn oldAttr = getAttr(attr.getName());
            if (oldAttr == null) {
                appendAlterAttribute(attrSb, "ADD", " ", attr);
            } else if (!oldAttr.getType().equals(attr.getType()) ||
                    !Objects.equals(attr.getCollation(), oldAttr.getCollation())) {
                appendAlterAttribute(attrSb, "ALTER", " TYPE ", attr);
            }
        }

        for (AbstractColumn attr : getAttrs()) {
            if (newCompositeType.getAttr(attr.getName()) == null) {
                appendAlterAttribute(attrSb, "DROP", ",", attr);
            }
        }

        if (attrSb.length() > 0) {
            // remove last comma
            attrSb.setLength(attrSb.length() - 1);
            sb.append("\n\nALTER TYPE ").append(getQualifiedName())
            .append(attrSb).append(';');
            isNeedDepcies.set(true);
        }
    }

    @Override
    public void compareComments(StringBuilder sb, PgStatement newObj) {
        super.compareComments(sb, newObj);

        PgCompositeType newType = (PgCompositeType) newObj;
        for (AbstractColumn newAttr : newType.getAttrs()) {
            AbstractColumn oldAttr = getAttr(newAttr.getName());
            if (oldAttr != null) {
                oldAttr.compareComments(sb, newAttr);
            } else if (newAttr.checkComments()) {
                sb.setLength(sb.length() + 1);
            }
        }
    }

    @Override
    public void appendAlterComments(StringBuilder sb, PgStatement newObj) {
        super.appendAlterComments(sb, newObj);

        PgCompositeType newType = (PgCompositeType) newObj;
        for (AbstractColumn newAttr : newType.getAttrs()) {
            AbstractColumn oldAttr = getAttr(newAttr.getName());
            if (oldAttr != null) {
                oldAttr.appendAlterComments(sb, newAttr);
            } else {
                newAttr.appendComments(sb);
            }
        }
    }

    private void appendAlterAttribute(StringBuilder attrSb, String action, String delimiter,
            AbstractColumn attr) {
        attrSb.append("\n\t").append(action).append(" ATTRIBUTE ")
        .append(PgDiffUtils.getQuotedName(attr.getName()))
        .append(delimiter);
        if (!action.equals("DROP")) {
            attrSb.append(attr.getType());
            if (attr.getCollation() != null) {
                attrSb.append(COLLATE)
                .append(attr.getCollation());
            }
            attrSb.append(",");
        }
    }

    public List<AbstractColumn> getAttrs() {
        return Collections.unmodifiableList(attrs);
    }

    public AbstractColumn getAttr(String name) {
        for (AbstractColumn att : attrs) {
            if (att.getName().equals(name)) {
                return att;
            }
        }
        return null;
    }

    public void addAttr(AbstractColumn attr) {
        attrs.add(attr);
        attr.setParent(this);
        resetHash();
    }

    @Override
    protected AbstractType getTypeCopy() {
        PgCompositeType copy = new PgCompositeType(getName());
        for (AbstractColumn attr : attrs) {
            copy.addAttr((AbstractColumn) attr.deepCopy());
        }
        return copy;
    }

    @Override
    public boolean compare(PgStatement obj) {
        if (obj instanceof PgCompositeType) {
            PgCompositeType type = (PgCompositeType) obj;
            return super.compare(type) && attrs.equals(type.attrs);
        }
        return false;
    }

    @Override
    public void computeHash(Hasher hasher) {
        hasher.putOrdered(attrs);
    }
}
