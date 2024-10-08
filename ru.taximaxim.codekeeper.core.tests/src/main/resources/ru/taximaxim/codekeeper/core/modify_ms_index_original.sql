SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO
CREATE TABLE [dbo].[table1](
    [c1] [int] NOT NULL,
    [c2] [int] NOT NULL,
    [c3] [varchar](100) NOT NULL)
GO

CREATE UNIQUE NONCLUSTERED INDEX [index_table1] ON [dbo].[table1] ([c1] DESC, [c2] ASC)
GO

--change [c1], [c3] cols in NONCLUSTERED COLUMNSTORE index
CREATE TABLE [dbo].[table0](
    [c1] [int] NOT NULL,
    [c2] [int] NOT NULL,
    [c3] [varchar](100) NOT NULL)
GO

CREATE NONCLUSTERED COLUMNSTORE INDEX [idx_table0] ON [dbo].[table0] ([c1], [c3])
WITH (ALLOW_PAGE_LOCKS = OFF, ALLOW_ROW_LOCKS = OFF, DATA_COMPRESSION = COLUMNSTORE)
ON [PRIMARY]
GO

--change random option in CLUSTERED COLUMNSTORE index
CREATE TABLE [dbo].[table2](
    [c1] [int] NOT NULL,
    [c2] [int] NOT NULL,
    [c3] [varchar](100) NOT NULL)
GO

CREATE CLUSTERED COLUMNSTORE INDEX [idx_table2] ON [dbo].[table2]
WITH (ALLOW_PAGE_LOCKS = OFF, ALLOW_ROW_LOCKS = OFF, DATA_COMPRESSION = COLUMNSTORE)--change
ON [PRIMARY]
GO

--change COLUMNSTORE
CREATE TABLE [dbo].[table3](
    [c1] [int] NOT NULL,
    [c2] [int] NOT NULL,
    [c3] [varchar](100) NOT NULL)
GO

CREATE NONCLUSTERED COLUMNSTORE INDEX [idx_table3] ON [dbo].[table3] ([c1], [c3])
WITH (ALLOW_PAGE_LOCKS = OFF, ALLOW_ROW_LOCKS = OFF, DATA_COMPRESSION = COLUMNSTORE)--change
ON [PRIMARY]
GO

--create table with COLUMNSTORE index in table
CREATE TABLE dbo.t1
(
    c1 INT,
    INDEX ix_1 COLUMNSTORE (c1)
);

--create table with COLUMNSTORE index with ORDER columns in table(MSSQL 2022)
CREATE TABLE [dbo].[table6](
	[col1] [int] NULL,
	[col2] [int] NULL,
	[col3] [int] NULL
) ON [PRIMARY]
WITH (DATA_COMPRESSION = COLUMNSTORE)
GO

CREATE CLUSTERED COLUMNSTORE INDEX [idx_table6] ON [dbo].[table6] ORDER (col2, col1)
WITH (ALLOW_PAGE_LOCKS = OFF, ALLOW_ROW_LOCKS = OFF, DATA_COMPRESSION = COLUMNSTORE)
ON [PRIMARY]
GO

--create table with COLUMNSTORE index in table
CREATE TABLE dbo.t2
(
    [c1] [int] NOT NULL,
    [c2] [int] NOT NULL,
    [c3] [varchar](100) NOT NULL,
    INDEX ix_2 CLUSTERED COLUMNSTORE  ORDER ([c2], [c3])
);
GO

--create table with COLUMNSTORE index in table, change ([c2], [c3])
CREATE TABLE dbo.t3
(
    [c1] [int] NOT NULL,
    [c2] [int] NOT NULL,
    [c3] [varchar](100) NOT NULL,
    INDEX ix_3 NONCLUSTERED COLUMNSTORE ([c2], [c3])
);
GO

-- table with CLUSTERED COLUMNSTORE index
CREATE TABLE [dbo].[t4](
	[c1] [int] NOT NULL,
	[c2] [int] NOT NULL,
	[c3] [varchar] (100) NOT NULL
) ON [PRIMARY]
WITH (DATA_COMPRESSION = COLUMNSTORE)
GO

CREATE CLUSTERED COLUMNSTORE INDEX [ix_4] ON [dbo].[t4]
ORDER ([c2], [c1])
WITH (ALLOW_PAGE_LOCKS = OFF, ALLOW_ROW_LOCKS = OFF, DATA_COMPRESSION = COLUMNSTORE)
ON [PRIMARY]
GO
