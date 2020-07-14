package ru.taximaxim.codekeeper.ui.sqledit;

import java.util.stream.Stream;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorInput;

import cz.startnet.utils.pgdiff.schema.PgObjLocation;
import ru.taximaxim.codekeeper.ui.pgdbproject.parser.PgDbParser;

public class SQLEditorHyperLinkDetector extends AbstractHyperlinkDetector {

    @Override
    public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region,
            boolean canShowMultipleHyperlinks) {
        SQLEditor editor = getAdapter(SQLEditor.class);
        IEditorInput input = editor.getEditorInput();
        if (input instanceof SQLEditorInput && ((SQLEditorInput) input).isReadOnly()) {
            return new IHyperlink[0];
        }

        PgDbParser parser = editor.getParser();

        int offset = region.getOffset();
        Stream<IHyperlink> links = Stream.empty();

        for (PgObjLocation obj : parser.getObjsForEditor(editor.getEditorInput())) {
            if (offset >= obj.getOffset()
                    && offset < (obj.getOffset() + obj.getObjLength())) {
                Stream<IHyperlink> stream = parser.getDefinitionsForObj(obj)
                        .map(def -> new SQLEditorHyperLink(
                                new Region(def.getOffset(), def.getObjLength()),
                                new Region(obj.getOffset(), obj.getObjLength()),
                                obj.getObjName(), def.getFilePath(), def.getLineNumber(),
                                editor.isMsSql()));
                links = Stream.concat(links, stream);
            }
        }

        IHyperlink[] result = links.toArray(IHyperlink[]::new);
        return result.length == 0 ? null : result;
    }
}