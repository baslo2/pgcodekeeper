package ru.taximaxim.codekeeper.ui.prefs;

import java.util.Arrays;

import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.texteditor.templates.TemplatePreferencePage;

import ru.taximaxim.codekeeper.ui.Activator;
import ru.taximaxim.codekeeper.ui.Log;
import ru.taximaxim.codekeeper.ui.sqledit.SQLEditorTemplateManager;

public class SQLEditorTemplatePrefPage extends TemplatePreferencePage {

    public SQLEditorTemplatePrefPage() {
        try {
            setPreferenceStore(Activator.getDefault().getPreferenceStore());
            setTemplateStore(SQLEditorTemplateManager.getInstance()
                    .getTemplateStore());
            setContextTypeRegistry(SQLEditorTemplateManager.getInstance()
                    .getContextTypeRegistry());
        } catch (Exception ex) {
            Log.log(Log.LOG_ERROR, "Cannot get sql templates", ex); //$NON-NLS-1$
        }
    }

    @Override
    protected boolean isShowFormatterSetting() {
        return false;
    }

    @Override
    protected Control createContents(Composite ancestor) {
        Control control = super.createContents(ancestor);
        getTableViewer().setContentProvider(new OnlyUserTemplatesContentProvider());
        return control;
    }

    private static class OnlyUserTemplatesContentProvider implements IStructuredContentProvider {

        private TemplateStore fStore;

        @Override
        public Object[] getElements(Object input) {
            return Arrays.stream(fStore.getTemplateData(false))
                    .filter(TemplatePersistenceData::isUserAdded).toArray();
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            fStore = (TemplateStore) newInput;
        }

        @Override
        public void dispose() {
            fStore = null;
        }
    }
}
