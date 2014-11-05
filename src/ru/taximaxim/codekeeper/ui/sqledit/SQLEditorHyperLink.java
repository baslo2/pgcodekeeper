package ru.taximaxim.codekeeper.ui.sqledit;

import java.nio.file.Path;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

public class SQLEditorHyperLink implements IHyperlink {

    private Path location;
    private IRegion region;
    private ITextViewer viewer;
    private String label;

    public SQLEditorHyperLink(IRegion region, String label, Path path, ITextViewer viewer) {

        this.region= region;
        this.location = path;
        this.viewer = viewer;
        this.label = label;
    }
    
    @Override
    public IRegion getHyperlinkRegion() {
        return region;
    }

    @Override
    public String getTypeLabel() {
        return label;
    }

    @Override
    public String getHyperlinkText() {
        return label;
    }

    @Override
    public void open() {
        if(location!=null)
        {
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            ITextEditor editor = null;
            try {
                editor = (ITextEditor)IDE.openEditor(page, location.toUri(), SQLEditor.ID, true);
                editor.selectAndReveal(region.getOffset(), region.getLength());
            } catch (PartInitException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
