/*******************************************************************************
 * Copyright 2017-2023 TAXTELECOM, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package ru.taximaxim.codekeeper.core.loader.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import ru.taximaxim.codekeeper.core.PgDiffUtils;
import ru.taximaxim.codekeeper.core.loader.JdbcQueries;
import ru.taximaxim.codekeeper.core.model.difftree.DbObjType;
import ru.taximaxim.codekeeper.core.parsers.antlr.statements.mssql.CreateMsAssembly;
import ru.taximaxim.codekeeper.core.schema.GenericColumn;
import ru.taximaxim.codekeeper.core.schema.MsAssembly;
import ru.taximaxim.codekeeper.core.schema.PgDatabase;

public class MsAssembliesReader {

    private final JdbcLoaderBase loader;
    private final PgDatabase db;

    public MsAssembliesReader(JdbcLoaderBase loader, PgDatabase db) {
        this.loader = loader;
        this.db = db;
    }

    public void read() throws SQLException, InterruptedException, XmlReaderException {
        loader.setCurrentOperation("assemblies query");
        String query = JdbcQueries.QUERY_MS_ASSEMBLIES.getQuery();
        try (ResultSet res = loader.runner.runScript(loader.statement, query)) {
            while (res.next()) {
                PgDiffUtils.checkCancelled(loader.monitor);
                String name = res.getString("name");
                loader.setCurrentObject(new GenericColumn(name, DbObjType.ASSEMBLY));

                MsAssembly ass = new MsAssembly(name);
                for (XmlReader bin : XmlReader.readXML(res.getString("binaries"))) {
                    ass.addBinary(CreateMsAssembly.formatBinary(bin.getString("b")));
                }

                ass.setVisible(res.getBoolean("is_visible"));

                int i = res.getInt("permission_set");
                if (i == 2) {
                    ass.setPermission("EXTERNAL_ACCESS");
                } else if (i == 3) {
                    ass.setPermission("UNSAFE");
                }

                loader.setOwner(ass, res.getString("owner"));
                loader.setPrivileges(ass, XmlReader.readXML(res.getString("acl")));

                db.addAssembly(ass);
            }
        }
    }
}
