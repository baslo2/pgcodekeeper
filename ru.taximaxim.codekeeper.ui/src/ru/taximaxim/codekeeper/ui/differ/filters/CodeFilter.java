package ru.taximaxim.codekeeper.ui.differ.filters;

import java.util.Map;
import java.util.Set;

import ru.taximaxim.codekeeper.core.model.difftree.TreeElement;
import ru.taximaxim.codekeeper.core.model.difftree.TreeElement.DiffSide;
import ru.taximaxim.codekeeper.core.schema.PgDatabase;
import ru.taximaxim.codekeeper.core.schema.PgStatement;
import ru.taximaxim.codekeeper.ui.differ.DiffTableViewer;
import ru.taximaxim.codekeeper.ui.differ.ElementMetaInfo;

/**
 * Contains information of code search
 *
 * @since 4.1.2.
 * @author galiev_mr
 *
 */
public class CodeFilter extends AbstractFilter {

    @Override
    public boolean checkElement(TreeElement el, Map<TreeElement, ElementMetaInfo> elementInfoMap,
            PgDatabase dbProject, PgDatabase dbRemote) {

        Set<TreeElement> elements = elementInfoMap.keySet();
        if (el.getSide() != DiffSide.RIGHT && checkSide(el, dbProject, elements)) {
            return true;
        }

        if (el.getSide() != DiffSide.LEFT) {
            return checkSide(el, dbRemote, elements);
        }

        return false;
    }

    private boolean checkSide(TreeElement el, PgDatabase db, Set<TreeElement> elements) {
        PgStatement statement = el.getPgStatement(db);
        if (statement != null) {
            if (searchMatches(statement.getCreationSQL())) {
                return true;
            }

            if (DiffTableViewer.isSubElement(el)) {
                PgStatement parent = statement.getParent();
                if (parent != null) {
                    return searchMatches(parent.getCreationSQL());
                }
            }

            if (DiffTableViewer.isContainer(el)) {
                return el.getChildren().stream().filter(elements::contains)
                        .map(e -> e.getPgStatement(db))
                        .anyMatch(s -> s != null && searchMatches(s.getCreationSQL()));
            }
        }

        return false;
    }
}
