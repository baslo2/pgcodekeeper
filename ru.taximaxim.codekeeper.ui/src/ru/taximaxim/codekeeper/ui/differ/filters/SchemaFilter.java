package ru.taximaxim.codekeeper.ui.differ.filters;

import java.util.Map;

import ru.taximaxim.codekeeper.core.model.difftree.DbObjType;
import ru.taximaxim.codekeeper.core.model.difftree.TreeElement;
import ru.taximaxim.codekeeper.core.schema.PgDatabase;
import ru.taximaxim.codekeeper.ui.differ.ElementMetaInfo;

/**
 * Contains information of search by container
 *
 * @since 4.2.0.
 * @author galiev_mr
 */
public class SchemaFilter extends AbstractFilter {

    @Override
    public boolean checkElement(TreeElement el, Map<TreeElement, ElementMetaInfo> elementInfoMap,
            PgDatabase dbProject, PgDatabase dbRemote) {

        String container = el.getContainerQName();
        if (!container.isEmpty()) {
            return searchMatches(container);
        } else if (el.getType() == DbObjType.SCHEMA) {
            return searchMatches(el.getName());
        }

        return false;
    }
}