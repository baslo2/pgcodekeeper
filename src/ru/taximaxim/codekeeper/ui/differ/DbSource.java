package ru.taximaxim.codekeeper.ui.differ;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.SubMonitor;

import ru.taximaxim.codekeeper.apgdiff.model.difftree.PgDbFilter2;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeElement;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeElement.DiffSide;
import ru.taximaxim.codekeeper.ui.Log;
import ru.taximaxim.codekeeper.ui.UIConsts.PROJ_PREF;
import ru.taximaxim.codekeeper.ui.externalcalls.IRepoWorker;
import ru.taximaxim.codekeeper.ui.externalcalls.JGitExec;
import ru.taximaxim.codekeeper.ui.externalcalls.PgDumper;
import ru.taximaxim.codekeeper.ui.fileutils.TempDir;
import ru.taximaxim.codekeeper.ui.fileutils.TempFile;
import ru.taximaxim.codekeeper.ui.localizations.Messages;
import ru.taximaxim.codekeeper.ui.pgdbproject.PgDbProject;
import cz.startnet.utils.pgdiff.loader.PgDumpLoader;
import cz.startnet.utils.pgdiff.schema.PgDatabase;

public abstract class DbSource {

    final private String origin;

    private PgDatabase dbObject;

    public String getOrigin() {
        return origin;
    }

    public PgDatabase getDbObject() {
        if (dbObject == null) {
            throw new NullPointerException(
                    Messages.dbSource_db_is_not_loaded_yet_object_is_null);
        }
        return dbObject;
    }

    public PgDatabase get(SubMonitor monitor) throws IOException {
        Log.log(Log.LOG_INFO, "Loading DB from " + origin); //$NON-NLS-1$
        
        dbObject = this.loadInternal(monitor);
        return dbObject;
    }

    protected DbSource(String origin) {
        this.origin = origin;
    }

    protected abstract PgDatabase loadInternal(SubMonitor monitor)
            throws IOException;

    public static DbSource fromDirTree(String dirTreePath, String encoding) {
        return new DbSourceDirTree(dirTreePath, encoding);
    }

    public static DbSource fromGit(PgDbProject proj, String privateKeyFile) {
        return new DbSourceRepo(null, proj, privateKeyFile);
    }

    public static DbSource fromGit(PgDbProject proj,
            String commitHash, String privateKeyFile) {
        return new DbSourceRepo(null, proj, commitHash, privateKeyFile);
    }

    public static DbSource fromGit(String url, String user,
            String pass, String commitHash, String encoding, String privateKeyFile) {
        return new DbSourceRepo(null, url, user, pass,
                commitHash, encoding, privateKeyFile);
    }

    public static DbSource fromProject(PgDbProject proj) {
        return new DbSourceProject(proj);
    }

    public static DbSource fromFile(String filename, String encoding) {
        return new DbSourceFile(filename, encoding);
    }

    public static DbSource fromDb(String exePgdump, String customParams,
            PgDbProject proj) {
        return new DbSourceDb(exePgdump, customParams, proj);
    }

    public static DbSource fromDb(String exePgdump, String customParams,
            String host, int port, String user, String pass, String dbname,
            String encoding) {
        return new DbSourceDb(exePgdump, customParams,
                host, port, user, pass, dbname, encoding);
    }

    public static DbSource fromFilter(DbSource src, TreeElement filter,
            DiffSide side) {
        return new DbSourceFilter(src, filter, side);
    }
    
    public static DbSource fromJdbc(String host, int port, String user, String pass, String dbname,
            String encoding) {
        return new DbSourceJdbc(host, port, user, pass, dbname, encoding);
    }
}

class DbSourceDirTree extends DbSource {

    final private String dirTreePath;

    final private String encoding;

    DbSourceDirTree(String dirTreePath, String encoding) {
        super(dirTreePath);

        this.dirTreePath = dirTreePath;
        this.encoding = encoding;
    }

    @Override
    protected PgDatabase loadInternal(SubMonitor monitor) {
        SubMonitor.convert(monitor, 1).newChild(1).subTask(Messages.dbSource_loading_tree);

        return PgDumpLoader.loadDatabaseSchemaFromDirTree(dirTreePath,
                encoding, false, false);
    }
}

class DbSourceRepo extends DbSource {

    final private IRepoWorker repo;

    final private String encoding;

    final private String rev;

    DbSourceRepo(String repoExec, PgDbProject proj, String privateKeyFile) {
        this(repoExec, proj, null, privateKeyFile);
    }

    public DbSourceRepo(String repoExec, PgDbProject proj, String rev, String privateKeyFile) {
        this(repoExec, 
                 proj.getString(PROJ_PREF.REPO_URL), proj
                        .getString(PROJ_PREF.REPO_USER), proj
                        .getString(PROJ_PREF.REPO_PASS), rev, proj
                        .getString(PROJ_PREF.ENCODING), privateKeyFile);
    }

