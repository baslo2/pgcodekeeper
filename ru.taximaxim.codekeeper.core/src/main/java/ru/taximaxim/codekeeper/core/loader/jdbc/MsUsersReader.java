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
import ru.taximaxim.codekeeper.core.schema.GenericColumn;
import ru.taximaxim.codekeeper.core.schema.MsUser;
import ru.taximaxim.codekeeper.core.schema.PgDatabase;

public class MsUsersReader {

    private final JdbcLoaderBase loader;
    private final PgDatabase db;

    public MsUsersReader(JdbcLoaderBase loader, PgDatabase db) {
        this.loader = loader;
        this.db = db;
    }

    public void read() throws SQLException, InterruptedException, XmlReaderException {
        loader.setCurrentOperation("users query");
        String query = JdbcQueries.QUERY_MS_USERS.getQuery();
        try (ResultSet res = loader.runner.runScript(loader.statement, query)) {
            while (res.next()) {
                PgDiffUtils.checkCancelled(loader.monitor);
                String name = res.getString("name");
                loader.setCurrentObject(new GenericColumn(name, DbObjType.USER));

                MsUser user = new MsUser(name);
                user.setLogin(res.getString("loginname"));

                user.setSchema(res.getString("schema_name"));
                user.setLanguage(res.getString("default_lang"));
                user.setAllowEncrypted(res.getBoolean("allow_encrypted"));

                loader.setPrivileges(user, XmlReader.readXML(res.getString("acl")));
                db.addUser(user);
            }
        }
    }
}
