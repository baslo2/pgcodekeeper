SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO
CREATE VIEW [dbo].[autoadmin_backup_configurations22]
AS
WITH XMLNAMESPACES (N'http://schemas.datacontract.org/2004/07/Microsoft.SqlServer.SmartAdmin.SmartBackupAgent' as sb) ,  rg as (select 1 as j) 
select * from rg
GO