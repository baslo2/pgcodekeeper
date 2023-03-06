package ru.taximaxim.codekeeper.ui.job;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.services.IEvaluationService;

import ru.taximaxim.codekeeper.ui.UiSync;

public abstract class SingletonEditorJob extends EditorJob {

    private final String evalProperty;
    private boolean done;

    public String getEvalProperty() {
        return evalProperty;
    }

    protected SingletonEditorJob(String name, IEditorPart editorPart, String evalProperty) {
        super(name, editorPart);
        this.evalProperty = evalProperty;

        addJobChangeListener(new JobChangeAdapter() {

            @Override
            public void aboutToRun(IJobChangeEvent event) {
                IWorkbenchPartSite site = getEditorPart().getSite();
                UiSync.exec(site.getShell(), () -> {
                    IEvaluationService servise = site.getService(IEvaluationService.class);
                    if (servise != null) {
                        servise.requestEvaluation(evalProperty);
                    }
                });
            }

            @Override
            public void done(IJobChangeEvent event) {
                done = true;
                IWorkbenchPartSite site = getEditorPart().getSite();
                UiSync.exec(site.getShell(), () -> {
                    IEvaluationService servise = site.getService(IEvaluationService.class);
                    if (servise != null) {
                        servise.requestEvaluation(evalProperty);
                    }
                });
            }
        });
    }

    @Override
    public boolean belongsTo(Object family) {
        return SingletonEditorJob.class == family;
    }

    public boolean blocks(IEditorPart editorBlocked) {
        return !done && getEditorPart().equals(editorBlocked);
    }
}
