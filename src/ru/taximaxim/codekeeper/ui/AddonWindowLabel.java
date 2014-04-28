package ru.taximaxim.codekeeper.ui;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

import ru.taximaxim.codekeeper.ui.externalcalls.JGitExec;
import ru.taximaxim.codekeeper.ui.pgdbproject.PgDbProject;

public class AddonWindowLabel {
    @Inject
    IEventBroker eventBroker;
    @Inject
    EModelService service;
    
    @PostConstruct
    private void blah(){
        
    }
    
    @Inject
    private void updateLabel(
            final PgDbProject proj,
            @Optional @Named("__DUMMY__") @EventTopic(UIConsts.EVENT_REOPEN_PROJECT) PgDbProject proj2,
            MApplication app, EModelService service) throws IOException {
        String windowLabel = "pgCodekeeper";
        MWindow find = (MWindow) service.find("ru.taximaxim.codekeeper.mainapp.mainwindow", app);
        if (proj != null) {
            String p = proj.getRepoRoot().toString();
            windowLabel += "   " + proj.getProjectWorkingDir() + "   [branch: " + 
                    new JGitExec().getCurrentBranch(p) + "]";
        }
        if (find != null) {
            find.setLabel(windowLabel);
        }
    }
}