    DbSourceRepo(String repoExec, String url, String user,
            String pass, String rev, String encoding, String privateKeyFile) {
        super(url + (rev.isEmpty() ? "" : "@" + rev)); //$NON-NLS-1$ //$NON-NLS-2$
        repo = new JGitExec(url, user, pass, privateKeyFile);
        this.encoding = encoding;
        this.rev = rev;
    }

    @Override
    protected PgDatabase loadInternal(SubMonitor monitor) throws IOException {
        SubMonitor pm = SubMonitor.convert(monitor, 2);

        try (TempDir tmpDir = new TempDir("tmp_repo_")) { //$NON-NLS-1$
            File dir = tmpDir.get();

            pm.newChild(1).subTask(Messages.dbSource_repository_rev_checkout);
            repo.repoCheckOut(dir, rev);

            pm.newChild(1).subTask(Messages.dbSource_loading_tree);
            // TODO Implement reading subdir to be passed to loadDBSchema...
            return PgDumpLoader.loadDatabaseSchemaFromDirTree(
                    dir.getAbsolutePath(), encoding, false, false);
        }
    }
}

class DbSourceProject extends DbSource {

    final private PgDbProject proj;

    DbSourceProject(PgDbProject proj) {
        super(proj.getProjectFile().getAbsolutePath());

        this.proj = proj;
    }

    @Override
    protected PgDatabase loadInternal(SubMonitor monitor) {
        SubMonitor.convert(monitor, 1).newChild(1).subTask(Messages.dbSource_loading_tree);

        return PgDumpLoader.loadDatabaseSchemaFromDirTree(proj
                .getProjectWorkingDir().getAbsolutePath(), proj
                .getString(PROJ_PREF.ENCODING), false, false);
    }
}

class DbSourceFile extends DbSource {

    final private String filename;

    final private String encoding;

    DbSourceFile(String filename, String encoding) {
        super(filename);

        this.filename = filename;
        this.encoding = encoding;
    }

    @Override
    protected PgDatabase loadInternal(SubMonitor monitor) {
        SubMonitor.convert(monitor, 1).newChild(1).subTask(Messages.dbSource_loading_dump);

        return PgDumpLoader.loadDatabaseSchemaFromDump(filename, encoding,
                false, false);
    }
}

class DbSourceDb extends DbSource {

    private final String exePgdump;
    private final String customParams;

    private final String host, user, pass, dbname, encoding;
    private final int port;

    DbSourceDb(String exePgdump, String customParams, PgDbProject props) {
        this(exePgdump, customParams,
                props.getString(PROJ_PREF.DB_HOST),
                props.getInt(PROJ_PREF.DB_PORT),
                props.getString(PROJ_PREF.DB_USER),
                props.getString(PROJ_PREF.DB_PASS),
                props.getString(PROJ_PREF.DB_NAME),
                props.getString(PROJ_PREF.ENCODING));
    }

    DbSourceDb(String exePgdump, String customParams,
            String host, int port, String user, String pass,
            String dbname, String encoding) {
        super((dbname.isEmpty() ? "unknown_db" : dbname) + "@" //$NON-NLS-1$ //$NON-NLS-2$
                + (host.isEmpty() ? "unknown_host" : host)); //$NON-NLS-1$

        this.exePgdump = exePgdump;
        this.customParams = customParams;
        this.host = host;
        this.port = port;
        this.user = user;
        this.pass = pass;
        this.dbname = dbname;
        this.encoding = encoding;
    }

    @Override
    protected PgDatabase loadInternal(SubMonitor monitor) throws IOException {
        SubMonitor pm = SubMonitor.convert(monitor, 2);

        try (TempFile tf = new TempFile("tmp_dump_", ".sql")) { //$NON-NLS-1$ //$NON-NLS-2$
            File dump = tf.get();

            pm.newChild(1).subTask(Messages.dbSource_executing_pg_dump);

            new PgDumper(exePgdump, customParams,
                    host, port, user, pass, dbname, encoding,
                    dump.getAbsolutePath()).pgDump();

            pm.newChild(1).subTask(Messages.dbSource_loading_dump);

            return PgDumpLoader.loadDatabaseSchemaFromDump(
                    dump.getAbsolutePath(), encoding, false, false);
        }
    }
}

class DbSourceFilter extends DbSource {

    final DbSource src;

    final TreeElement filter;

    final DiffSide side;

    DbSourceFilter(DbSource src, TreeElement filter, DiffSide side) {
        super(Messages.dbSource_filter_on + src.getOrigin());
        this.src = src;
        this.filter = filter;
        this.side = side;
    }

    @Override
    protected PgDatabase loadInternal(SubMonitor monitor) throws IOException {
        PgDatabase db;
        try {
            db = src.getDbObject();
        } catch (NullPointerException ex) {
            db = src.get(monitor);
        }

        return new PgDbFilter2(db, filter, side).apply();
    }
}
