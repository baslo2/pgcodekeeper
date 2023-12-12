SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO
CREATE TABLE [dbo].[table1](
    [c1] [int] NOT NULL,
    [c2] [int] NOT NULL,
    [c3] [varchar](100) NOT NULL)
GO

CREATE CLUSTERED INDEX [index_c2] ON [dbo].[table1] ([c2]) WITH (ONLINE = ON, RESUMABLE = ON, MAX_DURATION = 240 MINUTES)
GO

CREATE CLUSTERED INDEX [idx_1] ON [dbo].[table1] ([c1]) WITH (ONLINE = ON (WAIT_AT_LOW_PRIORITY (MAX_DURATION = 5 MINUTES, ABORT_AFTER_WAIT = BLOCKERS)));
GO