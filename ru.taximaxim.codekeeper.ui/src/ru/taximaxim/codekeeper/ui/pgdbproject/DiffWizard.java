package ru.taximaxim.codekeeper.ui.pgdbproject;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeElement.DiffSide;
import ru.taximaxim.codekeeper.ui.Activator;
import ru.taximaxim.codekeeper.ui.UIConsts;
import ru.taximaxim.codekeeper.ui.UIConsts.EDITOR;
import ru.taximaxim.codekeeper.ui.UIConsts.FILE;
import ru.taximaxim.codekeeper.ui.dialogs.ExceptionNotifier;
import ru.taximaxim.codekeeper.ui.differ.DbSource;
import ru.taximaxim.codekeeper.ui.differ.DiffTableViewer;
import ru.taximaxim.codekeeper.ui.differ.Differ;
import ru.taximaxim.codekeeper.ui.differ.TreeDiffer;
import ru.taximaxim.codekeeper.ui.localizations.Messages;
import ru.taximaxim.codekeeper.ui.sqledit.StringEditorInput;

public class DiffWizard extends Wizard implements IPageChangingListener {

    private PageDiff pageDiff;
    private PagePartial pagePartial;

    private final PgDbProject proj;
    private final IPreferenceStore mainPrefs;

    public DiffWizard(PgDbProject proj, IPreferenceStore mainPrefs) {
        this.proj = proj;
        this.mainPrefs = mainPrefs;

        setWindowTitle(Messages.diffWizard_Diff);
        setDefaultPageImageDescriptor(ImageDescriptor.createFromURL(
                Activator.getContext().getBundle().getResource(FILE.ICONAPPWIZ)));
        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages() {
        pageDiff = new PageDiff(Messages.diffWizard_diff_parameters, mainPrefs, proj);
        pagePartial = new PagePartial(Messages.diffWizard_diff_tree, mainPrefs);

        addPage(pageDiff);
        addPage(pagePartial);
    }

    @Override
    public void createPageControls(Composite pageContainer) {
        super.createPageControls(pageContainer);

        getShell().addShellListener(new ShellAdapter() {

            @Override
            public void shellActivated(ShellEvent e) {
                getShell().removeShellListener(this);

                getShell().pack();
            }
        });

        ((WizardDialog) getContainer()).addPageChangingListener(this);
    }

    @Override
    public void handlePageChanging(PageChangingEvent e) {
        if (e.getCurrentPage() == pageDiff && e.getTargetPage() == pagePartial) {
            DbSource dbSource = pageDiff.getDbSource();
            DbSource dbTarget = pageDiff.getDbTarget();
            TreeDiffer treediffer = new TreeDiffer(dbSource, dbTarget, false);

            try {
                getContainer().run(true, true, treediffer);
            }  catch (InvocationTargetException ex) {
                e.doit = false;
                ExceptionNotifier.notifyDefault(Messages.error_in_differ_thread, ex);
                return;
            } catch (InterruptedException ex) {
                // cancelled
                e.doit = false;
                return;
            }

            pagePartial.setData(dbSource.getOrigin(), dbTarget.getOrigin(), treediffer);
            getShell().pack();
        }
    }

    @Override
    public boolean canFinish() {
        if (getContainer().getCurrentPage() != pagePartial) {
            return false;
        }
        return super.canFinish();
    }

    @Override
    public boolean performFinish() {
        try {
            TreeDiffer treediffer = pagePartial.getTreeDiffer();
            Differ differ = new Differ(treediffer.getDbSource().getDbObject(),
                    treediffer.getDbTarget().getDbObject(),
                    treediffer.getDiffTree(), false, pageDiff.getTimezone());
            getContainer().run(true, true, differ);

            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
            .openEditor(new StringEditorInput(differ.getDiffDirect(), "Diff Wizard Result"),
                    EDITOR.ROLLON);
            return true;
        } catch (InvocationTargetException ex) {
            ExceptionNotifier.notifyDefault(Messages.error_in_differ_thread, ex);
        } catch (InterruptedException ex) {
            // cancelled
        } catch (PartInitException ex) {
            ExceptionNotifier.notifyDefault(ex.getLocalizedMessage(), ex);
        }
        return false;
    }
}

class PageDiff extends WizardPage implements Listener {

