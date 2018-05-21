package ru.taximaxim.codekeeper.ui.dbstore;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;

import cz.startnet.utils.pgdiff.loader.JdbcConnector;
import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts;
import ru.taximaxim.codekeeper.ui.Activator;
import ru.taximaxim.codekeeper.ui.UiSync;
import ru.taximaxim.codekeeper.ui.localizations.Messages;
import ru.taximaxim.codekeeper.ui.properties.IgnoreListProperties.IgnoreListEditor;

public class DbStoreEditorDialog extends TrayDialog {

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final String DEFAULT_PORT = "5432";

    private final DbInfo dbInitial;
    private DbInfo dbInfo;
    private String entryNameDefinedByUser;

    private Text txtName;
    private Text txtDbName;
    private Text txtDbUser;
    private Text txtDbPass;
    private Text txtDbHost;
    private Text txtDbPort;
    private CLabel lblWarnDbPass;
    private Button btnReadOnly;
    private Button btnGenerateName;

    private IgnoreListEditor listEditor;

    public DbInfo getDbInfo(){
        return dbInfo;
    }

    public DbStoreEditorDialog(Shell shell, DbInfo dbInitial) {
        super(shell);
        this.dbInitial = dbInitial;
    }

    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.dbStoreEditorDialog_db_store_editor);
        newShell.addShellListener(new ShellAdapter() {

            @Override
            public void shellActivated(ShellEvent e) {
                // one time listener
                newShell.removeShellListener(this);

                boolean generateEntryName = true;
                String dbHost = DEFAULT_HOST;
                String dbPort = DEFAULT_PORT;
                String dbName = null;
                String dbUser = null;

                if (dbInitial != null) {
                    dbHost = dbInitial.getDbHost();
                    dbHost = !dbHost.isEmpty() ? dbHost : DEFAULT_HOST;
                    txtDbHost.setText(dbHost);

                    dbPort = getVerifiedPort(Integer.toString(dbInitial.getDbPort()));
                    txtDbPort.setText(dbPort);

                    dbName = dbInitial.getDbName();
                    txtDbName.setText(dbName);

                    dbUser = dbInitial.getDbUser();
                    txtDbUser.setText(dbUser);

                    txtDbPass.setText(dbInitial.getDbPass());
                    btnReadOnly.setSelection(dbInitial.isReadOnly());
                    listEditor.setInputList(dbInitial.getIgnoreFiles());

                    generateEntryName = dbInitial.isGeneratedName();

                    String entryName = dbInitial.getName();
                    if (!generateEntryName) {
                        entryNameDefinedByUser = entryName;
                    }
                } else {
                    txtDbHost.setText(dbHost);
                    txtDbPort.setText(dbPort);
                    txtDbPass.setText("");//$NON-NLS-1$
                }

                btnGenerateName.setSelection(generateEntryName);

                fillTxtNameField(generateEntryName, dbUser, dbHost, dbPort, dbName);
            }
        });
    }

    private String getVerifiedPort(String dbPort) {
        return dbPort.isEmpty() || "0".equals(dbPort) ? DEFAULT_PORT : dbPort; //$NON-NLS-1$
    }

    private String generateEntryName(String dbUser, String dbHost, String dbPort, String dbName) {
        StringBuilder entryNameSb = new StringBuilder();

        if (dbUser != null && !dbUser.isEmpty()) {
            entryNameSb.append(dbUser).append('@');
        }

        entryNameSb.append(dbHost == null || dbHost.isEmpty() ? DEFAULT_HOST : dbHost);

        if (!DEFAULT_PORT.equals(dbPort)) {
            entryNameSb.append(':').append(dbPort);
        }

        if (dbName != null && !dbName.isEmpty()) {
            entryNameSb.append("//").append(dbName);
        }

        return entryNameSb.toString();
    }

    private void fillTxtNameField(boolean generateEntryName, String dbUser, String dbHost,
            String dbPort, String dbName) {
        if (generateEntryName) {
            txtName.setText(generateEntryName(dbUser, dbHost, dbPort, dbName));
        } else {
            txtName.setText(entryNameDefinedByUser != null ? entryNameDefinedByUser
                    : generateEntryName(dbUser, dbHost, dbPort, dbName));
        }

        txtName.setEnabled(!generateEntryName);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        TabFolder tabFolder = new TabFolder(area, SWT.BORDER);

        //// Creating tab item "Db Info" and fill it by components.

        Composite tabAreaDb = createTabItemWithComposite(tabFolder, Messages.dbStoreEditorDialog_db_info);

        new Label(tabAreaDb, SWT.NONE).setText(Messages.dB_host);

        Composite areaHostPort = new Composite(tabAreaDb, SWT.NULL);
        GridLayout gl = new GridLayout(3, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        areaHostPort.setLayout(gl);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = 500;
        areaHostPort.setLayoutData(gd);

        txtDbHost = new Text(areaHostPort, SWT.BORDER);
        txtDbHost.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(areaHostPort, SWT.NONE).setText(Messages.dbPicker_port);

        txtDbPort = new Text(areaHostPort, SWT.BORDER);
        gd = new GridData(60, SWT.DEFAULT);
        txtDbPort.setLayoutData(gd);
        txtDbPort.addVerifyListener(e -> {

            try {
                if (!e.text.isEmpty() && Integer.valueOf(e.text) < 0) {
                    e.doit = false;
                }
            } catch(NumberFormatException ex) {
                e.doit = false;
            }
        });

        new Label(tabAreaDb, SWT.NONE).setText(Messages.dB_name);

        txtDbName = new Text(tabAreaDb, SWT.BORDER);
        txtDbName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(tabAreaDb, SWT.NONE).setText(Messages.dB_user);

        txtDbUser = new Text(tabAreaDb, SWT.BORDER);
        txtDbUser.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(tabAreaDb, SWT.NONE).setText(Messages.dB_password);

        txtDbPass = new Text(tabAreaDb, SWT.BORDER | SWT.PASSWORD);
        txtDbPass.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        txtDbPass.addModifyListener(e -> {
            GridData data = (GridData) lblWarnDbPass.getLayoutData();

            if (txtDbPass.getText().isEmpty() != data.exclude) {
                lblWarnDbPass.setVisible(data.exclude);
                data.exclude = !data.exclude;

                // ensures correct pack during shell activation
                UiSync.exec(getShell(), () -> getShell().pack());
            }
        });

        lblWarnDbPass = new CLabel(tabAreaDb, SWT.NONE);
        lblWarnDbPass.setImage(Activator.getEclipseImage(ISharedImages.IMG_OBJS_WARN_TSK));
        lblWarnDbPass.setText(Messages.warning_providing_password_here_is_insecure_use_pgpass_instead);
        lblWarnDbPass.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));

        btnReadOnly = new Button(tabAreaDb, SWT.CHECK);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        btnReadOnly.setLayoutData(gd);
        btnReadOnly.setText(Messages.DbStoreEditorDialog_read_only);
        btnReadOnly.setToolTipText(Messages.DbStoreEditorDialog_read_only_description);

        Label separator = new Label(tabAreaDb, SWT.HORIZONTAL | SWT.SEPARATOR);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        separator.setLayoutData(gd);

        new Label(tabAreaDb, SWT.NONE).setText(Messages.entry_name);

        Composite areaEntryName = new Composite(tabAreaDb, SWT.NULL);
        gl = new GridLayout(2, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        areaEntryName.setLayout(gl);
        areaEntryName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        txtName = new Text(areaEntryName, SWT.BORDER);
        txtName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        btnGenerateName = new Button(areaEntryName, SWT.CHECK);
        gd = new GridData(130, SWT.DEFAULT);
        btnGenerateName.setLayoutData(gd);
        btnGenerateName.setText(Messages.DbStoreEditorDialog_auto_generation);
        btnGenerateName.setToolTipText(Messages.DbStoreEditorDialog_auto_generation_description);
        btnGenerateName.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                fillTxtNameField(btnGenerateName.getSelection(), txtDbUser.getText(),
                        txtDbHost.getText(), getVerifiedPort(txtDbPort.getText()),
                        txtDbName.getText());
            }
        });

        //// Creating tab item "Ignored objects files" and fill it by components.

        listEditor = new IgnoreListEditor(createTabItemWithComposite(tabFolder,
                Messages.DbStoreEditorDialog_ignore_file_list));

        return area;
    }

    /**
     * Creates a tab item with its own composite.
     *
     * @param tabFolder TabFolder object
     * @param tabText text for tab item
     * @return the composite belonging to the created tab item
     */
    private Composite createTabItemWithComposite(TabFolder tabFolder, String tabText) {
        Composite tabComposite = new Composite(tabFolder, SWT.NULL);
        GridLayout gl = new GridLayout(2, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        tabComposite.setLayout(gl);
        tabComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        TabItem tabItem = new TabItem(tabFolder, SWT.NULL);
        tabItem.setText(tabText);
        tabItem.setControl(tabComposite);
        return tabComposite;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        Button btnTestConnection = createButton(parent, IDialogConstants.CLIENT_ID, Messages.DbStoreEditorDialog_test_connection, true);
        btnTestConnection.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                int style;
                String message;
                String port = txtDbPort.getText();

                try {
                    int dbport = port.isEmpty() ? 0 : Integer.parseInt(port);

                    try (Connection connection = new JdbcConnector(txtDbHost.getText(), dbport,
                            txtDbUser.getText(), txtDbPass.getText(),
                            txtDbName.getText(), ApgdiffConsts.UTC).getConnection()) {
                        style = SWT.OK;
                        message = Messages.DbStoreEditorDialog_successfull_connection;
                    }
                } catch (NumberFormatException ex) {
                    message = MessageFormat.format(
                            Messages.dbStoreEditorDialog_not_valid_port_number,
                            port);
                    style = SWT.ICON_ERROR;
                } catch (SQLException | IOException ex) {
                    message = Messages.DbStoreEditorDialog_failed_connection_reason + ex.getLocalizedMessage();
                    style = SWT.ICON_ERROR;
                }

                MessageBox mb = new MessageBox(getShell(), style);
                mb.setText(style == SWT.ERROR ? Messages.DbStoreEditorDialog_failed_connection : Messages.DbStoreEditorDialog_success);
                mb.setMessage(message);
                mb.open();
            }
        });

        super.createButtonsForButtonBar(parent);
    }

    @Override
    protected void okPressed() {
        int dbport;
        String port = txtDbPort.getText();
        if (txtDbPort.getText().isEmpty()) {
            dbport = 0;
        } else {
            try {
                dbport = Integer.parseInt(port);
            } catch (NumberFormatException ex) {
                MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR);
                mb.setText(Messages.dbStoreEditorDialog_cannot_save_entry);
                mb.setMessage(MessageFormat.format(
                        Messages.dbStoreEditorDialog_not_valid_port_number,
                        port));
                mb.open();
                return;
            }
        }

        dbInfo = new DbInfo(txtName.getText(), txtDbName.getText(),
                txtDbUser.getText(), txtDbPass.getText(),
                txtDbHost.getText(), dbport, btnReadOnly.getSelection(),
                btnGenerateName.getSelection(), listEditor.getList());
        super.okPressed();
    }

    @Override
    protected boolean isResizable() {
        return true;
    }
}