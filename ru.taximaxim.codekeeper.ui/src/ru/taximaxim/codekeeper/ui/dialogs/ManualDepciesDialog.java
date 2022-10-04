package ru.taximaxim.codekeeper.ui.dialogs;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import ru.taximaxim.codekeeper.core.schema.PgStatement;
import ru.taximaxim.codekeeper.ui.localizations.Messages;

public class ManualDepciesDialog extends TrayDialog {

    private final List<Entry<PgStatement, PgStatement>> depciesSource;
    private final List<Entry<PgStatement, PgStatement>> depciesTarget;
    private final Map<String, PgStatement> objectsSource;
    private final Map<String, PgStatement> objectsTarget;
    private final String groupSourceName;
    private final String groupTargetName;

    private ManualDepciesGroup depcyGroupSource;
    private ManualDepciesGroup depcyGroupTarget;

    public List<Entry<PgStatement, PgStatement>> getDepciesSourceList() {
        return depcyGroupSource.getDepciesList();
    }

    public List<Entry<PgStatement, PgStatement>> getDepciesTargetList() {
        return depcyGroupTarget.getDepciesList();
    }

    public ManualDepciesDialog(Shell shell,
            List<Entry<PgStatement, PgStatement>> depciesSource,
            List<Entry<PgStatement, PgStatement>> depciesTarget,
            Map<String, PgStatement> objectsSource, Map<String, PgStatement> objectsTarget,
            String groupSourceName, String groupTargetName) {
        super(shell);

        this.depciesSource = depciesSource;
        this.depciesTarget = depciesTarget;
        this.objectsSource = objectsSource;
        this.objectsTarget = objectsTarget;
        this.groupSourceName = groupSourceName;
        this.groupTargetName = groupTargetName;

        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.ManualDepciesDialog_set_add_depcies);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        depcyGroupSource = new ManualDepciesGroup(area, SWT.NONE,
                depciesSource, objectsSource, groupSourceName);
        depcyGroupTarget = new ManualDepciesGroup(area, SWT.NONE,
                depciesTarget, objectsTarget, groupTargetName);
        return area;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }
}
