CREATE TYPE [dbo].[type1] AS TABLE(
	[c1] [int] NOT NULL,
	[c2] [int] NOT NULL,
	INDEX [q] NONCLUSTERED HASH
(
	[c1]
) WITH ( BUCKET_COUNT = 2),
	PRIMARY KEY NONCLUSTERED  HASH ([c2]) WITH (BUCKET_COUNT = 2),
	CHECK  ((c1 > 0))
)
WITH ( MEMORY_OPTIMIZED = ON )
GO

CREATE TYPE [dbo].[type2] AS TABLE(
	[col1] [int] NOT NULL,
	UNIQUE CLUSTERED  ([col1] DESC)
)
GO
