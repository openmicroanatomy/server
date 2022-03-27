create table OWNER
(
    DTYPE VARCHAR(31) not null,
    ID VARCHAR(255) not null
        primary key,
    NAME VARCHAR(255),
    EMAIL VARCHAR(255),
    OAUTH BOOLEAN,
    PASSWORD VARCHAR(255),
    ORGANIZATION_ID VARCHAR(255),
    constraint FK557GJLJP6GNYQ4XLK2QLQOJLK
        foreign key (ID) references OWNER (ID)
            on delete cascade
);

create table PASSWORDRESETREQUEST
(
    ID VARCHAR(255) not null
        primary key,
    EXPIRYDATE BIGINT,
    TOKEN VARCHAR(255),
    USER_ID VARCHAR(255),
    constraint FKMJ0SRR9CCITERRH40V6KDSIOY
        foreign key (USER_ID) references OWNER (ID)
);

create table SLIDES
(
    ID VARCHAR(255) not null
        primary key,
    NAME VARCHAR(255),
    OWNER_ID VARCHAR(255),
    constraint FKB3RFYHGH51SHO6IEYAG6XW179
        foreign key (OWNER_ID) references OWNER (ID)
);

create table USER_ROLES
(
    USER_ID VARCHAR(255) not null,
    ROLES INTEGER,
    constraint FKLQOWBYKGDC6D6EMKGJ0TFN0IY
        foreign key (USER_ID) references OWNER (ID)
);

create table WORKSPACES
(
    ID VARCHAR(255) not null
        primary key,
    NAME VARCHAR(255),
    OWNER_ID VARCHAR(255),
    constraint FK5SU1IFOSSA5KKB57SPRI91500
        foreign key (OWNER_ID) references OWNER (ID)
            on delete cascade
);

create table READ
(
    OWNER_ID VARCHAR(255) not null,
    WORKSPACE_ID VARCHAR(255) not null,
    primary key (OWNER_ID, WORKSPACE_ID),
    constraint FKBBMQTWHMR2QKCK3GOESR7RVOS
        foreign key (WORKSPACE_ID) references OWNER (ID),
    constraint FKTWTU20QTK0X2G3UUT3K7MPOA
        foreign key (OWNER_ID) references WORKSPACES (ID)
);

create table SUBJECTS
(
    ID VARCHAR(255) not null
        primary key,
    NAME VARCHAR(255),
    WORKSPACE_ID VARCHAR(255),
    constraint FK1OUT12IRMOMCK9XDVBOMO1UQT
        foreign key (WORKSPACE_ID) references WORKSPACES (ID)
            on delete cascade
);

create table PROJECTS
(
    ID VARCHAR(255) not null
        primary key,
    CREATEDAT BIGINT not null,
    DESCRIPTION VARCHAR(255),
    HIDDEN BOOLEAN not null,
    MODIFIEDAT BIGINT not null,
    NAME VARCHAR(255),
    SUBJECT_ID VARCHAR(255),
    constraint FKI721USV3T3L19R2NAV6CE4W82
        foreign key (SUBJECT_ID) references SUBJECTS (ID)
            on delete cascade
);

create table WRITE
(
    OWNER_ID VARCHAR(255) not null,
    WORKSPACE_ID VARCHAR(255) not null,
    primary key (OWNER_ID, WORKSPACE_ID),
    constraint FK4ND6S1H8675RR2NXOF92F60V1
        foreign key (OWNER_ID) references WORKSPACES (ID),
    constraint FKR30Q4N64SJPEMQKUH03EAY9DO
        foreign key (WORKSPACE_ID) references OWNER (ID)
);
