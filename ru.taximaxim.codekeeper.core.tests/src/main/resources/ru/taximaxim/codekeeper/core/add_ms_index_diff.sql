SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO
CREATE TABLE [dbo].[t4](
	[c1] [int] NOT NULL,
	[c2] [int] NOT NULL,
	[c3] [varchar](100) NOT NULL
)
GO

CREATE CLUSTERED INDEX [index_c2] ON [dbo].[table1] ([c2])
WITH (ONLINE = ON, RESUMABLE = ON, MAX_DURATION = 240 MINUTES)
GO

CREATE CLUSTERED INDEX [idx_1] ON [dbo].[table1] ([c1])
WITH (ONLINE = ON (WAIT_AT_LOW_PRIORITY (MAX_DURATION = 5 MINUTES, ABORT_AFTER_WAIT = BLOCKERS)))
GO

CREATE CLUSTERED COLUMNSTORE INDEX [idx_table0] ON [dbo].[table0]
WITH (ALLOW_PAGE_LOCKS = OFF, ALLOW_ROW_LOCKS = OFF, DATA_COMPRESSION = COLUMNSTORE)
GO

CREATE NONCLUSTERED COLUMNSTORE INDEX [idx_table3] ON [dbo].[table3]
WITH (ALLOW_PAGE_LOCKS = OFF, ALLOW_ROW_LOCKS = OFF, DATA_COMPRESSION = COLUMNSTORE)
GO

CREATE NONCLUSTERED COLUMNSTORE INDEX [idx_table6] ON [dbo].[table6] ([col2], [col1])
ORDER ([col3], [col1])
WITH (ALLOW_PAGE_LOCKS = OFF, ALLOW_ROW_LOCKS = OFF, DATA_COMPRESSION = COLUMNSTORE)
ON [PRIMARY]
GO

CREATE CLUSTERED COLUMNSTORE INDEX [ix_4] ON [dbo].[t4]
ORDER ([c2], [c1])
GO