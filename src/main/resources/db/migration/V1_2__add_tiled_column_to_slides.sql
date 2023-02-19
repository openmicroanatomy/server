alter table SLIDES add TILED BOOLEAN default FALSE;

-- Mark all existing tiles as tiled; default only for future slides.
update SLIDES set TILED = TRUE;
