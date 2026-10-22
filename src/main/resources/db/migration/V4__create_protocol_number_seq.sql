-- V4: protokol numarasi sirasi. Numara uygulamada "P-<yil>-<6 haneli sira>" biciminde uretilir.
-- Sequence, es zamanli isteklerde bile ayni degeri iki kez vermez (benzersizlik garantisi).
CREATE SEQUENCE protocol_number_seq START WITH 1 INCREMENT BY 1;
