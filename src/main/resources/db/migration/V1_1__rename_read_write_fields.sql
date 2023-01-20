alter table READ alter column WORKSPACE_ID rename to USER_ID;
alter table READ alter column OWNER_ID rename to WORKSPACE_ID;

alter table WRITE alter column WORKSPACE_ID rename to USER_ID;
alter table WRITE alter column OWNER_ID rename to WORKSPACE_ID;