    private final IPreferenceStore mainPrefs;
    private final PgDbProject proj;

    private DbSourcePicker dbSource, dbTarget;
    private ComboViewer cmbTimezone;

    public PageDiff(String pageName, IPreferenceStore mainPrefs, PgDbProject proj) {
        super(pageName, pageName, null);

        this.mainPrefs = mainPrefs;
        this.proj = proj;
        setDescription(Messages.diffwizard_diffpage_select);
    }

    public DbSource getDbSource() {
        return dbSource.getDbSource();
    }

    public DbSource getDbTarget() {
        return dbTarget.getDbSource();
    }

    public String getTimezone() {
        return cmbTimezone.getCombo().getText();
    }

    public void setTimezone(String timezone) {
        cmbTimezone.getCombo().setText(timezone);
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, true));

        dbSource = new DbSourcePicker(container, "Source", mainPrefs, this);
        dbSource.setLayoutData(new GridData(GridData.FILL_BOTH));

        dbTarget = new DbSourcePicker(container, "Target", mainPrefs, this);
        dbTarget.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite compTz = new Composite(container, SWT.NONE);
        GridLayout gl = new GridLayout(2, false);
        gl.marginWidth = gl.marginHeight = 0;
        compTz.setLayout(gl);

        new Label(compTz, SWT.NONE).setText("DB timezone:");

        cmbTimezone = new ComboViewer(compTz, SWT.DROP_DOWN | SWT.BORDER);
        cmbTimezone.setContentProvider(ArrayContentProvider.getInstance());
        cmbTimezone.setLabelProvider(new LabelProvider());
        cmbTimezone.setInput(UIConsts.TIME_ZONES);
        cmbTimezone.getCombo().setText(ApgdiffConsts.UTC);

        if (proj != null) {
            dbTarget.setDbStore(new StructuredSelection(proj.getProject().getLocation().toFile()));
        }

        setControl(container);
    }

    @Override
    public boolean isPageComplete() {
        String err = null;

        if (getDbSource() == null) {
            err = Messages.diffwizard_diffpage_source_warning;
        } else if (getDbTarget() == null) {
            err = Messages.diffwizard_diffpage_target_warning;
        } else if (getTimezone().isEmpty()) {
            err = "Please select DB timezone";
        }

        setErrorMessage(err);
        return err == null;
    }

    @Override
    public void handleEvent(Event event) {
        getWizard().getContainer().updateButtons();
        getWizard().getContainer().updateMessage();
    }
}

class PagePartial extends WizardPage {

    private final IPreferenceStore mainPrefs;
    private TreeDiffer treeDiffer;
    private Label lblSource, lblTarget;
    private DiffTableViewer diffTable;

    public void setData(String source, String target, TreeDiffer treeDiffer) {
        this.treeDiffer = treeDiffer;
        lblSource.setText(source);
        lblTarget.setText(target);
        diffTable.setInput(treeDiffer.getDbSource(), treeDiffer.getDbTarget(), treeDiffer.getDiffTree(), null);
    }

    public TreeDiffer getTreeDiffer() {
        return treeDiffer;
    }

    public PagePartial(String pageName, IPreferenceStore mainPrefs) {
        super(pageName, pageName, null);
        this.mainPrefs = mainPrefs;
        setDescription(Messages.diffwizard_pagepartial_description);
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, true));

        new Label(container, SWT.NONE).setText(Messages.diffWizard_source);
        new Label(container, SWT.NONE).setText(Messages.diffWizard_target);
        lblSource = new Label(container, SWT.WRAP);
        lblTarget = new Label(container, SWT.WRAP);

        diffTable = new DiffTableViewer(container, mainPrefs, false, DiffSide.LEFT);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
        gd.widthHint = 400;
        gd.heightHint = 240;
        diffTable.setLayoutData(gd);

        setControl(container);
    }
}
